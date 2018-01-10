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

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataEditor;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteController;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.geecko.QuickLyric.App;
import com.geecko.QuickLyric.broadcastReceiver.MusicBroadcastReceiver;
import com.geecko.QuickLyric.fragment.LyricsViewFragment;
import com.geecko.QuickLyric.utils.MediaControllerCallback;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;

import static com.geecko.QuickLyric.utils.NotificationUtil.NOTIFICATION_ID;

@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.KITKAT)
public class NotificationListenerService extends android.service.notification.NotificationListenerService implements RemoteController.OnClientUpdateListener, MediaControllerCallback.MetadataUpdateListener {

    public static final String EXTRA_FOREGROUND_APPS = "android.foregroundApps";

    private static int scrobblingProcessPID = -1;
    private static WeakReference<RemoteController> mRemoteController = new WeakReference<>(null);
    private IBinder mBinder;
    private MediaControllerCallback mediaControllerCallback;
    private String track;
    private String artist;
    private Object durationObject;

    @Override
    public IBinder onBind(Intent intent) {
        if (mBinder == null)
            mBinder = super.onBind(intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            onListenerConnected();
        return mBinder;
    }

    @Override
    @SuppressWarnings("NewApi")
    public void onCreate() {
        super.onCreate();
        if (!isListeningAuthorized(this))
            return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mRemoteController = new WeakReference<>(new RemoteController(this, this));
            mRemoteController.get().setArtworkConfiguration(3000, 3000);
            if (!((AudioManager) getSystemService(Context.AUDIO_SERVICE)).registerRemoteController(mRemoteController.get())) {
                throw new RuntimeException("Error while registering RemoteController!");
            }
            mediaControllerCallback = new MediaControllerCallback(this);
        } else {
            startScrobblingService();
            // disableNotificationListenerService();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mBinder = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (mRemoteController != null && mRemoteController.get() != null)
                ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).unregisterRemoteController(mRemoteController.get());
        }
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Intent intent = new Intent("com.geecko.QuickLyric.NLS_CONNECTED");
        sendBroadcast(intent);
        MusicBroadcastReceiver.disableBroadcastReceiver(this);
    }

    @Override
    @TargetApi(24)
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        requestRebind(new ComponentName(getApplicationContext(), NotificationListenerService.class));
    }

    private void startScrobblingService() {
        Log.d("geecko", "NLS starting Scrobbler");
        Intent intent = new Intent(getApplicationContext(), ScrobblerService.class);
        startService(intent);
    }

    public static boolean isListeningAuthorized(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = context.getPackageName();

        return !(enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName));
    }

    public static boolean isNotificationListenerServiceEnabled(Context context) {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(context);
        return packageNames.contains(context.getPackageName());
    }

    public static boolean isAppScrobbling(Context context) {
        if (context == null)
            return false;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> pids = manager.getRunningAppProcesses();
        if (scrobblingProcessPID == -1) {
            String needle = Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT ? "com.geecko.QuickLyric:NLS" : "com.geecko.QuickLyric";
            for (int i = 0; i < pids.size(); i++) {
                ActivityManager.RunningAppProcessInfo info = pids.get(i);
                if (info.processName.equalsIgnoreCase(needle)) {
                    scrobblingProcessPID = info.pid;
                    break;
                }
            }
        }

        ComponentName serviceComponent = new ComponentName(context, Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT ?
                NotificationListenerService.class : ScrobblerService.class);
        boolean serviceRunning = false;
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        if (runningServices != null ) {
            for (ActivityManager.RunningServiceInfo service : runningServices) {
                if ((service.service.equals(serviceComponent) && service.pid == scrobblingProcessPID)) {
                    serviceRunning = true;
                    break;
                }
            }
        }
        if (!serviceRunning) {
            StackTraceElement traceElement = Thread.currentThread().getStackTrace()[3];
            Bundle bundle = new Bundle();
            bundle.putString("file", traceElement.getFileName());
            bundle.putString("method", traceElement.getMethodName());
        }
        return serviceRunning;
    }

    private void disableNotificationListenerService() {
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(getApplicationContext(), NotificationListenerService.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    /* KitKat stuff */

    private boolean isRemoteControllerPlaying;
    private boolean mHasBug = true;

    @Override
    public void onClientChange(boolean clearing) {
        // isListeningAuthorized(NotificationListenerService.this);
    }

    @Override
    public void onClientPlaybackStateUpdate(int state) {
        this.isRemoteControllerPlaying = state == RemoteControlClient.PLAYSTATE_PLAYING;
    }


    public static long getRemotePlayerPosition() {
        return mRemoteController.get() != null ?
                Math.min(3600000,mRemoteController.get().getEstimatedMediaPosition()) : -1L;
    }

        @Override
    public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed) {
        this.isRemoteControllerPlaying = state == RemoteControlClient.PLAYSTATE_PLAYING;
        mHasBug = false;
        if (currentPosMs > 3600000)
            currentPosMs = -1L;
        SharedPreferences current = getSharedPreferences("current_music", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = current.edit();
        editor.putLong("position", currentPosMs);

        if (isRemoteControllerPlaying) {
            long currentTime = System.currentTimeMillis();
            editor.putLong("startTime", currentTime);
        } else {
            NotificationManager notificationManager =
                    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
            notificationManager.cancel(NOTIFICATION_ID);
            notificationManager.cancel(8);
        }
        editor.putBoolean("playing", isRemoteControllerPlaying);
        editor.apply();

        if (App.isMainActivityVisible() && isRemoteControllerPlaying) {
            Intent internalIntent = new Intent(LyricsViewFragment.UPDATE_LYRICS_ACTION);
            internalIntent
                    .putExtra("artist", current.getString("artist", ""))
                    .putExtra("track", current.getString("track", ""));
            LyricsViewFragment.sendIntent(NotificationListenerService.this, internalIntent);
        }
        Log.d("geecko", "PlaybackStateUpdate - position stored: " + currentPosMs);

        long position = getRemotePlayerPosition();

        if (durationObject instanceof Double) {
            if (artist != null && !artist.isEmpty())
                mediaControllerCallback.broadcast(this, artist, track, isRemoteControllerPlaying, (Double) durationObject, position, null);
        } else if (durationObject instanceof Integer) {
            if (artist != null && !artist.isEmpty())
                mediaControllerCallback.broadcast(this, artist, track, isRemoteControllerPlaying, (Integer) durationObject, position, null);
        } else if (durationObject instanceof Long)
            if (artist != null && !artist.isEmpty())
                mediaControllerCallback.broadcast(this, artist, track, isRemoteControllerPlaying, (Long) durationObject, position, null);
    }

    @Override
    public void onClientTransportControlUpdate(int transportControlFlags) {
        if (mHasBug && mRemoteController.get() != null) {
            long position = mRemoteController.get().getEstimatedMediaPosition();
            if (position > 3600000)
                position = -1L;
            SharedPreferences current = getSharedPreferences("current_music", Context.MODE_PRIVATE);
            current.edit().putLong("position", position).apply();
            if (isRemoteControllerPlaying) {
                long currentTime = System.currentTimeMillis();
                current.edit().putLong("startTime", currentTime).apply();
            }
            Log.d("geecko", "TransportControlUpdate - position stored: " + position);
        }
    }

    @Override
    public void onClientMetadataUpdate(RemoteController.MetadataEditor metadataEditor) {
        // isRemoteControllerPlaying = true;

        durationObject = metadataEditor.getObject(MediaMetadataRetriever.METADATA_KEY_DURATION, 1200); //allow it to pass if not present
        artist = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ARTIST,
                metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, ""));
        track = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_TITLE, "");
        Bitmap artwork = metadataEditor.getBitmap(MediaMetadataEditor.BITMAP_KEY_ARTWORK, null);

        mediaControllerCallback.saveArtwork(this, artwork, artist, track);
    }

    @Override
    public void onMetadataUpdated(Bundle metadata) {
    }

    /* NLS Stuff */

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!isAppScrobbling(this))
                    startScrobblingService();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) // Hide that "
                snoozeSystemNotification(sbn);
        }
    }

    @RequiresApi(26)
    private void snoozeSystemNotification(StatusBarNotification sbn) {
        if (sbn.getPackageName().equals("android") && sbn.getNotification().extras.containsKey(EXTRA_FOREGROUND_APPS)) {
            String key = sbn.getNotification().extras.getString(Notification.EXTRA_TITLE);
            if (key == null)
                return;

            String[] svcs = sbn.getNotification().extras.getStringArray(EXTRA_FOREGROUND_APPS);
            String packageName =  getApplicationContext().getPackageName();
            boolean snooze = false;
            for (String svc : svcs) {
                if (packageName.equals(svc)) {
                    snooze = true;
                    break;
                }
            }

            if (snooze)
                snoozeNotification(sbn.getKey(), 10000000000000L);
            //Long.MAX_VALUE = 9223372036854775807 = 292.5 million years -> not working
            //10000000000000 = 317.09792 years -> working
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }
}
