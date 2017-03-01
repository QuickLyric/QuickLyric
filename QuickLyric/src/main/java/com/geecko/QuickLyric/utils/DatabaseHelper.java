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

package com.geecko.QuickLyric.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.geecko.QuickLyric.lyrics.Lyrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 7;
    private static final String DATABASE_NAME = "QuickLyric";
    public static final String TABLE_NAME = "lyrics";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_TRACK = "track";
    private static final String KEY_ORIGINAL_ARTIST = "original_artist";
    private static final String KEY_ORIGINAL_TRACK = "original_track";
    private static final String KEY_LYRICS = "lyrics";
    private static final String KEY_URL = "url";
    private static final String KEY_LRC = "isLRC";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_COVER_URL = "cover";
    private static final String KEY_WRITER = "writer";
    private static final String KEY_COPYRIGHT = "copyright";
    public static final String[] columns = {KEY_ARTIST, KEY_TRACK, KEY_LYRICS, KEY_URL, KEY_SOURCE,
            KEY_COVER_URL, KEY_ORIGINAL_ARTIST, KEY_ORIGINAL_TRACK, KEY_LRC, KEY_WRITER, KEY_COPYRIGHT};
    private static final String DICTIONARY_TABLE_CREATE = "CREATE TABLE " + TABLE_NAME + " (" + KEY_ARTIST + " TINYTEXT, " + KEY_TRACK + " TINYTEXT, " + KEY_LYRICS + " TINYTEXT, " + KEY_URL + " TINYTEXT," + KEY_SOURCE + " TINYTEXT," + KEY_COVER_URL + " TINYTEXT," + KEY_ORIGINAL_ARTIST + " TINYTEXT, " + KEY_ORIGINAL_TRACK + " TINYTEXT, " + KEY_LRC + " BIT);";
    private static DatabaseHelper sInstance;
    private boolean closed = false;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3 || (oldVersion < 5 && db.query(TABLE_NAME, null, null, null, null, null, null, "1")
                .getColumnIndex(KEY_ORIGINAL_ARTIST) < 0)) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + KEY_ORIGINAL_ARTIST + " TINYTEXT;");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + KEY_ORIGINAL_TRACK + " TINYTEXT;");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + KEY_LRC + " BIT;");
        }
        if (oldVersion < 4) {
            db.execSQL("DELETE FROM "+TABLE_NAME);
        }
        if (oldVersion < 7 && db.query(TABLE_NAME, null, null, null, null, null, null, "1")
                .getColumnIndex(KEY_WRITER) < 0) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + KEY_WRITER + " TINYTEXT;");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + KEY_COPYRIGHT + " TINYTEXT;");
        }
    }

    public static List<Lyrics> search(SQLiteDatabase database, String searchQuery) {
        List<Lyrics> results;
        Object[] keywords = searchQuery.split(" ");
        String query = "";
        for (int i = 0; i < keywords.length; ++i) {
            String keyword = String.valueOf(i + 1);
            query = query + "(artist LIKE %" + keyword +
                    "$s OR track LIKE %" + keyword +
                    "$s OR original_artist LIKE %" + keyword +
                    "$s OR original_track LIKE %" + keyword + "$s) AND ";
            keywords[i] = "'%" + ((String) keywords[i]).replaceAll("'", "''") + "%'";
        }
        query = query.substring(0, query.length() - 5);

        Cursor cursor = database.query(TABLE_NAME, null, String.format(query,
                keywords), null, null, null, null);
        results = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            Lyrics l = new Lyrics(Lyrics.SEARCH_ITEM);
            l.setArtist(cursor.getString(0));
            l.setTitle(cursor.getString(1));
            l.setText(cursor.getString(2));
            l.setURL(cursor.getString(3));
            l.setSource(cursor.getString(4));
            l.setCoverURL(cursor.getString(5));
            l.setOriginalArtist(cursor.getString(6));
            l.setOriginalTitle(cursor.getString(7));
            l.setLRC(cursor.getInt(8) > 0);
            l.setWriter(cursor.getString(9));
            l.setCopyright(cursor.getString(10));
            results.add(l);
        }
        cursor.close();
        return results;
    }

    public List<Lyrics> search(String query) {
        SQLiteDatabase database = getReadableDatabase();
        return search(database, query);
    }

    public List<List> listMetadata() {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_NAME, new String[] {columns[6], columns[7]},
                null, null, null, null, null);
        ArrayList<List> output = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                output.add(Arrays.asList(cursor.getString(0), cursor.getString(1)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return output;
    }

    public Lyrics get(String[] metaData) {
        String[] args;
        if (metaData.length < 4) {
            args = new String[4];
            System.arraycopy(metaData, 0, args, 0, metaData.length);
            System.arraycopy(metaData, 0, args, 2, metaData.length);
        } else
            args = metaData;
        String[] columns = DatabaseHelper.columns;
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_NAME, null, String.format("(upper(%s) = upper(?) AND upper(%s) = upper(?)) OR (upper(%s)=upper(?) AND upper(%s) = upper(?))",
                columns[0], columns[1], columns[6], columns[7]), args, null, null, null);
        int count = cursor.getCount();
        Lyrics result = null;
        if (count > 0) {
            cursor.moveToFirst();
            result = new Lyrics(Lyrics.POSITIVE_RESULT);
            result.setArtist(cursor.getString(0));
            result.setTitle(cursor.getString(1));
            result.setText(cursor.getString(2));
            result.setURL(cursor.getString(3));
            result.setSource(cursor.getString(4));
            result.setCoverURL(cursor.getString(5));
            result.setOriginalArtist(cursor.getString(6));
            result.setOriginalTitle(cursor.getString(7));
            result.setLRC(cursor.getInt(8) > 0);
            result.setWriter(cursor.getString(9));
            result.setCopyright(cursor.getString(10));
        }
        cursor.close();
        return result;
    }

    public Lyrics[] getLyricsByArtist(String artist) {
        String[] columns = DatabaseHelper.columns;
        String order = String.format("LTRIM(Replace(%s, 'The ', '')) COLLATE NOCASE DESC,%s COLLATE NOCASE ASC", columns[0], columns[1]);
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_NAME, null, String.format("upper(%s)=upper(?) OR upper(%s)=upper(?)",
                columns[0], columns[6]), new String[]{artist, artist}, null, null, order);
        Lyrics[] results = new Lyrics[cursor.getCount()];
        cursor.moveToFirst();
        if (cursor.getCount() > 0)
            do {
                Lyrics result = new Lyrics(Lyrics.POSITIVE_RESULT);
                result.setArtist(cursor.getString(0));
                result.setTitle(cursor.getString(1));
                result.setText(cursor.getString(2));
                result.setURL(cursor.getString(3));
                result.setSource(cursor.getString(4));
                result.setCoverURL(cursor.getString(5));
                result.setOriginalArtist(cursor.getString(6));
                result.setOriginalTitle(cursor.getString(7));
                result.setLRC(cursor.getInt(8) > 0);
                result.setWriter(cursor.getString(9));
                result.setCopyright(cursor.getString(10));
                results[cursor.getPosition()] = result;
            } while (cursor.moveToNext());
        cursor.close();
        return results;
    }

    public void updateCover(String artist, String title, String coverUrl) {
        SQLiteDatabase database = getWritableDatabase();
        if (database != null) {
            database.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                values.put(KEY_COVER_URL, coverUrl);
                database.update(TABLE_NAME, values, String.format("(upper(%s)=upper(?) AND upper(%s)=upper(?) AND %s='')", KEY_ARTIST, KEY_TRACK, KEY_URL), new String[]{artist, title});
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }
    }

    private int getColumnsCount(SQLiteDatabase database) {
        return database.query(TABLE_NAME, null, null, null, null, null, null).getColumnCount();
    }

    private static boolean presenceCheck(SQLiteDatabase database, String[] metaData) {
        int count = 0;
        if (database != null) {
            database.beginTransaction();
            try {
                Cursor cursor = database.query(TABLE_NAME, null, String.format("(upper(%s)=upper(?) AND upper(%s)=upper(?)) OR (upper(%s)=upper(?) AND upper(%s)=upper(?))",
                        columns[0], columns[1], columns[6], columns[7]), metaData, null, null, null);
                count = cursor.getCount();
                cursor.close();
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }
        return (count != 0);
    }

    public boolean presenceCheck(String[] metaData) {
        SQLiteDatabase database = getReadableDatabase();
        return presenceCheck(database, metaData);
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        this.closed = true;
        super.close();
    }
}