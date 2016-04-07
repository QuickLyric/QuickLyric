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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.fragment.LocalLyricsFragment;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.utils.DatabaseHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class DBContentLister extends AsyncTask<Object, Void, ArrayList<ArrayList<Lyrics>>> {
    private LocalLyricsFragment localLyricsFragment;

    public DBContentLister(LocalLyricsFragment localLyricsFragment) {
        this.localLyricsFragment = localLyricsFragment;
    }

    @Override
    protected void onPreExecute() {
        localLyricsFragment.setListShown(false);
    }

    @Override
    protected ArrayList<ArrayList<Lyrics>> doInBackground(Object... params) {
        if (localLyricsFragment == null || localLyricsFragment.getActivity() == null)
            return new ArrayList<>(0);
        SharedPreferences sharedPreferences = localLyricsFragment.getActivity().getSharedPreferences("local_sort_order", Context.MODE_PRIVATE);
        int orderColumn = sharedPreferences.getInt("mode", 0);
        boolean descending;
        String[] columns;
        switch (orderColumn) {
            default:
                descending = (sharedPreferences.getInt("order_artist", 1) == 1);
                columns = new String[]{DatabaseHelper.columns[0], DatabaseHelper.columns[1]};
                break;
            case 1:
                descending = (sharedPreferences.getInt("order_title", 1) == 1);
                columns = new String[]{DatabaseHelper.columns[1], DatabaseHelper.columns[0]};
                break;
        }
        String query = String.format("LTRIM(Replace(%s, 'The ', '')) COLLATE NOCASE %s,%s COLLATE NOCASE ASC", columns[0], (descending ? "DESC" : "ASC"), columns[1]);
        SQLiteDatabase database = ((MainActivity) localLyricsFragment.getActivity()).database;
        if (database != null) {
            Cursor cursor = database.query(DatabaseHelper.TABLE_NAME, null, null, null, null, null, query);
            cursor.moveToFirst();
            ArrayList<ArrayList<Lyrics>> results = new ArrayList<>(cursor.getCount());
            HashMap<String, ArrayList<Lyrics>> map = new HashMap<>();
            if (cursor.moveToFirst())
                do {
                    Lyrics l = new Lyrics(Lyrics.POSITIVE_RESULT);
                    l.setArtist(cursor.getString(0));
                    l.setTitle(cursor.getString(1));
                    l.setText(cursor.getString(2));
                    l.setURL(cursor.getString(3));
                    l.setSource(cursor.getString(4));
                    l.setCoverURL(cursor.getString(5));
                    l.setOriginalArtist(cursor.getString(6));
                    l.setOriginalTitle(cursor.getString(7));
                    l.setLRC(cursor.getInt(8) == 1);
                    if (map.get(l.getArtist()) == null)
                        map.put(l.getArtist(), new ArrayList<Lyrics>());
                    ArrayList<Lyrics> artistSubGroup = map.get(l.getArtist());
                    artistSubGroup.add(l);
                } while (!cursor.isClosed() && database.isOpen() && cursor.moveToNext());
            ArrayList<String> keys = new ArrayList<>(map.keySet());
            Collections.sort(keys);
            for (String key : keys)
                results.add(map.get(key));
            cursor.close();
            return results;
        } else
            return new ArrayList<>(0);
    }

    protected void onPostExecute(final ArrayList<ArrayList<Lyrics>> results) {
            localLyricsFragment.update(results);
    }
}
