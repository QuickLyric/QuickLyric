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
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ObservableScrollView;

public class RefreshIcon extends FloatingActionButton implements Animation.AnimationListener {

    private final RotateAnimation rotateAnimation;
    private ImageView shadow;
    private com.melnykov.fab.ObservableScrollView scrollView;
    private boolean mRunning = false;
    private boolean mEnded = false;

    public RefreshIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        rotateAnimation = new RotateAnimation(1, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setDuration(1100);
        rotateAnimation.setAnimationListener(this);
        rotateAnimation.setInterpolator(new LinearInterpolator());
    }

    @Override
    public void show() {
        super.show();
        shadow.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isVisible())
                    shadow.setVisibility(VISIBLE);
            }
        }, 200);
    }

    @Override
    public void hide() {
        shadow.setVisibility(GONE);
        super.hide();
    }

    public void setShadow(View shadow) {
        this.shadow = (ImageView) shadow;
    }

    public void startAnimation() {
        if (!mRunning) {
            startAnimation(rotateAnimation);
            mRunning = true;
            mEnded = false;
        }
        if (this.getTranslationY() != 0)
            this.show();
        if (scrollView != null)
            scrollView.setOnScrollChangedListener(null);
    }

    public void stopAnimation() {
        if (mRunning) {
            mEnded = true;
        }
        attachToScrollView(scrollView);
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

    @Override
    public void attachToScrollView(ObservableScrollView scrollView) {
        this.scrollView = scrollView;
        super.attachToScrollView(scrollView);
    }
}
