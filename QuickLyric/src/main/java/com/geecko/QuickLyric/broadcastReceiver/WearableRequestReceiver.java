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
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.tasks.DownloadThread;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.view.LrcView;

public class WearableRequestReceiver extends BroadcastReceiver implements Lyrics.Callback {
    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        Lyrics lyrics = DatabaseHelper.getInstance(context)
                .get(new String[]{intent.getStringExtra("artist"), intent.getStringExtra("track")});
        if (lyrics == null)
            new DownloadThread(this, false, intent.getStringExtra("artist"), intent.getStringExtra("track")).start();
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

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(mContext);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);

        Intent activityIntent = new Intent("com.geecko.QuickLyric.getLyrics")
                .putExtra("TAGS", new String[]{lyrics.getArtist(), lyrics.getTrack()});
        PendingIntent openAction = PendingIntent.getActivity(mContext, 0, activityIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        if (sharedPref.getString("pref_theme", "0").equals("0"))
            notifBuilder.setColor(mContext.getResources().getColor(R.color.primary));
        BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText(lyrics.getText() != null ? Html.fromHtml(lyrics.getText()) : "");

        notifBuilder.setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(mContext.getString(R.string.app_name))
                .setContentText(String.format("%s - %s", lyrics.getArtist(), lyrics.getTrack()))
                .setStyle(bigStyle)
                .setGroup("Lyrics_Notification")
                .setOngoing(false)
                .setGroupSummary(false)
                .setContentIntent(openAction)
                .setVisibility(-1); // Notification.VISIBILITY_SECRET

        if (lyrics.getFlag() < 0)
            notifBuilder.extend(new NotificationCompat.WearableExtender()
                    .setContentIntentAvailableOffline(false));

        Notification notif = notifBuilder.build();

        NotificationManagerCompat.from(mContext).notify(8, notif);
    }
}
