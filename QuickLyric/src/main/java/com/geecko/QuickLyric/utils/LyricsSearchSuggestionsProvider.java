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

import android.app.SearchManager;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;

import com.geecko.QuickLyric.BuildConfig;

public class LyricsSearchSuggestionsProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "QuickLyric.LyricsSearchSuggestionsProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;
    private final String mIconUri; // a drawable ID as a String will also do!

    public LyricsSearchSuggestionsProvider() {
        setupSuggestions(AUTHORITY, MODE);
        mIconUri = "android.resource://" + BuildConfig.APPLICATION_ID + "/drawable/ic_history";
    }

    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        class Wrapper extends CursorWrapper {
            Wrapper(Cursor c) {
                super(c);
            }

            public String getString(int columnIndex) {
                if (columnIndex != -1 && columnIndex == getColumnIndex(SearchManager.SUGGEST_COLUMN_ICON_1))
                    return mIconUri;
                return super.getString(columnIndex);
            }
        }

        Cursor output = new Wrapper(super.query(uri, projection, selection, selectionArgs, sortOrder));
        if (output.moveToLast() && selectionArgs[0].equals(output.getString(2)))
            return null;
        return output;
    }
}