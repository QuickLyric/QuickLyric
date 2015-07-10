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

package com.geecko.QuickLyric.view;

import android.content.Context;
import android.os.Parcelable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

import com.geecko.QuickLyric.utils.RefreshButtonBehavior;

public class RefreshIcon extends FloatingActionButton implements Animation.AnimationListener {

    private final RotateAnimation rotateAnimation;
    private boolean mRunning = false;
    public static boolean mEnded = false;

    public RefreshIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        rotateAnimation = new RotateAnimation(1, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setDuration(1100);
        rotateAnimation.setAnimationListener(this);
        rotateAnimation.setInterpolator(new LinearInterpolator());
    }

    public void show() {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) getLayoutParams();
        RefreshButtonBehavior behavior = (RefreshButtonBehavior) params.getBehavior();
        if (behavior != null)
            behavior.animateIn(this);
    }

    public void hide() {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) getLayoutParams();
        RefreshButtonBehavior behavior = (RefreshButtonBehavior) params.getBehavior();
        if (behavior != null)
            behavior.animateOut(this);
    }

    public void startAnimation() {
        if (!mRunning) {
            startAnimation(rotateAnimation);
            mRunning = true;
            mEnded = false;
        }
    }

    public void stopAnimation() {
        if (mRunning)
            mEnded = true;
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onAnimationEnd(Animation animation) {
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
        if (mEnded || !mRunning) {
            this.clearAnimation();
            mRunning = false;
        }
    }
}
