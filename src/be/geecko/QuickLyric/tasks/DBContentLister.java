package be.geecko.QuickLyric.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import java.util.ArrayList;

import be.geecko.QuickLyric.MainActivity;
import be.geecko.QuickLyric.fragment.LocalLyricsFragment;
import be.geecko.QuickLyric.lyrics.Lyrics;
import be.geecko.QuickLyric.utils.DatabaseHelper;

public class DBContentLister extends AsyncTask<Object, Void, ArrayList<Lyrics>> {
    private LocalLyricsFragment localLyricsFragment;

    @Override
    protected ArrayList<Lyrics> doInBackground(Object... params) {
        localLyricsFragment = (LocalLyricsFragment) params[0];
        SharedPreferences sharedPreferences = localLyricsFragment.getActivity().getSharedPreferences("local_sort_order", Context.MODE_PRIVATE);
        int orderColumn = sharedPreferences.getInt("mode", 0);
        boolean descending;
        String[] columns;
        switch (orderColumn) {
            default:
                descending = (sharedPreferences.getInt("order_artist", 1) == 1);
                columns = new String[]{DatabaseHelper.columns[0],DatabaseHelper.columns[1]};
                break;
            case 1:
                descending = (sharedPreferences.getInt("order_title", 1) == 1);
                columns = new String[]{DatabaseHelper.columns[1],DatabaseHelper.columns[0]};
                break;
        }
        String orderBy = String.format("LTRIM(Replace(%s, 'The ', '')) %s,%s ASC", columns[0], (descending ? "DESC" : "ASC"), columns[1]);
        SQLiteDatabase database = ((MainActivity) localLyricsFragment.getActivity()).database;
        if (database != null) {
            Cursor cursor = database.query("lyrics", null, null, null, null, null, orderBy);
            cursor.moveToFirst();
            ArrayList<Lyrics> results = new ArrayList<>(cursor.getCount());
            if (cursor.moveToFirst())
                do {
                    Lyrics l = new Lyrics(Lyrics.POSITIVE_RESULT);
                    l.setArtist(cursor.getString(0));
                    l.setTitle(cursor.getString(1));
                    l.setText(cursor.getString(2));
                    l.setURL(cursor.getString(3));
                    l.setSource(cursor.getString(4));
                    l.setCoverURL(cursor.getString(5));
                    results.add(cursor.getPosition(), l);
                } while (cursor.moveToNext());
            cursor.close();
            return results;
        } else
            return new ArrayList<>(0);
    }

    protected void onPostExecute(final ArrayList<Lyrics> results) {
        if (!results.equals(localLyricsFragment.lyricsArray) || results.size() == 0)
            localLyricsFragment.update(results);
    }
}
