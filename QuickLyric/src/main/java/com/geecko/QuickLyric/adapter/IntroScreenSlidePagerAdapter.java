package com.geecko.QuickLyric.adapter;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.Html;
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
import android.widget.TextView;
import android.widget.Toast;

import com.geecko.QuickLyric.AboutActivity;
import com.geecko.QuickLyric.BuildConfig;
import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.fragment.LyricsViewFragment;
import com.geecko.QuickLyric.services.NotificationListenerService;
import com.geecko.QuickLyric.utils.AnimatorActionListener;
import com.geecko.QuickLyric.utils.PermissionsChecker;
import com.geecko.QuickLyric.utils.WhiteListUtil;
import com.geecko.QuickLyric.view.BubblePopImageView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static android.support.v4.widget.DrawerLayout.LOCK_MODE_UNLOCKED;

/**
 * This file is part of QuickLyric
 * Copyright © 2017 QuickLyric SPRL
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
            R.color.material_red_A700
    };
    private Class[] tutorialScreens = new Class[]{
            Tutorial_0.class,
            Tutorial_1.class,
            Tutorial_2.class,
            Tutorial_4.class
    };
    private Activity mActivity;
    private ViewPager mPager;
    private boolean hasClicked = false;
    private int mCurrentPage;

    private final View.OnTouchListener exitTouchListener = new View.OnTouchListener() {
        VelocityTracker mVelocityTracker;
        float mDownX;
        float mSwipeSlop = -1;
        boolean mSwiping;

        @Override
        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouch(final View v, MotionEvent event) {
            if (mCurrentPage != (rightToLeft ? 0 : getCount() - 1) || !Tutorial_4.nlEnabled)
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
                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                    break;
                case android.view.MotionEvent.ACTION_MOVE: {
                    if (mVelocityTracker != null)
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
                            ((View) v.getParent()).setAlpha(1f);
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
                                .setListener(new AnimatorActionListener(() -> {
                                    // Restore animated values
                                    if (remove) {
                                        int MOVE_DURATION = 500;
                                        v.animate().setDuration(MOVE_DURATION).translationY(0);
                                        v.animate().setListener(new AnimatorActionListener(() -> {
                                            mSwiping = false;
                                            IntroScreenSlidePagerAdapter.this.onAnimationEnd();
                                        }, AnimatorActionListener.ActionType.END));
                                    } else {
                                        ((View) v.getParent()).setAlpha(1);
                                        v.setTranslationX(0);
                                    }
                                }, AnimatorActionListener.ActionType.END));
                    }
                }
                mSwiping = false;
                break;
                default:
                    return true;
            }
            return false;
        }
    };


    public IntroScreenSlidePagerAdapter(final Activity activity) {
        super(activity.getFragmentManager());
        this.mActivity = activity;
        mPager = mActivity.findViewById(R.id.pager);
        mPager.setOnTouchListener(exitTouchListener);
        if (Build.VERSION.SDK_INT >= 17)
            rightToLeft = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == 1;
        if (rightToLeft) {
            List<Integer> list = Arrays.asList(colors);
            Collections.reverse(list);
            colors = (Integer[]) list.toArray();
        }
        ImageButton pagerArrow = mActivity.findViewById(R.id.pager_arrow);
        Button okButton = mActivity.findViewById(R.id.pager_ok);
        okButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !Tutorial_4.nlEnabled) {
                final ViewGroup nlFrame = activity.findViewById(R.id.NL_frame);
                final ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                        Color.parseColor("#30000000"), Color.parseColor("#80FFFFFF"));
                colorAnimation.addUpdateListener(animation -> nlFrame.setBackgroundColor((int) animation.getAnimatedValue()));
                colorAnimation.setInterpolator(new LinearOutSlowInInterpolator());
                colorAnimation.setRepeatCount(3);
                colorAnimation.setRepeatMode(ValueAnimator.REVERSE);
                colorAnimation.setDuration(650L);
                colorAnimation.start();
            } else if (!hasClicked) {
                exitAction();
                hasClicked = true;
            }
        });
        pagerArrow.setOnClickListener(v -> nextAction());
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
        int count = 3;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            count += 1;
        }
        return count;
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
        MainActivity.setNavBarColor(mActivity.getWindow(), mActivity.getTheme(), (int)
                evaluator.evaluate(0.5f, mActivity.getResources().getColor(R.color.action_dark),
                        background));
        MainActivity.setStatusBarColor(mActivity.getWindow(), mActivity.getTheme(), (int)
                evaluator.evaluate(0.5f, mActivity.getResources().getColor(R.color.action_dark),
                        background));

        View gearA;
        View gearB;

        BubblePopImageView tableImageView = tutorialLayout.findViewById(R.id.table);
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
                View bigFab = tutorialLayout.findViewById(R.id.big_fab);
                if (bigFab != null) {
                    bigFab.setTranslationX((rightToLeft ? -1f : 1f) *
                            (1f - positionOffset) * (bigFab.getMeasuredWidth() / 3f));
                    if (mCurrentPage == 1 ^ rightToLeft)
                        bigFab.setRotation(positionOffset * 360f);
                    else
                        bigFab.setRotation((1f - positionOffset) * 360f);
                }
                break;
            /*case 2:
                View redKey = tutorialLayout.findViewById(R.id.intro_3_red_key);
                View yellowKey = tutorialLayout.findViewById(R.id.intro_3_yellow_key);
                if (redKey != null && yellowKey != null) {
                    if (redKey.getMeasuredHeight() < redKey.getResources().getDimensionPixelSize(R.dimen.dp) * 15) {
                        redKey.setVisibility(View.INVISIBLE);
                        yellowKey.setVisibility(View.INVISIBLE);
                        break;
                    } else {
                        redKey.setVisibility(View.VISIBLE);
                        yellowKey.setVisibility(View.VISIBLE);
                    }
                    redKey.setTranslationY(330f * (1 - positionOffset));
                    yellowKey.setTranslationY(290f * Math.min(1.3f * (1 - positionOffset), 1.0f));
                    yellowKey.setTranslationX(105f * Math.min(1.3f * (1 - positionOffset), 1.0f));
                }
                gearA = tutorialLayout.findViewById(R.id.redGear);
                gearB = tutorialLayout.findViewById(R.id.blueGear);
                if (3 == count - 2 && gearA != null && gearB != null) {
                    gearA.setRotation(-180f * positionOffset);
                    gearB.setRotation(180f * positionOffset);
                }
                break;*/
            case 2:
                gearA = tutorialLayout.findViewById(R.id.redGear);
                gearB = tutorialLayout.findViewById(R.id.blueGear);
                if (gearA != null && gearB != null) {
                    gearA.setRotation(-180f * positionOffset);
                    gearB.setRotation(180f * positionOffset);
                }
                break;
        }
    }

    @Override
    public void onPageSelected(int position) {
        Button pagerButton = mActivity.findViewById(R.id.pager_button);
        ImageButton pagerArrow = mActivity.findViewById(R.id.pager_arrow);
        Button okButton = mActivity.findViewById(R.id.pager_ok);
        if ((rightToLeft && position == 0) || (!rightToLeft && position >= getCount() - 1)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                pagerButton.setText("");
                pagerButton.setEnabled(false);
            } else {
                pagerButton.setText(R.string.no_results_why);
                pagerButton.setOnClickListener((v) -> new AlertDialog.Builder(v.getContext()).setTitle(v.getContext().getString(R.string.notification_access))
                        .setMessage(Html.fromHtml(v.getContext().getString(R.string.notification_access_explanation)))
                        .setIcon(R.drawable.icon)
                        .show());
            }
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
                IntroScreenSlidePagerAdapter.this.onAnimationEnd();
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }
        });
        ((RelativeLayout) mPager.getParent()).startAnimation(slideOut);
    }

    private void onAnimationEnd() {
        ((RelativeLayout) mPager.getParent()).setVisibility(View.GONE);
        if (mActivity instanceof MainActivity) {
            ((MainActivity) mActivity).focusOnFragment = true;
            if (((MainActivity) mActivity).mDrawerToggle != null)
                ((DrawerLayout) ((MainActivity) mActivity).drawer).setDrawerLockMode(LOCK_MODE_UNLOCKED);
            mActivity.invalidateOptionsMenu();
            SharedPreferences.Editor editor =
                    mActivity.getSharedPreferences("intro_slides", Context.MODE_PRIVATE).edit();
            editor.putBoolean("seen", true);
            editor.apply();
            if (mActivity instanceof MainActivity) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                        !NotificationListenerService.isAppScrobbling(mActivity)) {
                    LyricsViewFragment lyricsViewFragment = (LyricsViewFragment)
                            mActivity.getFragmentManager().findFragmentByTag(MainActivity.LYRICS_FRAGMENT_TAG);
                    lyricsViewFragment.showBrokenScrobblingWarning();
                }
            }
            MainActivity.setStatusBarColor(mActivity.getWindow(),
                    mActivity.getTheme(), null);
        } else if (mActivity instanceof AboutActivity)
            ((AboutActivity) mActivity).setStatusBarColor(null);

        MainActivity.setNavBarColor(mActivity.getWindow(),
                mActivity.getTheme(), null);
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
            BubblePopImageView tableImageView = getActivity().findViewById(R.id.table);
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
        // Ad page
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.tutorial_3, container, false);
        }
    }

    @SuppressWarnings("deprecation")
    public static class Tutorial_4 extends Fragment {
        private static final int OVERLAY_REQUEST_CODE = 14118;
        // Last page: optional NotificationListener page

        static boolean nlEnabled = true;
        static boolean buttonClicked = false;
        static boolean autostartEnabled = false;
        private boolean floatingEnabledDefault;

        @SuppressLint("NewApi")
        @Override
        public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View layout = inflater.inflate(R.layout.tutorial_4, container, false);

            TextView link = layout.findViewById(R.id.NL_link);
            View floatingFrame = layout.findViewById(R.id.floating_frame);
            nlEnabled = NotificationListenerService.isListeningAuthorized(getActivity());
            ((ViewGroup) link.getParent()).setOnClickListener(v -> {
                if (!nlEnabled || BuildConfig.DEBUG) {
                    if (!autostartEnabled && WhiteListUtil.openBootSpecialMenu(inflater.getContext())) {
                        Toast.makeText(getActivity(), getString(R.string.miui_autostart, Build.BRAND), Toast.LENGTH_LONG).show();
                        autostartEnabled = true;
                    } else {
                        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                        Toast.makeText(getActivity(), R.string.nls_enable_prompt, Toast.LENGTH_LONG).show();
                        MainActivity.waitingForListener = true;
                        final BroadcastReceiver nlsReceiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                if (context != null) {
                                    intent = new Intent(context, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent);
                                    context.unregisterReceiver(this);
                                }
                            }
                        };
                        final IntentFilter iFilter = new IntentFilter("com.geecko.QuickLyric.NLS_CONNECTED");
                        layout.getContext().registerReceiver(nlsReceiver, iFilter);
                        buttonClicked = true;
                    }
                    SharedPreferences prefs = getActivity().getSharedPreferences("intro_slides", Context.MODE_PRIVATE);
                }
            });
            if (floatingEnabledDefault)
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("pref_overlay", true).apply();

            showFloatingFrameButton(layout);
            floatingFrame.setOnClickListener(view -> {
                boolean currentState = ((SwitchCompat) getView().findViewById(R.id.floating_switch)).isChecked();
                if (!currentState && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(getActivity())) {
                    final Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getActivity().getPackageName()));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    ((SwitchCompat) getView().findViewById(R.id.floating_switch)).setChecked(!currentState);
                    PermissionsChecker.displayMIUIPopupPermission(getActivity());
                }
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("pref_overlay", !currentState).apply();
                SharedPreferences prefs = getActivity().getSharedPreferences("intro_slides", Context.MODE_PRIVATE);
            });
            ((ViewGroup) link.getParent()).setClickable(true);
            return layout;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == OVERLAY_REQUEST_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getView().postDelayed(() -> {
                    boolean floatingEnabled = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_overlay", false);
                    boolean floatingPermitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(getActivity());
                    ((SwitchCompat) getView().findViewById(R.id.floating_switch)).setChecked(floatingEnabled && floatingPermitted);
                }, 650);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
                return;
            boolean floatingEnabled = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_overlay", floatingEnabledDefault);
            boolean floatingPermitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(getActivity());
            nlEnabled = NotificationListenerService.isListeningAuthorized(getActivity());
            ((SwitchCompat) getView().findViewById(R.id.NL_switch)).setChecked(nlEnabled);
            ((SwitchCompat) getView().findViewById(R.id.floating_switch)).setChecked(floatingEnabled && floatingPermitted);
            View okButton = getActivity().findViewById(R.id.pager_ok);
            if (okButton != null)
                getActivity().findViewById(R.id.pager_ok).setAlpha(nlEnabled ? 1f : 0.4f);
            showFloatingFrameButton(getView());
            MainActivity.waitingForListener = false;
        }

        @TargetApi(19)
        private void showFloatingFrameButton(View layout) {
            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            boolean shouldHideFloatingFrame = false;

            ViewGroup frame = layout.findViewById(R.id.NL_frame);
            View floatingFrame = layout.findViewById(R.id.floating_frame);
            floatingFrame.setVisibility(nlEnabled && !shouldHideFloatingFrame ? View.VISIBLE : isLandscape ? View.GONE : View.INVISIBLE);
            if (frame != null) {
                frame.setVisibility(isLandscape && nlEnabled ? View.INVISIBLE : View.VISIBLE);
                RelativeLayout.LayoutParams frameParams = (RelativeLayout.LayoutParams) frame.getLayoutParams();

                frameParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                frameParams.setMargins(frameParams.leftMargin, frameParams.topMargin, frameParams.rightMargin, 0);
                frameParams.removeRule(RelativeLayout.CENTER_VERTICAL);
                frame.setLayoutParams(frameParams);
            }
        }
    }
}
