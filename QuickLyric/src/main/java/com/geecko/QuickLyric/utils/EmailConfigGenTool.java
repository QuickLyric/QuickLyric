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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArraySet;

import com.geecko.QuickLyric.BuildConfig;
import com.geecko.QuickLyric.services.NotificationListenerService;
import com.geecko.QuickLyric.tasks.DownloadThread;

import java.util.Arrays;
import java.util.Locale;

public class EmailConfigGenTool {

    public static String genConfig(Context context) {
        SharedPreferences currentMusicPrefs = context.getSharedPreferences("current_music", Context.MODE_PRIVATE);
        return String.format(Locale.US, "Package version: %s %d\r\n" +
                        "Device model: %s %s\r\n" +
                        "API version: %s\r\n" +
                        "Locale: %s\r\n" +
                        "Premium: %s\r\n" +
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? "Notification Listener: %s %s %s \r\n" : "%s%s%s\r\n") +
                        (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT ? "Players used: %s\r\n" : "%s\r\n") +
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? "Storage permission granted: %s\r\n" : "%s\r\n") +
                        "Latest Track: %s - %s\r\n" +
                        "Latest Result: %s\r\n" +
                        "\r\n\r\n%s\r\n\r\n" +
                        "---------------------\r\n\r\nInsert your message here:\r\n",
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                Build.MANUFACTURER,
                Build.MODEL,
                Build.VERSION.SDK_INT,
                Locale.getDefault().getDisplayLanguage(),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
                        NotificationListenerService.isListeningAuthorized(context) ? "Authorized" : "Not Authorized" : "",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
                        NotificationListenerService.isNotificationListenerServiceEnabled(context) ? "Enabled" : "Disabled" : "",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
                        NotificationListenerService.isAppScrobbling(context) ? "Running" : "Not Running" : "",
                Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT ?
                        Arrays.toString(context.getSharedPreferences("NotificationListenerService", Context.MODE_PRIVATE)
                                .getStringSet("players_used", new ArraySet<>()).toArray()) : "",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) : "",
                currentMusicPrefs.getString("artist", null),
                currentMusicPrefs.getString("track", null),
                DownloadThread.getStoredResult(context),
                PreferenceManager.getDefaultSharedPreferences(context).getAll()
        );
    }
}
