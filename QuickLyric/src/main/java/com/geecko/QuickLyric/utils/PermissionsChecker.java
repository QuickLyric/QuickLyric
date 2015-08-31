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

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

public class PermissionsChecker {

    public static boolean requestPermission(Activity activity, String permission, int rationale, int requestCode) {
        if (hasPermission(activity, permission))
            return true;
        if (activity.shouldShowRequestPermissionRationale(permission) && rationale != 0)
            Toast.makeText(activity, rationale, Toast.LENGTH_LONG).show();

        activity.requestPermissions(new String[]{permission}, requestCode);
        return false;
    }

    public static boolean hasPermission(Activity activity, String permission) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
}
