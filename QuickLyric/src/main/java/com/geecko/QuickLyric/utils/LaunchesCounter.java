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
import android.os.Bundle;


public class LaunchesCounter {

    private static String prefName = "total_launches";

    public static int getLaunchCount(Context context) {
        return context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getInt(prefName, 0);
    }

    public static void increaseLaunchCount(Context context, boolean floating) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("floating", floating);
        context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).edit()
                .putInt(prefName, getLaunchCount(context) + 1).apply();
    }
}
