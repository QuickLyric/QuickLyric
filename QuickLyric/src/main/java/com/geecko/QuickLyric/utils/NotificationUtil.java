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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.services.LyricsOverlayService;


public class NotificationUtil {

    public static final String TRACK_NOTIF_CHANNEL = "track_visible_notif";
    public static final String TRACK_NOTIF_HIDDEN_CHANNEL = "track_hidden_notif";
    private static final String TRANSLATION_CHANNEL = "translation_notif";
    public static final int NOTIFICATION_ID = 6;

    public static Notification makeNotification(Context context, String artist, String track, long duration, boolean retentionNotif, boolean isPlaying) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean prefOverlay = sharedPref.getBoolean("pref_overlay", false) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context));
        int notificationPref = prefOverlay ? 2 : Integer.valueOf(sharedPref.getString("pref_notifications", "0"));

        Intent activityIntent = new Intent(context.getApplicationContext(), MainActivity.class)
                .setAction("com.geecko.QuickLyric.getLyrics")
                .putExtra("retentionNotif", retentionNotif)
                .putExtra("TAGS", new String[]{artist, track});
        Intent wearableIntent = new Intent("com.geecko.QuickLyric.SEND_TO_WEARABLE")
                .putExtra("artist", artist).putExtra("track", track).putExtra("duration", duration);
        final Intent overlayIntent = new Intent(context.getApplicationContext(), LyricsOverlayService.class)
                .setAction(LyricsOverlayService.CLICKED_FLOATING_ACTION);

        PendingIntent overlayPending = PendingIntent.getService(context.getApplicationContext(), 0, overlayIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent openAppPending = PendingIntent.getActivity(context.getApplicationContext(), 0, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent wearablePending = PendingIntent.getBroadcast(context.getApplicationContext(), 8, wearableIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Action wearableAction =
                new NotificationCompat.Action.Builder(R.drawable.ic_watch,
                        context.getString(R.string.wearable_prompt), wearablePending)
                        .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(notificationPref == 1 && isPlaying ? TRACK_NOTIF_CHANNEL : TRACK_NOTIF_HIDDEN_CHANNEL,
                    context.getString(R.string.pref_notifications),
                    notificationPref == 1 && isPlaying ? NotificationManager.IMPORTANCE_LOW : NotificationManager.IMPORTANCE_MIN);
            notificationChannel.setShowBadge(false);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context.getApplicationContext(), notificationPref == 1 && isPlaying ? TRACK_NOTIF_CHANNEL : TRACK_NOTIF_HIDDEN_CHANNEL);
        NotificationCompat.Builder wearableNotifBuilder = new NotificationCompat.Builder(context.getApplicationContext(), TRACK_NOTIF_HIDDEN_CHANNEL);

        int[] themes = new int[]{R.style.Theme_QuickLyric, R.style.Theme_QuickLyric_Red,
                R.style.Theme_QuickLyric_Purple, R.style.Theme_QuickLyric_Indigo,
                R.style.Theme_QuickLyric_Green, R.style.Theme_QuickLyric_Lime,
                R.style.Theme_QuickLyric_Brown, R.style.Theme_QuickLyric_Dark};
        int themeNum = Integer.valueOf(sharedPref.getString("pref_theme", "0"));

        context.setTheme(themes[themeNum]);

        notifBuilder.setSmallIcon(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? R.drawable.ic_notif : R.drawable.ic_notif4)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(String.format("%s - %s", artist, track))
                .setContentIntent(prefOverlay ? overlayPending : openAppPending)
                .setVisibility(-1) // Notification.VISIBILITY_SECRET
                .setGroup("Lyrics_Notification")
                .setColor(ColorUtils.getPrimaryColor(context))
                .setShowWhen(false)
                .setGroupSummary(true);

        wearableNotifBuilder.setSmallIcon(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? R.drawable.ic_notif : R.drawable.ic_notif4)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(String.format("%s - %s", artist, track))
                .setContentIntent(openAppPending)
                .setVisibility(-1) // Notification.VISIBILITY_SECRET
                .setGroup("Lyrics_Notification")
                .setOngoing(false)
                .setColor(ColorUtils.getPrimaryColor(context))
                .setGroupSummary(false)
                .setShowWhen(false)
                .extend(new NotificationCompat.WearableExtender().addAction(wearableAction));

        if (notificationPref == 2) {
            notifBuilder.setOngoing(true).setPriority(-2); // Notification.PRIORITY_MIN
            wearableNotifBuilder.setPriority(-2);
        } else
            notifBuilder.setPriority(-1); // Notification.PRIORITY_LOW

        Notification notif = notifBuilder.build();
        Notification wearableNotif = wearableNotifBuilder.build();

        if (notificationPref == 2 || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            notif.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        if (notificationPref == 1)
            notif.flags |= Notification.FLAG_AUTO_CANCEL;

        try {
            context.getPackageManager().getPackageInfo("com.google.android.wearable.app", PackageManager.GET_META_DATA);
            NotificationManagerCompat.from(context).notify(8, wearableNotif); // TODO Make Android Wear app
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return notif;
    }

    public static void showTranslationNotif(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(TRANSLATION_CHANNEL,
                    context.getString(R.string.pref_notifications),
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        Intent crowdinIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://crowdin.com/project/quicklyric"));
        PendingIntent crowdinPending = PendingIntent.getActivity(context.getApplicationContext(), 0,
                crowdinIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification = new NotificationCompat.Builder(context, TRANSLATION_CHANNEL)
                .setSmallIcon(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? R.mipmap.ic_launcher : R.drawable.ic_notif4)
                .setContentTitle(context.getString(R.string.translate_us))
                .setContentText(context.getString(R.string.about_crowdin))
                .setContentIntent(crowdinPending)
                .setColor(ColorUtils.getPrimaryColor(context))
                .setShowWhen(false)
                .build();
        notificationManager.notify(9, notification);
    }
}
