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
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.view.OverlayContentLayout;

import java.lang.ref.WeakReference;

public class WriteToDatabaseTask extends AsyncTask<Object, Void, Boolean> {

    private WeakReference<Fragment> fragment;
    private WeakReference<Context> mContext = new WeakReference<>(null);
    private MenuItem item;
    private Lyrics[] lyricsArray;
    private WeakReference<LocalLyricsFragment> mLocalLyricsFragment;
    private WeakReference<OverlayContentLayout> mOverlay;

    public WriteToDatabaseTask() {
        super();
    }

    public WriteToDatabaseTask(LocalLyricsFragment fragment) {
        super();
        this.mLocalLyricsFragment = new WeakReference<>(fragment);
    }

    @Override
    public Boolean doInBackground(Object... params) {
        lyricsArray = new Lyrics[params.length - 2];
        SQLiteDatabase database;
        if (params[0] instanceof Fragment) {
            fragment = new WeakReference<>((Fragment) params[0]);
            mContext = new WeakReference<>(fragment.get().getActivity());
            if (mContext == null || !(mContext.get() instanceof MainActivity))
                cancel(true);
            database = DatabaseHelper.getInstance(mContext.get()).getWritableDatabase();
        } else if (params[0] instanceof OverlayContentLayout) {
            mOverlay = new WeakReference<>((OverlayContentLayout) params[0]);
            mContext = new WeakReference<>(mOverlay.get().getContext());
            database = DatabaseHelper.getInstance(mContext.get()).getWritableDatabase();
        } else
            database = (SQLiteDatabase) params[0];
        if (!database.isOpen())
            return null;
        item = (MenuItem) params[1];
        if (params[2] instanceof Lyrics[])
            lyricsArray = (Lyrics[]) params[2];
        else
            for (int i = 0; i < lyricsArray.length; i++) {
                lyricsArray[i] = (Lyrics) params[i + 2];
            }
        boolean result = false;
        String[] columns = DatabaseHelper.columns;
        if (database != null && database.isOpen()) {
            database.beginTransaction();
            try {
                for (Lyrics lyrics : lyricsArray) {
                    if ("user-submission".equals(lyrics.getSource()) || "disapproved".equals(lyrics.getSource()))
                        continue;
                    Lyrics storedLyrics = DatabaseHelper.getInstance(mContext.get()).get(new String[]{lyrics.getArtist(), lyrics.getTitle(),
                            lyrics.getOriginalArtist(), lyrics.getOriginalTitle()});
                    if ((storedLyrics == null || (!storedLyrics.isLRC() && lyrics.isLRC()))
                            && !"Storage".equals(lyrics.getSource())) {
                        ContentValues values = new ContentValues(2);
                        values.put(columns[0], lyrics.getArtist());
                        values.put(columns[1], lyrics.getTitle());
                        values.put(columns[2], lyrics.getText());
                        values.put(columns[3], lyrics.getURL());
                        values.put(columns[4], lyrics.getSource());
                        if (lyrics.getCoverURL() != null && lyrics.getCoverURL().startsWith("http://"))
                            values.put(columns[5], lyrics.getCoverURL());
                        values.put(columns[6], lyrics.getOriginalArtist());
                        values.put(columns[7], lyrics.getOriginalTitle());
                        values.put(columns[8], lyrics.isLRC() ? 1 : 0);
                        values.put(columns[9], lyrics.getWriter());
                        values.put(columns[10], lyrics.getCopyright());
                        database.delete(DatabaseHelper.TABLE_NAME, String.format("%s=? AND %s=?", columns[0], columns[1]), new String[]{lyrics.getArtist(), lyrics.getTitle()});
                        if (fragment != null && fragment.get() != null && fragment.get() instanceof LyricsViewFragment)
                            ((LyricsViewFragment) fragment.get()).lyricsPresentInDB = true;
                        else if (mOverlay != null && mOverlay.get() != null)
                            mOverlay.get().lyricsPresentInDB = true;
                        database.insert(DatabaseHelper.TABLE_NAME, null, values);
                        result = true;
                    } else if (mContext != null) { // if called from activity or overlay, not service
                        database.delete(DatabaseHelper.TABLE_NAME, String.format("%s=? AND %s=?", columns[0], columns[1]), new String[]{lyrics.getArtist(), lyrics.getTitle()});
                        if (fragment != null && fragment.get() != null && fragment.get() instanceof LyricsViewFragment)
                            ((LyricsViewFragment) fragment.get()).lyricsPresentInDB = false;
                        else if (mOverlay != null && mOverlay.get() != null)
                            mOverlay.get().lyricsPresentInDB = true;
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
        if (result == null)
            return;
        int message = result ? R.string.lyrics_saved : R.string.lyrics_removed;
        if ((fragment != null && fragment.get() instanceof LyricsViewFragment) || (mOverlay != null && mOverlay.get() != null)) {
            SharedPreferences sharedPref =
                    PreferenceManager.getDefaultSharedPreferences(mContext.get());
            if (!sharedPref.getBoolean("pref_auto_save", true))
                Toast.makeText(mContext.get(), message, Toast.LENGTH_SHORT).show();
            item.setIcon(result ? R.drawable.ic_trash : R.drawable.ic_save);
            item.setTitle(result ? R.string.remove_action : R.string.save_action);
        } else if (fragment != null && fragment.get() instanceof LocalLyricsFragment && mLocalLyricsFragment != null) {
            LongSparseArray<Integer> topMap = mLocalLyricsFragment.get().collectTopPositions();
            mLocalLyricsFragment.get().addObserver(topMap);
            for (final Lyrics lyrics : lyricsArray) {
                int position = ((LocalAdapter) mLocalLyricsFragment.get().getExpandableListAdapter())
                        .getGroupPosition(lyrics.getArtist());
                if (!result)
                    ((LocalAdapter) mLocalLyricsFragment.get().getExpandableListAdapter())
                            .removeArtistFromCache(lyrics.getArtist());
                else
                    ((LocalAdapter) mLocalLyricsFragment.get().getExpandableListAdapter())
                            .addArtist(lyrics.getArtist());
                if (result && fragment.get().getView() != null && position != -1) {
                    position = ((LocalAdapter) mLocalLyricsFragment.get().getExpandableListAdapter())
                            .getGroupPosition(lyrics.getArtist());
                    mLocalLyricsFragment.get().getMegaListView().expandGroup(position);
                }
            }
            View.OnClickListener actionClickListener = snackbar -> {
                for (Lyrics lyrics : lyricsArray)
                    mLocalLyricsFragment.get().animateUndo(lyrics);
            };
            if (!result && fragment.get().getView() != null) {
                Snackbar.make(fragment.get().getActivity().findViewById(R.id.root_view), message, Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, actionClickListener)
                        .setActionTextColor(mContext.get().getResources().getColor(R.color.accent_light)).show();
            }
        }
    }
}