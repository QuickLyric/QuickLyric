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
import android.text.TextUtils;

public class FingerprintDatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Chromaprint";
    public static final String TABLE_NAME = "fingerprints_lyrics";
    private static final String KEY_FILEPATH = "filepath";
    private static final String KEY_FINGERPRINT = "filepath";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_TRACK = "track";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_ORIGINAL_ARTIST = "original_artist";
    private static final String KEY_ORIGINAL_TRACK = "original_track";
    public static final String[] columns = {KEY_FILEPATH, KEY_FINGERPRINT, KEY_ARTIST, KEY_TRACK, KEY_DURATION,
            KEY_ORIGINAL_ARTIST, KEY_ORIGINAL_TRACK};
    private static final String DICTIONARY_TABLE_CREATE = "CREATE TABLE " + TABLE_NAME + " (" + KEY_FILEPATH + " TINYTEXT, " + KEY_ARTIST + " TINYTEXT, " + KEY_TRACK + " TINYTEXT, "
            + KEY_DURATION + " smallint, " + KEY_ORIGINAL_ARTIST + " TINYTEXT, " + KEY_ORIGINAL_TRACK + " TINYTEXT);";
    private static FingerprintDatabaseHelper sInstance;
    private boolean closed = false;

    public static synchronized FingerprintDatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new FingerprintDatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private FingerprintDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Nothing yet
    }

    public String getFingerprint(String[] metaData) {
        String[] args;
        if (metaData.length < 4) {
            args = new String[4];
            System.arraycopy(metaData, 0, args, 0, metaData.length);
            System.arraycopy(metaData, 0, args, 2, metaData.length);
        } else
            args = metaData;
        if (args[0] == null || args[1] == null)
            return null;

        String[] columns = FingerprintDatabaseHelper.columns;
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_NAME, null, String.format("(upper(%s) = upper(?) AND upper(%s) = upper(?)) OR (upper(%s)=upper(?) AND upper(%s) = upper(?))",
                columns[2], columns[3], columns[5], columns[6]), args, null, null, null);
        int count = cursor.getCount();
        String result = null;
        if (count > 0) {
            cursor.moveToFirst();
            result = cursor.getString(1);
        }
        cursor.close();
        return result;
    }

    public String getFingerprint(String path) {
        if (TextUtils.isEmpty(path))
            return null;

        String[] columns = FingerprintDatabaseHelper.columns;
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_NAME, null, String.format("(%s = (?))",
                columns[0]), new String[] {path}, null, null, null);
        int count = cursor.getCount();
        String result = null;
        if (count > 0) {
            cursor.moveToFirst();
            result = cursor.getString(0);
        }
        cursor.close();
        return result;
    }

    public void insertFingerprint(String filepath, String fingerprint, String artist, String title, int duration, String originalArtist, String originalTitle) {
        if (getFingerprint(filepath) != null)
            return;

        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_FILEPATH, filepath);
        values.put(KEY_FINGERPRINT, fingerprint);
        values.put(KEY_ARTIST, artist);
        values.put(KEY_TRACK, title);
        values.put(KEY_DURATION, duration);
        values.put(KEY_ORIGINAL_ARTIST, originalArtist);
        values.put(KEY_ORIGINAL_TRACK, originalTitle);
        database.insert(TABLE_NAME, KEY_FINGERPRINT, values);
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