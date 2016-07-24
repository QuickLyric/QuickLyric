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

package com.geecko.QuickLyric.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.tasks.DownloadThread;
import com.geecko.QuickLyric.tasks.WriteToDatabaseTask;
import com.geecko.QuickLyric.utils.DatabaseHelper;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BatchDownloaderService extends IntentService implements Lyrics.Callback {
    // Sets the amount of time an idle thread will wait for a task before terminating
    private static final int KEEP_ALIVE_TIME = 1;
    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    // Sets the threadpool size to 8
    private static final int CORE_POOL_SIZE = 2;
    private static final int MAXIMUM_POOL_SIZE = 8;
    private final ThreadPoolExecutor mDownloadThreadPool;
    private SQLiteDatabase database;
    private int total = 0;
    private int count = 0;
    private int successCount = 0;

    public BatchDownloaderService() {
        super("Batch Downloader Service");
        mDownloadThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, new LinkedBlockingQueue<Runnable>());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onHandleIntent(intent);
        return START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        this.database = new DatabaseHelper(getApplicationContext()).getWritableDatabase();
        Uri content = intent.getExtras().getParcelable("uri");
        Set<String> providersSet = PreferenceManager.getDefaultSharedPreferences(this)
                .getStringSet("pref_providers", new TreeSet<String>());
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_lrc", false))
            providersSet.add("ViewLyrics");
        DownloadThread.setProviders(providersSet);
        if (content != null) {
            String[] projection = new String[]{"artist", "title"};
            String selection = "artist IS NOT NULL AND artist <> '<unknown>'";
            Cursor cursor = getContentResolver().query(content, projection, selection, null, null);
            if (cursor == null)
                return;
            total = cursor.getCount();
            updateProgress();
            while (cursor.moveToNext()) {
                String artist = cursor.getString(0);
                String title = cursor.getString(1);
                mDownloadThreadPool.execute(DownloadThread.getRunnable(this, true, artist, title));
            }
            cursor.close();
        } else {
            @SuppressWarnings("unchecked")
            ArrayList<String[]> savedTracks = (ArrayList<String[]>) intent.getExtras().get("spotifyTracks");
            if (savedTracks != null) {
                total = savedTracks.size();
                updateProgress();
                for (String[] track : savedTracks) {
                    if (track != null)
                        mDownloadThreadPool.execute(DownloadThread.getRunnable(this, true, track[0], track[1]));
                }
            }
        }
    }

    @Override
    public void onLyricsDownloaded(Lyrics lyrics) {
        count++;
        updateProgress();
        if (lyrics.getFlag() == Lyrics.POSITIVE_RESULT && this.database != null) {
            WriteToDatabaseTask task = new WriteToDatabaseTask();
            task.onPostExecute(task.doInBackground(this.database, null, lyrics));
            successCount++;
        }
    }

    private void updateProgress() {
        NotificationManager manager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
        if (count < total) {
            NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this);
            notifBuilder.setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(String.format(getString(R.string.dl_progress), count, total))
                    .setProgress(total, count, false)
                    .setShowWhen(false);
            Notification notif = notifBuilder.build();
            notif.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
            if (count == 0)
                startForeground(1, notif);
            else
                manager.notify(1, notif);
        } else {
            stopForeground(true);
            Intent refreshIntent = new Intent("com.geecko.QuickLyric.updateDBList");
            PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 4, refreshIntent,
                    PendingIntent.FLAG_ONE_SHOT);
            String text = getResources()
                    .getQuantityString(R.plurals.dl_finished_desc, successCount, successCount);
            NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this);
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
}