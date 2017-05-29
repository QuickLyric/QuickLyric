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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.cgollner.unclouded.preferences.SwitchPreferenceCompat;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.utils.play.Premium;

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
        if (Premium.isPremium(getContext()))
            super.onClick();
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View view = super.getView(convertView, parent);
        if (!Premium.isPremium(getContext())) {
            ImageView imageView = new ImageView(getContext());
            imageView.setImageResource(R.drawable.ic_unlock_premium);
            int dp = (int) getContext().getResources().getDimension(R.dimen.dp);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams
                    (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.gravity = Gravity.END;
            layoutParams.setMargins(15 * dp, 3 * dp, 15 * dp, 3 * dp);
            imageView.setLayoutParams(layoutParams);
            ((ViewGroup) view).removeViewAt(2);
            ((ViewGroup) view).addView(imageView);
            view.setAlpha(0.6f);
        }
        return view;
    }
}
