/* Hello! This code is heavily based on what I could make out from the original Last.FM Scrobbler for Xperia
 * You can find the original app here: https://play.google.com/store/apps/details?id=com.mobilesolutionworks.semcmusic.scrobbler
 * Thanks to that author for figuring it out. I fixed it up a little and removed compatibility with other scrobblers. */

package com.geecko.QuickLyric.broadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class XperiaBroadcastReceiver extends BroadcastReceiver {
    public static final String LAST_FM_META_CHANGED = "fm.last.android.metachanged";
    public static final String LAST_FM_PLAYBACK_COMPLETE = "fm.last.android.playbackcomplete";
    public static final String LAST_FM_PLAYBACK_PAUSED = "fm.last.android.playbackpaused";

    public void onReceive(Context paramContext, Intent intent) {
        String type;
        String state = intent.getAction();

        switch (state) {
            default:
                type = LAST_FM_META_CHANGED;
                break;
            case "com.sonyericsson.music.playbackcontrol.ACTION_PAUSED":
                type = LAST_FM_PLAYBACK_PAUSED;
                break;
            case "com.sonyericsson.music.TRACK_COMPLETED":
                type = LAST_FM_PLAYBACK_COMPLETE;
                break;
        }

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Intent localIntent = new Intent(type);
            localIntent.putExtra("artist",
                    bundle.getString("ARTIST_NAME"));
            localIntent.putExtra("album",
                    bundle.getString("ALBUM_NAME"));
            localIntent.putExtra("track",
                    bundle.getString("TRACK_NAME"));
            localIntent.putExtra("playing",
                    !type.equals(LAST_FM_PLAYBACK_PAUSED));
            localIntent.putExtra("duration",
                    bundle.getInt("TRACK_DURATION") / 1000);
            localIntent.putExtra("source", "semc");
            int i = bundle.getInt("TRACK_POSITION", -1);
            if (i != -1) {
                localIntent.putExtra("position", i / 1000);
            }
            paramContext.sendBroadcast(localIntent);
        }
    }
}