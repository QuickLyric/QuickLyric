package be.geecko.QuickLyric.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ScrollView;

import com.melnykov.fab.*;
import com.melnykov.fab.ObservableScrollView;

import be.geecko.QuickLyric.MainActivity;
import be.geecko.QuickLyric.R;

public class RefreshIcon extends FloatingActionButton implements Animation.AnimationListener {

    private final RotateAnimation rotateAnimation;
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

    public void startAnimation() {
        if (!mRunning) {
            startAnimation(rotateAnimation);
            mRunning = true;
            mEnded = false;
        }
        if (this.getTranslationY() != 0)
            this.show();
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
