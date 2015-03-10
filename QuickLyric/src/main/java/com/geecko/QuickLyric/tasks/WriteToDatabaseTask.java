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

import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.Toast;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.fragment.LyricsViewFragment;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.listeners.ActionClickListener;

public class WriteToDatabaseTask extends AsyncTask<Object, Void, Boolean> {

    private Fragment fragment;
    private Context mContext;
    private MenuItem item;
    private Lyrics[] lyricsArray;

    @Override
    protected Boolean doInBackground(Object... params) {
        lyricsArray = new Lyrics[params.length - 2];
        fragment = (Fragment) params[0];
        item = (MenuItem) params[1];
        if (params[2] instanceof Lyrics[])
            lyricsArray = (Lyrics[]) params[2];
        else
            for (int i = 0; i < lyricsArray.length; i++) {
                lyricsArray[i] = (Lyrics) params[i + 2];
            }
        mContext = fragment.getActivity();
        String table = "lyrics";
        SQLiteDatabase database = ((MainActivity) mContext).database;
        Boolean result = false;
        String[] columns = DatabaseHelper.columns;
        if (database != null) {
            for (Lyrics lyrics : lyricsArray)
                if (!DatabaseHelper.presenceCheck(database, new String[]{lyrics.getArtist(), lyrics.getTrack()})) {
                    ContentValues values = new ContentValues(2);
                    values.put(columns[0], lyrics.getArtist());
                    values.put(columns[1], lyrics.getTrack());
                    values.put(columns[2], lyrics.getText());
                    values.put(columns[3], lyrics.getURL());
                    values.put(columns[4], lyrics.getSource());
                    values.put(columns[5], lyrics.getCoverURL());
                    database.insert(table, null, values);
                    if (fragment instanceof LyricsViewFragment)
                        ((LyricsViewFragment) fragment).lyricsPresentInDB = true;
                    result = true;
                } else {
                    database.delete(table, String.format("%s=? AND %s=?", columns[0], columns[1]), new String[]{lyrics.getArtist(), lyrics.getTrack()});
                    if (fragment instanceof LyricsViewFragment)
                        ((LyricsViewFragment) fragment).lyricsPresentInDB = false;
                    result = false;
                }
        }
        return result;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        int message = result ? R.string.lyrics_saved : R.string.lyrics_removed;
        if (fragment instanceof LyricsViewFragment) {
            SharedPreferences sharedPref =
                    PreferenceManager.getDefaultSharedPreferences(mContext);
            if (!sharedPref.getBoolean("pref_auto_save", false))
                Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            item.setIcon(result ? R.drawable.ic_trash : R.drawable.ic_save);
            item.setTitle(result ? R.string.remove_action : R.string.save_action);
        } else {
            ActionClickListener actionClickListener = new ActionClickListener() {
                @Override
                public void onActionClicked(Snackbar snackbar) {
                    new WriteToDatabaseTask().execute(fragment, null, lyricsArray);
                }
            };
            if (!result) {
                final Typeface roboto = Typeface
                        .createFromAsset(mContext.getAssets(), "fonts/Roboto-Bold.ttf");
                Snackbar.with(mContext).text(message).actionLabel(R.string.undo)
                        .actionColorResource(R.color.accent_light).actionLabelTypeface(roboto)
                        .actionListener(actionClickListener).show((MainActivity) mContext);
            }
            new DBContentLister().execute(fragment);
        }
    }
}