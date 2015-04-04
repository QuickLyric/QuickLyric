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

package com.geecko.QuickLyric.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Process;
import android.widget.Toast;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.fragment.LyricsViewFragment;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;

public class ParseTask extends AsyncTask<Object, Object, String[]> {

    private final boolean showMsg;
    private Lyrics currentLyrics;
    private LyricsViewFragment lyricsViewFragment;
    private Context mContext;

    public ParseTask(LyricsViewFragment fragment, boolean showMsg) {
        this.lyricsViewFragment = fragment;
        this.showMsg = showMsg;
    }

    @Override
    protected String[] doInBackground(Object... arg0) {
        mContext = lyricsViewFragment.getActivity();
        if (mContext == null)
            cancel(true);
        currentLyrics = (Lyrics) arg0[0];
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        SharedPreferences preferences = mContext.getSharedPreferences("current_music", Context.MODE_PRIVATE);
        String[] music = new String[2];
        music[0] = preferences.getString("artist", "Michael Jackson");
        music[1] = preferences.getString("track", "Bad");
        return music;
    }

    @Override
    protected void onPostExecute(String[] metaData) {
        if (currentLyrics != null && metaData[0].equalsIgnoreCase(currentLyrics.getOriginalArtist())
                && metaData[1].equalsIgnoreCase(currentLyrics.getOriginalTrack())
                && currentLyrics.getFlag() == Lyrics.POSITIVE_RESULT) {
            if (showMsg)
                Toast.makeText(mContext, mContext.getString(R.string.no_refresh), Toast.LENGTH_LONG).show();
        } else {
            SQLiteDatabase sqLiteDatabase = ((MainActivity) lyricsViewFragment.getActivity()).database;
            if (DatabaseHelper.presenceCheck(sqLiteDatabase, metaData))
                lyricsViewFragment.update(DatabaseHelper.get(sqLiteDatabase, metaData),
                        lyricsViewFragment.getView(), true);
            else if (OnlineAccessVerifier.check(mContext)) {
                lyricsViewFragment.fetchLyrics(metaData[0], metaData[1]);
            } else {
                Lyrics lyrics = new Lyrics(Lyrics.ERROR);
                lyrics.setArtist(metaData[0]);
                lyrics.setTitle(metaData[1]);
                lyricsViewFragment.update(lyrics, lyricsViewFragment.getView(), true);
            }
        }
    }
}
