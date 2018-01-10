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
import android.content.pm.PackageManager;

public class RomanizeUtil {

    public static boolean detectIdeographic(String s) {
        if (s != null)
            for (int i = 0; i < s.length(); ) {
                int codepoint = s.codePointAt(i);
                i += Character.charCount(codepoint);
                if (isIdeographic(codepoint)) {
                    return true;
                }
            }
        return false;
    }

    private static boolean isIdeographic(int codepoint) {
            return isCJK(codepoint) || isJapanese(codepoint) || isKoreanHangul(codepoint);
    }

    private static boolean isCJK(int codepoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codepoint);
        return (Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(block) ||
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A.equals(block) ||
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B.equals(block) ||
                Character.UnicodeBlock.CJK_COMPATIBILITY.equals(block) ||
                Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS.equals(block) ||
                Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS.equals(block) ||
                Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT.equals(block) ||
                Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT.equals(block) ||
                Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION.equals(block) ||
                Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS.equals(block) ||
                Character.UnicodeBlock.KANGXI_RADICALS.equals(block) ||
                Character.UnicodeBlock.IDEOGRAPHIC_DESCRIPTION_CHARACTERS.equals(block));
    }

    private static boolean isJapanese(int codepoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codepoint);
        return (Character.UnicodeBlock.HIRAGANA.equals(block) ||
                Character.UnicodeBlock.KATAKANA.equals(block) ||
                Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS.equals(block));
    }

    private static boolean isKoreanHangul(int codepoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codepoint);
        return (Character.UnicodeBlock.HANGUL_JAMO.equals(block) ||
                Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO.equals(block) ||
                Character.UnicodeBlock.HANGUL_SYLLABLES.equals(block));
    }

    public static boolean isRomanizerInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo("com.quicklyric.romanizer", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return false;
    }
}
