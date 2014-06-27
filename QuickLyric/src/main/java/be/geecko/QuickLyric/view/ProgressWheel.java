package be.geecko.QuickLyric.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import be.geecko.QuickLyric.R;

/**
 * An indicator of progress, similar to Android's ProgressBar.
 * Can be used in 'spin mode' or 'increment mode'
 *
 * @author Todd Davies
 *         <p/>
 *         Licensed under the Creative Commons Attribution 3.0 license see:
 *         http://creativecommons.org/licenses/by/3.0/
 */
public class ProgressWheel extends View {

    private int barLength = 60;
    private int barWidth = 5;
    private int rimWidth = 5;
    private int textSize = 20;

    //Colors (with defaults)
    private int barColor = Color.parseColor("#CCFF9E52");
    private int circleColor = Color.WHITE;
    private int rimColor = Color.LTGRAY;
    private int textColor = 0xFF000000;

    //Paints
    private final Paint barPaint = new Paint();
    private final Paint circlePaint = new Paint();
    private final Paint textPaint = new Paint();

    //Drawable
    private Bitmap microphone;
    private Drawable touchFeedbackDrawable;

    //Rectangles
    @SuppressWarnings("unused")
    private RectF rectBounds = new RectF();
    private RectF circleBounds = new RectF();
    private final Rect innerBounds = new Rect();
    private RectF bitmapBounds = new RectF();

    //Animation
    //The amount of pixels to move the bar by on each draw
    private int spinSpeed = 2;
    //The number of milliseconds to wait inbetween each draw
    private int delayMillis = 0;
    private final Handler spinHandler = new Handler() {
        /**
         * This is the code that will increment the progress variable
         * and so spin the wheel
         */
        @Override
        public void handleMessage(Message msg) {
            invalidate();
            if (isSpinning) {
                progress += spinSpeed;
                if (progress > 360) {
                    progress = 0;
                }
                spinHandler.sendEmptyMessageDelayed(0, delayMillis);
            }
            //super.handleMessage(msg);
        }
    };
    private int progress = 0;
    private boolean isSpinning = false;

    public ProgressWheel(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttributes(context.obtainStyledAttributes(attrs, R.styleable.ProgressWheel));
        Resources resources = context.getResources();
        if (resources != null) {
            microphone = BitmapFactory.decodeResource(resources, R.drawable.microphone);
            touchFeedbackDrawable = resources.getDrawable(R.drawable.id_selector);
        }
    }

    //----------------------------------
    //Setting up stuff
    //----------------------------------

    /**
     * Now we know the dimensions of the view, setup the bounds and paints
     */
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setupBounds();
        setupPaints();
        invalidate();
    }

    /**
     * Set the properties of the paints we're using to
     * draw the progress wheel
     */
    private void setupPaints() {
        barPaint.setColor(barColor);
        barPaint.setAntiAlias(true);
        barPaint.setStyle(Style.STROKE);
        barPaint.setStrokeWidth(barWidth * 4);

        circlePaint.setColor(circleColor);
        circlePaint.setAntiAlias(true);
        circlePaint.setStyle(Style.FILL);

        textPaint.setColor(textColor);
        textPaint.setStyle(Style.FILL);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(textSize);


    }

    /**
     * Set the bounds of the component
     */
    private void setupBounds() {
        int paddingTop = this.getPaddingTop();
        int paddingBottom = this.getPaddingBottom();
        int paddingLeft = this.getPaddingLeft();
        int paddingRight = this.getPaddingRight();

        ViewGroup.LayoutParams layoutParams = this.getLayoutParams();
        if (layoutParams != null) {

            int minRadius = microphone.getHeight();
            DisplayMetrics displayMetrics;
            Context context = getContext();
            if (context != null) {
                displayMetrics = context.getResources().getDisplayMetrics();
                int innerMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, displayMetrics);
                float ratio = (float) microphone.getWidth() / (float) microphone.getHeight();

                if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT)
                    layoutParams.height = minRadius;
                if (layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT)
                    layoutParams.width = minRadius;

                rectBounds = new RectF(paddingLeft,
                        paddingTop,
                        layoutParams.width - paddingRight,
                        layoutParams.height - paddingBottom);


                circleBounds = new RectF(paddingLeft + barWidth+ rimWidth,
                        paddingTop + barWidth + rimWidth,
                        layoutParams.width - paddingRight - barWidth - rimWidth,
                        layoutParams.height - paddingBottom - barWidth - rimWidth);

                circleBounds.round(innerBounds);

                bitmapBounds = new RectF(layoutParams.width / 2 - microphone.getWidth() / 2 + (innerMargin * ratio),
                        innerMargin,
                        layoutParams.width / 2 + microphone.getWidth() / 2 - innerMargin * ratio,
                        microphone.getHeight() - innerMargin);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension((int) rectBounds.width() + getPaddingLeft() + getPaddingRight(), (int) rectBounds.height() + getPaddingTop() + getPaddingBottom());
    }

    private void parseAttributes(TypedArray a) {
        barWidth = (int) a.getDimension(R.styleable.ProgressWheel_barWidth,
                barWidth);

        rimWidth = (int) a.getDimension(R.styleable.ProgressWheel_rimWidth,
                rimWidth);

        spinSpeed = (int) a.getDimension(R.styleable.ProgressWheel_spinSpeed,
                spinSpeed);

        delayMillis = a.getInteger(R.styleable.ProgressWheel_delayMillis,
                delayMillis);
        if (delayMillis < 0) {
            delayMillis = 0;
        }

        barColor = a.getColor(R.styleable.ProgressWheel_barColor, barColor);

        barLength = (int) a.getDimension(R.styleable.ProgressWheel_barLength,
                barLength);

        textSize = (int) a.getDimension(R.styleable.ProgressWheel_textSize,
                textSize);

        textColor = a.getColor(R.styleable.ProgressWheel_textColor,
                textColor);

        rimColor = a.getColor(R.styleable.ProgressWheel_rimColor,
                rimColor);

        circleColor = a.getColor(R.styleable.ProgressWheel_circleColor,
                circleColor);
    }

    //----------------------------------
    //Animation stuff
    //----------------------------------

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Draw the bar
        if (isSpinning) {
            canvas.drawArc(circleBounds, progress - 90, barLength, false,
                    barPaint);
        } else {
            canvas.drawArc(circleBounds, -90, progress, false, barPaint);
        }
        //Draw the text (attempts to center it horizontally and vertically)
        touchFeedbackDrawable.setBounds(innerBounds);
        touchFeedbackDrawable.draw(canvas);
        canvas.drawBitmap(microphone, null, bitmapBounds, null);
    }

    @Override
    protected void drawableStateChanged() {
        if (touchFeedbackDrawable != null) {
            touchFeedbackDrawable.setState(getDrawableState());
            invalidate();
        }
        super.drawableStateChanged();
    }

    /**
     * Reset the count (in increment mode)
     */
    public void resetCount() {
        progress = 0;
        invalidate();
    }

    /**
     * Turn off spin mode
     */
    public void stopSpinning() {
        isSpinning = false;
        progress = 0;
        spinHandler.removeMessages(0);
    }


    /**
     * Puts the view on spin mode
     */
    public void spin() {
        isSpinning = true;
        spinHandler.sendEmptyMessage(0);
    }

    /**
     * Increment the progress by 1 (of 360)
    public void incrementProgress() {
        isSpinning = false;
        progress++;
        spinHandler.sendEmptyMessage(0);
    }
    */

    /**
     * Set the progress to a specific value
     */
    public void setProgress(int i) {
        isSpinning = false;
        progress = i;
        spinHandler.sendEmptyMessage(0);
    }

    //----------------------------------
    //Getters + setters
    //----------------------------------
/*
    public int getCircleRadius() {
        return circleRadius;
    }

    public int getProgress() {
        return progress;
    }

    public void setCircleRadius(int circleRadius) {
        this.circleRadius = circleRadius;
    }

    public int getBarLength() {
        return barLength;
    }

    public void setBarLength(int barLength) {
        this.barLength = barLength;
    }

    public int getBarWidth() {
        return barWidth;
    }

    public void setBarWidth(int barWidth) {
        this.barWidth = barWidth;
    }

    public int getTextSize() {
        return textSize;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public int getPaddingTop() {
        return paddingTop;
    }

    public void setPaddingTop(int paddingTop) {
        this.paddingTop = paddingTop;
    }

    public int getPaddingBottom() {
        return paddingBottom;
    }

    public void setPaddingBottom(int paddingBottom) {
        this.paddingBottom = paddingBottom;
    }

    public int getPaddingLeft() {
        return paddingLeft;
    }

    public void setPaddingLeft(int paddingLeft) {
        this.paddingLeft = paddingLeft;
    }

    public int getPaddingRight() {
        return paddingRight;
    }

    public void setPaddingRight(int paddingRight) {
        this.paddingRight = paddingRight;
    }

    public int getBarColor() {
        return barColor;
    }

    public void setBarColor(int barColor) {
        this.barColor = barColor;
    }

    public int getCircleColor() {
        return circleColor;
    }

    public void setCircleColor(int circleColor) {
        this.circleColor = circleColor;
        setupPaints();
    }

    public int getRimColor() {
        return rimColor;
    }

    public void setRimColor(int rimColor) {
        this.rimColor = rimColor;
    }


    public Shader getRimShader() {
        return rimPaint.getShader();
    }

    public void setRimShader(Shader shader) {
        this.rimPaint.setShader(shader);
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public int getSpinSpeed() {
        return spinSpeed;
    }

    public void setSpinSpeed(int spinSpeed) {
        this.spinSpeed = spinSpeed;
    }

    public int getRimWidth() {
        return rimWidth;
    }

    public void setRimWidth(int rimWidth) {
        this.rimWidth = rimWidth;
    }

    public int getDelayMillis() {
        return delayMillis;
    }

    public void setDelayMillis(int delayMillis) {
        this.delayMillis = delayMillis;
    }

    public boolean isSpinning() {
        return isSpinning;
    }
*/
}