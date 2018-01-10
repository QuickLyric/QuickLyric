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

package com.geecko.QuickLyric.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.cgollner.unclouded.preferences.SwitchPreferenceCompat;

public class NightSwitchPreference extends SwitchPreferenceCompat {
    public NightSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NightSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NightSwitchPreference(Context context) {
        super(context);
    }

    @Override
    protected void onClick() {
        super.onClick();
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View view = super.getView(convertView, parent);

        boolean containsCoin = false;
        for (int i = 0; !containsCoin && i < ((ViewGroup) view).getChildCount(); i++) {
            View child = ((ViewGroup) view).getChildAt(i);
            if (child instanceof ImageView && "coin".equals(child.getTag()))
                containsCoin = true;
        }

        return view;
    }
}
