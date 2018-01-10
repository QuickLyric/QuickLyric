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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.geecko.QuickLyric.fragment.LocalLyricsFragment;
import com.geecko.QuickLyric.utils.DatabaseHelper;

import java.lang.ref.WeakReference;
import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

public class DBContentLister extends AsyncTask<Object, Void, String[]> {
    private WeakReference<LocalLyricsFragment> localLyricsFragment;

    public DBContentLister(LocalLyricsFragment localLyricsFragment) {
        this.localLyricsFragment = new WeakReference<>(localLyricsFragment);
    }

    @Override
    protected void onPreExecute() {
        localLyricsFragment.get().setListShown(false);
    }

    @Override
    protected String[] doInBackground(Object... params) {
        if (localLyricsFragment == null || localLyricsFragment.get().getActivity() == null)
            return new String[0];
        String[] columns = new String[]{DatabaseHelper.columns[0], DatabaseHelper.columns[1]};
        String query = String.format("LTRIM(Replace(%s, 'The ', '')) COLLATE NOCASE DESC,%s COLLATE NOCASE ASC", columns[0], columns[1]);
        SQLiteDatabase database = DatabaseHelper.getInstance(localLyricsFragment.get().getActivity()).getReadableDatabase();
        if (database != null) {
            Cursor cursor = database.query(DatabaseHelper.TABLE_NAME, new String[] {DatabaseHelper.columns[0]},
                    null, null, null, null, query);
            cursor.moveToFirst();
            Collator collator = Collator.getInstance();
            collator.setStrength(Collator.PRIMARY);
            TreeSet<String> set = new TreeSet<>(collator);
            if (cursor.moveToFirst())
                do {
                    if (!set.contains(cursor.getString(0)))
                        set.add(cursor.getString(0));
                } while (!cursor.isClosed() && database.isOpen() && cursor.moveToNext());
            List<String> artists = Arrays.asList(set.toArray(new String[set.size()]));
            Collections.sort(artists, String.CASE_INSENSITIVE_ORDER);
            cursor.close();
            return (String[]) artists.toArray();
        } else
            return new String[0];
    }

    protected void onPostExecute(final String[] results) {
        localLyricsFragment.get().update(results);
    }
}
