package com.geecko.QuickLyric.view;

/**
 * Copyright 2015 Bartosz Lipinski
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;

import java.lang.ref.WeakReference;

public class ControllableAppBarLayout extends AppBarLayout {
    private AppBarLayout.Behavior mBehavior;
    private WeakReference<CoordinatorLayout> mParent;
    private ToolbarChange mQueuedChange = ToolbarChange.NONE;
    private boolean mAfterFirstDraw = false;

    public ControllableAppBarLayout(Context context) {
        super(context);
    }

    public ControllableAppBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!(getLayoutParams() instanceof CoordinatorLayout.LayoutParams) || !(getParent() instanceof CoordinatorLayout)) {
            throw new IllegalStateException("ControllableAppBarLayout must be a direct child of CoordinatorLayout.");
        } else {
            mParent = new WeakReference<>((CoordinatorLayout) getParent());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mBehavior == null) {
            mBehavior = (Behavior) ((CoordinatorLayout.LayoutParams) getLayoutParams()).getBehavior();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (r - l > 0 && b - t > 0 && mAfterFirstDraw && mQueuedChange != ToolbarChange.NONE) {
            analyzeQueuedChange();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!mAfterFirstDraw) {
            mAfterFirstDraw = true;
            if (mQueuedChange != ToolbarChange.NONE) {
                analyzeQueuedChange();
            }
        }
    }

    private synchronized void analyzeQueuedChange() {
        switch (mQueuedChange) {
            case COLLAPSE:
                performCollapsingWithoutAnimation();
                break;
            case COLLAPSE_WITH_ANIMATION:
                performCollapsingWithAnimation();
                break;
            case EXPAND:
                performExpandingWithoutAnimation();
                break;
            case EXPAND_WITH_ANIMATION:
                performExpandingWithAnimation();
                break;
        }

        mQueuedChange = ToolbarChange.NONE;
    }

    public void collapseToolbar() {
        collapseToolbar(false);
    }

    public void collapseToolbar(boolean withAnimation) {
        mQueuedChange = withAnimation ? ToolbarChange.COLLAPSE_WITH_ANIMATION : ToolbarChange.COLLAPSE;
        requestLayout();
    }

    public void expandToolbar() {
        expandToolbar(false);
    }

    public void expandToolbar(boolean withAnimation) {
        setExpanded(true, withAnimation);
    }

    private void performCollapsingWithoutAnimation() {
        if (mParent.get() != null && mBehavior != null) {
            mBehavior.onNestedPreScroll(mParent.get(), this, null, 0, getHeight(), new int[]{0, 0});
        }
    }

    private void performCollapsingWithAnimation() {
        if (mParent.get() != null && mBehavior != null) {
            mBehavior.onNestedFling(mParent.get(), this, null, 0, getHeight(), true);
        }
    }

    private void performExpandingWithoutAnimation() {
        if (mParent.get() != null && mBehavior != null) {
            mBehavior.setTopAndBottomOffset(0);
        }
    }

    private void performExpandingWithAnimation() {
        if (mParent.get() != null && mBehavior != null) {
            mBehavior.onNestedFling(mParent.get(), this, null, 0, -getHeight() * 5, false);
        }
    }

    private enum ToolbarChange {
        COLLAPSE,
        COLLAPSE_WITH_ANIMATION,
        EXPAND,
        EXPAND_WITH_ANIMATION,
        NONE
    }

}