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
import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.geecko.QuickLyric.App;
import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.adapter.DrawerAdapter;
import com.geecko.QuickLyric.adapter.RecentTracksAdapter;
import com.geecko.QuickLyric.event.RecentsAddedEvent;
import com.geecko.QuickLyric.event.RecentsRemovedEvent;
import com.squareup.leakcanary.RefWatcher;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class RecentTracksFragment extends Fragment {


    public boolean showTransitionAnim = true;
    public boolean isActiveFragment = false;
    private RecentTracksAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private ViewFlipper flipper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        setHasOptionsMenu(true);
        View layout = inflater.inflate(R.layout.recent_tracks_view, container, false);
        mRecyclerView = layout.findViewById(R.id.track_list_view);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new RecentTracksAdapter(getActivity());
        mRecyclerView.setAdapter(mAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                mLayoutManager.getOrientation());
        mRecyclerView.addItemDecoration(dividerItemDecoration);
        mRecyclerView.setNestedScrollingEnabled(false);
        flipper = layout.findViewById(R.id.recents_viewflipper);
        TextView emptyText = layout.findViewById(R.id.local_empty_database_textview);
        String str = getActivity().getResources().getString(R.string.recents_empty_database);
        emptyText.setText(str);
        chooseView();
        return layout;
    }

    private void chooseView() {
        int items = mAdapter.getItemCount();
        flipper.setDisplayedChild(items == 0 ? 0 : 1);
    }

    @Override
    public void onActivityCreated(Bundle onSavedInstanceState) {
        super.onActivityCreated(onSavedInstanceState);
        if (mRecyclerView != null) {
            View fragmentView = getView();
            TypedValue typedValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
            if (fragmentView != null)
                fragmentView.setBackgroundColor(typedValue.data);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(RecentsAddedEvent event) {
        int top = mLayoutManager.findFirstCompletelyVisibleItemPosition();
        mAdapter.notifyItemInserted(event.index);
        if (top == 0)
            mLayoutManager.scrollToPosition(0);
        chooseView();
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(RecentsRemovedEvent event) {
        mAdapter.notifyItemRemoved(event.index);
        chooseView();
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

        mRecyclerView.setPadding(mRecyclerView.getPaddingLeft(),
                mRecyclerView.getPaddingTop(),
                mRecyclerView.getPaddingRight(),
                (getActivity().findViewById(R.id.ad_container) == null) ? 0 : getActivity().findViewById(R.id.ad_container).getMeasuredHeight());

        this.isActiveFragment = true;
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
                mainActivity.findViewById(R.id.toolbar_layout);
        if (mainActivity.focusOnFragment) // focus is on Fragment
        {
            if (actionBar.getTitle() == null || !actionBar.getTitle().equals(this.getString(R.string.recent_tracks_title)))
                toolbarLayout.setTitle(getString(R.string.recent_tracks_title));
            inflater.inflate(R.menu.recent_tracks, menu);
        } else
            menu.clear();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//
//        }
        return false;
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
        RefWatcher refWatcher = App.getRefWatcher(getActivity());
        refWatcher.watch(this);
        EventBus.getDefault().unregister(this);
        super.onDestroy();

    }
}
