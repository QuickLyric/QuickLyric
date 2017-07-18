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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.geecko.QuickLyric.R;

import java.util.Hashtable;

public class LyricsTextFactory implements ViewSwitcher.ViewFactory {

    private final Context mContext;

    public LyricsTextFactory(Context context) {
        this.mContext = context;
    }

    @Override
    public View makeView() {
        TextView t = new TextView(mContext);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pref_opendyslexic", false))
            t.setTypeface(FontCache.get("opendyslexic", mContext));
        else
            t.setTypeface(FontCache.get("light", mContext));
        TypedValue colorValue = new TypedValue();
        mContext.getTheme().resolveAttribute(android.R.attr.textColorPrimary, colorValue, true);
        t.setTextColor(Color.BLACK);
        t.setLineSpacing(mContext.getResources().getDimensionPixelSize(R.dimen.line_spacing), 1);
        setSelectable(t);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, mContext.getResources().getDimension(R.dimen.txt_size));
        return t;
    }

    @SuppressLint("newAPI")
    public void setSelectable(TextView t) {
        t.setTextIsSelectable(true);
    }

    public static class FontCache {

        private static Hashtable<String, Typeface> fontCache = new Hashtable<>();

        public static Typeface get(String name, Context context) {
            Typeface tf = fontCache.get(name);
            if (tf == null) {
                try {
                    if (name.contains("dyslexic"))
                        tf = Typeface.createFromAsset(context.getAssets(), "fonts/opendyslexic.otf");
                    else if (name.equals("bold"))
                        tf = Typeface.create("sans-serif", Typeface.BOLD);
                    else if (name.equals("light"))
                        tf = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Light.ttf");
                    else if (name.equals("medium"))
                        tf = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Medium.ttf");
                    else
                        tf = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Regular.ttf");
                } catch (Exception e) {
                    return null;
                }
                fontCache.put(name, tf);
            }
            return tf;
        }
    }
}
