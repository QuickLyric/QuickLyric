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

package com.geecko.QuickLyric.broadcastReceiver;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BadParcelableException;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;

import com.geecko.QuickLyric.App;
import com.geecko.QuickLyric.fragment.LyricsViewFragment;
import com.geecko.QuickLyric.model.Recents;
import com.geecko.QuickLyric.model.Track;
import com.geecko.QuickLyric.services.LyricsOverlayService;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.LastOpenedUtils;
import com.geecko.QuickLyric.utils.NotificationUtil;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;

import static com.geecko.QuickLyric.utils.NotificationUtil.NOTIFICATION_ID;

public class MusicBroadcastReceiver extends BroadcastReceiver {

    private static boolean autoUpdate = false;
    private static boolean spotifyPlaying = false;

    public static void forceAutoUpdate(boolean force) {
        autoUpdate = force;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean lengthFilter = sharedPref.getBoolean("pref_filter_20min", true);

        if (extras != null)
            try {
                extras.getInt("state");
            } catch (BadParcelableException e) {
                return;
            }

        if (extras == null || extras.getInt("state") > 1 //Tracks longer than 20min are presumably not songs
                || (extras.getString("artist") == null && extras.getString("track") == null)
                || lengthFilter && (
                ((extras.get("duration") instanceof Long && extras.getLong("duration") > 1200000)
                        || (extras.get("duration") instanceof Double && extras.getDouble("duration") > 1200000)
                        || (extras.get("duration") instanceof Integer && extras.getInt("duration") > 1200))
                        || ((extras.get("secs") instanceof Long && extras.getLong("secs") > 1200000)
                        || (extras.get("secs") instanceof Double && extras.getDouble("secs") > 1200000)
                        || (extras.get("secs") instanceof Integer && extras.getInt("secs") > 1200))
        )) {
            return;
        }

        String artist = extras.getString("artist");
        String track = extras.getString("track");
        String player = extras.getString("player");
        long position = extras.containsKey("position") && extras.get("position") instanceof Long ?
                extras.getLong("position") : -1;
        if (extras.get("position") instanceof Double)
            position = Double.valueOf(extras.getDouble("position")).longValue();
        boolean isPlaying = extras.getBoolean(extras.containsKey("playstate") ? "playstate" : "playing", true);
        Object durationObject = extras.get("duration");
        Long duration = durationObject == null ? -1 : durationObject instanceof Long ? (Long) durationObject :
                durationObject instanceof Double ? ((Double) durationObject).longValue() :
                        durationObject instanceof Float ? ((Float) durationObject).longValue() :
                                durationObject instanceof Integer ? (((Integer) durationObject).longValue() * 1000) :
                                        durationObject instanceof String ? (Double.valueOf((String) durationObject)).longValue() : -1;

        if (intent.getAction().equals("com.amazon.mp3.metachanged")) {
            artist = extras.getString("com.amazon.mp3.artist");
            track = extras.getString("com.amazon.mp3.track");
        } else if (intent.getAction().equals("com.spotify.music.metadatachanged"))
            isPlaying = spotifyPlaying;
        else if (intent.getAction().equals("com.spotify.music.playbackstatechanged"))
            spotifyPlaying = isPlaying;

        if (artist != null && artist.trim().startsWith("<") && artist.trim().endsWith(">") && artist.contains("unknown"))
            artist = "";

        if (!TextUtils.isEmpty(player) && player.contains("youtube") && (TextUtils.isEmpty(artist) || TextUtils.isEmpty(track) ||
                (!artist.contains("VEVO") && !track.contains("-")))) {
            return;
        }

        SharedPreferences current = context.getSharedPreferences("current_music", Context.MODE_PRIVATE);
        String currentArtist = current.getString("artist", "");
        String currentTrack = current.getString("track", "");

        SharedPreferences.Editor editor = current.edit();
        editor.putString("artist", artist);
        editor.putString("track", track);
        editor.putString("player", player);
        if (!(currentArtist.equals(artist) && currentTrack.equals(track) && position == -1))
            editor.putLong("position", position);
        editor.putBoolean("playing", isPlaying);
        editor.putLong("duration", duration);
        if (isPlaying) {
            editor.putLong("startTime", System.currentTimeMillis());
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT)
            editor.commit();
        else
            editor.apply();

        Recents.getInstance(context).add(new Track(track, artist));

        autoUpdate = autoUpdate || sharedPref.getBoolean("pref_auto_refresh", false);
        boolean prefOverlay = sharedPref.getBoolean("pref_overlay", false) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context));
        int notificationPref = prefOverlay ? 2 : Integer.valueOf(sharedPref.getString("pref_notifications", "0"));
        boolean retentionNotif = false;

        if (player != null && player.contains("youtube") && notificationPref == 1)
            notificationPref = 0;
        else if (!prefOverlay && System.currentTimeMillis() - LastOpenedUtils.getLastOpenedDate(context) > 7 * 24 * 3600 * 1000) {
            // Retention notification
            notificationPref = 1;
            retentionNotif = true;
        }

        if (!extras.getBoolean("notifications_allowed", true)) {
            notificationPref = 0;
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationPref == 0)
            notificationPref = 2;

        if (autoUpdate && App.isMainActivityVisible()) {
            Intent internalIntent = new Intent(LyricsViewFragment.UPDATE_LYRICS_ACTION);
            internalIntent.putExtra("artist", artist).putExtra("track", track);
            LyricsViewFragment.sendIntent(context, internalIntent);
            forceAutoUpdate(false);
        }


        boolean inDatabase = DatabaseHelper.getInstance(context).presenceCheck(new String[]{artist, track, artist, track});

        if (notificationPref != 0 && (isPlaying || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                && (inDatabase || OnlineAccessVerifier.check(context))) {
            Notification notif = NotificationUtil.makeNotification(context, artist, track, duration, retentionNotif, isPlaying);
            if (prefOverlay)
                LyricsOverlayService.showCustomFloatingView(context, player, notif, new String[] {artist, track}, duration);
            else
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notif);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);

        if (!prefOverlay)
            LyricsOverlayService.removeCustomFloatingView(context);
        else if (!isPlaying || (!inDatabase && !OnlineAccessVerifier.check(context)))
            LyricsOverlayService.hideFloatingView(context);
    }

    public static void disableBroadcastReceiver(Context context) {
        int flag=(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
        ComponentName component=new ComponentName(context.getApplicationContext(), MusicBroadcastReceiver.class);
        context.getPackageManager().setComponentEnabledSetting(component, flag, PackageManager.DONT_KILL_APP);
    }
}
