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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BadParcelableException;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.util.TypedValue;

import com.geecko.QuickLyric.App;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.fragment.LyricsViewFragment;
import com.geecko.QuickLyric.utils.ColorUtils;
import com.geecko.QuickLyric.model.Recents;
import com.geecko.QuickLyric.model.Track;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;

public class MusicBroadcastReceiver extends BroadcastReceiver {

    static boolean autoUpdate = false;
    static boolean spotifyPlaying = false;

    public static void forceAutoUpdate(boolean force) {
        autoUpdate = force;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        /** Google Play Music
         //bool streaming				long position
         //long albumId					String album
         //bool currentSongLoaded		String track
         //long ListPosition			long ListSize
         //long id						bool playing
         //long duration				int previewPlayType
         //bool supportsRating			int domain
         //bool albumArtFromService		String artist
         //int rating					bool local
         //bool preparing				bool inErrorState
         */

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
                || (lengthFilter && (extras.get("duration") instanceof Long && extras.getLong("duration") > 1200000)
                || (extras.get("duration") instanceof Double && extras.getDouble("duration") > 1200000)
                || (extras.get("duration") instanceof Integer && extras.getInt("duration") > 1200))
                || (lengthFilter && (extras.get("secs") instanceof Long && extras.getLong("secs") > 1200000)
                || (extras.get("secs") instanceof Double && extras.getDouble("secs") > 1200000)
                || (extras.get("secs") instanceof Integer && extras.getInt("secs") > 1200))
                || (extras.containsKey("com.maxmpz.audioplayer.source") && Build.VERSION.SDK_INT >= 19))
            return;

        String artist = extras.getString("artist");
        String track = extras.getString("track");
        long position = extras.containsKey("position") && extras.get("position") instanceof Long ?
                extras.getLong("position") : -1;
        if (extras.get("position") instanceof Double)
            position = Double.valueOf(extras.getDouble("position")).longValue();
        boolean isPlaying = extras.getBoolean(extras.containsKey("playstate") ? "playstate" : "playing", true);

        if (intent.getAction().equals("com.amazon.mp3.metachanged")) {
            artist = extras.getString("com.amazon.mp3.artist");
            track = extras.getString("com.amazon.mp3.track");
        } else if (intent.getAction().equals("com.spotify.music.metadatachanged"))
            isPlaying = spotifyPlaying;
        else if (intent.getAction().equals("com.spotify.music.playbackstatechanged"))
            spotifyPlaying = isPlaying;

        if ((artist == null || "".equals(artist))  //Could be problematic
                || (track == null || "".equals(track)
                || track.startsWith("DTNS"))) // Ignore one of my favorite podcasts
            return;

        SharedPreferences current = context.getSharedPreferences("current_music", Context.MODE_PRIVATE);
        String currentArtist = current.getString("artist", "");
        String currentTrack = current.getString("track", "");

        SharedPreferences.Editor editor = current.edit();
        editor.putString("artist", artist);
        editor.putString("track", track);
        if (!(artist.equals(currentArtist) && track.equals(currentTrack) && position == -1))
            editor.putLong("position", position);
        editor.putBoolean("playing", isPlaying);
        if (isPlaying) {
            long currentTime = System.currentTimeMillis();
            editor.putLong("startTime", currentTime);
        }
        editor.apply();

        Recents.getInstance(context).add(new Track(track, artist));

        autoUpdate = autoUpdate || sharedPref.getBoolean("pref_auto_refresh", false);
        int notificationPref = Integer.valueOf(sharedPref.getString("pref_notifications", "0"));

        if (autoUpdate && App.isActivityVisible()) {
            Intent internalIntent = new Intent("Broadcast");
            internalIntent.putExtra("artist", artist).putExtra("track", track);
            LyricsViewFragment.sendIntent(context, internalIntent);
            forceAutoUpdate(false);
        }


        boolean inDatabase = DatabaseHelper.getInstance(context).presenceCheck(new String[]{artist, track, artist, track});

        if (notificationPref != 0 && isPlaying
                && (inDatabase || OnlineAccessVerifier.check(context))) {
            Intent activityIntent = new Intent("com.geecko.QuickLyric.getLyrics")
                    .putExtra("TAGS", new String[]{artist, track});
            Intent wearableIntent = new Intent("com.geecko.QuickLyric.SEND_TO_WEARABLE")
                    .putExtra("artist", artist).putExtra("track", track);
            PendingIntent openAppPending = PendingIntent.getActivity(context, 0, activityIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            PendingIntent wearablePending =
                    PendingIntent.getBroadcast(context, 8, wearableIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.Action wearableAction =
                    new NotificationCompat.Action.Builder(R.drawable.ic_watch,
                            context.getString(R.string.wearable_prompt), wearablePending)
                            .build();

            NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context);
            NotificationCompat.Builder wearableNotifBuilder = new NotificationCompat.Builder(context);

            int[] themes = new int[]{R.style.Theme_QuickLyric, R.style.Theme_QuickLyric_Red,
                    R.style.Theme_QuickLyric_Purple, R.style.Theme_QuickLyric_Indigo,
                    R.style.Theme_QuickLyric_Green, R.style.Theme_QuickLyric_Lime,
                    R.style.Theme_QuickLyric_Brown, R.style.Theme_QuickLyric_Dark};
            int themeNum = Integer.valueOf(sharedPref.getString("pref_theme", "0"));

            context.setTheme(themes[themeNum]);

            notifBuilder.setSmallIcon(R.drawable.ic_notif)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(String.format("%s - %s", artist, track))
                    .setContentIntent(openAppPending)
                    .setVisibility(-1) // Notification.VISIBILITY_SECRET
                    .setGroup("Lyrics_Notification")
                    .setColor(ColorUtils.getPrimaryColor(context))
                    .setGroupSummary(true);

            wearableNotifBuilder.setSmallIcon(R.drawable.ic_notif)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(String.format("%s - %s", artist, track))
                    .setContentIntent(openAppPending)
                    .setVisibility(-1) // Notification.VISIBILITY_SECRET
                    .setGroup("Lyrics_Notification")
                    .setOngoing(false)
                    .setColor(ColorUtils.getPrimaryColor(context))
                    .setGroupSummary(false)
                    .extend(new NotificationCompat.WearableExtender().addAction(wearableAction));

            if (notificationPref == 2) {
                notifBuilder.setOngoing(true).setPriority(-2); // Notification.PRIORITY_MIN
                wearableNotifBuilder.setPriority(-2);
            } else
                notifBuilder.setPriority(-1); // Notification.PRIORITY_LOW

            Notification notif = notifBuilder.build();
            Notification wearableNotif = wearableNotifBuilder.build();

            if (notificationPref == 2)
                notif.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
            else
                notif.flags |= Notification.FLAG_AUTO_CANCEL;

            NotificationManagerCompat.from(context).notify(0, notif);
            try {
                context.getPackageManager().getPackageInfo("com.google.android.wearable.app", PackageManager.GET_META_DATA);
                NotificationManagerCompat.from(context).notify(8, wearableNotif);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        } else if (track.equals(current.getString("track", "")))
            NotificationManagerCompat.from(context).cancel(0);
    }
}
