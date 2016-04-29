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
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.BuildConfig;

import java.util.Locale;

public class EmailConfigGenTool {

    public static String genConfig(Context context) { //ToDo Premium
        return String.format(Locale.US, "Package version: %s\n" +
                        "Device model: %s %s\n" +
                        "API version: %s\n" +
                        "Locale: %s\n" +
                        "\n\n%s\n\n" +
                        "--- \n \n \n",
                BuildConfig.VERSION_NAME,
                Build.MANUFACTURER,
                Build.MODEL,
                Build.VERSION.SDK_INT,
                Locale.getDefault().getDisplayLanguage(),
                PreferenceManager.getDefaultSharedPreferences(context).getAll()
        );
    }
}
