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

package com.geecko.QuickLyric.utils;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.util.ArraySet;
import android.util.Log;

import com.geecko.QuickLyric.broadcastReceiver.MusicBroadcastReceiver;
import com.geecko.QuickLyric.services.NotificationListenerService;
import com.geecko.QuickLyric.services.ScrobblerService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MediaControllerCallback {

    private static MediaSessionManager.OnActiveSessionsChangedListener sessionListener;
    private final MetadataUpdateListener metadataListener;
    private MediaController controller;
    private static WeakReference<MediaController> sController = new WeakReference<>(null);
    private MediaController.Callback controllerCallback;
    private Handler handler = new Handler();
    private Bitmap lastBitmap;

    public MediaControllerCallback(MetadataUpdateListener metadataListener) {
        this.metadataListener = metadataListener;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void registerFallbackControllerCallback(Context context, MediaControllerCallback controllerCallback) {
        MediaSessionManager mediaSessionManager = ((MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE));
        ComponentName className = new ComponentName(context.getApplicationContext(), NotificationListenerService.class);
        if (sessionListener != null)
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener);
        sessionListener = list -> controllerCallback.registerActiveSessionCallback(context, list);
        mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, className);
        controllerCallback.registerActiveSessionCallback(context, mediaSessionManager.getActiveSessions(className));
    }

    public void registerActiveSessionCallback(Context context, List<MediaController> controllers) {
        if (controllers.size() > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            controller = controllers.get(0);
            sController = new WeakReference<>(controller);
            if (controllerCallback != null) {
                for (MediaController ctlr : controllers)
                    ctlr.unregisterCallback(controllerCallback);
            } else {
                controllerCallback = new MediaController.Callback() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onPlaybackStateChanged(PlaybackState state) {
                        super.onPlaybackStateChanged(state);
                        if (state == null)
                            return;
                        if (isInvalidPackage(controller))
                            return;
                        boolean isPlaying = state.getState() == PlaybackState.STATE_PLAYING;
                        if (!isPlaying) {
                            NotificationManager notificationManager =
                                    ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
                            notificationManager.cancel(0);
                            notificationManager.cancel(8);
                        }
                        savePlayerName(controller.getPackageName(), context);
                        if (controller != controller)
                            return; //ignore inactive sessions
                        broadcastControllerState(context, controller, isPlaying);
                    }

                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onMetadataChanged(MediaMetadata metadata) {
                        super.onMetadataChanged(metadata);
                        if (controller != controller)
                            return;
                        if (metadata == null)
                            return;
                        if (isInvalidPackage(controller))
                            return;
                        savePlayerName(controller.getPackageName(), context);
                        broadcastControllerState(context, controller, null);
                    }
                };
            }
            controller.registerCallback(controllerCallback);
            if (isInvalidPackage(controller))
                return;
            broadcastControllerState(context, controller, null);
        }
    }

    public static long getActiveControllerPosition(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && sController.get() != null) {
            PlaybackState state = sController.get().getPlaybackState();
            if (state != null)
                return state.getPosition();
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            long kitkatPosition = NotificationListenerService.getRemotePlayerPosition();
            if (kitkatPosition >= 0)
                return kitkatPosition;
        }
        SharedPreferences preferences = context.getSharedPreferences("current_music", Context.MODE_PRIVATE);
        long startTime = preferences.getLong("startTime", System.currentTimeMillis());
        long distance = System.currentTimeMillis() - startTime;
        long position = preferences.getLong("position", -1L);
        if (preferences.getBoolean("playing", true) && position >= 0L)
            position += distance;
        return position;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void removeControllerCallback() {
        if (controllerCallback != null && controller != null) {
            controller.unregisterCallback(controllerCallback);
        }
        controllerCallback = null;
    }

    @TargetApi(21)
    public void broadcastControllerState(Context context, MediaController controller, Boolean isPlaying) {
        final MediaController[] controllers = new MediaController[]{controller};
        final Boolean[] playing = new Boolean[]{isPlaying};
        handler.postDelayed(() -> {
            MediaMetadata metadata = controllers[0].getMetadata();
            PlaybackState playbackState = controllers[0].getPlaybackState();
            if (metadata == null)
                return;
            String artist = null;
            try {
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                if (artist == null)
                    artist = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
            } catch (Exception ignored) {
            }
            String track = null;
            try {
                track = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            } catch (Exception ignored) {
            }
            Bitmap artwork = null;
            try {
                artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if (artwork == null)
                    artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
            } catch (Exception ignored) {
            }

            double duration;
            try {
                duration =(double) metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            } catch (RuntimeException ignored) {
                duration = 0;
            }
            long position = duration == 0 || playbackState == null ? -1 : playbackState.getPosition();

            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_filter_20min", true) && duration > 1200000)
                return;
            if (playing[0] == null)
                playing[0] = playbackState != null && playbackState.getState() == PlaybackState.STATE_PLAYING;

            saveArtwork(context, artwork, artist, track);

            String player = controllers[0].getPackageName();

            if ("com.aimp.player".equals(player)) // Aimp is awful
                position = -1;
            broadcast(context, artist, track, playing[0], duration, position, player);
        }, 100);
    }

    public void broadcast(Context context, String artist, String track, boolean playing, int duration, long position, String player) {
        Intent localIntent = new Intent("com.android.music.metachanged");
        localIntent.putExtra("artist", artist);
        localIntent.putExtra("track", track);
        localIntent.putExtra("playing", playing);
        localIntent.putExtra("duration", duration);
        localIntent.putExtra("player", player);
        Log.d("title", track);
        if (position != -1)
            localIntent.putExtra("position", position);

        String notifPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).getString("pref_notifications", "0");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            new MusicBroadcastReceiver().onReceive(context, localIntent);
        } else {
            if (!(context instanceof ScrobblerService) || notifPref.equals("0"))
                new MusicBroadcastReceiver().onReceive(context, localIntent);
            else if (metadataListener != null && playing)
                metadataListener.onMetadataUpdated(localIntent.getExtras());
        }
    }

    public void broadcast(Context context, String artist, String track, boolean playing, double duration, long position, String player) {
        Intent localIntent = new Intent("com.android.music.metachanged");
        localIntent.putExtra("artist", artist);
        localIntent.putExtra("track", track);
        localIntent.putExtra("playing", playing);
        localIntent.putExtra("duration", duration);
        localIntent.putExtra("player", player);
        if (position != -1)
            localIntent.putExtra("position", position);

        String notifPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).getString("pref_notifications", "0");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            new MusicBroadcastReceiver().onReceive(context, localIntent);
        } else {
            if (!(context instanceof ScrobblerService) || notifPref.equals("0"))
                new MusicBroadcastReceiver().onReceive(context, localIntent);
            else if (metadataListener != null)
                metadataListener.onMetadataUpdated(localIntent.getExtras());
        }
    }

    public void saveArtwork(Context context, Bitmap artwork, String artist, String track) {
        File artworksDir = new File(context.getCacheDir(), "artworks");
        if (artwork != null && (artworksDir.exists() || artworksDir.mkdir())) {
            File artworkFile = new File(artworksDir, artist + track + ".png");
            if (!artworkFile.exists())
                try {
                    //noinspection ResultOfMethodCallIgnored
                    artworkFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            if (artworkFile.length() == 0 || !artwork.sameAs(lastBitmap)) { //prevent many writes of the same Bitmap
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
                        {
                            fos.flush();
                            fos.getFD().sync();
                            fos.close();
                        }
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (lastBitmap != null) {
                lastBitmap.recycle();
            }
            lastBitmap = artwork;
        }
    }

    /* Lollipop Stuff */
    private static void savePlayerName(String packageName, Context context) {
        if (packageName != null) {
            Set<String> usedPlayers = getUsedPlayersList(context);
            usedPlayers.add(packageName);
            context.getSharedPreferences("NotificationListenerService", Context.MODE_PRIVATE)
                    .edit().putStringSet("players_used", usedPlayers).apply();
        }
    }

    public static Set<String> getUsedPlayersList(Context context) {
        return context.getSharedPreferences("NotificationListenerService", Context.MODE_PRIVATE)
                    .getStringSet("players_used", new ArraySet<>());
    }

    @TargetApi(21)
    private boolean isInvalidPackage(MediaController controller) {
        String playerPackageName = controller.getPackageName();
        return playerPackageName != null && (playerPackageName.contains(".chrome") || playerPackageName.contains("firefox") ||
                playerBlacklist.contains(playerPackageName));
    }

    private static final List<String> playerBlacklist = Arrays.asList(
            "au.com.shiftyjelly.pocketcasts", "com.bambuna.podcastaddict", "tunein.player, sanity.freeaudiobooks",
            "com.audible.application", "sanity.podcast.freak", "com.samsung.android.video", "tv.twitch.android.app",
            "tv.molotov.app", "com.netflix.mediaclient", "com.android.server.telecom", "tunein.player", "radiotime.player");

    public interface MetadataUpdateListener {
        void onMetadataUpdated(Bundle metadata);
    }
}
