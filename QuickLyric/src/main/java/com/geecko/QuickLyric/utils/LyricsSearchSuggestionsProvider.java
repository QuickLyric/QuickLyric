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

package com.geecko.QuickLyric.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LyricsSearchSuggestionsProvider extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_NAME = "QuickLyric_searches";
    private static final String TABLE_NAME = "search_suggestions";
    private static final String KEY_SUGGESTION = "suggestion";
    private static final String KEY_DATE = "access_date";
    private static final String DICTIONARY_TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" + KEY_SUGGESTION + " TINYTEXT NOT NULL PRIMARY KEY," + KEY_DATE + " INTEGER);";
    private static LyricsSearchSuggestionsProvider sInstance;

    private LyricsSearchSuggestionsProvider(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized LyricsSearchSuggestionsProvider getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new LyricsSearchSuggestionsProvider(context.getApplicationContext());
        }
        return sInstance;
    }

    public String[] getHistory() {
        String[] results;
        String query = "";
        query = query + KEY_DATE + " > 0 ORDER BY "+ KEY_DATE + " DESC";

        SQLiteDatabase database = getReadableDatabase();

        Cursor cursor = database.query(TABLE_NAME, null, query, null, null, null, null);
        results = new String[cursor.getCount()];
        while (cursor.moveToNext()) {
            results[cursor.getPosition()] = cursor.getString(0);
        }
        cursor.close();
        database.close();
        return results;
    }

    public String[] search(String searchQuery) {
        if (searchQuery == null || searchQuery.isEmpty())
            return new String[] {};

        String[] results;
        String query = "";
        query = query + "suggestion LIKE '" + searchQuery + "%' ORDER BY "+ KEY_DATE + " DESC";

        Cursor cursor = getReadableDatabase().query(TABLE_NAME, null, query, null, null, null, null);
        results = new String[cursor.getCount()];
        while (cursor.moveToNext()) {
            results[cursor.getPosition()] = cursor.getString(0);
        }
        cursor.close();
        return results;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE "+TABLE_NAME+";");
        onCreate(db);
    }

    public void saveQuery(String searchQuery) {
        ContentValues values = new ContentValues(2);
        values.put(KEY_SUGGESTION, searchQuery);
        values.put(KEY_DATE, System.currentTimeMillis());

        SQLiteDatabase database = getWritableDatabase();
        database.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void deleteQuery(String suggestion) {
        SQLiteDatabase database = getWritableDatabase();
        database.delete(TABLE_NAME, KEY_SUGGESTION+ "='" + suggestion.replaceAll("'", "''") + "';", null);
    }
}