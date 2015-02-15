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

package be.geecko.QuickLyric.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Process;
import android.widget.Toast;

import be.geecko.QuickLyric.MainActivity;
import be.geecko.QuickLyric.R;
import be.geecko.QuickLyric.fragment.LyricsViewFragment;
import be.geecko.QuickLyric.lyrics.Lyrics;
import be.geecko.QuickLyric.utils.DatabaseHelper;
import be.geecko.QuickLyric.utils.OnlineAccessVerifier;

public class ParseTask extends AsyncTask<Object, Object, String[]> {

    private Lyrics currentLyrics;
    private LyricsViewFragment lyricsViewFragment;
    private Context mContext;

    @Override
    protected String[] doInBackground(Object... arg0) {
        lyricsViewFragment = (LyricsViewFragment) arg0[0];
        mContext = lyricsViewFragment.getActivity();
        currentLyrics = (Lyrics) arg0[1];
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        SharedPreferences preferences = mContext.getSharedPreferences("current_music", Context.MODE_PRIVATE);
        String[] music = new String[2];
        music[0] = preferences.getString("artist", "Michael Jackson");
        music[1] = preferences.getString("track", "Bad");
        return music;
    }

    @Override
    protected void onPostExecute(String[] metaData) {
        if (currentLyrics != null && metaData[0].equals(currentLyrics.getOriginalArtist()) && metaData[1].equals(currentLyrics.getOriginalTrack()) && currentLyrics.getFlag() == Lyrics.POSITIVE_RESULT)
            Toast.makeText(mContext, mContext.getString(R.string.no_refresh), Toast.LENGTH_LONG).show();
        else {
            SQLiteDatabase sqLiteDatabase = ((MainActivity) lyricsViewFragment.getActivity()).database;
            if (DatabaseHelper.presenceCheck(sqLiteDatabase, metaData))
                lyricsViewFragment.update(DatabaseHelper.get(sqLiteDatabase, metaData),
                        lyricsViewFragment.getView(), true);
            else if (OnlineAccessVerifier.check(mContext)) {
                lyricsViewFragment.startRefreshAnimation();
                if (lyricsViewFragment.currentDownload != null && lyricsViewFragment.currentDownload.getStatus() != Status.FINISHED) {
                    if (lyricsViewFragment.currentDownload.interruptible)
                        lyricsViewFragment.currentDownload.cancel(true);
                    else
                        return;
                }
                lyricsViewFragment.currentDownload = new DownloadTask();
                lyricsViewFragment.currentDownload.execute(mContext, metaData[0], metaData[1]);
            } else {
                Lyrics lyrics = new Lyrics(Lyrics.ERROR);
                lyrics.setArtist(metaData[0]);
                lyrics.setTitle(metaData[1]);
                lyricsViewFragment.update(lyrics, lyricsViewFragment.getView(), true);
            }
        }
    }
}
