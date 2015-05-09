package com.geecko.QuickLyric.utils;

import android.animation.ArgbEvaluator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;

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
public class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter implements ViewPager.OnPageChangeListener {
    public boolean rightToLeft = false;
    private Integer[] colors = new Integer[]{
            android.R.color.holo_orange_dark,
            android.R.color.holo_red_light,
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

    public ScreenSlidePagerAdapter(FragmentManager fm, Activity activity) {
        super(fm);
        this.mActivity = activity;
        mPager = ((ViewPager) mActivity.findViewById(R.id.pager));
        if (Build.VERSION.SDK_INT >= 17)
            rightToLeft = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == 1;
        if (rightToLeft) {
            List<Integer> list = Arrays.asList(colors);
            Collections.reverse(list);
            colors = (Integer[]) list.toArray();
        }
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
        return ((MainActivity) mActivity).mDrawerToggle == null ? 4 : 5;
    }

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
    }

    @Override
    public void onPageSelected(int position) {
        Button pagerButton = ((Button) mActivity.findViewById(R.id.pager_button));
        Button pagerArrow = ((Button) mActivity.findViewById(R.id.pager_arrow));
        if ((rightToLeft && position == 0) || (!rightToLeft && position >= getCount() - 1)) {
            pagerButton.setText("");
            pagerButton.setEnabled(false);
            pagerArrow.setText(android.R.string.ok);
            pagerArrow.setOnClickListener(exitClickListener);
        } else {
            pagerButton.setText(R.string.skip);
            pagerButton.setEnabled(true);
            pagerArrow.setText(R.string.pager_arrow);
            pagerArrow.setOnClickListener(nextClickListener);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
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
                        mActivity.getSharedPreferences("tutorial", Context.MODE_PRIVATE).edit();
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
        // Drawer page
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.tutorial_4, container, false);
        }
    }
}