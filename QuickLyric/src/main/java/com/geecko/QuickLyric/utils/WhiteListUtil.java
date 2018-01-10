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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class WhiteListUtil {
    public static boolean openBootSpecialMenu(Context context) {
        try {
            if ("Letv".equalsIgnoreCase(Build.BRAND)) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"));
                context.startActivity(intent);
            } else {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
                context.startActivity(intent);
            }
        } catch (Exception e) {
            try {
                context.startActivity(new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")));
            } catch (Exception e2) {
                return false;
            }
        }
        return true;
    }
}
