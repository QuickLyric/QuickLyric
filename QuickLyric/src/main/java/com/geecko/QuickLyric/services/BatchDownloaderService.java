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

package com.geecko.QuickLyric.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.provider.LyricsChart;
import com.geecko.QuickLyric.tasks.Id3Reader;
import com.geecko.QuickLyric.tasks.WriteToDatabaseTask;
import com.geecko.QuickLyric.utils.Chromaprint;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.OkHttp3Stack;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class BatchDownloaderService extends IntentService implements Lyrics.Callback,
        Response.Listener<String>, Response.ErrorListener {
    private static final String BATCH_NOTIF_CHANNEL = "BATCH_NOTIFICATIONS_CHANNEL";
    private static final int CORE_POOL_SIZE = 2;
    private static final int MAXIMUM_POOL_SIZE = 8;
    private static final long KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    private int total;
    private CountDownLatch countDown;
    private int successCount = 0;
    private boolean foreground;
    private RequestQueue requestQueue;
    private OkHttpClient client = null;

    public BatchDownloaderService() {
        super("Batch Downloader Service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onHandleIntent(intent);
        return START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (client == null)
            getClient();
        Uri content = intent.getExtras().getParcelable("uri");
        boolean lrc = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_lrc", true);
        TreeSet<List> savedLyricsSet = new TreeSet<>((o1, o2) -> {
            int comparison = ((String) o1.get(0)).compareTo((String) o2.get(0));
            if (comparison == 0)
                comparison = ((String) o1.get(1)).compareTo((String) o2.get(1));
            return comparison;
        });
        savedLyricsSet.addAll(DatabaseHelper.getInstance(this).listMetadata());
        ArrayList<String[]> newSongsMetadata;
        int total = 0;
        if (content != null) {
            String[] projection = new String[]{"artist", "title"};
            String selection = "";
            Cursor cursor = getContentResolver().query(content, projection, selection, null, null);
            if (cursor == null)
                return;
            total = cursor.getCount();
            countDown = new CountDownLatch(cursor.getCount());
            newSongsMetadata = new ArrayList<>();
            while (cursor.moveToNext()) {
                String artist = cursor.getString(0);
                String title = cursor.getString(1);
                if (TextUtils.isEmpty(title) || TextUtils.isEmpty(artist) || savedLyricsSet.contains(Arrays.asList(artist.toLowerCase(), title.toLowerCase()))) {
                    total--;
                    countDown.countDown();
                    continue;
                }
                newSongsMetadata.add(new String[]{artist, title});
            }
            cursor.close();
        } else { // Spotify
            //noinspection unchecked
            newSongsMetadata = (ArrayList<String[]>) intent.getExtras().get("spotifyTracks");
            if (newSongsMetadata != null) {
                total = newSongsMetadata.size();
                countDown = new CountDownLatch(newSongsMetadata.size());
            }
        }
        this.total = total;
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, new LinkedBlockingQueue<>());

        if (countDown != null && countDown.getCount() > 0) {
            updateProgress();
            final Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024);
            final Network network = new BasicNetwork(new OkHttp3Stack(client));
            requestQueue = new RequestQueue(cache, network, 8);
            requestQueue.start();
            for (String[] track : newSongsMetadata) {
                String artist = track[0];
                String title = track[1];
                threadPool.execute(() -> {
                    try {
                        Request request;
                        File musicFile = Id3Reader.getFile(BatchDownloaderService.this, artist, title, false);
                        Chromaprint.Fingerprint fingerprint = null;
                        if (musicFile != null && musicFile.getAbsolutePath().substring(musicFile.getName().length() - 4).equalsIgnoreCase(".mp3") && musicFile.exists() && musicFile.canRead()) {
                            fingerprint = Chromaprint.getFingerprintForPath(BatchDownloaderService.this, musicFile.getAbsolutePath());
                        }
                        request = LyricsChart.getVolleyRequest(lrc, BatchDownloaderService.this, BatchDownloaderService.this, fingerprint, artist, title);
                        requestQueue.add(request);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    @Override
    public void onLyricsDownloaded(Lyrics lyrics) {
        updateProgress();
        if (lyrics.getFlag() == Lyrics.POSITIVE_RESULT) {
            WriteToDatabaseTask task = new WriteToDatabaseTask();
            Boolean written = task.doInBackground(DatabaseHelper.getInstance(this).getWritableDatabase(), null, lyrics);
            task.onPostExecute(written);
            if (written)
                successCount++;
        }
    }

    private void updateProgress() {
        NotificationManager manager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            manager.createNotificationChannel(new NotificationChannel(BATCH_NOTIF_CHANNEL, getString(R.string.scan_action), NotificationManager.IMPORTANCE_LOW));

        countDown.countDown();
        int count = (int) (total - countDown.getCount());
        if (count < total) {
            NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this, BATCH_NOTIF_CHANNEL);
            notifBuilder.setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(String.format(getString(R.string.dl_progress), count, total))
                    .setProgress(total, count, false)
                    .setShowWhen(false);
            Notification notif = notifBuilder.build();
            notif.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
            if (!foreground) {
                startForeground(1, notif);
                foreground = true;
            } else
                manager.notify(1, notif);
        } else {
            if (requestQueue != null)
                requestQueue.stop();
            stopForeground(true);
            Intent refreshIntent = new Intent("com.geecko.QuickLyric.updateDBList");
            PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 4, refreshIntent,
                    PendingIntent.FLAG_ONE_SHOT);
            String text = getResources()
                    .getQuantityString(R.plurals.dl_finished_desc, successCount, successCount);
            NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this, BATCH_NOTIF_CHANNEL);
            notifBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
            notifBuilder.setContentIntent(pendingIntent);
            notifBuilder.setContentTitle(getString(R.string.dl_finished));
            notifBuilder.setContentText(text);
            Notification notif = notifBuilder.build();
            notif.flags |= Notification.FLAG_AUTO_CANCEL;
            manager.notify(1, notif);
            stopSelf();
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        updateProgress();
        error.printStackTrace();
    }

    @Override
    public void onResponse(String response) {
        try {
            onLyricsDownloaded(LyricsChart.fromXml(response));
        } catch (IndexOutOfBoundsException e) {
            updateProgress();
            e.printStackTrace();
        }
    }

    private void getClient() {
        client = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();
    }
}