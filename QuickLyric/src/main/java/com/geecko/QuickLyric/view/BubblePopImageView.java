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

/*
 * This file is part of OpenLRC
 * Created by qibin0506.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * or
 * https://github.com/qibin0506/OpenLRC/blob/master/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.geecko.QuickLyric.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.geecko.QuickLyric.R;

public class BubblePopImageView extends android.support.v7.widget.AppCompatImageView {
    private int mViewWidth;
    private int mViewHeight;
    private Paint mBubblePaint;
    private Paint mIconPaint;
    private Paint mScreenPaint;
    private RectF mGoogleMusicRect;
    private RectF mSpotifyRect;
    private RectF mShuttleRect;
    private RectF mScreenRect;
    private Bitmap mIcon1;
    private Bitmap mIcon2;
    private Bitmap mIcon3;
    private Bitmap mScreenBitmap;
    private float mRadius;
    private final boolean mEditMode;

    float[] gPlayMusicCenter;
    float[] spotifyCenter;
    float[] shuttleCenter;


    public BubblePopImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mEditMode = isInEditMode();
        initViews(attrs);
    }

    public BubblePopImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mEditMode = isInEditMode();
        initViews(attrs);
    }

    private void initViews(AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs,
                R.styleable.Bubble);

        mIcon1 = BitmapFactory.decodeResource(getResources(),
                ta.getResourceId(R.styleable.Bubble_icon1, R.drawable.ic_error));
        mIcon2 = BitmapFactory.decodeResource(getResources(),
                ta.getResourceId(R.styleable.Bubble_icon2, R.drawable.ic_error));
        mIcon3 = BitmapFactory.decodeResource(getResources(),
                ta.getResourceId(R.styleable.Bubble_icon3, R.drawable.ic_error));
        mScreenBitmap = BitmapFactory.decodeResource(getResources(),
                ta.getResourceId(R.styleable.Bubble_screen, R.drawable.ic_error));

        ta.recycle();

        mBubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBubblePaint.setColor(Color.WHITE);

        mIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScreenPaint = new Paint();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mViewWidth = getMeasuredWidth();
        mViewHeight = getMeasuredHeight();

        int actualHeight;
        int actualWidth;
        int offsetW = 0;
        int offsetH = 0;
        if (mViewHeight > mViewWidth) {
            actualHeight = getMeasuredWidth();
            actualWidth = getMeasuredWidth();
            offsetH = (mViewHeight - actualHeight) / 2;
        }
        else {
            actualWidth = getMeasuredHeight();
            actualHeight = getMeasuredHeight();
            offsetW = (mViewWidth - actualWidth) / 2;
        }
        mRadius = actualHeight/7;

        gPlayMusicCenter = new float[]{offsetW + actualWidth * 0.295f, offsetH + actualHeight * 0.29f};
        spotifyCenter = new float[]{offsetW + actualWidth * 0.79f, offsetH + actualHeight * 0.5f};
        shuttleCenter = new float[]{offsetW + actualWidth * 0.64f, offsetH + actualHeight * 0.76f};
        float[] screenCenter = new float[]{offsetW + actualWidth * 0.5f, offsetH + actualHeight * 0.5f};
        mScreenRect = new RectF(screenCenter[0] - actualWidth * 0.105f, screenCenter[1] - actualHeight * 0.095f,
                screenCenter[0] + actualWidth * 0.077f, screenCenter[1] + actualHeight * 0.11f);
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mEditMode)
            setProgress(1f);

        canvas.save();
        canvas.clipRect(0, 0, mViewWidth, mViewHeight);

        canvas.drawOval(mGoogleMusicRect, mBubblePaint);
        canvas.drawOval(mSpotifyRect, mBubblePaint);
        canvas.drawOval(mShuttleRect, mBubblePaint);

        canvas.drawBitmap(mIcon1, null, mGoogleMusicRect, mIconPaint);
        canvas.drawBitmap(mIcon2, null, mSpotifyRect, mIconPaint);
        canvas.drawBitmap(mIcon3, null, mShuttleRect, mIconPaint);

        canvas.drawBitmap(mScreenBitmap, null, mScreenRect, mScreenPaint);

        canvas.restore();
        requestLayout();
    }

    public void setProgress(float progress) {
        if (gPlayMusicCenter == null)
            onSizeChanged(0, 0, 0, 0);

        final int radius = (int) (mRadius * progress);

        mGoogleMusicRect = new RectF(gPlayMusicCenter[0] - radius, gPlayMusicCenter[1] - radius,
                gPlayMusicCenter[0] + radius, gPlayMusicCenter[1] + radius);
        mSpotifyRect = new RectF(spotifyCenter[0] - radius, spotifyCenter[1] - radius,
                spotifyCenter[0] + radius, spotifyCenter[1] + radius);
        mShuttleRect = new RectF(shuttleCenter[0] - radius, shuttleCenter[1] - radius,
                shuttleCenter[0] + radius, shuttleCenter[1] + radius);

        if (progress > 0.66f)
            mScreenPaint.setAlpha((int) Math.max(((progress - 2f / 3f) * 3 * 255), 0f));
        else
            mScreenPaint.setAlpha(0);

        invalidate();
    }
}