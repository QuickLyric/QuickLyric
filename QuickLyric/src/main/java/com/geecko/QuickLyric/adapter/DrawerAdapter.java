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

package com.geecko.QuickLyric.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.geecko.QuickLyric.R;

public class DrawerAdapter extends ArrayAdapter<String> {

    private final String[] stringArray;
    private final Drawable[] drawableArray;
    private int selectedItem;

    public DrawerAdapter(Context context, String[] strings) {
        super(context, R.layout.drawer_row, strings);
        this.stringArray = strings;
        Drawable drawable1 = context.getResources().getDrawable(R.drawable.ic_lyrics);
        Drawable drawable2 = context.getResources().getDrawable(R.drawable.ic_timer);
        Drawable drawable3 = context.getResources().getDrawable(R.drawable.ic_menu_storage);
        Drawable drawable4 = context.getResources().getDrawable(R.drawable.ic_menu_settings);
        Drawable drawable5 = context.getResources().getDrawable(R.drawable.ic_send_feedback);
        Drawable drawable6 = context.getResources().getDrawable(R.drawable.ic_info);
        this.drawableArray = new Drawable[]{drawable1, drawable2, drawable3, null, drawable4, drawable5, drawable6};
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (position == 3) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.drawer_separator, parent, false);
                convertView.setId(position);
            }
        } else {
            if (convertView == null || convertView.getId() != position) {
                convertView = inflater.inflate(R.layout.drawer_row, parent, false);
                if (convertView != null) {
                    TextView textView = (TextView) convertView;
                    convertView.setId(position);
                    if (position == 5)
                        textView.setText("");
                    else {
                        textView.setText(stringArray[position]);
                        textView.setCompoundDrawablesWithIntrinsicBounds
                                (drawableArray[position], null, null, null);
                    }
                }
            }
            if (convertView != null) {
                TextView textView = (TextView) convertView;
                Typeface roboto = Typeface
                        .createFromAsset(getContext().getAssets(), "fonts/Roboto-Medium.ttf");
                textView.setTypeface(roboto);
                if (position == selectedItem) {
                    TypedValue typedValue = new TypedValue();
                    Resources.Theme theme = getContext().getTheme();
                    theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true);
                    int primaryDark = typedValue.data;
                    ((ListView) parent).setSelectionFromTop(position, convertView.getTop());
                    textView.setTextColor(primaryDark);
                    if (textView.getCompoundDrawables()[0] != null)
                        textView.getCompoundDrawables()[0].setColorFilter(
                                primaryDark,
                                PorterDuff.Mode.SRC_IN);
                    TypedValue backgroundValue = new TypedValue();
                    getContext().getTheme().resolveAttribute(android.R.attr.galleryItemBackground, backgroundValue, true);
                    convertView.setBackgroundColor(backgroundValue.data);
                } else {
                    TypedValue colorValue = new TypedValue();
                    getContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, colorValue, true);
                    textView.setTextColor(colorValue.data);
                    convertView.setBackgroundColor(Color.TRANSPARENT);
                    if (textView.getCompoundDrawables()[0] != null)
                        textView.getCompoundDrawables()[0].clearColorFilter();
                }
            } else
                return null;
        }
        return convertView;
    }

    public void setSelectedItem(int position) {
        selectedItem = position;
    }

    public int getSelectedItem() {
        return selectedItem;
    }

}
