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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;

import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.utils.LyricsTextFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LrcView extends View {

    private TreeMap<Long, String> dictionnary;

    private long mCurrentTime = 0l;
    private long mNextTime = 0l;

    private int mViewWidth;
    private int mLrcHeight;
    private int mRows;
    private float mTextSize;
    private float mDividerHeight;

    private Paint mNormalPaint;
    private Paint mCurrentPaint;
    private List<Long> mTimes;

    public LrcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews(attrs);
    }

    public LrcView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews(attrs);
    }

    private void initViews(AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs,
                R.styleable.Lrc);
        mTextSize = ta.getDimension(R.styleable.Lrc_textSize, 50.0f);
        mRows = ta.getInteger(R.styleable.Lrc_rows, 5);
        mDividerHeight = ta.getDimension(R.styleable.Lrc_dividerHeight, 0.0f);

        int normalTextColor = ta.getColor(R.styleable.Lrc_normalTextColor,
                0xffffffff);
        int currentTextColor = ta.getColor(R.styleable.Lrc_currentTextColor,
                0xff00ffde);

        ta.recycle();

        mLrcHeight = (int) (mTextSize + mDividerHeight) * (mRows + 1) + 5;

        mNormalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCurrentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mNormalPaint.setTextSize(mTextSize);
        Typeface light = LyricsTextFactory.FontCache.get("light", getContext());
        mNormalPaint.setTypeface(light);
        mNormalPaint.setColor(normalTextColor);
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("pref_opendyslexic", false))
            mNormalPaint.setTypeface(LyricsTextFactory.FontCache.get("dyslexic", getContext()));
        mCurrentPaint.setTextSize(mTextSize);
        mCurrentPaint.setTypeface(LyricsTextFactory.FontCache.get("bold", getContext()));
        mCurrentPaint.setColor(currentTextColor);
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("pref_opendyslexic", false))
            mCurrentPaint.setTypeface(LyricsTextFactory.FontCache.get("dyslexic", getContext()));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mViewWidth = getMeasuredWidth();
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredHeight = MeasureSpec.makeMeasureSpec(mLrcHeight, MeasureSpec.AT_MOST);
        setMeasuredDimension(widthMeasureSpec, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dictionnary.isEmpty())
            return;

        int currentLine = Math.max(mTimes.indexOf(mCurrentTime), 0);

        canvas.save();

        // canvas.translate(0, -((currentLine - 3) * (mTextSize + mDividerHeight)));
        int overflow;
        int contained;

        int breakOffset = 0;

        if (currentLine - 1 >= 0) {
            long previousTime = mTimes.get(currentLine - 1);
            String previousLrc = dictionnary.get(previousTime);
            overflow = previousLrc.length() - mNormalPaint.breakText(previousLrc, true, mViewWidth, null);
            contained = previousLrc.length() - overflow;
            String cutPrevious = previousLrc.substring(0, contained);
            cutPrevious = cutPrevious.substring(0, overflow > 0 && cutPrevious.contains(" ") ? cutPrevious.lastIndexOf(" ") : contained);
            float previousX = (mViewWidth - mNormalPaint.measureText(cutPrevious)) / 2;
            canvas.drawText(cutPrevious, previousX,
                    (mTextSize + mDividerHeight) + breakOffset, mNormalPaint);
            if (overflow > 0) {
                if (previousLrc.contains(" "))
                    previousLrc = previousLrc.substring(previousLrc.substring(0, contained).lastIndexOf(" ") +1);
                else
                    previousLrc = previousLrc.substring(contained);
                previousX = (mViewWidth - mNormalPaint.measureText(previousLrc)) / 2;
                canvas.drawText(previousLrc, previousX,
                        (mTextSize + mDividerHeight) * 2 + breakOffset, mNormalPaint);
                breakOffset += mTextSize + mDividerHeight;
            }
        }

        String currentLrc = dictionnary.get(mCurrentTime);
        if (currentLrc == null) {
            mCurrentTime = dictionnary.firstKey();
            currentLrc = dictionnary.get(mCurrentTime);
        }
        overflow = currentLrc.length() - mCurrentPaint.breakText(currentLrc, true, mViewWidth, null);
        contained = currentLrc.length() - overflow;
        String cutLrc = currentLrc.substring(0, contained);
        cutLrc = cutLrc.substring(0, overflow > 0 && cutLrc.contains(" ") ? cutLrc.lastIndexOf(" ") : contained);
        float currentX = (mViewWidth - mCurrentPaint.measureText(cutLrc)) / 2;
        canvas.drawText(cutLrc, currentX,
                (mTextSize + mDividerHeight) * 2 + breakOffset, mCurrentPaint);
        if (overflow > 0) {
            if (currentLrc.contains(" "))
                currentLrc = currentLrc.substring(currentLrc.substring(0, contained).lastIndexOf(" ") + 1);
            else
                currentLrc = currentLrc.substring(contained);
            currentX = (mViewWidth - mCurrentPaint.measureText(currentLrc)) / 2;
            canvas.drawText(currentLrc, currentX,
                    (mTextSize + mDividerHeight) * 3 + breakOffset, mCurrentPaint);
            breakOffset += mTextSize + mDividerHeight;
        }

        for (int i = currentLine + 1; i < Math.min(currentLine + mRows - 2, dictionnary.size()); i++) {
            String lrc = dictionnary.get(mTimes.get(i));
            overflow = lrc.length() - mNormalPaint.breakText(lrc, true, mViewWidth, null);
            contained = lrc.length() - overflow;
            cutLrc = lrc.substring(0, contained);
            cutLrc = cutLrc.substring(0, overflow > 0 && cutLrc.contains(" ") ? cutLrc.lastIndexOf(" ") : contained);
            float x = (mViewWidth - mNormalPaint.measureText(cutLrc)) / 2;
            canvas.drawText(cutLrc, x,
                    (mTextSize + mDividerHeight) * (2 + i - currentLine) + breakOffset, mNormalPaint);

            if (overflow > 0) {
                if (lrc.contains(" "))
                    lrc = lrc.substring(lrc.substring(0, contained).lastIndexOf(" ") + 1);
                else
                    lrc = lrc.substring(contained);
                x = (mViewWidth - mNormalPaint.measureText(lrc)) / 2;
                canvas.drawText(lrc, x, (mTextSize + mDividerHeight) * (2 + i - currentLine + 1) + breakOffset, mNormalPaint);
                breakOffset += mTextSize + mDividerHeight;
            }
        }

        mLrcHeight = (int) (mTextSize + mDividerHeight) * (mRows + 1) + breakOffset + 5;
        canvas.clipRect(0, 0, mViewWidth, mLrcHeight);

        canvas.restore();
        requestLayout();
    }

    private Long parseTime(String time) {
        String[] min = time.split(":");
        String[] sec;
        if (!min[1].contains("."))
            min[1] += ".00";
        sec = min[1].split("\\.");

        long minInt = Long.parseLong(min[0].replaceAll("\\D+", "")
                .replaceAll("\r", "").replaceAll("\n", "").trim());
        long secInt = Long.parseLong(sec[0].replaceAll("\\D+", "")
                .replaceAll("\r", "").replaceAll("\n", "").trim());
        long milInt = Long.parseLong(sec[1].replaceAll("\\D+", "")
                .replaceAll("\r", "").replaceAll("\n", "").trim());

        return minInt * 60 * 1000 + secInt * 1000 + milInt * 10;
    }

    private String[] parseLine(String line) {
        Matcher matcher = Pattern.compile("\\[.+\\].+").matcher(line);
        if (!matcher.matches()) {
            return null;
        }

        if (line.endsWith("]"))
            line += " ";
        line = line.replaceAll("\\[", "");
        String[] result = line.split("\\]");
        try {
            for (int i = 0; i < result.length - 1; ++i)
                result[i] = String.valueOf(parseTime(result[i]));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
            return null;
        }

        return result;
    }

    public synchronized void changeCurrent(long time) {
        mNextTime = dictionnary.lastKey();
        if (time < mNextTime)
            mNextTime = dictionnary.higherKey(time);
        mCurrentTime = dictionnary.firstKey();
        if (time > mCurrentTime)
            mCurrentTime = dictionnary.floorKey(time);
        postInvalidate();
    }

    public boolean isFinished() {
        return mTimes.isEmpty() || mTimes.get(mTimes.size() - 1) <= mCurrentTime;
    }

    public void setSourceLrc(String lrc) {
        mNextTime = 0;
        mCurrentTime = 0;

        List<String> texts = new ArrayList<>();
        mTimes = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new StringReader(lrc));

        String line;
        String[] arr;
        try {
            while (null != (line = reader.readLine())) {
                arr = parseLine(line);
                if (null == arr) {
                    continue;
                }

                if (1 == arr.length) {
                    String last = texts.remove(texts.size() - 1);
                    texts.add(last + arr[0]);
                    continue;
                }
                for (int i = 0; i < arr.length - 1; i++) {
                    mTimes.add(Long.parseLong(arr[i]));
                    texts.add(arr[arr.length - 1]);
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Collections.sort(mTimes);
        dictionnary = new TreeMap<>();
        for (int i = 0; i < mTimes.size(); i++) {
            dictionnary.put(mTimes.get(i), texts.get(i));
        }
        Collections.sort(mTimes);
    }
}