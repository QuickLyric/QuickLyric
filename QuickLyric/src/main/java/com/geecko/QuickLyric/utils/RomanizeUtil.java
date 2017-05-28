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

import android.os.Build;

import org.atilika.kuromoji.Token;
import org.atilika.kuromoji.Tokenizer;

import java.util.List;

import me.xuender.unidecode.Unidecode;

public class RomanizeUtil {

    public static String romanize(String s) {
        if (containsJapanese(s)) {
            List<Token> list = Tokenizer.builder().build().tokenize(s);
            StringBuilder builder = new StringBuilder();
            for (Token token : list) {
                builder.append(token.isKnown() ? token.getReading() : token.getSurfaceForm());
                if (!(token.getSurfaceForm().equals("<") || token.getSurfaceForm().equals("br") || token.getSurfaceForm().equals(">")))
                    builder.append(" ");
            }
            s = builder.toString().trim();
            s = s.replaceAll("\\s+!", "!").replaceAll("\\s+\\?", "?").replaceAll("\\s+:", ":");
        }
        return addSpacesBeforeUppercase(Unidecode.decode(s));
    }

    public static boolean detectIdeographic(String s) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            return Character.isIdeographic(codepoint);
        else {
            return isCJK(codepoint) || isJapanese(codepoint) || isKoreanHangul(codepoint);
        }
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

    private static boolean containsJapanese(String str) {
        for (int i = 0; i < str.toCharArray().length; i++) {
            if (isJapanese(str.codePointAt(i)))
                return true;
        }
        return false;
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

    private static String addSpacesBeforeUppercase(String s) {
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            int codepoint = Character.codePointAt(s, i);
            if (Character.isUpperCase(c) && !Character.isSpaceChar(s.charAt(i - Character.charCount(codepoint)))) {
                s = s.substring(0, i) + ' ' + s.substring(i, s.length());
                i++;
            }
        }
        return s;
    }
}
