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
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaMetadataEditor;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteController;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.geecko.QuickLyric.App;
import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.broadcastReceiver.MusicBroadcastReceiver;
import com.geecko.QuickLyric.fragment.LyricsViewFragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.KITKAT)
public class NotificationListenerService extends android.service.notification.NotificationListenerService
        implements RemoteController.OnClientUpdateListener {

    private RemoteController mRemoteController;
    private MediaController mMediaController;
    private boolean isRemoteControllerPlaying;
    private boolean mHasBug = true;
    private MediaSessionManager.OnActiveSessionsChangedListener listener;
    private MediaController.Callback controllerCallback;
    private Bitmap lastBitmap;

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    @SuppressWarnings("NewApi")
    @TargetApi(21)
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mRemoteController = new RemoteController(this, this);
            if (!((AudioManager) getSystemService(Context.AUDIO_SERVICE)).registerRemoteController(mRemoteController)
                    && mRemoteController.clearArtworkConfiguration()) {
                throw new RuntimeException("Error while registering RemoteController!");
            }
        } else {
            listener = new MediaSessionManager.OnActiveSessionsChangedListener() {
                @Override
                public void onActiveSessionsChanged(List<MediaController> controllers) {
                    registerActiveSessionCallback(controllers);
                }
            };
            MediaSessionManager manager = ((MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE));
            ComponentName className = new ComponentName(this, getClass());

            manager.addOnActiveSessionsChangedListener(listener, className);

            registerActiveSessionCallback(manager.getActiveSessions(className));
        }
    }

    @TargetApi(21)
    private void registerActiveSessionCallback(List<MediaController> controllers) {
        if (controllers.size() > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final MediaController controller = controllers.get(0);
            mMediaController = controller;
            if (controllerCallback != null)
            {
                for (MediaController ctlr : controllers)
                {
                    ctlr.unregisterCallback(controllerCallback);
                }
            }

            controllerCallback = new MediaController.Callback() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onPlaybackStateChanged(PlaybackState state) {
                    super.onPlaybackStateChanged(state);
                    if (mMediaController != controller)
                        return; //ignore inactive sessions
                    if (state == null)
                        return;
                    if (isInvalidPackage(controller))
                        return;
                    boolean isPlaying = state.getState() == PlaybackState.STATE_PLAYING;
                    if (!isPlaying) {
                        NotificationManager notificationManager =
                                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
                        notificationManager.cancel(0);
                        notificationManager.cancel(8);
                    }
                    broadcastControllerState(controller, isPlaying);
                }
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onMetadataChanged(MediaMetadata metadata) {
                    super.onMetadataChanged(metadata);
                    if (mMediaController != controller)
                        return;
                    if (metadata == null)
                        return;
                    if (isInvalidPackage(controller))
                        return;
                    broadcastControllerState(controller, null);
                }
            };
            controller.registerCallback(controllerCallback);
            if (isInvalidPackage(controller))
                return;

            broadcastControllerState(controller, null);
        }
    }

    @TargetApi(21)
    private boolean isInvalidPackage(MediaController controller)
    {
        return ("com.google.android.youtube".equals(controller.getPackageName()));
    }

    @TargetApi(21)
    private void broadcastControllerState(MediaController controller, Boolean isPlaying) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        MediaMetadata metadata = controller.getMetadata();
        PlaybackState playbackState = controller.getPlaybackState();
        if (metadata == null)
            return;
        String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        String track = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        Bitmap artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);

        double duration = (double) metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        long position = duration == 0 || playbackState == null ? -1 : playbackState.getPosition();

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_filter_20min", true) && duration > 1200000)
            return;

        if (isPlaying == null)
            isPlaying = playbackState != null && playbackState.getState() == PlaybackState.STATE_PLAYING;

        saveArtwork(artwork, artist, track, isPlaying);

        broadcast(artist, track, isPlaying, duration, position);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).unregisterRemoteController(mRemoteController);
        else
            ((MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE))
                    .removeOnActiveSessionsChangedListener(listener);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }

    private void broadcast(String artist, String track, boolean playing, int duration, long position) {
        Intent localIntent = new Intent("com.android.music.metachanged");
        localIntent.putExtra("artist", artist);
        localIntent.putExtra("track", track);
        localIntent.putExtra("playing", playing);
        localIntent.putExtra("duration", duration);
        if (position != -1)
            localIntent.putExtra("position", position);
        new MusicBroadcastReceiver().onReceive(this, localIntent);
    }

    private void broadcast(String artist, String track, boolean playing, double duration, long position) {
        Intent localIntent = new Intent("com.android.music.metachanged");
        localIntent.putExtra("artist", artist);
        localIntent.putExtra("track", track);
        localIntent.putExtra("playing", playing);
        localIntent.putExtra("duration", duration);
        if (position != -1)
            localIntent.putExtra("position", position);
        new MusicBroadcastReceiver().onReceive(this, localIntent);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        if (MainActivity.waitingForListener) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    public static boolean isListeningAuthorized(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = context.getPackageName();

        return !(enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName));
    }

    /* KitKat stuff */
    @Override
    public void onClientChange(boolean clearing) {
        isListeningAuthorized(this);
    }

    @Override
    public void onClientPlaybackStateUpdate(int state) {
        this.isRemoteControllerPlaying = state == RemoteControlClient.PLAYSTATE_PLAYING;
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
            notificationManager.cancel(0);
            notificationManager.cancel(8);
        }
        editor.putBoolean("playing", isRemoteControllerPlaying);
        editor.apply();

        if (App.isActivityVisible() && isRemoteControllerPlaying) {
            Intent internalIntent = new Intent("Broadcast");
            internalIntent
                    .putExtra("artist", current.getString("artist", ""))
                    .putExtra("track", current.getString("track", ""));
            LyricsViewFragment.sendIntent(this, internalIntent);
        }
        Log.d("geecko", "PlaybackStateUpdate - position stored: " + currentPosMs);
    }

    @Override
    public void onClientTransportControlUpdate(int transportControlFlags) {
        if (mHasBug) {
            long position = mRemoteController.getEstimatedMediaPosition();
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
        long position = mRemoteController.getEstimatedMediaPosition();
        if (position > 3600000)
            position = -1L;

        Object durationObject = metadataEditor.getObject(MediaMetadataRetriever.METADATA_KEY_DURATION, 1200); //allow it to pass if not present
        String artist = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ARTIST,
                metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, ""));
        String track = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_TITLE, "");
        Bitmap artwork = metadataEditor.getBitmap(MediaMetadataEditor.BITMAP_KEY_ARTWORK, null);

        if (durationObject instanceof Double) {
            if (artist != null && !artist.isEmpty())
                broadcast(artist, track, isRemoteControllerPlaying, (Double) durationObject, position);
        } else if (durationObject instanceof Integer) {
            if (artist != null && !artist.isEmpty())
                broadcast(artist, track, isRemoteControllerPlaying, (Integer) durationObject, position);
        } else if (durationObject instanceof Long)
            if (artist != null && !artist.isEmpty())
                broadcast(artist, track, isRemoteControllerPlaying, (Long) durationObject, position);

        saveArtwork(artwork, artist, track, isRemoteControllerPlaying);
        Log.d("geecko", "MetadataUpdate - position stored: " + position);
    }

    private void saveArtwork(Bitmap artwork, String artist, String track, boolean isPlaying) {
        File artworksDir = new File(getCacheDir(), "artworks");
        if (artwork != null && (artworksDir.exists() || artworksDir.mkdir())) {
            File artworkFile = new File(artworksDir, artist + track + ".png");
            if (!artworkFile.exists())
                try {
                    //noinspection ResultOfMethodCallIgnored
                    artworkFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            if (isPlaying && !artwork.sameAs(lastBitmap)) { //prevent many writes of the same Bitmap
                FileOutputStream fos = null;
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                artwork.compress(Bitmap.CompressFormat.PNG, 100, stream);
                try {
                    fos = new FileOutputStream(artworkFile);
                    stream.writeTo(fos);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (fos != null)
                            fos.close();
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (lastBitmap != null)
            {
                lastBitmap.recycle();
            }
            lastBitmap = artwork;
        }
    }
}
