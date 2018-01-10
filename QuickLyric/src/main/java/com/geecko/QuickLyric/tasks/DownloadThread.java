/*
 * *
 *  * This file is part of QuickLyric
 *  * Copyright © 2017 QuickLyric SPRL
 *  *
 *  * QuickLyric is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * QuickLyric is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  * You should have received a copy of the GNU General Public License
 *  * along with QuickLyric.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.geecko.QuickLyric.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import com.geecko.QuickLyric.BuildConfig;
import com.geecko.QuickLyric.fragment.LyricsViewFragment;
import com.geecko.QuickLyric.provider.LyricsChart;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.services.LyricsOverlayService;
import com.geecko.QuickLyric.services.ScrobblerService;
import com.geecko.QuickLyric.utils.Chromaprint;
import com.geecko.QuickLyric.utils.FingerprintDatabaseHelper;
import com.geecko.QuickLyric.utils.YoutubeCategoryChecker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/*
 * *
 *  * This file is part of QuickLyric
 *  * Copyright © 2017 QuickLyric SPRL
 *  *
 *  * QuickLyric is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * QuickLyric is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  * You should have received a copy of the GNU General Public License
 *  * along with QuickLyric.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

public class DownloadThread extends Thread {

    private static final ConcurrentHashMap<String, Boolean> blockMap = new ConcurrentHashMap<>();

    public static boolean LRC = false;
    private static String storedResult;

    private boolean concurrentAllowed;

    public DownloadThread(final Lyrics.Callback callback, final String player, long duration, @Nullable File musicFile, final String... params) {
        this(new WeakReference<>(callback), player, duration, musicFile, params);
    }

    public DownloadThread(final WeakReference<Lyrics.Callback> callback, final String player, long duration, @Nullable File musicFile, final String... params) {
        super(DownloadThread.getRunnable(callback, player, duration, getCallerClassname(), musicFile, params));
    }

    private static Runnable getRunnable(final Reference<Lyrics.Callback> callback, String player, long duration, String callerClass, @Nullable File musicFile, final String... params) {
        final Lyrics.Callback otherCallback = (callback.get() instanceof LyricsOverlayService.OverlayServiceCallback || callback.get() instanceof ScrobblerService.ScrobblerCallback) ?
                callback.get() : null;
        return new Runnable() {

            Chromaprint.Fingerprint fingerprint = null;

            @SuppressWarnings("unchecked")
            public Lyrics download(String url, String artist, String title) {
                return LyricsChart.fromURL(url, artist, title, LRC);
            }

            @SuppressWarnings("unchecked")
            public Lyrics download(String artist, String title, File musicFile, boolean allowLrc) {
                Context context = null;
                if (callback != null) {
                    context = getContextFromCallback(callback);
                    if (context != null && musicFile != null)
                        fingerprint = Chromaprint.getFingerprintForPath(context, musicFile.getAbsolutePath());
                }
                Lyrics lyrics = LyricsChart.fromMetaData(artist, title, LRC && allowLrc, fingerprint, player);

                if (context != null && fingerprint != null && lyrics != null && lyrics.getFlag() == Lyrics.POSITIVE_RESULT) {
                    FingerprintDatabaseHelper.getInstance(context.getApplicationContext())
                            .insertFingerprint(musicFile.getAbsolutePath(), fingerprint.getFingerprint(),
                                    lyrics.getArtist(), lyrics.getTitle(), fingerprint.getDuration(), artist, title);
                }
                return lyrics;
            }

            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                Lyrics lyrics = new Lyrics(Lyrics.ERROR);
                String artist = null;
                String title = null;
                String url = null;
                switch (params.length) {
                    case 3: // URL + tags
                        artist = params[1];
                        title = params[2];
                    case 1: // URL
                        url = params[0];
                        lyrics = download(url, artist, title);
                        break;
                    default: // just tags
                        artist = params[0];
                        title = params[1];
                        if (musicFile == null && (TextUtils.isEmpty(artist) || TextUtils.isEmpty(title)))
                            break;

                        if (player != null && player.contains("youtube")) { // YouTube is special
                            if (YoutubeCategoryChecker.isMusicVideo(artist, title)) {
                                String[] correction = correctTags(artist, title);
                                lyrics = download(artist, title, musicFile, isCorrectLength(correction[0], correction[1], duration));
                                if (lyrics.getFlag() != Lyrics.POSITIVE_RESULT && !TextUtils.isEmpty(title)) {
                                    ArrayList<Lyrics> results = LyricsChart.search(title);
                                    lyrics = results.isEmpty() ? lyrics : download(results.get(0).getURL(), results.get(0).getArtist(), results.get(0).getTitle());
                                }
                                lyrics.setOriginalArtist(artist);
                                lyrics.setOriginalTitle(title);
                            } else
                                lyrics = new Lyrics(Lyrics.NEGATIVE_RESULT);
                        } else {
                            lyrics = download(params[0], params[1], musicFile, true);
                        }
                }

                if (lyrics.getFlag() != Lyrics.POSITIVE_RESULT && (player == null || !player.contains("youtube"))) {
                    String[] correction = correctTags(artist, title);
                    if (TextUtils.isEmpty(correction[0]) && !TextUtils.isEmpty(correction[1]))
                        correction[0] = guessArtist(correction[1]);
                    if (!correction[0].equals(artist) || !correction[1].equals(title) || url != null) {
                        if (!TextUtils.isEmpty(correction[0]) && !TextUtils.isEmpty(correction[1])) {
                            lyrics = download(correction[0], correction[1], musicFile, url == null);
                        }
                    }
                    lyrics.setOriginalArtist(artist);
                    lyrics.setOriginalTitle(title);
                }
                if (lyrics.getArtist() == null) {
                    if (artist != null) {
                        lyrics.setArtist(artist);
                        lyrics.setTitle(title);
                    } else {
                        lyrics.setArtist("");
                        lyrics.setTitle("");
                    }
                }

                if (lyrics != null) {
                    Context context = getContextFromCallback(callback);
                    storedResult = String.format(Locale.getDefault(), "%d_%d_%s_%s_%b_%b_%s_%s_%s",
                            lyrics.getFlag(), lyrics.getErrorCode(), artist, title, musicFile != null && musicFile.exists(), fingerprint != null, lyrics.getArtist(), lyrics.getTitle(), lyrics.getSource());
                    if (context != null) {
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                        sharedPref.edit().putString("download_result", storedResult).apply();
                    }
                }
                threadMsg(lyrics);
            }

            private void threadMsg(Lyrics lyrics) {
                blockMap.remove(callerClass);
                if (lyrics != null) {
                    Message msgObj = handler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putSerializable("lyrics", lyrics);
                    msgObj.setData(b);
                    handler.sendMessage(msgObj);
                }
            }

            // Define the Handler that receives messages from the thread and update the progress
            private final Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    Lyrics result = (Lyrics) msg.getData().getSerializable("lyrics");
                    if (result != null) {
                        if (callback.get() != null) {
                            callback.get().onLyricsDownloaded(result);
                        } else if (otherCallback != null) {
                            otherCallback.onLyricsDownloaded(result);
                        }
                    }
                }
            };
        };
    }

    private static Context getContextFromCallback(Reference<Lyrics.Callback> callback) {
        if (callback == null || callback.get() == null)
            return null;
        Context context = null;
        if (callback.get() instanceof LyricsViewFragment)
            context = ((LyricsViewFragment) callback.get()).getActivity();
        else if (callback.get() instanceof View)
            context = ((View) callback.get()).getContext();
        else if (callback.get() instanceof LyricsOverlayService.OverlayServiceCallback) {
            context = ((LyricsOverlayService.OverlayServiceCallback) callback.get()).getContext();
        }
        return context;
    }

    private static String guessArtist(String title) {
        try {
            title = URLEncoder.encode("\"" + title + "\"", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url(String.format("https://musicbrainz.org/ws/2/recording/?query=release:%s&fmt=json", title))
                .header("User-Agent", BuildConfig.APPLICATION_ID)
                .get()
                .build();
        try {
            Response response = client.newCall(request).execute();
            JSONObject jsonResponse = new JSONObject(response.body().string());
            JSONObject recording = jsonResponse.getJSONArray("recordings").getJSONObject(0);
            JSONObject artist = recording.getJSONArray("artist-credit").getJSONObject(0).getJSONObject("artist");
            return artist.getString("name");
        } catch (Exception ignored) {
        }

        return "";
    }

    private static boolean isCorrectLength(String artist, String title, long duration) {
        try {
            title = URLEncoder.encode("\"" + title + "\"", "UTF-8");
            artist = URLEncoder.encode("\"" + artist + "\"", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return false;
        }
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url(String.format("https://musicbrainz.org/ws/2/recording/?query=release:%s+artist:%s&fmt=json", title, artist))
                .header("User-Agent", BuildConfig.APPLICATION_ID)
                .get()
                .build();
        try {
            Response response = client.newCall(request).execute();
            JSONObject jsonResponse = new JSONObject(response.body().string());
            JSONArray recordings = jsonResponse.getJSONArray("recordings");
            JSONObject recording = null;
            for (int i = 0; i < recordings.length() && recording == null; i++) {
                if (recordings.getJSONObject(i).has("length"))
                    recording = recordings.getJSONObject(i);
            }
            return Math.abs(recording.getLong("length") - duration) < 8000;
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

        return false;
    }

    public static String[] correctTags(String artist, String title) {
        if (artist == null)
            artist = "";
        if (title == null)
            title = "";

        title = title.replaceAll("\\[.*\\]", "").replaceAll("_", " ").trim();
        artist = artist.replaceAll("\\[.*\\]", "").replaceAll("_", " ").trim();

        // Split if artist name is in title
        if (title.contains(" - ")) {
            String[] tags = title.split(" - ");
            artist = tags[0].trim();
            title = tags[1].trim();
        } else {
            String correctedArtist = artist.replaceAll("\\(.*\\)", "")
                    .replaceAll(" \\- .*", "").trim();
            title = title.replaceAll("\\(.*\\)", "").replaceAll(" \\- .*", "").trim();
            String[] separatedArtists = correctedArtist.split(", ");
            artist = separatedArtists[separatedArtists.length - 1];
        }

        // Remove numbers and dashes at start of title
        title = title.trim();
        while (title.length() > 2 && Character.isDigit(title.charAt(0))) {
            title = title.substring(1).trim();
            if (title.startsWith("-"))
                title = title.substring(1).trim();
        }
        // Remove "feat."
        Matcher matcher = Pattern.compile("(?i)(\\sf(ea)?t\\.?\\s)").matcher(title);
        if (matcher.find())
            title = title.substring(0, matcher.start());
        matcher = Pattern.compile("(?i)(\\sf(ea)?t\\.?\\s)").matcher(artist);
        if (matcher.find())
            artist = artist.substring(0, matcher.start());
        return new String[]{artist, title};
    }

    public static String getStoredResult(Context context) {
        return storedResult == null ? PreferenceManager.getDefaultSharedPreferences(context).getString("download_result", "") : storedResult;
    }

    public DownloadThread allowConcurrentExecutions(boolean allow) {
        this.concurrentAllowed = allow;
        return this;
    }

    private static String getCallerClassname() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean mark = false;
        for (StackTraceElement aStackTrace : stackTrace) {
            if (!mark && aStackTrace.getClassName().equals(DownloadThread.class.getName()))
                mark = true;
            else if (mark && !aStackTrace.getClassName().equals(DownloadThread.class.getName()))
                return aStackTrace.getClassName();
        }
        return null;
    }

    @Override
    public synchronized void start() {
        if (!concurrentAllowed) {
            String callerClass = getCallerClassname();
            if (blockMap.containsKey(callerClass)) // Max. 1 execution per class
                return;
            blockMap.put(callerClass, true);
        }
        super.start();
    }
}