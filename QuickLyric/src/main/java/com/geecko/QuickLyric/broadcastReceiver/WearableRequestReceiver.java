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
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Html;

import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.tasks.DownloadThread;
import com.geecko.QuickLyric.tasks.Id3Reader;
import com.geecko.QuickLyric.utils.ColorUtils;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.NotificationUtil;
import com.geecko.QuickLyric.view.LrcView;

import java.lang.ref.WeakReference;

public class WearableRequestReceiver extends BroadcastReceiver implements Lyrics.Callback {
    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        Lyrics lyrics = DatabaseHelper.getInstance(context)
                .get(new String[]{intent.getStringExtra("artist"), intent.getStringExtra("track")});
        if (lyrics == null)
            new DownloadThread(new WeakReference<>(this), null, intent.getLongExtra("duration", 0L), Id3Reader.getFile(context, intent.getStringExtra("artist"), intent.getStringExtra("track"), false),
                    intent.getStringExtra("artist"), intent.getStringExtra("track")).start();
        else
            onLyricsDownloaded(lyrics);
    }

    @Override
    public void onLyricsDownloaded(Lyrics lyrics) {
        if (lyrics.isLRC()) {
            LrcView lrcView = new LrcView(mContext, null);
            lrcView.setOriginalLyrics(lyrics);
            lrcView.setSourceLrc(lyrics.getText());
            lyrics.setText(lrcView.getStaticLyrics().getText());
        }

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(mContext, NotificationUtil.TRACK_NOTIF_CHANNEL);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);

        Intent activityIntent = new Intent("com.geecko.QuickLyric.getLyrics")
                .putExtra("TAGS", new String[]{lyrics.getArtist(), lyrics.getTitle()});
        PendingIntent openAction = PendingIntent.getActivity(mContext, 0, activityIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        BigTextStyle bigStyle = new BigTextStyle();
        bigStyle.bigText(lyrics.getText() != null ? Html.fromHtml(lyrics.getText()) : "");

        int[] themes = new int[]{R.style.Theme_QuickLyric, R.style.Theme_QuickLyric_Red,
                R.style.Theme_QuickLyric_Purple, R.style.Theme_QuickLyric_Indigo,
                R.style.Theme_QuickLyric_Green, R.style.Theme_QuickLyric_Lime,
                R.style.Theme_QuickLyric_Brown, R.style.Theme_QuickLyric_Dark};
        int themeNum = Integer.valueOf(sharedPref.getString("pref_theme", "0"));
        int notificationPref = Integer.valueOf(sharedPref.getString("pref_notifications", "0"));

        mContext.setTheme(themes[themeNum]);

        notifBuilder.setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(mContext.getString(R.string.app_name))
                .setContentText(String.format("%s - %s", lyrics.getArtist(), lyrics.getTitle()))
                .setStyle(bigStyle)
                .setGroup("Lyrics_Notification")
                .setOngoing(false)
                .setColor(ColorUtils.getPrimaryColor(mContext))
                .setGroupSummary(false)
                .setContentIntent(openAction)
                .setVisibility(-1); // Notification.VISIBILITY_SECRET

        if (notificationPref == 2)
            notifBuilder.setPriority(-2);

        if (lyrics.getFlag() < 0)
            notifBuilder.extend(new NotificationCompat.WearableExtender()
                    .setContentIntentAvailableOffline(false));

        Notification notif = notifBuilder.build();

        NotificationManagerCompat.from(mContext).notify(8, notif);
    }
}
