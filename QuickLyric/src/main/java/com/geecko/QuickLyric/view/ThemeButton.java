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
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;

import com.geecko.QuickLyric.R;

public class ThemeButton extends android.support.v7.widget.AppCompatImageButton {

    private Drawable mCheckMark;

    public ThemeButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews();
    }

    public ThemeButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews();
    }

    public ThemeButton(Context context) {
        super(context);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        mCheckMark.setState(getDrawableState());

        //redraw
        invalidate();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldwidth, int oldheight) {
        super.onSizeChanged(width, height, oldwidth, oldheight);
        mCheckMark.setBounds(width / 6, height / 6, width - width / 6, height - height / 6);
    }

    @SuppressWarnings("deprecation")
    private void initViews() {
        mCheckMark = getResources().getDrawable(R.drawable.anim_action_accept);
        mCheckMark.setCallback(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ColorStateList imageTintList = getImageTintList();
            if (imageTintList == null) {
                return;
            }

            setColorFilter(imageTintList.getDefaultColor(), PorterDuff.Mode.SRC_IN);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        mCheckMark.draw(canvas);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || (who == mCheckMark);
    }

    @TargetApi(11)
    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        mCheckMark.jumpToCurrentState();
    }
}
