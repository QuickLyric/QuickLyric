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


public class RatingUtils {

    private static String preferencesName = "MainActivity";
    private static long sLastFailTime = 0L;

    public static void trackSuccess(Context context) {
        int successCount = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getInt("success", 0);
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().putInt("success", ++successCount).apply();
    }

    public static void trackFail(Context context) {
        int failCount = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getInt("fail", 0);
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().putInt("fail", ++failCount).apply();
        RatingUtils.sLastFailTime = System.currentTimeMillis();
    }

    public static void trackFailedToRefresh(Context context) {
        int failedRefreshCount = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getInt("refresh_fail", 0);
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().putInt("refresh_fail", ++failedRefreshCount).apply();
        RatingUtils.sLastFailTime = System.currentTimeMillis();
    }

    public static boolean isGenerallySuccessful(Context context) {
        int successCount = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getInt("success", 0);
        int failCount = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getInt("fail", 0);

        // User is happy if success rate is > 70%
        // Don't show if last fail was less than 300 seconds ago
        return successCount / 2.33 >= failCount || System.currentTimeMillis() - sLastFailTime > 300000L;
    }

    public static boolean shouldPromptFeedback(Context context) {
        int failedRefreshCount = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getInt("refresh_fail", 0);
        int successCount = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getInt("success", 0);
        int failCount = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getInt("fail", 0);
        int daysSinceInstall = RetentionUtils.recordTimeDifference(context);
        return OnlineAccessVerifier.check(context) && daysSinceInstall < 7 && successCount + failCount > 4 && (!isGenerallySuccessful(context) || failedRefreshCount > successCount * 1.5f);
    }

    public static boolean shouldPromptGoodXP(Context context) {
        int failedRefreshCount = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getInt("refresh_fail", 0);
        int successCount = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getInt("success", 0);
        int failCount = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getInt("fail", 0);
        int daysSinceInstall = RetentionUtils.recordTimeDifference(context);
        return OnlineAccessVerifier.check(context) && daysSinceInstall > 1 && successCount + failCount > 6 && isGenerallySuccessful(context) && failedRefreshCount < successCount * 1.5f;
    }
}
