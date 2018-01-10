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

import android.content.res.Resources;
import android.util.SparseArray;

import com.geecko.QuickLyric.BuildConfig;
import com.geecko.QuickLyric.R;

public class ChangelogStringBuilder {

    public static String getChangelog(Resources resources, int versionCode) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] changes = resources.getStringArray(R.array.changelog);
        SparseArray<String> map = new SparseArray<>();
        int max = versionCode;
        for (String change : changes) {
            String[] parts = change.split("§");
            int version = Integer.valueOf(parts[0]);
            map.append(version, parts[1]);
            if (version > max)
                max = version;
        }
        for (int i = max; i > versionCode; i--) {
            String change = map.get(i);
            if (change == null)
                continue;
            stringBuilder.append("<b>Update ");
            if (i == BuildConfig.VERSION_CODE)
                stringBuilder.append(BuildConfig.VERSION_NAME);
            else
                stringBuilder.append(i);
            stringBuilder.append(":</b><br/>");
            stringBuilder.append(change);
            stringBuilder.append("<br/>");
        }
        return stringBuilder.toString();
    }
}
