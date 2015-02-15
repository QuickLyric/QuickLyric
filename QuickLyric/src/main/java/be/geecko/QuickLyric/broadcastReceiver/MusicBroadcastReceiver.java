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

package be.geecko.QuickLyric.broadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import be.geecko.QuickLyric.App;
import be.geecko.QuickLyric.fragment.LyricsViewFragment;

public class MusicBroadcastReceiver extends BroadcastReceiver {

    private boolean mAutoUpdate = false;

    public void setAutoUpdate(boolean force){
        this.mAutoUpdate = force;
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
        boolean lengthFilter = sharedPref.getBoolean("filter_20min", true);

        if (extras == null || extras.getInt("state") > 1 //Tracks longer than 20min are presumably not songs
                || (lengthFilter && (extras.get("duration") instanceof Long && extras.getLong("duration") > 1200000)
                || (extras.get("duration") instanceof Double && extras.getDouble("duration") > 1200000)
                || (extras.get("duration") instanceof Integer && extras.getInt("duration") > 1200)))
            return;

        String artist = extras.getString("artist");
        String track = extras.getString("track");

        if (intent.getAction().equals("com.amazon.mp3.metachanged")) {
            artist = extras.getString("com.amazon.mp3.artist");
            track = extras.getString("com.amazon.mp3.track");
        }

        if ((artist == null || "".equals(artist) || artist.contains("Unknown"))  //Could be problematic
                || (track == null || "".equals(track) || track.contains("Unknown")
                || track.startsWith("TN2") || track.startsWith("DTNS"))) // Ignore my favorite podcasts
            return;

        SharedPreferences current = context.getSharedPreferences("current_music", Context.MODE_PRIVATE);

        String currentArtist = current.getString("artist", "Michael Jackson");
        String currentTrack = current.getString("track", "Bad");

        if (!currentArtist.equals(artist) || !currentTrack.equals(track)) {

            SharedPreferences.Editor editor = current.edit();
            editor.putString("artist", artist);
            editor.putString("track", track);
            editor.apply();

            mAutoUpdate = mAutoUpdate || sharedPref.getBoolean("pref_auto_refresh", false);

            if (App.isActivityVisible() && mAutoUpdate) {
                Intent internalIntent = new Intent("Broadcast");
                internalIntent.putExtra("artist", artist).putExtra("track", track);
                LyricsViewFragment.sendIntent(context, internalIntent);
                setAutoUpdate(false);
            }
        }
    }
}