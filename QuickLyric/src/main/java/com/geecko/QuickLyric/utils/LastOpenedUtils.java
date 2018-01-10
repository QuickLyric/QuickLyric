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

import android.content.Context;

public class LastOpenedUtils {
    private static String fileName = "last_opened";
    private static String prefName = "epoch";

    public static long getLastOpenedDate(Context context) {
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
                .getLong(prefName, System.currentTimeMillis());
    }

    public static void setLastOpenedDate(Context context) {
        context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
                .edit().putLong(prefName, System.currentTimeMillis()).apply();
    }
}
