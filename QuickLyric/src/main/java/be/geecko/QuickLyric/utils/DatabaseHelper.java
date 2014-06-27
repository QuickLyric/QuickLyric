package be.geecko.QuickLyric.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import be.geecko.QuickLyric.lyrics.Lyrics;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "QuickLyric";
    private static final String TABLE_NAME = "lyrics";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_TRACK = "track";
    private static final String KEY_LYRICS = "lyrics";
    private static final String KEY_URL = "url";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_COVER_URL = "cover";
    public static final String[] columns = {KEY_ARTIST, KEY_TRACK, KEY_LYRICS, KEY_URL, KEY_SOURCE, KEY_COVER_URL};
    private static final String DICTIONARY_TABLE_CREATE = "CREATE TABLE " + TABLE_NAME + " (" + KEY_ARTIST + " TINYTEXT, " + KEY_TRACK + " TINYTEXT, " + KEY_LYRICS + " TINYTEXT, " + KEY_URL + " TINYTEXT," + KEY_SOURCE + " TINYTEXT," + KEY_COVER_URL + " TINYTEXT);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public static Lyrics get(SQLiteDatabase database, String[] metaData) {
        String[] columns = DatabaseHelper.columns;
        Cursor cursor = database.query(TABLE_NAME, null, String.format("%s=? AND %s=?", columns[0], columns[1]), metaData, null, null, null);
        int count = cursor.getCount();
        if (count > 0) {
            cursor.moveToFirst();
            Lyrics l = new Lyrics(Lyrics.POSITIVE_RESULT);
            l.setArtist(cursor.getString(0));
            l.setTitle(cursor.getString(1));
            l.setText(cursor.getString(2));
            l.setURL(cursor.getString(3));
            l.setSource(cursor.getString(4));
            l.setCoverURL(cursor.getString(5));
            return l;
        } else
            return null;
    }

    public static boolean presenceCheck(SQLiteDatabase database, String[] metaData) {
        String[] columns = DatabaseHelper.columns;
        Cursor cursor = database.query(TABLE_NAME, null, String.format("%s=? AND %s=?", columns[0], columns[1]), metaData, null, null, null);
        int count = cursor.getCount();
        return (count != 0);
    }
}