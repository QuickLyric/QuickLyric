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
import android.util.TypedValue;

import com.geecko.QuickLyric.R;

public class ColorUtils {

    public static int getAccentColorResource(Context context) {
        TypedValue accentColor = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorAccent, accentColor, true);
        return accentColor.resourceId;
    }

    public static int getAccentColor(Context context) {
        TypedValue accentColor = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorAccent, accentColor, true);
        return accentColor.data;
    }

    public static int getPrimaryColorResource(Context context) {
        TypedValue primaryColor = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorPrimary, primaryColor, true);
        return primaryColor.resourceId;
    }

    public static int getPrimaryColor(Context context) {
        TypedValue primaryColor = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorPrimary, primaryColor, true);
        return primaryColor.data;
    }

    public static int getDarkPrimaryColor(Context context) {
        TypedValue primaryColor = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorPrimaryDark, primaryColor, true);
        return primaryColor.data;
    }
}
