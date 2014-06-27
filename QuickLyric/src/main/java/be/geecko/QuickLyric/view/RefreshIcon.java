package be.geecko.QuickLyric.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

public class RefreshIcon extends ImageView implements Animation.AnimationListener {

    private final RotateAnimation rotateAnimation = new RotateAnimation(1, 360, Animation.RELATIVE_TO_SELF, (float) 0.5, Animation.RELATIVE_TO_SELF, (float) 0.5);
    private boolean mRunning = false;
    private boolean mEnded = false;

    public RefreshIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (rotateAnimation.getDuration() != 1100) {
            rotateAnimation.setRepeatCount(Animation.INFINITE);
            rotateAnimation.setDuration(1100);
            rotateAnimation.setAnimationListener(this);
            rotateAnimation.setInterpolator(new LinearInterpolator());
        }
    }

    public void startAnimation() {
        if (!mRunning) {
            startAnimation(rotateAnimation);
            mRunning = true;
            mEnded = false;
        }
    }

    public void stopAnimation() {
        if (mRunning) {
            mEnded = true;
        }
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
}
