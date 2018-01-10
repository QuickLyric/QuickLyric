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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.Toast;

import com.geecko.QuickLyric.services.LyricsOverlayService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PermissionsChecker {

    @TargetApi(23)
    public static boolean requestPermission(Activity activity, String permission, int rationale, int requestCode) {
        if (shouldNotRequestPermission(activity, permission, rationale))
            return true;
        activity.requestPermissions(new String[]{permission}, requestCode);
        Bundle bundle = new Bundle();
        bundle.putString("permission", permission);
        bundle.putBoolean("never_ask", PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("never_ask_checked", false));
        return false;
    }

    @TargetApi(23)
    public static boolean requestPermission(Fragment fragment, String permission, int rationale, int requestCode) {
        if (shouldNotRequestPermission(fragment.getActivity(), permission, rationale))
            return true;
        fragment.requestPermissions(new String[]{permission}, requestCode);
        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static boolean shouldNotRequestPermission(Activity activity, String permission, int rationale) {
        if (hasPermission(activity, permission))
            return true;
        if (LyricsOverlayService.isRunning())
            LyricsOverlayService.removeCustomFloatingView(activity);
        if (activity.shouldShowRequestPermissionRationale(permission) && rationale != 0)
            Toast.makeText(activity, rationale, Toast.LENGTH_LONG).show();
        return false;
    }

    @TargetApi(23)
    public static boolean hasPermission(Context context, String permission) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || context.getPackageManager().checkPermission(permission, context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
    }

    public static void displayMIUIPopupPermission(Context context) {
        if (isMIUI()) {
            try {
                // MIUI 8
                Intent localIntent = new Intent("miui.intent.action.APP_PERM_EDITOR");
                localIntent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity");
                localIntent.putExtra("extra_pkgname", context.getPackageName());
                context.startActivity(localIntent);
            } catch (Exception e) {
                try {
                    // MIUI 5/6/7
                    Intent localIntent = new Intent("miui.intent.action.APP_PERM_EDITOR");
                    localIntent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
                    localIntent.putExtra("extra_pkgname", context.getPackageName());
                    context.startActivity(localIntent);
                } catch (Exception e1) {
                    // Otherwise jump to application details
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                    intent.setData(uri);
                    context.startActivity(intent);
                }
            }
        }
    }

    private static boolean isMIUI() {
        if (Build.MANUFACTURER.equals("Xiaomi")) {
            try {
                Properties prop = new Properties();
                prop.load(new FileInputStream(new File(Environment.getRootDirectory(), "build.prop")));
                return prop.getProperty("ro.miui.ui.version.code", null) != null
                        || prop.getProperty("ro.miui.ui.version.name", null) != null
                        || prop.getProperty("ro.miui.internal.storage", null) != null;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return false;
    }
}
