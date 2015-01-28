package be.geecko.QuickLyric.utils;

import android.animation.ArgbEvaluator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.RelativeLayout;

import be.geecko.QuickLyric.MainActivity;
import be.geecko.QuickLyric.R;

/**
 * This file is part of QuickLyric
 * Created by geecko on 3/01/15.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter implements ViewPager.OnPageChangeListener {
    private int[] colors = new int[]{android.R.color.holo_orange_dark, R.color.deep_red, android.R.color.holo_orange_light};
    private Activity mActivity;
    private ViewPager mPager;
    private boolean hasClicked = false;

    public ScreenSlidePagerAdapter(FragmentManager fm, Activity activity) {
        super(fm);
        this.mActivity = activity;
        mPager = ((ViewPager) mActivity.findViewById(R.id.pager));
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            default:
                return new Tutorial_0();
            case 1:
                return new Tutorial_1();
            case 2:
                return new Tutorial_2();
        }
    }

    @Override
    public int getCount() {
        return ((MainActivity) mActivity).mDrawerToggle == null ? 2 : 3;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        View tutorialLayout = mActivity.findViewById(R.id.tutorial_layout);
        if (position < getCount() - 1) {
            ArgbEvaluator evaluator = new ArgbEvaluator();
            Object background = evaluator
                    .evaluate(positionOffset, mActivity.getResources().getColor(colors[position]),
                            mActivity.getResources().getColor(colors[position + 1]));
            tutorialLayout.setBackgroundColor((int) background);
            ((MainActivity) mActivity).setStatusBarColor((int)
                    evaluator.evaluate(0.5f, Color.parseColor("#aa0c0006"), background));
        } else
            tutorialLayout.setBackgroundResource(android.R.color.holo_orange_light);
    }

    @Override
    public void onPageSelected(int position) {
        Button pagerButton = ((Button) mActivity.findViewById(R.id.pager_button));
        Button pagerArrow = ((Button) mActivity.findViewById(R.id.pager_arrow));
        if (position < getCount() - 1) {
            pagerButton.setText(R.string.skip);
            pagerButton.setEnabled(true);
            pagerArrow.setText(R.string.pager_arrow);
            pagerArrow.setOnClickListener(nextClickListener);
        } else {
            pagerButton.setText("");
            pagerButton.setEnabled(false);
            pagerArrow.setText(android.R.string.ok);
            pagerArrow.setOnClickListener(exitClickListener);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    public void nextAction() {
        mPager.setCurrentItem(mPager.getCurrentItem() + 1, true);
    }

    public void exitAction() {
        Animation slideOut = new TranslateAnimation(0f, -2000f, 0f, 0f);
        slideOut.setInterpolator(new AccelerateInterpolator());
        slideOut.setDuration(700);

        slideOut.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                ((RelativeLayout) mPager.getParent()).setVisibility(View.GONE);
                ((MainActivity) mActivity).focusOnFragment = true;
                if (((MainActivity) mActivity).mDrawerToggle != null) {
                    ((MainActivity) mActivity).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    ((MainActivity) mActivity).mDrawerToggle.setDrawerIndicatorEnabled(true);
                }
                mActivity.invalidateOptionsMenu();
                SharedPreferences.Editor editor =
                        mActivity.getSharedPreferences("tutorial", Context.MODE_PRIVATE).edit();
                editor.putBoolean("seen", true);
                editor.apply();
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }
        });
        ((MainActivity) mActivity).setStatusBarColor(mActivity.getResources()
                .getColor(R.color.primary_dark));
        ((RelativeLayout) mPager.getParent()).startAnimation(slideOut);
    }

    private View.OnClickListener nextClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            nextAction();
        }
    };

    private View.OnClickListener exitClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!hasClicked) {
                exitAction();
                hasClicked = true;
            }
        }
    };

    public static class Tutorial_0 extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.tutorial_0, container, false);
        }
    }

    public static class Tutorial_1 extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.tutorial_1, container, false);
        }
    }

    public static class Tutorial_2 extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.tutorial_2, container, false);
        }
    }
}