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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.geecko.QuickLyric.R;

public class SwipeRefreshLayout extends android.support.v4.widget.SwipeRefreshLayout {
    public SwipeRefreshLayout(Context context) {
        super(context);
    }

    public SwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(23)
    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                               int dyUnconsumed) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dxConsumed, null);
        if (dyUnconsumed < 0) {
            CoordinatorLayout coordinatorLayout = (CoordinatorLayout) getParent().getParent().getParent();
            coordinatorLayout.onNestedScroll(coordinatorLayout.findViewById(R.id.toolbar_layout),
                    dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
            if (coordinatorLayout.getScrollY() == 0 && !isRefreshing())
                super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed / 2);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return !isRefreshing() && super.onTouchEvent(ev);
    }
}