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
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.util.LongSparseArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.adapter.LocalAdapter;
import com.geecko.QuickLyric.fragment.LocalLyricsFragment;
import com.geecko.QuickLyric.fragment.LyricsViewFragment;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.utils.DatabaseHelper;

public class WriteToDatabaseTask extends AsyncTask<Object, Void, Boolean> {

    private Fragment fragment;
    private Context mContext;
    private MenuItem item;
    private Lyrics[] lyricsArray;
    private LocalLyricsFragment mLocalLyricsFragment;

    public WriteToDatabaseTask() {
        super();
    }

    public WriteToDatabaseTask(LocalLyricsFragment fragment) {
        super();
        this.mLocalLyricsFragment = fragment;
    }

    @Override
    public Boolean doInBackground(Object... params) {
        lyricsArray = new Lyrics[params.length - 2];
        SQLiteDatabase database;
        if (params[0] instanceof Fragment) {
            fragment = (Fragment) params[0];
            mContext = fragment.getActivity();
            if (mContext == null || !(mContext instanceof MainActivity))
                cancel(true);
            database = DatabaseHelper.getInstance(mContext).getWritableDatabase();
        } else
            database = (SQLiteDatabase) params[0];
        item = (MenuItem) params[1];
        if (params[2] instanceof Lyrics[])
            lyricsArray = (Lyrics[]) params[2];
        else
            for (int i = 0; i < lyricsArray.length; i++) {
                lyricsArray[i] = (Lyrics) params[i + 2];
            }
        boolean result = true;
        String[] columns = DatabaseHelper.columns;
        if (database != null && database.isOpen()) {
            database.beginTransaction();
            try {
                for (Lyrics lyrics : lyricsArray) {
                    Lyrics storedLyrics = DatabaseHelper.getInstance(mContext).get(new String[]{lyrics.getArtist(), lyrics.getTrack(),
                            lyrics.getOriginalArtist(), lyrics.getOriginalTrack()});
                    if ((storedLyrics == null || (!storedLyrics.isLRC() && lyrics.isLRC()))
                            && !"Storage".equals(lyrics.getSource())) {
                        ContentValues values = new ContentValues(2);
                        values.put(columns[0], lyrics.getArtist());
                        values.put(columns[1], lyrics.getTrack());
                        values.put(columns[2], lyrics.getText());
                        values.put(columns[3], lyrics.getURL());
                        values.put(columns[4], lyrics.getSource());
                        values.put(columns[5], lyrics.getCoverURL());
                        values.put(columns[6], lyrics.getOriginalArtist());
                        values.put(columns[7], lyrics.getOriginalTrack());
                        values.put(columns[8], lyrics.isLRC() ? 1 : 0);
                        database.delete(DatabaseHelper.TABLE_NAME, String.format("%s=? AND %s=?", columns[0], columns[1]), new String[]{lyrics.getArtist(), lyrics.getTrack()});
                        database.insert(DatabaseHelper.TABLE_NAME, null, values);
                        if (fragment instanceof LyricsViewFragment)
                            ((LyricsViewFragment) fragment).lyricsPresentInDB = true;
                        result = true;
                    } else if (mContext != null) { // if called from activity, not service
                        database.delete(DatabaseHelper.TABLE_NAME, String.format("%s=? AND %s=?", columns[0], columns[1]), new String[]{lyrics.getArtist(), lyrics.getTrack()});
                        if (fragment instanceof LyricsViewFragment)
                            ((LyricsViewFragment) fragment).lyricsPresentInDB = false;
                        result = false;
                    }
                    database.yieldIfContendedSafely();
                }
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }
        return result;
    }

    @Override
    public void onPostExecute(Boolean result) {
        int message = result ? R.string.lyrics_saved : R.string.lyrics_removed;
        if (fragment instanceof LyricsViewFragment) {
            SharedPreferences sharedPref =
                    PreferenceManager.getDefaultSharedPreferences(mContext);
            if (!sharedPref.getBoolean("pref_auto_save", true))
                Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            item.setIcon(result ? R.drawable.ic_trash : R.drawable.ic_save);
            item.setTitle(result ? R.string.remove_action : R.string.save_action);
        } else if (fragment instanceof LocalLyricsFragment) {
            LongSparseArray<Integer> topMap = mLocalLyricsFragment.collectTopPositions();
            mLocalLyricsFragment.addObserver(topMap);
            int position = ((LocalAdapter) mLocalLyricsFragment.getExpandableListAdapter())
                    .getGroupPosition(lyricsArray[0].getArtist());
            if (!result)
                ((LocalAdapter) mLocalLyricsFragment.getExpandableListAdapter())
                        .removeArtistFromCache(lyricsArray[0].getArtist());
            else
                ((LocalAdapter) mLocalLyricsFragment.getExpandableListAdapter())
                        .addArtist(lyricsArray[0].getArtist());
            View.OnClickListener actionClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View snackbar) {
                    mLocalLyricsFragment.animateUndo(lyricsArray[0]);
                }
            };
            if (!result && fragment.getView() != null) {
                Snackbar.make(fragment.getView(), message, Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, actionClickListener)
                        .setActionTextColor(mContext.getResources().getColor(R.color.accent_light)).show();
            } else if (fragment.getView() != null && position == -1) {
                position = ((LocalAdapter) mLocalLyricsFragment.getExpandableListAdapter())
                        .getGroupPosition(lyricsArray[0].getArtist());
                mLocalLyricsFragment.getMegaListView().expandGroupWithAnimation(position);
            }
        }
    }
}