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
import android.text.TextUtils;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Chromaprint {

    @SuppressWarnings("JniMissingFunction")
    private static native String fpCalc(String[] args);

    private static String fpcalc(String path) {
        try {
            System.loadLibrary("fpcalc");
        } catch (UnsatisfiedLinkError e) {
            Log.e("chromaprint", "Could not load library libfpcalc.so : " + e);
            return null;
        }

        String[] args = {"-length", "16", path};
        String output;
        try {
            output = fpCalc(args);
        } catch (Exception e) {
            output = null;
            Log.e("chromaprint", "Exception when executing fpcalc" + e);
        }
        return output;
    }

    public static Fingerprint getFingerprintForPath(Context context, String path) {
        String fingerprint = FingerprintDatabaseHelper.getInstance(context).getFingerprint(path);
        if (TextUtils.isEmpty(fingerprint))
            fingerprint = fpcalc(path);

        return fingerprint == null ? null : new Fingerprint(fingerprint);
    }

    public static class Fingerprint {

        private String fingerprint;
        private int duration = -1;

        public Fingerprint(String text) {
            Matcher matcher = Pattern.compile("(?s)(?i).*?DURATION=(\\d*).*FINGERPRINT=(.*?)").matcher(text);
            int groupCount = matcher.groupCount();
            if (matcher.matches()) {
                String durationStr = matcher.group(groupCount - 1);
                if (!TextUtils.isEmpty(matcher.group(groupCount)) && !TextUtils.isEmpty(durationStr)) {
                    duration = Integer.parseInt(durationStr);
                    fingerprint = matcher.group(groupCount);
                }
            }
            if (TextUtils.isEmpty(fingerprint) || duration == -1)
                new IllegalArgumentException("Can't find duration & fingerprint from following text: " + text).printStackTrace();
        }

        public int getDuration() {
            return duration;
        }

        public String getFingerprint() {
            return fingerprint;
        }

        public boolean isValid() {
            return fingerprint != null && duration != -1;
        }
    }
}
