/*
 * *
 *  * This file is part of QuickLyric
 *  * Created by geecko
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

package com.geecko.QuickLyric.services;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatDelegate;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.geecko.QuickLyric.App;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.utils.AnimatorActionListener;
import com.geecko.QuickLyric.utils.NightTimeVerifier;
import com.geecko.QuickLyric.view.OverlayContentLayout;
import com.geecko.QuickLyric.view.OverlayLayout;

import io.codetail.animation.ViewAnimationUtils;
import jp.co.recruit_lifestyle.android.floatingview.FloatingView;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewListener;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewManager;

import static android.os.Build.VERSION_CODES.M;

public class LyricsOverlayService extends Service implements FloatingViewListener, OverlayLayout.OverlayLayoutListener, View.OnTouchListener {

    private static final int NOTIFICATION_ID = 908114;

    private static final String PREF_KEY_LAST_POSITION_X = "last_position_x";
    private static final String PREF_KEY_LAST_POSITION_Y = "last_position_y";

    private static final String UPDATE_NOTIFICATION_ACTION = "notification_action";
    public static final String HIDE_FLOATING_ACTION = "hide_action";
    public static final String SHOW_FLOATING_ACTION = "show_action";
    private static final String STOP_FLOATING_ACTION = "stop_action";
    public static final String CLICKED_FLOATING_ACTION = "clicked_action";

    public static boolean sRunning;;

    private FloatingViewManager mFloatingViewManager;
    private WindowManager mWindowManager;
    private View mBubbleView;
    private Lyrics mLyrics;
    private OverlayLayout mOverlayWindow;
    private int deployedMarginX;
    private int deployedMarginY;
    private boolean mInOverlay;
    private BroadcastReceiver receiver;
    private boolean mDoPullBack;
    private boolean mSizeHasChanged;
    private int selectedTheme;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean overlayOnclick = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_overlay_behavior", "0")) == 1;
        if (mFloatingViewManager != null) {
            if (intent != null) {
                if (HIDE_FLOATING_ACTION.equals(intent.getAction())) {
                    if (mBubbleView == null)
                        mFloatingViewManager.setDisplayMode(FloatingViewManager.DISPLAY_MODE_HIDE_ALWAYS);
                    else {
                        mBubbleView.animate().alpha(0f).setDuration(200).setInterpolator(new AccelerateInterpolator())
                                .setListener(new AnimatorActionListener(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mFloatingViewManager != null)
                                            mFloatingViewManager.setDisplayMode(FloatingViewManager.DISPLAY_MODE_HIDE_ALWAYS);
                                    }
                                }, AnimatorActionListener.ActionType.END));
                    }
                } else if (SHOW_FLOATING_ACTION.equals(intent.getAction())) {
                    if (mBubbleView == null)
                        mFloatingViewManager.setDisplayMode(FloatingViewManager.DISPLAY_MODE_HIDE_FULLSCREEN);
                    else {
                        mBubbleView.setAlpha(0f);
                        mFloatingViewManager.setDisplayMode(FloatingViewManager.DISPLAY_MODE_HIDE_FULLSCREEN);
                        mBubbleView.animate().alpha(1f).setDuration(128).setInterpolator(new DecelerateInterpolator());
                        if (mFloatingViewManager.getTargetFloatingView() != null)
                            mFloatingViewManager.getTargetFloatingView().setOnTouchListener(this);
                    }
                } else if (STOP_FLOATING_ACTION.equals(intent.getAction()) && !isInOverlay()) {
                    this.sRunning = false;
                    if (mBubbleView != null) {
                        mBubbleView.animate().alpha(0f).setDuration(200).setInterpolator(new AccelerateInterpolator())
                                .setListener(new AnimatorActionListener(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            destroy();
                                        } catch (Exception ignored) {
                                        }
                                        mBubbleView = null;
                                        stopForeground(true);
                                    }
                                }, AnimatorActionListener.ActionType.END));
                    } else {
                        mFloatingViewManager = null;
                        stopForeground(true);
                    }
                } else if (CLICKED_FLOATING_ACTION.equals(intent.getAction())) {
                    if (!App.isAppVisible()) {
                        if (mBubbleView == null) {
                            createBubbleView(intent);
                            createOverlayWindow();
                        }
                        if (mFloatingViewManager.getTargetFloatingView() != null)
                            mFloatingViewManager.getTargetFloatingView().setOnTouchListener(this);
                        if (!mInOverlay) {
                            int[] positions = moveBubbleForOverlay();
                            doOverlay(positions);
                        }
                    }
                } else if (UPDATE_NOTIFICATION_ACTION.equals(intent.getAction())) {
                    Notification notif = (Notification) intent.getExtras().get("notification");
                    NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notif);
                    if (isInOverlay()) {
                        OverlayContentLayout overlayContent = (OverlayContentLayout) mOverlayWindow.getTag();
                        overlayContent.onMetadataParsed(intent.getExtras().getStringArray("metadata"), false, false);
                    }
                }
            }
            return START_STICKY;
        } else if ((Build.VERSION.SDK_INT < M || Settings.canDrawOverlays(this)) &&
                (intent != null && intent.getExtras() != null && intent.getExtras().get("notification") != null) && !App.isAppVisible()) {
            this.sRunning = true;

            mFloatingViewManager = new FloatingViewManager(this, this);
            mFloatingViewManager.setFixedTrashIconImage(R.drawable.ic_overlay_close);
            mFloatingViewManager.setActionTrashIconImage(R.drawable.ic_overlay_action);
            loadDynamicOptions();

            if (mWindowManager == null)
                mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

            if (!overlayOnclick)
                createBubbleView(intent);
            createOverlayWindow();
            if (mFloatingViewManager.getTargetFloatingView() != null)
                mFloatingViewManager.getTargetFloatingView().setOnTouchListener(this);

            startForeground(NOTIFICATION_ID, (Notification) intent.getExtras().get("notification"));
        }
        return START_REDELIVER_INTENT;
    }

    @SuppressLint("InflateParams")
    private void createOverlayWindow() {
        if (mOverlayWindow != null)
            return;

        selectedTheme = getSelectedTheme();
        setTheme(selectedTheme);

        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);

        mOverlayWindow = (OverlayLayout) LayoutInflater.from(new ContextThemeWrapper(getBaseContext(), selectedTheme)).inflate(R.layout.overlay_window, null, false);
        OverlayContentLayout overlayContentLayout = mOverlayWindow.findViewById(R.id.overlay_content);
        overlayContentLayout.setTag(this);
        mOverlayWindow.setTag(overlayContentLayout);
        mOverlayWindow.setListener(this);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onBackpressed();
            }
        };
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(Intent.ACTION_SCREEN_OFF);
        iFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(receiver, iFilter);
    }

    @SuppressLint("InflateParams")
    private void createBubbleView(Intent intent) {
        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        LayoutInflater inflater = LayoutInflater.from(this);
        mBubbleView = inflater.inflate(R.layout.floating_bubble, null, false);
        mBubbleView.setAlpha(0f);
        mBubbleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FloatingView floatingView = mFloatingViewManager.getTargetFloatingView();
                WindowManager.LayoutParams lp = (WindowManager.LayoutParams) floatingView.getLayoutParams();
                final int x = lp.x;
                final int y = lp.y;
                if (!isInOverlay()) {
                    moveBubbleForOverlay();
                } else {
                    exitOverlay(true, false, x, y);
                }
            }
        });
        mBubbleView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return true;
            }
        });
        final FloatingViewManager.Options options = loadOptions(metrics);
        mFloatingViewManager.addViewToWindow(mBubbleView, options);

        if (intent == null || !HIDE_FLOATING_ACTION.equals(intent.getAction()))
            mBubbleView.animate().alpha(1f).setDuration(200).setInterpolator(new AccelerateInterpolator());

        if (this.deployedMarginX == 0)
            this.deployedMarginX = (int) (17 * metrics.density);

        if (this.deployedMarginY == 0)
            this.deployedMarginY = (int) (3 * metrics.density);
    }

    private boolean isInOverlay() {
        return mOverlayWindow != null && mOverlayWindow.getWindowToken() != null;
    }

    private int[] moveBubbleForOverlay() {
        if (mFloatingViewManager == null)
            return null;

        this.mInOverlay = true;
        this.mDoPullBack = false;

        final FloatingView floatingView = mFloatingViewManager.getTargetFloatingView();
        final WindowManager.LayoutParams lp = (WindowManager.LayoutParams) floatingView.getLayoutParams();
        floatingView.post(new Runnable() {
            @Override
            public void run() {
                floatingView.moveTo(lp.x, lp.y, floatingView.getPositionLimits().right - deployedMarginX, floatingView.getPositionLimits().bottom - deployedMarginY, true);
            }
        });
        return new int[] {lp.x, lp.y};
    }

    private void doOverlay(int... params) {
        if (params == null || params.length != 2)
            return;

        int startX = params[0];
        int startY = params[1];

        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);

        if (mSizeHasChanged || selectedTheme != getSelectedTheme()) {
            mSizeHasChanged = false;
            if (isInOverlay())
                mWindowManager.removeView(mOverlayWindow);
            mOverlayWindow = null;
            createOverlayWindow();
        }
        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mOverlayWindow.getLayoutParams();
        layoutParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount = 0.65f;
        mOverlayWindow.setRevealCenter(startX + (mBubbleView.getWidth() / 2), metrics.heightPixels - startY - (mBubbleView.getHeight() / 2));
        mOverlayWindow.setVisibility(View.VISIBLE);
        mWindowManager.addView(mOverlayWindow, layoutParams);
    }

    private void exitOverlay(final boolean moveBubble, final boolean stopService, Object... params) {
        if (!this.mInOverlay)
            return;

        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);

        int cx;
        int cy;
        this.mInOverlay = false;
        if (moveBubble) {
            int x = (Integer) params[0];
            int y = (Integer) params[1];
            final SharedPreferences savedPosition = getSharedPreferences("overlay_position", Context.MODE_PRIVATE);
            cx = savedPosition.getInt(PREF_KEY_LAST_POSITION_X, x);
            cy = savedPosition.getInt(PREF_KEY_LAST_POSITION_Y, y);
            Rect limits = mFloatingViewManager.getTargetFloatingView().getPositionLimits();
            cx = Math.abs(cx - limits.right) < cx ? limits.right : limits.left;
            mFloatingViewManager.getTargetFloatingView().moveTo(x, y, cx, cy, true);
            mFloatingViewManager.getTargetFloatingView().setBlockMoveToEdge(false);
        } else {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) mFloatingViewManager.getTargetFloatingView().getLayoutParams();
            cx = lp.x;
            cy = lp.y;
        }
        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mOverlayWindow.getLayoutParams();
        layoutParams.flags &= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        mWindowManager.updateViewLayout(mOverlayWindow, layoutParams);

        cx = cx + (mBubbleView.getWidth() / 2);
        cy = metrics.heightPixels - cy - (mBubbleView.getHeight() / 2);

        int dx = Math.max(cx, mOverlayWindow.getWidth() - cx);
        int dy = Math.max(cy, mOverlayWindow.getHeight() - cy);
        float finalRadius = (float) Math.hypot(dx, dy);

        mOverlayWindow.setRevealCenter((Integer) null);
        Animator animator =
                ViewAnimationUtils.createCircularReveal(mOverlayWindow.getChildAt(0), cx, cy, finalRadius, 0);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(300L);
        animator.addListener(new AnimatorActionListener(new Runnable() {
            @Override
            public void run() {
                mOverlayWindow.setVisibility(View.INVISIBLE);
                if (mOverlayWindow != null && mOverlayWindow.getWindowToken() != null)
                    mWindowManager.removeView(mOverlayWindow);
                if (stopService)
                    stopSelf();
            }
        }, AnimatorActionListener.ActionType.END));
        animator.start();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        destroy();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onFinishFloatingView() {
        if (isInOverlay())
            exitOverlay(false, true);
        else
            stopSelf();
    }

    @Override
    public void onTouchFinished(boolean isFinishing, int x, int y) {
        if (!isFinishing) {
            int left = mFloatingViewManager.getTargetFloatingView().getPositionLimits().left;
            int right = mFloatingViewManager.getTargetFloatingView().getPositionLimits().right;
            int closestSide = Math.min(Math.abs(x - right), x) == x ? Math.min(x, left) : Math.max(x, right);

            // Save the last position
            final SharedPreferences.Editor editor = getSharedPreferences("overlay_position", Context.MODE_PRIVATE).edit();
            editor.putInt(PREF_KEY_LAST_POSITION_X, closestSide);
            editor.putInt(PREF_KEY_LAST_POSITION_Y, y);
            editor.apply();

            if (mDoPullBack)
                moveBubbleForOverlay();

            if (mInOverlay && mOverlayWindow.getWindowToken() == null)
                doOverlay(x, y);
        }
    }

    private void destroy() {
        if (mFloatingViewManager != null) {
            mFloatingViewManager.removeAllViewToWindow();
            mFloatingViewManager = null;
        }
    }

    private int getSelectedTheme() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int[] themes = new int[]{R.style.Theme_QuickLyric, R.style.Theme_QuickLyric_Red,
                R.style.Theme_QuickLyric_Purple, R.style.Theme_QuickLyric_Indigo,
                R.style.Theme_QuickLyric_Green, R.style.Theme_QuickLyric_Lime,
                R.style.Theme_QuickLyric_Brown, R.style.Theme_QuickLyric_Dark};

        int selectedTheme;
        int themeNum = Integer.valueOf(sharedPref.getString("pref_theme", "0"));
        boolean nightMode = sharedPref.getBoolean("pref_night_mode", false);
        if (nightMode && NightTimeVerifier.check(this))
            selectedTheme = R.style.Theme_QuickLyric_Night;
        else
            selectedTheme = themes[themeNum];
        return selectedTheme;
    }

    private void loadDynamicOptions() {
        mFloatingViewManager.setDisplayMode(App.isAppVisible() ?
                FloatingViewManager.DISPLAY_MODE_HIDE_ALWAYS : FloatingViewManager.DISPLAY_MODE_HIDE_FULLSCREEN);
    }

    private FloatingViewManager.Options loadOptions(DisplayMetrics metrics) {
        final FloatingViewManager.Options options = new FloatingViewManager.Options();

        options.shape = FloatingViewManager.SHAPE_CIRCLE;
        options.usePhysics = true;
        options.moveDirection = FloatingViewManager.MOVE_DIRECTION_THROWN;
        options.animateInitialMove = true;
        options.overMargin = (int) (12 * metrics.density);
        options.floatingViewX = metrics.widthPixels;
        options.floatingViewY = (int) (metrics.heightPixels * 0.66f);

        final SharedPreferences savedPosition = getSharedPreferences("overlay_position", Context.MODE_PRIVATE);
        options.floatingViewX = savedPosition.getInt(PREF_KEY_LAST_POSITION_X, options.floatingViewX);
        options.floatingViewY = savedPosition.getInt(PREF_KEY_LAST_POSITION_Y, options.floatingViewY);

        options.floatingViewX += options.floatingViewX == 0 ? -options.overMargin : options.overMargin;

        return options;
    }

    public static boolean isRunning() {
        return sRunning;
    }

    public static void showCustomFloatingView(Context context, Notification notif, String[] metadata) {
        if (Build.VERSION.SDK_INT < M || Settings.canDrawOverlays(context)) {
            Intent intent = new Intent(context, LyricsOverlayService.class);
            intent.setAction(UPDATE_NOTIFICATION_ACTION);
            intent.putExtra("notification", notif);
            intent.putExtra("metadata", metadata);
            context.getApplicationContext().startService(intent);
        }
    }

    public static void removeCustomFloatingView(Context context) {
        Intent makeItStop = new Intent(context, LyricsOverlayService.class);
        makeItStop.setAction(LyricsOverlayService.STOP_FLOATING_ACTION);
        context.getApplicationContext().startService(makeItStop);
    }

    @Override
    public void onBackpressed() {
        if (isInOverlay()) {
            WindowManager.LayoutParams lp =
                    (WindowManager.LayoutParams) mFloatingViewManager.getTargetFloatingView().getLayoutParams();
            exitOverlay(true, false, lp.x, lp.y);
        }
    }

    @Override
    public void onSizeChanged() {
        mSizeHasChanged = true;
        if (isInOverlay()) {
            createOverlayWindow();
            moveBubbleForOverlay();
            doOverlay(0, 0);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        boolean output = mFloatingViewManager.onTouch(view, event);
        if (isInOverlay()) {
            if (event.getAction() == MotionEvent.ACTION_MOVE && !mDoPullBack) {
                exitOverlay(false, false);
                mDoPullBack = true;
                mFloatingViewManager.getTargetFloatingView().setBlockMoveToEdge(true);
            }
        }

        return output;
    }

    public Lyrics getLyrics() {
        return mLyrics;
    }

    public void setLyrics(Lyrics mLyrics) {
        this.mLyrics = mLyrics;
    }

}