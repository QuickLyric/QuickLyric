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

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;

public class RefreshButtonBehavior extends FloatingActionButton.Behavior {

    private static final android.view.animation.Interpolator INTERPOLATOR =
            new FastOutSlowInInterpolator();
    private boolean mIsAnimatingOut = false;
    public static boolean visible = true;

    public RefreshButtonBehavior(Context context, AttributeSet attrs) {
        super();
    }

    // Same animation that FloatingActionButton.Behavior uses to
    // hide the FAB when the AppBarLayout exits
    public void animateOut(final FloatingActionButton button) {
        int translationY = button.getHeight() + ((CoordinatorLayout.LayoutParams) button.getLayoutParams()).bottomMargin;
        ViewCompat.animate(button).translationY(translationY)
                .setInterpolator(INTERPOLATOR).withLayer()
                .setListener(new ViewPropertyAnimatorListener() {
                    public void onAnimationStart(View view) {
                        RefreshButtonBehavior.this.mIsAnimatingOut = true;
                    }

                    public void onAnimationCancel(View view) {
                        RefreshButtonBehavior.this.mIsAnimatingOut = false;
                    }

                    public void onAnimationEnd(View view) {
                        RefreshButtonBehavior.this.mIsAnimatingOut = false;
                        view.setVisibility(View.INVISIBLE);
                        visible = false;
                    }
                }).start();
    }

    // Same animation that FloatingActionButton.Behavior
    // uses to show the FAB when the AppBarLayout enters
    public void animateIn(FloatingActionButton button) {
        if (!button.isEnabled())
            return;
        button.setVisibility(View.VISIBLE);
        visible = true;
        ViewCompat.animate(button).translationY(0)
                .setInterpolator(INTERPOLATOR).withLayer().setListener(null)
                .start();
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout,
                                       FloatingActionButton child, View directTargetChild, View target, int nestedScrollAxes) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL ||
                super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target,
                        nestedScrollAxes);
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child,
                               View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed,
                dyUnconsumed);

        if ((dyUnconsumed > 0 || dyConsumed > 25) && !this.mIsAnimatingOut && child.getVisibility() == View.VISIBLE) {
            animateOut(child);
        } else if ((dyUnconsumed < 0 || dyConsumed < -10) && child.getVisibility() != View.VISIBLE) {
            animateIn(child);
        }
    }
}
