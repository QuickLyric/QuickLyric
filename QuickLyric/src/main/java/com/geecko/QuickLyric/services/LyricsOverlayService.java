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
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.ContextThemeWrapper;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import com.geecko.QuickLyric.App;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.tasks.DownloadThread;
import com.geecko.QuickLyric.utils.AnimatorActionListener;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.NightTimeVerifier;
import com.geecko.QuickLyric.utils.ResizeHandleTouchListener;
import com.geecko.QuickLyric.view.OverlayContentLayout;
import com.geecko.QuickLyric.view.OverlayLayout;

import java.lang.ref.WeakReference;

import io.codetail.animation.ViewAnimationUtils;
import jp.co.recruit_lifestyle.android.floatingview.FloatingView;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewListener;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewManager;

import static android.os.Build.VERSION_CODES.M;
import static com.geecko.QuickLyric.utils.NotificationUtil.NOTIFICATION_ID;

public class LyricsOverlayService extends Service implements FloatingViewListener, OverlayLayout.OverlayLayoutListener, View.OnTouchListener, ResizeHandleTouchListener.ResizeHandleCallback {

    private static final String PREF_KEY_LAST_POSITION_X = "last_position_x";
    private static final String PREF_KEY_LAST_POSITION_Y = "last_position_y";

    private static final String UPDATE_NOTIFICATION_ACTION = "notification_action";
    public static final String HIDE_FLOATING_ACTION = "hide_action";
    public static final String SHOW_FLOATING_ACTION = "show_action";
    private static final String STOP_FLOATING_ACTION = "stop_action";
    public static final String CLICKED_FLOATING_ACTION = "clicked_action";
    private static final String WINDOW_DIMENSIONS_PREF = "overlay_window_dimensions";

    public static boolean sRunning;
    private static Handler sStopHandler = new Handler();
    private static Runnable sRunnable;

    private static Lyrics sLyrics;
    private FloatingViewManager mFloatingViewManager;
    private WindowManager mWindowManager;
    private View mBubbleView;
    private OverlayLayout mOverlayWindow;
    private int deployedMarginX;
    private int deployedMarginY;
    private boolean mInOverlay;
    private BroadcastReceiver receiver;
    private boolean mDoPullBack;
    private boolean mSizeHasChanged;
    private int selectedTheme;
    public boolean launchCountRaised;
    private boolean mIsMoving;
    private boolean mResizing;
    private int mSnackBarSize;
    private int[] originalWindowDimensions;
    private boolean mBubbleHidden = false;
    private ViewPropertyAnimator mCurrentAnimator;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean overlayOnclick = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_overlay_behavior", "0")) == 1;
        if (mFloatingViewManager != null && (mFloatingViewManager.getTargetFloatingView() == null || mFloatingViewManager.getTargetFloatingView().getWindowToken() != null)) {
            if (HIDE_FLOATING_ACTION.equals(intent.getAction())) {
                if (mBubbleView == null)
                    mFloatingViewManager.setDisplayMode(FloatingViewManager.DISPLAY_MODE_HIDE_ALWAYS);
                else if (!isInOverlay()) {
                    mBubbleHidden = true;
                    mCurrentAnimator = mBubbleView.animate().alpha(0f).setDuration(200).setInterpolator(new AccelerateInterpolator())
                            .setListener(new AnimatorActionListener(() -> {
                                if (mFloatingViewManager != null)
                                    mFloatingViewManager.setDisplayMode(FloatingViewManager.DISPLAY_MODE_HIDE_ALWAYS);
                            }, AnimatorActionListener.ActionType.END));
                }
            } else if (SHOW_FLOATING_ACTION.equals(intent.getAction())) {
                if (mBubbleHidden) {
                    mBubbleHidden = false;
                    mCurrentAnimator.setListener(null);
                    mCurrentAnimator.cancel();
                    mCurrentAnimator = null;
                }
                if (mBubbleView == null)
                    mFloatingViewManager.setDisplayMode(FloatingViewManager.DISPLAY_MODE_HIDE_FULLSCREEN);
                else {
                    mFloatingViewManager.setDisplayMode(FloatingViewManager.DISPLAY_MODE_HIDE_FULLSCREEN);
                    mBubbleView.post(() -> {
                        mCurrentAnimator = mBubbleView.animate().alpha(1f).setDuration(128).setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorActionListener(() -> {
                                    if (mBubbleHidden)
                                        mBubbleView.setAlpha(0f);
                                }, AnimatorActionListener.ActionType.END));
                    });
                    if (mFloatingViewManager.getTargetFloatingView() != null)
                        mFloatingViewManager.getTargetFloatingView().setOnTouchListener(this);
                }
                if (intent.getExtras() != null) {
                    if (intent.getExtras().get("lyrics") != null) {
                        setLyrics((Lyrics) intent.getExtras().get("lyrics"));
                        OverlayContentLayout overlayContentLayout = (((OverlayContentLayout) mOverlayWindow.getTag()));
                        if (overlayContentLayout != null)
                            overlayContentLayout.onLyricsDownloaded((Lyrics) intent.getExtras().get("lyrics"));
                    }
                    if (intent.getExtras().get("notification") != null) {
                        intent.setAction(UPDATE_NOTIFICATION_ACTION);
                        onStartCommand(intent, flags, startId);
                    }
                }
            } else if (STOP_FLOATING_ACTION.equals(intent.getAction()) && !isInOverlay()) {
                sRunning = false;
                if (mBubbleView != null) {
                    mBubbleHidden = true;
                    mBubbleView.animate().alpha(0f).setDuration(200).setInterpolator(new AccelerateInterpolator())
                            .setListener(new AnimatorActionListener(() -> {
                                try {
                                    destroy();
                                } catch (Exception ignored) {
                                }
                                mBubbleView = null;
                                stopOverlayService();
                            }, AnimatorActionListener.ActionType.END));
                } else {
                    mFloatingViewManager = null;
                    stopOverlayService();
                }
            } else if (CLICKED_FLOATING_ACTION.equals(intent.getAction())) {
                if (!App.isAppVisible()) {
                    mFloatingViewManager.setDisplayMode(FloatingViewManager.DISPLAY_MODE_HIDE_FULLSCREEN);
                    if (mBubbleView == null || mFloatingViewManager.getTargetFloatingView() == null || !ViewCompat.isAttachedToWindow(mFloatingViewManager.getTargetFloatingView())) {
                        createOverlayWindow();
                        createBubbleView(intent);
                    } else if (!mInOverlay) {
                        int[] positions = moveBubbleForOverlay(true);
                        doOverlay(positions);
                    }
                    if (mFloatingViewManager.getTargetFloatingView() != null)
                        mFloatingViewManager.getTargetFloatingView().setOnTouchListener(this);
                }
            } else if (UPDATE_NOTIFICATION_ACTION.equals(intent.getAction())) {
                Notification notif = (Notification) intent.getExtras().get("notification");
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notif);
                if (isInOverlay()) {
                    OverlayContentLayout overlayContent = (OverlayContentLayout) mOverlayWindow.getTag();
                    overlayContent.onMetadataParsed(intent.getExtras().getStringArray("metadata"), intent.getExtras().getLong("duration"),
                            true, true, false);
                }
            }
            return START_STICKY;
        } else if ((Build.VERSION.SDK_INT < M || Settings.canDrawOverlays(this)) && !App.isAppVisible() && (mFloatingViewManager == null
                || !isServiceRunningInForeground(this, getClass())) && (intent == null ||
                !(intent.getAction().equals(STOP_FLOATING_ACTION) || intent.getAction().equals(HIDE_FLOATING_ACTION)))) {

            if (mFloatingViewManager != null) {
                mFloatingViewManager.removeAllViewToWindow();
            }

            mFloatingViewManager = new FloatingViewManager(this, this);
            mFloatingViewManager.setFixedTrashIconImage(R.drawable.ic_overlay_close);
            mFloatingViewManager.setActionTrashIconImage(R.drawable.ic_overlay_action);
            loadDynamicOptions();

            if (mWindowManager == null)
                mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

            createOverlayWindow();
            if (!overlayOnclick)
                createBubbleView(intent);
            if (mFloatingViewManager.getTargetFloatingView() != null)
                mFloatingViewManager.getTargetFloatingView().setOnTouchListener(this);

            if (intent != null && intent.getExtras() != null && intent.getExtras().get("notification") != null && isServiceRunningInForeground(this, getClass())) {
                sRunning = true;
                startForeground(NOTIFICATION_ID, (Notification) intent.getExtras().get("notification"));
            } else if (CLICKED_FLOATING_ACTION.equals(intent.getAction())) {
                onStartCommand(intent, flags, startId);
            }
        }
        return START_REDELIVER_INTENT;
    }

    @SuppressLint("InflateParams, RestrictedApi")
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

        View resizeHandle = mOverlayWindow.findViewById(R.id.overlay_resize_handle);
        resizeHandle.setOnClickListener((view) -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Toast toast = Toast.makeText(getApplicationContext(), R.string.overlay_resize_onclick_prompt, Toast.LENGTH_LONG);
                toast.show();
            } else {
                Snackbar.make(overlayContentLayout, R.string.overlay_resize_onclick_prompt, Snackbar.LENGTH_LONG)
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onShown(Snackbar sb) {
                                super.onShown(sb);
                                onSnackbarShown(sb);
                            }
                        })
                        .show();
            }
        });

        resizeHandle.setOnTouchListener(new ResizeHandleTouchListener(this, mWindowManager, getResources()));

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
        mBubbleView.setOnClickListener(v -> {
            if (mFloatingViewManager == null)
                return;
            FloatingView floatingView = mFloatingViewManager.getTargetFloatingView();
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) floatingView.getLayoutParams();
            final int x = lp.x;
            final int y = lp.y;
            if (!isInOverlay()) {
                boolean portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
                if (mOverlayWindow != null && (portrait != mOverlayWindow.getHeight() > mOverlayWindow.getWidth()))
                    mSizeHasChanged = true;
                moveBubbleForOverlay(true);
            } else {
                exitOverlay(true, false, x, y);
            }
        });
        mBubbleView.setOnLongClickListener(view -> true);
        final FloatingViewManager.Options options = loadOptions(metrics);
        if (!ViewCompat.isAttachedToWindow(mOverlayWindow) && mOverlayWindow.getParent() == null) {
            WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mOverlayWindow.getLayoutParams();
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            layoutParams.dimAmount = 0.65f;
            mWindowManager.addView(mOverlayWindow, layoutParams);
        }
        mFloatingViewManager.addViewToWindow(mBubbleView, options);

        if (intent == null || !HIDE_FLOATING_ACTION.equals(intent.getAction())) {
            mBubbleHidden = false;
            mBubbleView.animate().alpha(1f).setDuration(200).setInterpolator(new AccelerateInterpolator())
                    .setListener(new AnimatorActionListener(() -> {
                        if (intent != null && CLICKED_FLOATING_ACTION.equals(intent.getAction()) && !mInOverlay) {
                            mBubbleView.postDelayed(() -> {
                                int[] positions = moveBubbleForOverlay(true);
                                doOverlay(positions);
                            }, 400L); // FIXME
                        }
                    }, AnimatorActionListener.ActionType.END));
            sRunning = true;
        }

        if (this.deployedMarginX == 0)
            this.deployedMarginX = (int) (17 * metrics.density);

        if (this.deployedMarginY == 0)
            this.deployedMarginY = (int) (3 * metrics.density);
    }

    public boolean isInOverlay() {
        return mOverlayWindow != null && ViewCompat.isAttachedToWindow(mOverlayWindow) && mOverlayWindow.getVisibility() == View.VISIBLE;
    }

    private int[] moveBubbleForOverlay(boolean withAnimation) {
        if (mFloatingViewManager == null || mFloatingViewManager.getTargetFloatingView() == null)
            return null;

        this.mInOverlay = true;
        this.mDoPullBack = false;

        final FloatingView floatingView = mFloatingViewManager.getTargetFloatingView();
        floatingView.setClickable(false);
        final WindowManager.LayoutParams lp = (WindowManager.LayoutParams) floatingView.getLayoutParams();
        if (!mIsMoving && withAnimation) {
            floatingView.post(() -> {
                mIsMoving = true;
                floatingView.moveTo(lp.x, lp.y, floatingView.getPositionLimits().right - deployedMarginX, floatingView.getPositionLimits().bottom - deployedMarginY, withAnimation);
            });
        } else if (!withAnimation) {
            lp.x = floatingView.getPositionLimits().right - deployedMarginX;
            lp.y = floatingView.getPositionLimits().bottom - deployedMarginY;
            mWindowManager.updateViewLayout(floatingView, lp);
        }
        return new int[]{lp.x, lp.y};
    }

    private void doOverlay(int... params) {
        if (params == null || params.length != 2)
            return;

        int startX = params[0];
        int startY = params[1];

        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);

        final FloatingView floatingView = mFloatingViewManager.getTargetFloatingView();
        if (mSizeHasChanged || selectedTheme != getSelectedTheme()) {
            mWindowManager.removeView(mOverlayWindow);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                mWindowManager.removeView(floatingView);
            mOverlayWindow = null;
            createOverlayWindow();
        }
        if (!ViewCompat.isAttachedToWindow(mOverlayWindow)) {
            WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mOverlayWindow.getLayoutParams();
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            layoutParams.dimAmount = 0.65f;
            int[] dimensions = getWindowDimensions();
            int marginTop = getResources().getDimensionPixelSize(R.dimen.overlay_window_padding);
            ViewGroup.LayoutParams windowLP = mOverlayWindow.getChildAt(0).getLayoutParams();
            windowLP.width = Math.min(dimensions[0], metrics.widthPixels);
            windowLP.height = dimensions[1] > metrics.heightPixels - marginTop * 1.3f ?
                    WindowManager.LayoutParams.MATCH_PARENT : dimensions[1];
            mWindowManager.addView(mOverlayWindow, layoutParams);
            if (floatingView.getParent() == null)
                mWindowManager.addView(floatingView, floatingView.getLayoutParams());
        }
        int margin = getResources().getDimensionPixelSize(R.dimen.overlay_window_padding);
        mOverlayWindow.getChildAt(0).post(() -> {
                    int xOffset = metrics.widthPixels - mOverlayWindow.getChildAt(0).getMeasuredWidth();
                    int yOffset = metrics.heightPixels - mOverlayWindow.getChildAt(0).getMeasuredHeight();
                    boolean portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
                    mOverlayWindow.setRevealCenter(
                            startX - xOffset + (portrait ? 0 : margin) + (floatingView.getWidth() / 2),
                            mOverlayWindow.getChildAt(0).getMeasuredHeight() - startY + yOffset - (portrait ? margin : 0) - (floatingView.getHeight() / 2)
                    );
                    mSizeHasChanged = false;
                    mOverlayWindow.setVisibility(View.VISIBLE);
                }
        );
        if (mOverlayWindow.getTag() != null && mOverlayWindow.getTag() instanceof OverlayContentLayout) {
            ((OverlayContentLayout) mOverlayWindow.getTag()).onOpened();
        }
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

        int margin = getResources().getDimensionPixelSize(R.dimen.overlay_window_padding);
        int xOffset = metrics.widthPixels - mOverlayWindow.getChildAt(0).getMeasuredWidth();
        boolean portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        cx = cx + (portrait ? 0 : margin) + (mBubbleView.getWidth() / 2);
        cy = metrics.heightPixels - cy - (portrait ? margin : 0) - (mBubbleView.getHeight() / 2);

        int dx = Math.max(cx, mOverlayWindow.getWidth() - cx);
        int dy = Math.max(cy, mOverlayWindow.getHeight() - cy);
        float finalRadius = (float) Math.hypot(dx, dy);

        mOverlayWindow.setRevealCenter((Integer) null);
        Animator animator =
                ViewAnimationUtils.createCircularReveal(mOverlayWindow.getChildAt(0), cx - xOffset, cy, finalRadius, 0);

        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(300L);
        animator.addListener(new AnimatorActionListener(() -> {
            if (mOverlayWindow != null && ViewCompat.isAttachedToWindow(mOverlayWindow))
                mOverlayWindow.setVisibility(View.GONE);
            if (stopService)
                stopOverlayService();
            mIsMoving = false;
        }, AnimatorActionListener.ActionType.END));
        animator.start();
    }

    private void stopOverlayService() {
        sRunning = false;
        stopForeground(true);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (receiver != null)
            unregisterReceiver(receiver);
        if (mOverlayWindow != null && mOverlayWindow.findViewById(R.id.overlay_content) != null) {
            ((OverlayContentLayout) mOverlayWindow.findViewById(R.id.overlay_content)).unregisterNetworkCallback();
        }
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
        boolean openViaNotification = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this)
                .getString("pref_overlay_behavior", "0")) == 1;

        if (isInOverlay())
            exitOverlay(false, !openViaNotification);
        else
            stopOverlayService();
    }

    @Override
    public void onTouchFinished(boolean isFinishing, int x, int y) {
        mIsMoving = false;
        if (!isFinishing) {
            int left = mFloatingViewManager.getTargetFloatingView().getPositionLimits().left;
            int right = mFloatingViewManager.getTargetFloatingView().getPositionLimits().right;
            int closestSide = Math.min(Math.abs(x - right), x) == x ? Math.min(x, left) : Math.max(x, right);

            if (!isInOverlay()) {
                // Save the last position
                final SharedPreferences.Editor editor = getSharedPreferences("overlay_position", Context.MODE_PRIVATE).edit();
                editor.putInt(PREF_KEY_LAST_POSITION_X, closestSide);
                editor.putInt(PREF_KEY_LAST_POSITION_Y, y);
                editor.apply();
            }

            if (mDoPullBack)
                moveBubbleForOverlay(true);

            if (mInOverlay && !isInOverlay()) {
                mFloatingViewManager.getTargetFloatingView().setClickable(true);
                doOverlay(x, y);
            }
        }
    }

    private void destroy() {
        if (mFloatingViewManager != null && mFloatingViewManager.getTargetFloatingView() != null) {
            mFloatingViewManager.removeAllViewToWindow();
            mFloatingViewManager = null;
        }
        if (mWindowManager != null && mOverlayWindow != null && ViewCompat.isAttachedToWindow(mOverlayWindow))
            mWindowManager.removeView(mOverlayWindow);
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

    private void saveWindowDimensions() {
        boolean portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (mOverlayWindow != null && mOverlayWindow.getChildAt(0) != null && mOverlayWindow.getChildAt(0).getMeasuredHeight() != 0)
            getSharedPreferences(WINDOW_DIMENSIONS_PREF, Context.MODE_PRIVATE)
                    .edit().putInt(portrait ? "w" : "w_land", mOverlayWindow.getChildAt(0).getMeasuredWidth())
                    .putInt(portrait ? "h" : "h_land", mOverlayWindow.getChildAt(0).getMeasuredHeight()).apply();
    }

    private int[] getWindowDimensions() {
        boolean portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        int w = getSharedPreferences(WINDOW_DIMENSIONS_PREF, Context.MODE_PRIVATE)
                .getInt(portrait ? "w" : "w_land", WindowManager.LayoutParams.MATCH_PARENT);
        int h = getSharedPreferences(WINDOW_DIMENSIONS_PREF, Context.MODE_PRIVATE)
                .getInt(portrait ? "h" : "h_land", WindowManager.LayoutParams.MATCH_PARENT);
        return new int[]{w > 0 ? w : ViewGroup.LayoutParams.MATCH_PARENT, h > 0 ? h : ViewGroup.LayoutParams.MATCH_PARENT};
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

    public static void showCustomFloatingView(Context context, final String player, final Notification notif, final String[] metadata, long duration) {
        if (App.isMainActivityVisible())
            return;
        if (!(context instanceof Activity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) &&
                (Build.VERSION.SDK_INT < M || Settings.canDrawOverlays(context))) {
            OverlayServiceCallback callback = new OverlayServiceCallback(context.getApplicationContext(), notif, metadata, duration);
            for (int i = 0; i < metadata.length; i++)
                if (metadata[i] == null) metadata[i] = "";
            if (getLyrics() == null || getLyrics().getFlag() == Lyrics.ERROR || (metadata != null && !(metadata[0].equals(getLyrics().getArtist()) && metadata[1].equals(getLyrics().getTitle())))) {
                Lyrics savedLyrics = DatabaseHelper.getInstance(context).get(metadata);
                if (savedLyrics != null)
                    callback.onLyricsDownloaded(savedLyrics);
                else {
                    DownloadThread.LRC = PreferenceManager.getDefaultSharedPreferences(context)
                            .getBoolean("pref_lrc", true);
                    new DownloadThread(new WeakReference<>(callback), player, duration, null, metadata[0], metadata[1]).start(); // FIXME use queue
                }
            } else {
                callback.onLyricsDownloaded(null);
            }
        }
        stopCountDown();
    }

    public static void removeCustomFloatingView(Context context) {
        Intent makeItStop = new Intent(context, LyricsOverlayService.class);
        makeItStop.setAction(LyricsOverlayService.STOP_FLOATING_ACTION);
        try {
            context.getApplicationContext().startService(makeItStop);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    public static void hideFloatingView(Context context) {
        if (!(context instanceof Activity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)) {
            Intent hideIntent = new Intent(context, LyricsOverlayService.class);
            hideIntent.setAction(LyricsOverlayService.HIDE_FLOATING_ACTION);
            context.getApplicationContext().startService(hideIntent);
            startCountDown(context);
        }
    }

    private static void stopCountDown() {
        if (sRunnable != null)
            sStopHandler.removeCallbacks(sRunnable);
    }

    private static void startCountDown(Context context) {
        WeakReference<Context> contextReference = new WeakReference<>(context);
        sRunnable = sRunnable == null ? () -> {
            if (contextReference.get() != null)
                removeCustomFloatingView(contextReference.get());
        } : sRunnable;
        sStopHandler.postDelayed(sRunnable, 300000);
    }

    @Override
    public void onBackpressed() {
        if (mFloatingViewManager != null) {
            FloatingView floatingView = mFloatingViewManager.getTargetFloatingView();
            int x;
            int y;
            if (isInOverlay()) {
                if (floatingView != null) {
                    WindowManager.LayoutParams lp =
                            (WindowManager.LayoutParams) floatingView.getLayoutParams();
                    x = lp.x;
                    y = lp.y;
                } else {
                    final SharedPreferences savedPosition = getSharedPreferences("overlay_position", Context.MODE_PRIVATE);
                    x = savedPosition.getInt(PREF_KEY_LAST_POSITION_X, 0);
                    y = savedPosition.getInt(PREF_KEY_LAST_POSITION_Y, 0);
                }
                exitOverlay(true, false, x, y);
            }
        }
    }

    @Override
    public void onSizeChanged() {
        mSizeHasChanged = true;
        if (isInOverlay()) {
            int[] position = moveBubbleForOverlay(false);
            doOverlay(position);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (mFloatingViewManager == null)
            return false;
        boolean output = mFloatingViewManager.onTouch(view, event);
        if (isInOverlay()) {
            if (event.getAction() == MotionEvent.ACTION_MOVE && !mDoPullBack) {
                exitOverlay(false, false);
                mDoPullBack = true;
                mFloatingViewManager.getTargetFloatingView().setBlockMoveToEdge(true);
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (event.getEventTime() - event.getDownTime() < 200)
                view.performClick();
        }

        return output;
    }

    public static boolean isServiceRunningInForeground(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }

            }
        }
        return false;
    }

    public static Lyrics getLyrics() {
        return sLyrics;
    }

    public static void setLyrics(Lyrics mLyrics) {
        sLyrics = mLyrics;
    }

    public static void markLyricsAsReported() {
        sLyrics.setReported(true);
    }

    @Override
    public void onResizeStarted() {
        mResizing = true;
        this.originalWindowDimensions = new int[]{mOverlayWindow.getChildAt(0).getMeasuredWidth(),
                mOverlayWindow.getChildAt(0).getMeasuredHeight()};
    }

    @Override
    public void onResizeFinished() {
        mResizing = false;
        saveWindowDimensions();
    }

    @Override
    public void onHandleMoved(float newX, float newY, float oldX, float oldY, int screenW, int screenH) {
        ViewGroup.LayoutParams lp = mOverlayWindow.getChildAt(0).getLayoutParams();
        int wMinimum = screenW * 3 / 4;
        int hMinimum = screenH * 2 / 3 ;
        lp.width = Math.min(originalWindowDimensions[0] + (int) (oldX - newX), screenW);
        lp.width = Math.max(lp.width, wMinimum);
        lp.height = Math.min(originalWindowDimensions[1] - (int) (oldY - newY), screenH);
        lp.height = Math.max(lp.height, hMinimum);
        mOverlayWindow.updateViewLayout(mOverlayWindow.getChildAt(0), lp);
    }

    public void onSnackbarShown(Snackbar snackbar) {
        if (!mResizing) {
            mSnackBarSize = snackbar.getView().getMeasuredHeight();
            int[] position = new int[2];
            int height = mOverlayWindow.getChildAt(0).getMeasuredHeight();
            int maxHeight = mOverlayWindow.getMeasuredHeight() - mSnackBarSize;
            mOverlayWindow.getChildAt(0).getLocationInWindow(position);
            if (position[1] + height > maxHeight) {
                ViewGroup.LayoutParams lp = mOverlayWindow.getChildAt(0).getLayoutParams();
                lp.height = height - snackbar.getView().getMeasuredHeight();
                mOverlayWindow.updateViewLayout(mOverlayWindow.getChildAt(0), lp);
            }
        }
    }

    public static class OverlayServiceCallback implements Lyrics.Callback {

        private final WeakReference<Context> context;
        private final String[] metadata;
        private final Notification notif;
        private final long duration;

        private OverlayServiceCallback(Context context, Notification notif, String[] metadata, long duration) {
            this.context = new WeakReference<>(context);
            this.notif = notif;
            this.duration = duration;
            this.metadata = metadata;
        }

        @Override
        public void onLyricsDownloaded(Lyrics lyrics) {
            if (lyrics == null || lyrics.getFlag() == Lyrics.POSITIVE_RESULT && context.get() != null && notif != null && metadata != null) {
                Intent intent = new Intent(context.get(), LyricsOverlayService.class);
                intent.setAction(isRunning() ? SHOW_FLOATING_ACTION : UPDATE_NOTIFICATION_ACTION);
                intent.putExtra("notification", notif);
                intent.putExtra("metadata", metadata);
                intent.putExtra("duration", duration);
                if (lyrics != null)
                    intent.putExtra("lyrics", (Parcelable) lyrics);
                context.get().getApplicationContext().startService(intent);
            }
        }

        public Context getContext() {
            return context.get();
        }
    }
}