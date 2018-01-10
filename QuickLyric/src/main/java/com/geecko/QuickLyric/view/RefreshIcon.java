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
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;

import com.geecko.QuickLyric.utils.RefreshButtonBehavior;

public class RefreshIcon extends FloatingActionButton {
    public RefreshIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void show() {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) getLayoutParams();
        RefreshButtonBehavior behavior = (RefreshButtonBehavior) params.getBehavior();
        if (behavior != null && isEnabled())
            behavior.animateIn(this);
    }

    public void hide() {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) getLayoutParams();
        RefreshButtonBehavior behavior = (RefreshButtonBehavior) params.getBehavior();
        if (behavior != null)
            behavior.animateOut(this);
    }
}
