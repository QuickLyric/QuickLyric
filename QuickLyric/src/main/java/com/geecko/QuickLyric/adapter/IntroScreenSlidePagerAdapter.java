package com.geecko.QuickLyric.adapter;

import android.animation.ArgbEvaluator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.geecko.QuickLyric.App;
import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.utils.AnimatorActionListener;
import com.geecko.QuickLyric.view.BubblePopImageView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static android.support.v4.widget.DrawerLayout.LOCK_MODE_UNLOCKED;

/**
 * This file is part of QuickLyric
 * Created by geecko
 * <p/>
 * QuickLyric is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * QuickLyric is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with QuickLyric.  If not, see <http://www.gnu.org/licenses/>.
 */

public class IntroScreenSlidePagerAdapter extends FragmentStatePagerAdapter implements ViewPager.OnPageChangeListener {
    public boolean rightToLeft = false;
    private Integer[] colors = new Integer[]{
            R.color.accent_dark,
            R.color.bright_yellow,
            R.color.deep_red,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_dark
    };
    private Class[] tutorialScreens = new Class[]{
            Tutorial_0.class,
            Tutorial_1.class,
            Tutorial_2.class,
            Tutorial_3.class,
            Tutorial_4.class
    };
    private Activity mActivity;
    private ViewPager mPager;
    private boolean hasClicked = false;
    private int mCurrentPage;

    View.OnTouchListener exitTouchListener = new View.OnTouchListener() {
        VelocityTracker mVelocityTracker;
        float mDownX;
        float mSwipeSlop = -1;
        boolean mSwiping;

        @Override
        public boolean onTouch(final View v, MotionEvent event) {
            if (mPager.getCurrentItem() != (rightToLeft ? 0 : getCount() - 1))
                return false;
            int pointerId = event.getPointerId(event.getActionIndex());
            if (mSwipeSlop < 0) {
                mSwipeSlop = ViewConfiguration.get(mActivity)
                        .getScaledTouchSlop();
            }
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    mDownX = event.getX();
                    if (mVelocityTracker == null) {
                        // Retrieve a new VelocityTracker object to watch the velocity of a motion.
                        mVelocityTracker = VelocityTracker.obtain();
                    } else {
                        // Reset the velocity tracker back to its initial state.
                        mVelocityTracker.clear();
                    }
                    mVelocityTracker.addMovement(event);
                    break;
                case android.view.MotionEvent.ACTION_CANCEL:
                    ((View) v.getParent()).setAlpha(1);
                    v.setTranslationX(0);
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                    break;
                case android.view.MotionEvent.ACTION_MOVE: {
                    mVelocityTracker.addMovement(event);
                    float x = event.getX() + v.getTranslationX();
                    float deltaX = x - mDownX;
                    float deltaXAbs = Math.abs(deltaX);
                    if (!mSwiping) {
                        if (deltaXAbs > mSwipeSlop)
                            mSwiping = true;
                    }
                    if (mSwiping) {
                        boolean startDirection = rightToLeft ? deltaX < 0 : deltaX > 0;
                        if (startDirection) {
                            mPager.setAlpha(1f);
                            mSwiping = false;
                            return false;
                        }
                        v.setTranslationX(deltaX);
                        ((View) v.getParent()).setAlpha(1 - deltaXAbs / v.getWidth());
                    }
                }
                break;
                case android.view.MotionEvent.ACTION_UP: {
                    // User let go - figure out whether to animate the view out, or back into place
                    if (mSwiping) {
                        float x = event.getX() + v.getTranslationX();
                        float deltaX = x - mDownX;
                        float deltaXAbs = Math.abs(deltaX);
                        float fractionCovered;
                        float endX;
                        float endAlpha;
                        final boolean remove;
                        mVelocityTracker.computeCurrentVelocity(1000);
                        float velocityX = Math.abs(VelocityTrackerCompat.getXVelocity(mVelocityTracker, pointerId));
                        if (velocityX > 700 || deltaXAbs > v.getWidth() / 3) {
                            fractionCovered = deltaXAbs / v.getWidth();
                            endX = deltaX < 0 ? -v.getWidth() : v.getWidth();
                            endAlpha = 0;
                            remove = true;
                        } else {
                            // Not far enough - animate it back
                            fractionCovered = 1 - (deltaXAbs / v.getWidth());
                            endX = 0;
                            endAlpha = 1;
                            remove = false;
                        }
                        mVelocityTracker.clear();
                        int SWIPE_DURATION = 600;
                        long duration = (int) ((1 - fractionCovered) * SWIPE_DURATION);
                        ((View) v.getParent()).animate().setDuration(Math.abs(duration)).alpha(endAlpha);
                        v.animate().setDuration(Math.abs(duration)).translationX(endX)
                                .setListener(new AnimatorActionListener(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Restore animated values
                                        if (remove) {
                                            int MOVE_DURATION = 500;
                                            v.animate().setDuration(MOVE_DURATION).translationY(0);
                                            v.animate().setListener(new AnimatorActionListener(new Runnable() {
                                                public void run() {
                                                    mSwiping = false;
                                                    MainActivity activity = (MainActivity) v.getContext();
                                                    ((RelativeLayout) v.getParent()).setVisibility(View.GONE);
                                                    activity.focusOnFragment = true;
                                                    if (activity.mDrawerToggle != null)
                                                        ((DrawerLayout) activity.drawer).setDrawerLockMode(LOCK_MODE_UNLOCKED);
                                                    activity.invalidateOptionsMenu();
                                                    SharedPreferences.Editor editor =
                                                            activity.getSharedPreferences("slides", Context.MODE_PRIVATE).edit();
                                                    editor.putBoolean("seen", true);
                                                    editor.apply();
                                                    activity.setStatusBarColor(null);
                                                    activity.setNavBarColor(null);
                                                }
                                            }, AnimatorActionListener.ActionType.END));
                                        } else {
                                            ((View) v.getParent()).setAlpha(1);
                                            v.setTranslationX(0);
                                        }
                                    }
                                }, AnimatorActionListener.ActionType.END));
                    }
                }
                mSwiping = false;
                break;
                default:
                    return false;
            }
            return false;
        }
    };


    public IntroScreenSlidePagerAdapter(FragmentManager fm, Activity activity) {
        super(fm);
        this.mActivity = activity;
        mPager = ((ViewPager) mActivity.findViewById(R.id.pager));
        mPager.setOnTouchListener(exitTouchListener);
        if (!App.playStoreVariant)
            colors = Arrays.copyOfRange(colors, 0, 4);
        if (Build.VERSION.SDK_INT >= 17)
            rightToLeft = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == 1;
        if (rightToLeft) {
            List<Integer> list = Arrays.asList(colors);
            Collections.reverse(list);
            colors = (Integer[]) list.toArray();
        }
        ImageButton pagerArrow = ((ImageButton) mActivity.findViewById(R.id.pager_arrow));
        Button okButton = ((Button) mActivity.findViewById(R.id.pager_ok));
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasClicked) {
                    exitAction();
                    hasClicked = true;
                }
            }
        });
        pagerArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextAction();
            }
        });
    }

    @Override
    public Fragment getItem(int position) {
        if (rightToLeft)
            position = getCount() - position - 1;
        try {
            return (Fragment) tutorialScreens[position].newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return new Fragment();
        }
    }

    @Override
    public int getCount() {
        return App.playStoreVariant ? 5 : 4;
    }

    @SuppressWarnings({"deprecation", "ResourceAsColor"})
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        View tutorialLayout = mActivity.findViewById(R.id.tutorial_layout);
        ArgbEvaluator evaluator = new ArgbEvaluator();
        Object background = position < getCount() - 1 ?
                evaluator.evaluate(positionOffset, mActivity.getResources().getColor(colors[position]),
                        mActivity.getResources().getColor(colors[position + 1]))
                : mActivity.getResources().getColor(colors[position]);
        tutorialLayout.setBackgroundColor((int) background);
        ((MainActivity) mActivity).setNavBarColor((int)
                evaluator.evaluate(0.5f, mActivity.getResources().getColor(R.color.action_dark),
                        background));
        ((MainActivity) mActivity).setStatusBarColor((int)
                evaluator.evaluate(0.5f, mActivity.getResources().getColor(R.color.action_dark),
                        background));

        View bigFab = tutorialLayout.findViewById(R.id.big_fab);
        View handImage = tutorialLayout.findViewById(R.id.musicid_demo_hand_image);
        View soundImage = tutorialLayout.findViewById(R.id.musicid_demo_sound_image);
        BubblePopImageView tableImageView = (BubblePopImageView) tutorialLayout.findViewById(R.id.table);
        position = rightToLeft ? getCount() - 1 - position : position;
        if (rightToLeft && positionOffset > 0.0) {
            position -= 1;
            positionOffset = 1f - positionOffset;
            positionOffsetPixels = (int) (positionOffset * mPager.getWidth());
        }

        switch (position) {
            case 0:
                if (tableImageView != null) {
                    tableImageView.setProgress(positionOffset);
                    tableImageView.setTranslationX((rightToLeft ? -1f : 1f) * (1f - positionOffset) *
                            (tableImageView.getMeasuredWidth() / 3f));
                }
                break;
            case 1:
                if (tableImageView != null) {
                    tableImageView.setProgress(1f);
                    tableImageView.setTranslationX((rightToLeft ? 0.15f : -0.4f) * positionOffsetPixels);
                }
                if (bigFab != null) {
                    bigFab.setTranslationX((rightToLeft ? -1f : 1f) *
                            (1f - positionOffset) * (bigFab.getMeasuredWidth() / 3f));
                    if (mCurrentPage == 1 ^ rightToLeft)
                        bigFab.setRotation(positionOffset * 360f);
                    else
                        bigFab.setRotation((1f - positionOffset) * 360f);
                }
                break;
            case 2:
                if (bigFab != null)
                    bigFab.setTranslationX((!rightToLeft ? -0.4f : 0.4f) * positionOffsetPixels);
                if (soundImage != null && handImage != null) {
                    soundImage.setTranslationX(300f - 300f * positionOffset);
                    handImage.setTranslationX(-400f + 400f * positionOffset);
                }
                break;
            case 3:
                break;
        }
    }

    @Override
    public void onPageSelected(int position) {
        Button pagerButton = ((Button) mActivity.findViewById(R.id.pager_button));
        ImageButton pagerArrow = ((ImageButton) mActivity.findViewById(R.id.pager_arrow));
        Button okButton = ((Button) mActivity.findViewById(R.id.pager_ok));
        if ((rightToLeft && position == 0) || (!rightToLeft && position >= getCount() - 1)) {
            pagerButton.setText("");
            pagerButton.setEnabled(false);
            pagerArrow.setVisibility(View.GONE);
            okButton.setVisibility(View.VISIBLE);
        } else {
            pagerButton.setText(R.string.skip);
            pagerButton.setEnabled(true);
            pagerArrow.setVisibility(View.VISIBLE);
            pagerArrow.setScaleX(rightToLeft ? -1f : 1f);
            okButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == ViewPager.SCROLL_STATE_IDLE)
            this.mCurrentPage = mPager.getCurrentItem();
    }

    public void nextAction() {
        mPager.setCurrentItem(mPager.getCurrentItem() + (rightToLeft ? -1 : 1), true);
    }

    public void exitAction() {
        Animation slideOut = new TranslateAnimation(0f, -2000f, 0f, 0f);
        slideOut.setInterpolator(new AccelerateInterpolator());
        slideOut.setDuration(700);

        slideOut.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                ((RelativeLayout) mPager.getParent()).setVisibility(View.GONE);
                ((MainActivity) mActivity).focusOnFragment = true;
                if (((MainActivity) mActivity).mDrawerToggle != null)
                    ((DrawerLayout) ((MainActivity) mActivity).drawer).setDrawerLockMode(LOCK_MODE_UNLOCKED);
                mActivity.invalidateOptionsMenu();
                SharedPreferences.Editor editor =
                        mActivity.getSharedPreferences("slides", Context.MODE_PRIVATE).edit();
                editor.putBoolean("seen", true);
                editor.apply();
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }
        });
        ((MainActivity) mActivity).setStatusBarColor(null);
        ((MainActivity) mActivity).setNavBarColor(null);
        ((RelativeLayout) mPager.getParent()).startAnimation(slideOut);
    }

    public static class Tutorial_0 extends Fragment {
        // Welcome page
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.tutorial_0, container, false);
        }
    }

    public static class Tutorial_1 extends Fragment {
        // Players page
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.tutorial_1, container, false);
        }

        @Override
        public void onResume() {
            super.onResume();
            BubblePopImageView tableImageView = (BubblePopImageView) getActivity().findViewById(R.id.table);
            if (tableImageView != null)
                tableImageView.setProgress(1f);
        }
    }

    public static class Tutorial_2 extends Fragment {
        // Refresh page
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.tutorial_2, container, false);
        }
    }

    public static class Tutorial_3 extends Fragment {
        // MusicID page
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.tutorial_3, container, false);
        }
    }

    public static class Tutorial_4 extends Fragment {
        // Last page
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.tutorial_4, container, false);
        }
    }
}