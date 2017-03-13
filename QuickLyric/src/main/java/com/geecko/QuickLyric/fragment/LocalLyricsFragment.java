/*
 * *
 *  * This file is part of QuickLyric
 *  * Copyright © 2017 QuickLyric SPRL
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

package com.geecko.QuickLyric.fragment;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.ActivityManager;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.util.LongSparseArray;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.geecko.QuickLyric.App;
import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.adapter.DrawerAdapter;
import com.geecko.QuickLyric.adapter.LocalAdapter;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.services.BatchDownloaderService;
import com.geecko.QuickLyric.tasks.DBContentLister;
import com.geecko.QuickLyric.tasks.WriteToDatabaseTask;
import com.geecko.QuickLyric.utils.AnimatorActionListener;
import com.geecko.QuickLyric.utils.PermissionsChecker;
import com.geecko.QuickLyric.utils.Spotify;
import com.geecko.QuickLyric.view.AnimatedExpandableListView;
import com.geecko.QuickLyric.view.AnimatedExpandableListView.DummyView;
import com.geecko.QuickLyric.view.BackgroundContainer;
import com.squareup.leakcanary.RefWatcher;

import java.util.Arrays;

public class LocalLyricsFragment extends ListFragment {


    public static final int REQUEST_CODE = 0;
    public boolean showTransitionAnim = true;
    public boolean isActiveFragment = false;
    private AnimatedExpandableListView megaListView;
    private ProgressBar progressBar;
    private BackgroundContainer mBackgroundContainer;
    private boolean mSwiping;

    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        float mDownX;
        private int mSwipeSlop = -1;
        private boolean mItemPressed;
        private VelocityTracker mVelocityTracker = null;

        @Override
        public boolean onTouch(final View v, MotionEvent event) {
            int index = event.getActionIndex();
            int pointerId = event.getPointerId(index);

            if (mSwipeSlop < 0) {
                mSwipeSlop = ViewConfiguration.get(getActivity())
                        .getScaledTouchSlop();
            }
            final int groupPosition = v instanceof DummyView ?
                    ((LocalAdapter.ChildViewHolder) ((DummyView) v).views.get(0).getTag()).groupPosition :
                    ((LocalAdapter.ChildViewHolder) v.getTag()).groupPosition;
            int childPosition = 90;

            for (int c = 0; c < megaListView.getChildCount(); c++)
                if (megaListView.getChildAt(c) == v) {
                    childPosition = c;
                    break;
                }

            // v.onTouchEvent(event);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (mItemPressed) {
                        // Multi-item swipes not handled
                        return false;
                    }
                    mItemPressed = true;
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
                case MotionEvent.ACTION_CANCEL:
                    v.setAlpha(1);
                    v.setTranslationX(0);
                    if (((LocalAdapter) megaListView.getExpandableListAdapter())
                            .getGroup(groupPosition).length <= 1) {
                        View groupView = megaListView.getChildAt(childPosition - 1);
                        if (groupView != null && groupView.getTag() instanceof LocalAdapter.GroupViewHolder) {
                            groupView.setTranslationX(0);
                            groupView.setAlpha(1);
                        }
                    }
                    mItemPressed = false;
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                    break;
                case MotionEvent.ACTION_MOVE: {
                    mVelocityTracker.addMovement(event);
                    float x = event.getX() + v.getTranslationX();
                    float deltaX = x - mDownX;
                    float deltaXAbs = Math.abs(deltaX);
                    if (!mSwiping) {
                        if (deltaXAbs > mSwipeSlop) {
                            mSwiping = true;
                            getListView().requestDisallowInterceptTouchEvent(true);
                            int diff = 0;
                            if (((LocalAdapter) megaListView.getExpandableListAdapter())
                                    .getGroup(groupPosition).length <= 1) {
                                View groupView = megaListView.getChildAt(childPosition - 1);
                                if (groupView != null)
                                    diff = groupView.getHeight();
                            }
                            mBackgroundContainer.showBackground(v.getTop() - diff, v.getHeight() + diff);
                        }
                    }
                    if (mSwiping) {
                        v.setTranslationX((x - mDownX));
                        v.setAlpha(1 - deltaXAbs / v.getWidth());
                        if (((LocalAdapter) megaListView.getExpandableListAdapter())
                                .getGroup(groupPosition).length <= 1) {
                            View groupView = megaListView.getChildAt(childPosition - 1);
                            if (groupView != null && groupView.getTag() instanceof LocalAdapter.GroupViewHolder) {
                                groupView.setTranslationX((x - mDownX));
                                groupView.setAlpha(1 - deltaXAbs / v.getWidth());
                            }
                        }
                    }
                }
                break;
                case MotionEvent.ACTION_UP: {
                    // User let go - figure out whether to animate the view out, or back into place
                    if (mSwiping) {
                        final float x = event.getX() + v.getTranslationX();
                        float deltaX = x - mDownX;
                        final float deltaXAbs = Math.abs(deltaX);
                        float fractionCovered;
                        float endX;
                        float endAlpha;
                        final boolean remove;
                        mVelocityTracker.computeCurrentVelocity(1000);
                        float velocityX = Math.abs(VelocityTrackerCompat.getXVelocity(mVelocityTracker, pointerId));
                        if (velocityX > 700 || deltaXAbs > v.getWidth() / 4) {
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
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                        int SWIPE_DURATION = 600;
                        long duration = (int) ((1 - fractionCovered) * SWIPE_DURATION);
                        getListView().setEnabled(false);
                        v.animate().setDuration(Math.abs(duration))
                                .alpha(endAlpha).translationX(endX)
                                .setListener(new AnimatorActionListener(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (remove) {
                                            animateRemoval(v);
                                        } else {
                                            mBackgroundContainer.hideBackground();
                                            getListView().setEnabled(true);
                                        }
                                    }
                                }, AnimatorActionListener.ActionType.END));
                        if (((LocalAdapter) megaListView.getExpandableListAdapter())
                                .getGroup(groupPosition).length <= 1) {
                            View groupView = megaListView.getChildAt(childPosition - 1);
                            if (groupView != null && groupView.getTag() instanceof LocalAdapter.GroupViewHolder)
                                groupView.animate().setDuration(Math.abs(duration))
                                        .alpha(endAlpha).translationX(endX);
                        }
                    } else {
                        megaListView.performItemClick(v, megaListView.getPositionForView(v), 0L);
                    }
                }
                mSwiping = false;
                mItemPressed = false;
                break;
                default:
                    return false;
            }
            return true;
        }

        private void animateRemoval(View viewToRemove) {
            mBackgroundContainer.hideBackground();
            // Delete the item from the adapter

            LocalAdapter.ChildViewHolder childViewHolder =
                    viewToRemove instanceof DummyView ?
                            (LocalAdapter.ChildViewHolder) ((DummyView) viewToRemove).views.get(0).getTag() :
                            (LocalAdapter.ChildViewHolder) viewToRemove.getTag();
            new WriteToDatabaseTask(LocalLyricsFragment.this)
                    .execute(LocalLyricsFragment.this, null, childViewHolder.lyrics);
        }
    };

    public void animateUndo(Lyrics lyrics) {
        new WriteToDatabaseTask(LocalLyricsFragment.this)
                .execute(LocalLyricsFragment.this, null, lyrics);
    }

    public LongSparseArray<Integer> collectTopPositions() {
        final LongSparseArray<Integer> itemIdTopMap = new LongSparseArray<>();
        int firstVisiblePosition = megaListView.getFirstVisiblePosition();
        for (int i = 0; i < megaListView.getChildCount(); ++i) {
            View child = megaListView.getChildAt(i);
            int position = firstVisiblePosition + i;
            long itemId = megaListView.getAdapter().getItemId(position);
            itemIdTopMap.put(itemId, child.getTop());
        }
        return itemIdTopMap;
    }

    public void addObserver(final LongSparseArray<Integer> itemIdTopMap) {
        final boolean[] firstAnimation = {true};
        final ViewTreeObserver observer = megaListView.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);
                firstAnimation[0] = true;
                int firstVisiblePosition = megaListView.getFirstVisiblePosition();
                for (int i = 0; i < megaListView.getChildCount(); ++i) {
                    final View child = megaListView.getChildAt(i);
                    int position = firstVisiblePosition + i;
                    long itemId = getListView().getAdapter().getItemId(position);
                    Integer formerTop = itemIdTopMap.get(itemId);
                    int newTop = child.getTop();
                    if (formerTop != null) {
                        if (formerTop != newTop) {
                            int delta = formerTop - newTop;
                            child.setTranslationY(delta);
                            int MOVE_DURATION = 500;
                            child.animate().setDuration(MOVE_DURATION).translationY(0);
                            if (firstAnimation[0]) {
                                child.animate().setListener(new AnimatorActionListener(new Runnable() {
                                    public void run() {
                                        mSwiping = false;
                                        getListView().setEnabled(true);
                                    }
                                }, AnimatorActionListener.ActionType.END));
                                firstAnimation[0] = false;
                            }
                        }
                    } else {
                        // Animate new views along with the others. The catch is that they did not
                        // exist in the start state, so we must calculate their starting position
                        // based on neighboring views.
                        int childHeight = child.getHeight() + megaListView.getDividerHeight();
                        boolean isFurthest = true;
                        for (int j = 0; j < itemIdTopMap.size(); j++) {
                            Integer top = itemIdTopMap.valueAt(j);
                            if (top - childHeight > newTop) {
                                isFurthest = false;
                                break;
                            }
                        }
                        formerTop = newTop + (i > 0 ? childHeight : -childHeight);
                        int delta = formerTop - newTop;
                        int MOVE_DURATION = 500;
                        if (isFurthest) {
                            child.setTranslationY(delta);
                            child.animate().setDuration(MOVE_DURATION).translationY(0);
                        } else {
                            int translationX = formerTop > childHeight ?
                                    child.getWidth() : 0;
                            child.setTranslationX(translationX);
                            if (translationX == 0)
                                child.setTranslationY(formerTop - newTop);
                            child.animate().setDuration(MOVE_DURATION).translationX(0).translationY(0);
                        }
                        if (firstAnimation[0]) {
                            child.animate().setListener(new AnimatorActionListener(new Runnable() {
                                public void run() {
                                    getListView().setEnabled(true);
                                    mSwiping = false;
                                }
                            }, AnimatorActionListener.ActionType.END));
                            firstAnimation[0] = false;
                        }
                    }
                }
                if (firstAnimation[0]) {
                    mSwiping = false;
                    getListView().setEnabled(true);
                    firstAnimation[0] = false;
                }
                itemIdTopMap.clear();
                return true;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        setHasOptionsMenu(true);
        View layout = inflater.inflate(R.layout.local_listview, container, false);
        megaListView = (AnimatedExpandableListView) layout.findViewById(android.R.id.list);
        mBackgroundContainer = (BackgroundContainer) layout.findViewById(R.id.listViewBackground);
        progressBar = (ProgressBar) layout.findViewById(R.id.list_progress);
        return layout;
    }

    @Override
    public void onActivityCreated(Bundle onSavedInstanceState) {
        super.onActivityCreated(onSavedInstanceState);
        if (megaListView != null) {
            View fragmentView = getView();
            TypedValue typedValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
            if (fragmentView != null)
                fragmentView.setBackgroundColor(typedValue.data);
            megaListView.setDividerHeight(0);
            megaListView.setFastScrollEnabled(true);
            megaListView.setDrawSelectorOnTop(true);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        final MainActivity mainActivity = ((MainActivity) this.getActivity());
        super.onViewCreated(view, savedInstanceState);
        if (this.isHidden())
            return;

        DrawerAdapter drawerAdapter = ((DrawerAdapter) ((ListView) mainActivity.findViewById(R.id.drawer_list)).getAdapter());
        if (drawerAdapter.getSelectedItem() != 1) {
            drawerAdapter.setSelectedItem(1);
            drawerAdapter.notifyDataSetChanged();
        }

        if (!megaListView.hasOnGroupClickListener())
            megaListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {

                @Override
                public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                    final ImageView indicator = (ImageView) v.findViewById(R.id.group_indicator);
                    RotateAnimation anim;
                    if (megaListView.isGroupExpanded(groupPosition)) {
                        megaListView.collapseGroupWithAnimation(groupPosition);
                        if (indicator != null) {
                            anim = new RotateAnimation(180f, 360f, indicator.getWidth() / 2, indicator.getHeight() / 2);
                            anim.setInterpolator(new DecelerateInterpolator(3));
                            anim.setDuration(500);
                            anim.setFillAfter(true);
                            indicator.startAnimation(anim);
                        }
                    } else {
                        megaListView.expandGroupWithAnimation(groupPosition);
                        if (indicator != null) {
                            anim = new RotateAnimation(0f, 180f, indicator.getWidth() / 2, indicator.getHeight() / 2);
                            anim.setInterpolator(new DecelerateInterpolator(2));
                            anim.setDuration(500);
                            anim.setFillAfter(true);
                            indicator.startAnimation(anim);
                        }
                    }
                    return true;
                }
            });

        if (!megaListView.hasOnChildClickListener())
            megaListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v,
                                            int groupPosition, int childPosition, long id) {
                    if (mSwiping) {
                        mSwiping = false;
                        return false;
                    }
                    final MainActivity mainActivity = (MainActivity) getActivity();
                    megaListView.setOnChildClickListener(null); // prevents bug on double tap
                    mainActivity.updateLyricsFragment(R.animator.slide_out_start, R.animator.slide_in_start,
                            true, ((LocalAdapter) megaListView.getExpandableListAdapter())
                                    .getChild(groupPosition, childPosition));
                    return true;
                }
            });

        this.isActiveFragment = true;
        new DBContentLister(this).execute();
    }

    public void update(final String[] results) {
        if (getView() == null)
            return;
        int index = megaListView.getFirstVisiblePosition();
        View v = megaListView.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - megaListView.getPaddingTop());

        megaListView.setAdapter(new LocalAdapter(getActivity(), results, mTouchListener, megaListView));
        megaListView.setEmptyView(((ViewGroup) getView().findViewById(R.id.local_empty_database_textview).getParent()));
        getListView().setSelectionFromTop(index, top);
        setListShown(true);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden)
            this.onViewCreated(getView(), null);
        else
            this.isActiveFragment = false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MainActivity mainActivity = (MainActivity) this.getActivity();
        ActionBar actionBar = (mainActivity).getSupportActionBar();
        CollapsingToolbarLayout toolbarLayout =
                (CollapsingToolbarLayout) mainActivity.findViewById(R.id.toolbar_layout);
        if (mainActivity.focusOnFragment) // focus is on Fragment
        {
            if (actionBar.getTitle() == null || !actionBar.getTitle().equals(this.getString(R.string.local_title)))
                toolbarLayout.setTitle(getString(R.string.local_title));
            inflater.inflate(R.menu.local, menu);
        } else
            menu.clear();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
                if (!isMyServiceRunning(getActivity()))
                    showScanDialog();
                else
                    Toast.makeText(getActivity(), getString(R.string.dl_progress, 9, 9)
                            .replaceAll("9", ""), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    public void showScanDialog() {
        final String[] values = getResources().getStringArray(R.array.URI_values);
        CharSequence[] items = Arrays
                .copyOfRange(getResources().getStringArray(R.array.URI_labels), 0, values.length);
        AlertDialog.Builder choiceBuilder = new AlertDialog.Builder(getActivity());
        AlertDialog choiceDialog = choiceBuilder
                .setTitle(R.string.content_providers_title)
                .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface choiceDialog, int i) {
                        if (values[i].equals("Spotify")) {
                            AlertDialog.Builder spotifyChoiceBuilder = new AlertDialog.Builder(getActivity());
                            spotifyChoiceBuilder.setSingleChoiceItems(R.array.spotify_options, -1,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case 1:
                                                    Spotify.getPlaylistTracks(getActivity());
                                                    dialog.dismiss();
                                                    choiceDialog.dismiss();
                                                    break;
                                                case 0:
                                                    Spotify.getUserTracks(getActivity());
                                                    dialog.dismiss();
                                                    choiceDialog.dismiss();
                                                    break;
                                            }
                                        }
                                    }).show();
                            return;
                        } else {
                            if (!PermissionsChecker.requestPermission(getActivity(),
                                    "android.permission.READ_EXTERNAL_STORAGE",
                                    0,
                                    LocalLyricsFragment.REQUEST_CODE))
                                return;
                        }
                        final Uri contentProvider = Uri.parse(values[i]);
                        String[] projection = new String[]{"artist", "title"};
                        String selection = "artist IS NOT NULL AND artist <> '<unknown>'";
                        Cursor countCursor = getActivity().getContentResolver()
                                .query(contentProvider, projection, selection, null, null);
                        if (countCursor == null || countCursor.getCount() == 0) {
                            choiceDialog.cancel();
                            Toast.makeText(getActivity(),
                                    getString(R.string.scan_error_no_content),
                                    Toast.LENGTH_LONG)
                                    .show();
                            return;
                        }
                        int count = countCursor.getCount();
                        final int time = (int) Math.ceil(count / 500f);
                        countCursor.close();
                        choiceDialog.dismiss();
                        String prompt = getResources()
                                .getQuantityString(R.plurals.scan_dialog, count);
                        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(getActivity());
                        confirmDialog
                                .setTitle(R.string.warning)
                                .setMessage(String.format(prompt, count, time))
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Intent scanInfo = new Intent(getActivity(),
                                                BatchDownloaderService.class);
                                        scanInfo.putExtra("uri", contentProvider);
                                        getActivity().startService(scanInfo);
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .create().show();
                    }
                })
                .create();
        choiceDialog.show();
    }

    private boolean isMyServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager)context. getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BatchDownloaderService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        Log.i("Service not","running");
        return false;
    }

    public void setListShown(final boolean visible) {
        final Animation fadeIn = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
        final Animation fadeOut = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                progressBar.setVisibility(visible ? View.GONE : View.VISIBLE);
                megaListView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
        fadeIn.setAnimationListener(listener);
        fadeOut.setAnimationListener(listener);
        progressBar.startAnimation(visible ? fadeOut : fadeIn);
        megaListView.startAnimation(visible ? fadeIn : fadeOut);
    }

    public ExpandableListAdapter getExpandableListAdapter() {
        return megaListView.getExpandableListAdapter();
    }

    public AnimatedExpandableListView getMegaListView() {
        return megaListView;
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        Animator anim = null;
        if (showTransitionAnim) {
            if (nextAnim != 0)
                anim = AnimatorInflater.loadAnimator(getActivity(), nextAnim);
            showTransitionAnim = false;
        } else
            anim = AnimatorInflater.loadAnimator(getActivity(), R.animator.none);
        return anim;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = App.getRefWatcher(getActivity());
        refWatcher.watch(this);
    }
}
