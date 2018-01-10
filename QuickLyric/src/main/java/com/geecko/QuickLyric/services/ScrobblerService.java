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

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.geecko.QuickLyric.broadcastReceiver.MusicBroadcastReceiver;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.tasks.DownloadThread;
import com.geecko.QuickLyric.tasks.WriteToDatabaseTask;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.MediaControllerCallback;
import com.geecko.QuickLyric.utils.NotificationUtil;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.geecko.QuickLyric.utils.NotificationUtil.NOTIFICATION_ID;

@RequiresApi(21)
public class ScrobblerService extends Service implements MediaControllerCallback.MetadataUpdateListener {

    private static boolean sRunning;
    private Binder mBinder;
    private MediaControllerCallback mediaControllerCallback;
    private WeakReference<MediaSessionManager.OnActiveSessionsChangedListener> listener;

    @Override
    public IBinder onBind(Intent intent) {
        if (mBinder == null)
            mBinder = new Binder();
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sRunning = true;
        try {
            mediaControllerCallback = new MediaControllerCallback(this);
            MediaSessionManager.OnActiveSessionsChangedListener sessionsChangedListener =
                    list -> mediaControllerCallback.registerActiveSessionCallback(ScrobblerService.this, list);
            listener = new WeakReference<>(sessionsChangedListener);
            MediaSessionManager manager = ((MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE));
            ComponentName className = new ComponentName(getApplicationContext(), NotificationListenerService.class);
            manager.addOnActiveSessionsChangedListener(sessionsChangedListener, className);
            List<MediaController> controllers = manager.getActiveSessions(className);
            mediaControllerCallback.registerActiveSessionCallback(ScrobblerService.this, controllers);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                SharedPreferences current = getSharedPreferences("current_music", Context.MODE_PRIVATE);
                String artist = current.getString("artist", "");
                String track = current.getString("track", "");
                startForeground(NOTIFICATION_ID, NotificationUtil.makeNotification(this, artist, track, 0L, false, false));
            }
            if (!controllers.isEmpty())
                mediaControllerCallback.broadcastControllerState(this, controllers.get(0), null);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        Log.d("geecko", "mediaControllerCallback registered");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sRunning = false;
        this.mBinder = null;
        if (listener != null && listener.get() != null)
            ((MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE)).removeOnActiveSessionsChangedListener(listener.get());
        if (mediaControllerCallback != null) {
            mediaControllerCallback.removeControllerCallback();
            mediaControllerCallback = null;
        }
    }

    @Override
    public void onMetadataUpdated(Bundle extras) {
        // Check if lyrics are available before showing the notification

        String artist = extras.getString("artist");
        String track = extras.getString("track");
        SharedPreferences preferences = getSharedPreferences("current_music", Context.MODE_PRIVATE);
        String oldArtist = preferences.getString("artist", "");
        String oldTrack = extras.getString("track", "");

        if (!(oldArtist.equals(artist) && oldTrack.equals(track)))
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
        final Runnable broadcastRunnable = () -> {
            Intent localIntent = new Intent("com.android.music.metachanged");
            localIntent.putExtras(extras);
            new MusicBroadcastReceiver().onReceive(ScrobblerService.this, localIntent);
        };
        extras.putBoolean("notifications_allowed", false);
        boolean overlayAllowed = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("pref_overlay", true);
        if (extras.getBoolean("playing")) {
            if (DatabaseHelper.getInstance(getApplicationContext()).presenceCheck(new String[]{artist, track, artist, track})) {
                extras.putBoolean("notifications_allowed", true);
                broadcastRunnable.run();
                return;
            } else if (OnlineAccessVerifier.check(getApplicationContext()) &&
                    (overlayAllowed || !extras.getString("player", "").contains("youtube"))) {
                Object durationObject = extras.get("duration");
                Long duration = durationObject == null ? -1 : durationObject instanceof Long ? (Long) durationObject :
                        durationObject instanceof Double ? ((Double) durationObject).longValue() :
                                durationObject instanceof Float ? ((Float) durationObject).longValue() :
                                        (((Integer) durationObject).longValue() * 1000);
                new DownloadThread(new ScrobblerCallback(this, broadcastRunnable, extras),
                        extras.getString("player"), duration, null, artist, track).start();
            }
        }
        broadcastRunnable.run();
    }

    public static boolean isRunning() {
        return sRunning;
    }

    @SuppressLint("NewAPI")
    public static void startScrobbler(Context context) {
        if (isRunning())
            return;
        Intent i = new Intent(context, ScrobblerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(i);
        } else {
            context.startService(i);
        }
    }

    public static class ScrobblerCallback implements Lyrics.Callback {

        private final WeakReference<Context> contextRef;
        private final Bundle extras;
        private final Runnable broadcastRunnable;

        protected ScrobblerCallback(Context context, Runnable broadcastRunnable, Bundle extras) {
            this.extras = extras;
            this.contextRef = new WeakReference<>(context);
            this.broadcastRunnable = broadcastRunnable;
        }

        @Override
        public void onLyricsDownloaded(Lyrics lyrics) {
            if (lyrics.getFlag() == Lyrics.POSITIVE_RESULT && contextRef.get() != null) {
                if (PreferenceManager.getDefaultSharedPreferences(contextRef.get().getApplicationContext()).getBoolean("pref_auto_save", true))
                    new WriteToDatabaseTask().execute(DatabaseHelper.getInstance(contextRef.get()).getWritableDatabase(), null, lyrics);
                extras.putBoolean("notifications_allowed", true);
                broadcastRunnable.run();
            }
        }
    }
}
