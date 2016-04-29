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


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class NotificationListenerService extends android.service.notification.NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        String artist = null;
        String track = null;
        boolean isPlaying = false;
        int duration = -1;
        int position = -1;
        ViewGroup viewGroup = getNotificationLayout(sbn);
        String packageName = sbn.getPackageName();
        switch (packageName) {
            case "com.apple.android.music":
                track = ((TextView) ((ViewGroup) viewGroup.getChildAt(1))
                        .getChildAt(0)).getText().toString();
                artist = ((TextView) ((ViewGroup) viewGroup.getChildAt(1))
                        .getChildAt(2)).getText().toString();
                isPlaying = ((ViewGroup) viewGroup.getChildAt(3))
                        .getChildAt(1).getContentDescription().equals("Pause");
                break;
            case "com.kodarkooperativet.blackplayerfree":
                track = ((TextView) ((ViewGroup) ((ViewGroup) viewGroup.getChildAt(1))
                        .getChildAt(0)).getChildAt(0)).getText().toString();
                artist = ((TextView) ((ViewGroup) viewGroup.getChildAt(1))
                        .getChildAt(1)).getText().toString();
                break;
            case "com.saavn.android":
                track = ((TextView) ((ViewGroup) viewGroup.getChildAt(4))
                        .getChildAt(0)).getText().toString();
                artist = ((TextView) ((ViewGroup) viewGroup.getChildAt(4))
                        .getChildAt(1)).getText().toString();
                break;
            case "com.pandora.android":
                track = ((TextView) ((ViewGroup) ((ViewGroup) viewGroup.getChildAt(2))
                        .getChildAt(0)).getChildAt(0)).getText().toString();
                artist = ((TextView) ((ViewGroup) viewGroup.getChildAt(2))
                        .getChildAt(1)).getText().toString();
                break;
        }
        if ((artist != null && !artist.trim().isEmpty()) && (track != null && !track.trim().isEmpty()))
            broadcast(artist, track, isPlaying, duration, position);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }

    private void broadcast(String artist, String track, boolean playing, int duration, int position) {
        Intent localIntent = new Intent("com.android.music.metachanged");
        localIntent.putExtra("artist", artist);
        localIntent.putExtra("track", track);
        localIntent.putExtra("playing", playing);
        localIntent.putExtra("duration", duration);
        if (position != -1)
            localIntent.putExtra("position", position);
        sendBroadcast(localIntent);
    }

    private ViewGroup getNotificationLayout(StatusBarNotification sbn) {
        RemoteViews rv;
        if (sbn.getNotification().bigContentView != null)
            rv = sbn.getNotification().bigContentView;
        else
            rv = sbn.getNotification().contentView;
        Context context = null;
        try {
            context = createPackageContext(sbn.getPackageName(), Context.CONTEXT_RESTRICTED);
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        ViewGroup viewGroup = (ViewGroup) ((LayoutInflater) context
                .getSystemService(LAYOUT_INFLATER_SERVICE))
                .inflate(rv.getLayoutId(), null);
        rv.reapply(context, viewGroup);
        return viewGroup;
    }
}
