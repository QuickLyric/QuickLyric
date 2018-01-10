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

public class RetentionUtils {

    private static String prefName = "retention";
    private static String firstOpen = "first_open";
    private static String differenceString = "time_difference";

    public static long recordFirstOpen(Context context) {
        long epoch = System.currentTimeMillis();
        if (!context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                .getAll().containsKey(firstOpen))
            context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    .edit().putLong(firstOpen, epoch).apply();
        return epoch;
    }

    public static int recordTimeDifference(Context context) {
        int daysDifference = getDaysSinceFirstOpen(context);
        int weeksDifference = (daysDifference - (daysDifference % 7)) / 7;

        if (daysDifference > 0) {
            String difference = daysDifference + "_days_since_install";

            String recordedDifference = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    .getString(differenceString, "");

            if (!recordedDifference.equals(difference)) {
                context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                        .edit().putString(differenceString, difference).apply();
            }
        }

        return daysDifference;
    }

    public static int getDaysSinceFirstOpen(Context context) {
        long oldEpoch = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                .getLong(firstOpen, recordFirstOpen(context)) / 1000L / 3600L / 24L;
        long epoch = System.currentTimeMillis() / 1000L / 3600L / 24L;

        int daysDifference = (int) (epoch - oldEpoch);
        return daysDifference;
    }
}
