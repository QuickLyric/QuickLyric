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
import android.content.SharedPreferences;

import java.util.Calendar;

/**
 * This file is part of QuickLyric
 * Created by geecko on 11/03/15.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class NightTimeVerifier {

    public static boolean check(Context context) {
        Calendar c = Calendar.getInstance();
        int currentHour = c.get(Calendar.HOUR_OF_DAY);
        int currentMin = c.get(Calendar.MINUTE);

        SharedPreferences pref = context.getSharedPreferences("night_time", Context.MODE_PRIVATE);
        int startHour = pref.getInt("startHour", 42);
        int startMinute = pref.getInt("startMinute", 0);
        int endHour = pref.getInt("endHour", 0);
        int endMinute = pref.getInt("endMinute", 0);

        boolean beforeEnd = currentHour < endHour || (currentHour == endHour && currentMin < endMinute);
        boolean afterStart = currentHour > startHour || (currentHour == startHour && currentMin >= startMinute);

        return beforeEnd || afterStart;
    }

}
