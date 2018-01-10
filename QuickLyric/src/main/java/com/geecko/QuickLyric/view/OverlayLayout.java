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

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.services.LyricsOverlayService;
import com.geecko.QuickLyric.tasks.ParseTask;

import io.codetail.animation.ViewAnimationUtils;
import io.codetail.widget.RevealFrameLayout;

import static android.view.WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

public class OverlayLayout extends RevealFrameLayout{

    private OverlayLayoutListener listener;
    private Integer[] revealCenter;

    @SuppressLint("ClickableViewAccessibility")
    public OverlayLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        WindowManager.LayoutParams layoutParams =
                new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER_HORIZONTAL, Gravity.BOTTOM,
                        Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                                WindowManager.LayoutParams.TYPE_PHONE,
                        FLAG_FORCE_NOT_FULLSCREEN | FLAG_WATCH_OUTSIDE_TOUCH,
                        PixelFormat.TRANSLUCENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_ATTACHED_IN_DECOR;
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_force_screen_on", false))
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        setLayoutParams(layoutParams);
        setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                listener.onBackpressed();
            return false;
        });
    }

    public void setRevealCenter(Integer... params) {
        if (params == null || params[0] == null)
            this.revealCenter = null;
        else
            this.revealCenter = params;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if ((oldw != 0 && oldh != 0) && (w != oldw && oldh != h)) {
            listener.onSizeChanged();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK || event.getKeyCode() == KeyEvent.KEYCODE_HOME) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                listener.onBackpressed();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onVisibilityChanged(View view, int visibility) {
        super.onVisibilityChanged(view, visibility);
        if (visibility == View.VISIBLE && this == view) {
            Lyrics lyrics = LyricsOverlayService.getLyrics();
            if (lyrics != null)
                ((OverlayContentLayout) findViewById(R.id.overlay_content)).update(lyrics, true);
            new ParseTask(findViewById(R.id.overlay_content), getContext(), false, true, true).execute();

            if (revealCenter != null && ViewCompat.isAttachedToWindow(getChildAt(0))) {
                int cx = revealCenter[0];
                int cy = revealCenter[1];
                int dx = Math.max(cx, getWidth() - cx);
                int dy = Math.max(cy, getHeight() - cy);
                float finalRadius = (float) Math.hypot(dx, dy);

                Animator animator = ViewAnimationUtils.createCircularReveal(getChildAt(0), cx, cy, 0, finalRadius);
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.setDuration(300L);
                animator.start();
                revealCenter = null;
            }
        }
    }

    public void setListener(OverlayLayoutListener listener) {
        this.listener = listener;
    }

    public interface OverlayLayoutListener {
        void onBackpressed();

        void onSizeChanged();
    }

}
