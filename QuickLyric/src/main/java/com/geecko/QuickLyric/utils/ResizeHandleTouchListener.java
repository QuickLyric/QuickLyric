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

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.geecko.QuickLyric.R;

import java.lang.ref.WeakReference;

public class ResizeHandleTouchListener implements View.OnTouchListener {

    private final WeakReference<ResizeHandleCallback> callback;
    private final DisplayMetrics mDisplayMetrics;
    private boolean isInResizeMode;

    private float oldX;
    private float oldY;

    public ResizeHandleTouchListener(ResizeHandleCallback callback, WindowManager windowManager, Resources res) {
        this.callback = new WeakReference<>(callback);
        mDisplayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(mDisplayMetrics);
        int margin = res.getDimensionPixelSize(R.dimen.overlay_window_padding);
        int orientation = res.getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            mDisplayMetrics.heightPixels -= margin;
            mDisplayMetrics.heightPixels -= getNavigationBarSize(res);
        } else {
            mDisplayMetrics.widthPixels -= margin;
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (callback.get() != null) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN && !isInResizeMode) {
                isInResizeMode = true;
                callback.get().onResizeStarted();
                oldX = motionEvent.getRawX();
                oldY = motionEvent.getRawY();
            } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                this.callback.get().onHandleMoved(motionEvent.getRawX(), motionEvent.getRawY(), oldX, oldY,
                        mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels);
            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                if (isInResizeMode) {
                    callback.get().onResizeFinished();
                    isInResizeMode = false;
                }
                if (motionEvent.getEventTime() - motionEvent.getDownTime() < 200)
                    view.performClick();
            }
            return true;
        }
        return false;
    }

    private static int getNavigationBarSize(Resources resources) {
        int resourceId = resources.getIdentifier("navigation_bar_height",
                "dimen", "android");
        return resourceId > 0 ? resources.getDimensionPixelSize(resourceId) : 0;
    }

    public interface ResizeHandleCallback {
        void onResizeStarted();
        void onResizeFinished();
        void onHandleMoved(float newX, float newY, float oldX, float oldY, int screenW, int screenH);
    }
}
