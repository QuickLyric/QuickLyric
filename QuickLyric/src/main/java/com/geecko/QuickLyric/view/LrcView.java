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
import android.view.animation.AccelerateInterpolator;

import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.utils.AnimatorActionListener;
import com.geecko.QuickLyric.utils.LyricsTextFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LrcView extends View {

    public TreeMap<Long, String> dictionnary;

    private int mCurrentLine = -1;
    private int mNextLine = -1;
    private long mCurrentTime = 0L;
    private long mNextTime = 0L;

    private int mViewWidth;
    private int mLrcHeight;
    private int mRows;
    private float mTextSize;
    private float mDividerHeight;
    private int mLastOffset = 0;

    private Paint mNormalPaint;
    private Paint mCurrentPaint;
    private List<Long> mTimes;
    private Lyrics lyrics;
    private String uploader;

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
                R.styleable.LrcView);
        mTextSize = ta.getDimension(R.styleable.LrcView_textSize, 50.0f);
        mRows = ta.getInteger(R.styleable.LrcView_rows, 5);
        mDividerHeight = ta.getDimension(R.styleable.LrcView_dividerHeight, 0.0f);

        int normalTextColor = ta.getColor(R.styleable.LrcView_normalTextColor,
                0xffffffff);
        int currentTextColor = ta.getColor(R.styleable.LrcView_currentTextColor,
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
        int measuredHeight = MeasureSpec.makeMeasureSpec(mLrcHeight, MeasureSpec.EXACTLY);
        int measuredWidth = MeasureSpec.makeMeasureSpec(widthMeasureSpec, MeasureSpec.UNSPECIFIED);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dictionnary.isEmpty())
            return;

        int breakOffset = 0;

        // Line at the top
        if (mCurrentLine - 1 >= 0) {
            long previousTime = mTimes.get(mCurrentLine - 1); // FIXME Bug if mTimes is empty
            String previousLrc = dictionnary.get(previousTime);
            if (previousLrc != null) {
                breakOffset = drawDividedText(previousLrc, canvas, mTextSize + mDividerHeight, breakOffset, mNormalPaint);
            }
        }

        mLastOffset = breakOffset;

        // Highlighted line
        String currentLrc = dictionnary.get(mTimes.get(Math.max(mCurrentLine, 0)));
        if (currentLrc == null) {
            mCurrentTime = dictionnary.firstKey();
            currentLrc = dictionnary.get(mCurrentTime);
        }
        breakOffset = drawDividedText(currentLrc, canvas, (mTextSize + mDividerHeight) * 2, breakOffset, mCurrentPaint);

        if (mTimes.size() >= mCurrentLine + 1) {
            // Next lines
            for (int i = mCurrentLine + 1; i < Math.min(mCurrentLine + mRows - 2, mTimes.size()); i++) {
                String lrc = dictionnary.get(mTimes.get(i));
                if (lrc == null)
                    continue;
                breakOffset = drawDividedText(lrc, canvas,
                        (mTextSize + mDividerHeight) * (2 + i - mCurrentLine), breakOffset, mNormalPaint);
            }
        }

        mLrcHeight = (int) (mTextSize + mDividerHeight) * (mRows + 1) + breakOffset + 5;
        canvas.clipRect(0, 0, mViewWidth, mLrcHeight);

        requestLayout();
    }

    private int drawDividedText(String lrc, Canvas canvas, float y, int breakOffset, Paint paint) {
        int overflow = lrc.length() - mCurrentPaint.breakText(lrc, true, mViewWidth * 0.965f, null);
        int contained = lrc.length() - overflow;
        String cutLrc = lrc.substring(0, contained);
        cutLrc = cutLrc.substring(0, overflow > 0 && cutLrc.contains(" ") ? cutLrc.lastIndexOf(" ") : contained);
        float x = (mViewWidth - mCurrentPaint.measureText(cutLrc)) / 2;
        canvas.drawText(cutLrc, x, y + breakOffset, paint);

        if (overflow > 0) {
            float lineHeight = mTextSize + mDividerHeight; // todo move to field
            lrc = lrc.substring(cutLrc.length() + 1);
            breakOffset = drawDividedText(lrc, canvas, y + lineHeight, breakOffset, paint) + (int) lineHeight;
        }
        return breakOffset;
    }

    private Long parseTime(String time) {
        String[] min = time.split(":");
        String[] sec;
        if (!min[1].contains("."))
            min[1] += ".00";
        sec = min[1].split("\\.");
        sec[1] = sec[1].replaceAll("\\D+", "").replaceAll("\r", "").replaceAll("\n", "").trim();
        if (sec[1].length() > 3)
            sec[1] = sec[1].substring(0,3);

        long minInt = Long.parseLong(min[0].replaceAll("\\D+", "")
                .replaceAll("\r", "").replaceAll("\n", "").trim());
        long secInt = Long.parseLong(sec[0].replaceAll("\\D+", "")
                .replaceAll("\r", "").replaceAll("\n", "").trim());
        long milInt = Long.parseLong(sec[1]);

        return minInt * 60 * 1000 + secInt * 1000 + milInt * Double.valueOf(Math.pow(10, 3 - sec[1].length())).longValue();
    }

    private String[] parseLine(String line) {
        Matcher matcher = Pattern.compile("\\[.+\\].+").matcher(line);
        if (!matcher.matches() || line.contains("By:")) {
            if (line.contains("[by:") && line.length() > 6)
                this.uploader = line.substring(5, line.length() - 1);
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
        if (dictionnary == null || dictionnary.isEmpty())
            return;
        mNextTime = dictionnary.lastKey();
        if (time < mNextTime)
            mNextTime = dictionnary.higherKey(time);
        if (time > dictionnary.firstKey())
            mCurrentTime = dictionnary.floorKey(time);
        else
            mCurrentTime = dictionnary.firstKey();

        int currentLine = Math.max(mTimes.indexOf(mCurrentTime), 0);
        if (currentLine < mCurrentLine || mCurrentLine < 0) {
            mCurrentLine = currentLine;
            mNextLine = currentLine;
            setTranslationY(0);
        } else if (currentLine > mCurrentLine && currentLine > mNextLine && getTranslationY() == 0) { // We're moving forward, show neat animation
            animate().setListener(new AnimatorActionListener(() -> {
                mCurrentLine = currentLine;
                setTranslationY(0);
                invalidate();
            }, AnimatorActionListener.ActionType.END)).translationY(-(mTextSize + mDividerHeight + mLastOffset)).setDuration(300)
                    .setInterpolator(new AccelerateInterpolator()).start();
            mNextLine = currentLine;
        }
    }

    public boolean isFinished() {
        return mTimes.isEmpty() || mTimes.get(mTimes.size() - 1) <= mCurrentTime;
    }

    public boolean hasLyrics() {
        return mTimes != null && dictionnary != null && this.lyrics != null;
    }

    public void setSourceLrc(String lrc) {
        mNextTime = 0;
        mCurrentTime = 0;
        mCurrentLine = -1;
        mNextLine = -1;
        mLastOffset = 0;

        List<String> texts = new ArrayList<>();
        mTimes = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new StringReader(lrc));

        String line;
        String[] arr;
        try {
            while ((line = reader.readLine()) != null) {
                arr = parseLine(line);
                if (arr == null) {
                    continue;
                }

                if (arr.length == 1) {
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
            if (!(texts.get(i).startsWith("Album:") || texts.get(i).startsWith("Title:")
                    || texts.get(i).startsWith("Artist:"))
                    && (i > 2 || (!texts.get(i).contains(lyrics.getArtist()) &&
                    (uploader == null || !texts.get(i).contains(uploader)))))
                if (!(dictionnary.isEmpty() && texts.get(i).replaceAll("\\s", "").isEmpty()))
                    dictionnary.put(mTimes.get(i), texts.get(i));
        }
        Collections.sort(mTimes);
        if (mTimes.isEmpty()) {
            mTimes.add(0L);
        }
    }

    public long getLastLinePosition() {
        return mTimes == null || mTimes.isEmpty() ? Long.MAX_VALUE : Collections.max(mTimes);
    }

    public Lyrics getStaticLyrics() {
        Lyrics result = this.lyrics;
        StringBuilder text = new StringBuilder();
        Iterator<String> iterator = dictionnary.values().iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            if (text.length() == 0 && next.replaceAll("\\s", "").isEmpty())
                continue;
            text.append(next);
            if (iterator.hasNext())
                text.append("<br/>\n");
        }
        result.setText(text.toString());
        result.setLRC(false);
        return result;
    }

    public void setOriginalLyrics(Lyrics lyrics) {
        this.lyrics = lyrics;
    }
}