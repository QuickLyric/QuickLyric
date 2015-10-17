/*
 * *
 *  * This file is part of QuickLyric
 *  * Created by geecko
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

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import com.geecko.QuickLyric.lyrics.Bollywood;
import com.geecko.QuickLyric.lyrics.Genius;
import com.geecko.QuickLyric.lyrics.LyricWiki;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.lyrics.LyricsMania;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/*
 * *
 *  * This file is part of QuickLyric
 *  * Created by geecko
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

    private static final Class[] mainProviders =
            {
                    LyricWiki.class,
                    Genius.class,
                    LyricsMania.class
            };

    private static ArrayList<Class> providers = new ArrayList<>(Arrays.asList(mainProviders));

    public DownloadThread(final Lyrics.Callback callback, final String... params) {
        super(DownloadThread.getRunnable(callback, params));
    }

    public static void setProviders(List<Class> providers) {
        DownloadThread.providers = new ArrayList<>(Arrays.asList(mainProviders));
        DownloadThread.providers.addAll(0, providers);
        if (providers.contains(Bollywood.class)) {
            DownloadThread.providers.remove(Bollywood.class);
            DownloadThread.providers.add(Bollywood.class);
        }
    }

    public static void refreshProviders(Set<String> set) {
        ArrayList<Class> providers = new ArrayList<>();
        for (String name : set)
            try {
                providers.add(Class.forName("com.geecko.QuickLyric.lyrics." + name));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        DownloadThread.setProviders(providers);
    }

    public static Runnable getRunnable(final Lyrics.Callback callback, final String... params) {
        return new Runnable() {

            @SuppressWarnings("unchecked")
            public Lyrics download(String url, String artist, String title) {
                for (Class provider : providers) {
                    try {
                        if (url.contains((String) provider.getField("domain").get(null)))
                            return (Lyrics) provider.getMethod("fromURL",
                                    String.class, String.class, String.class)
                                    .invoke(null, url, artist, title);
                    } catch (Exception ignored) {
                    }
                }
                return new Lyrics(Lyrics.NO_RESULT);
            }

            @SuppressWarnings("unchecked")
            public Lyrics download(String artist, String title) {
                Lyrics result = new Lyrics(Lyrics.NO_RESULT);
                for (Class provider : providers) {
                    try {
                        Method fromMetaData = provider.getMethod("fromMetaData", String.class, String.class);
                        result = (Lyrics) fromMetaData.invoke(null, artist, title);
                    } catch (Exception ignored) {
                    }
                    if (result != null && result.getFlag() == Lyrics.POSITIVE_RESULT)
                        return result;
                }
                if (result != null && result.getFlag() != Lyrics.POSITIVE_RESULT) {
                    result.setArtist(artist);
                    result.setTitle(title);
                }
                return result;
            }



            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                Lyrics lyrics;
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
                        lyrics = download(params[0], params[1]);
                }
                if (lyrics.getFlag() != Lyrics.POSITIVE_RESULT && lyrics.getArtist() != null) {
                    artist = lyrics.getArtist();
                    title = lyrics.getTrack();
                    String[] correction = correctTags(artist, title);
                    if (!(correction[0].equals(artist) && correction[1].equals(title)) || url != null) {
                        lyrics = download(correction[0], correction[1]);
                        lyrics.setOriginalArtist(artist);
                        lyrics.setOriginalTitle(title);
                    }
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
                threadMsg(lyrics);
            }

            private void threadMsg(Lyrics lyrics) {
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
                    if (result != null)
                        callback.onLyricsDownloaded(result);
                }
            };
        };
    }

    public static String[] correctTags(String artist, String title) {
        if (artist == null || title == null)
            return new String[]{"", ""};
        String correctedArtist = artist.replaceAll("\\(.*\\)", "")
                .replaceAll(" \\- .*", "").trim();
        String correctedTrack = title.replaceAll("\\(.*\\)", "")
                .replaceAll("\\[.*\\]", "").replaceAll(" \\- .*", "").trim();
        String[] separatedArtists = correctedArtist.split(", ");
        correctedArtist = separatedArtists[separatedArtists.length - 1];
        return new String[]{correctedArtist, correctedTrack};
    }
}