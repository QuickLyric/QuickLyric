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

package com.geecko.QuickLyric.fragment;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.adapter.DrawerAdapter;
import com.geecko.QuickLyric.adapter.LocalAdapter;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.tasks.DBContentLister;
import com.geecko.QuickLyric.tasks.WriteToDatabaseTask;

import java.util.ArrayList;

public class LocalLyricsFragment extends ListFragment {

    private final AdapterView.OnItemClickListener standardOnClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final MainActivity mainActivity = (MainActivity) getActivity();
            getListView().setOnItemClickListener(null); // prevents bug on double tap
            mainActivity.updateLyricsFragment(R.animator.slide_out_start, R.animator.slide_in_start, true, lyricsArray.get(position));
        }
    };
    public boolean showTransitionAnim = true;
    public boolean isActiveFragment = false;
    public ArrayList<Lyrics> lyricsArray = null;
    private boolean unselectMode;
    private final AdapterView.OnItemClickListener actionOnClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            LocalAdapter adapter = ((LocalAdapter) getListAdapter());
            adapter.toggle(position);
            if (unselectMode != (adapter.getCheckedItemCount() == adapter.getCount())) {
                unselectMode = (adapter.getCheckedItemCount() == adapter.getCount());
                ((MainActivity) getActivity()).mActionMode.invalidate();
            }
        }
    };
    private boolean actionModeInitialized = false;
    private final ActionMode.Callback callback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.local_action_mode, menu);
            actionModeStatusBar(true);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            getListView().setOnItemClickListener(actionOnClickListener);
            MenuItem selectAllItem = menu.findItem(R.id.action_select_all);
            if (selectAllItem != null) {
                if (unselectMode)
                    selectAllItem.setTitle(R.string.unselect_all_action);
                else
                    selectAllItem.setTitle(R.string.select_all_action);
                return true;
            } else
                return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            LocalAdapter adapter = (LocalAdapter) getListAdapter();
            switch (menuItem.getItemId()) {
                case R.id.action_delete:
                    for (int i = 0; i < lyricsArray.size(); i++)
                        if (adapter.isItemChecked(i))
                            new WriteToDatabaseTask().execute(LocalLyricsFragment.this, null, lyricsArray.get(i));
                    actionMode.finish();
                    return true;
                case R.id.action_select_all:
                    adapter.checkAll(!unselectMode);
                    unselectMode = !unselectMode;
                    actionMode.invalidate();
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            if (actionModeInitialized) {
                ((LocalAdapter) getListAdapter()).checkAll(false);
                ((MainActivity) getActivity()).mActionMode = null;
                getListView().setOnItemClickListener(standardOnClickListener);
                actionModeInitialized = false;
            }
            actionModeStatusBar(false);
        }
    };

    @TargetApi(21)
    private void actionModeStatusBar(boolean actionMode) {
        MainActivity activity = (MainActivity) getActivity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.setStatusBarColor(actionMode ? activity.getResources()
                    .getColor(R.color.action_dark) : null);
            activity.setNavBarColor(actionMode ? activity.getResources()
                    .getColor(R.color.action) : null);
        }
    }

    @Override
    public void onActivityCreated(Bundle onSavedInstanceState) {
        super.onActivityCreated(onSavedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        setListShown(true);
        ListView listView = getListView();
        if (listView != null) {
            View fragmentView = getView();
            TypedValue typedValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
            if (fragmentView != null)
                fragmentView.setBackgroundColor(typedValue.data);
            listView.setDivider(new ColorDrawable(Color.parseColor("#cccccc")));
            listView.setDividerHeight(0);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                listView.setSelector(R.drawable.abc_list_selector_disabled_holo_dark);
            listView.setFastScrollEnabled(true);
            listView.setDrawSelectorOnTop(true);
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

        if (actionModeInitialized && mainActivity.mActionMode == null) {
            getListView().setOnItemClickListener(actionOnClickListener);
            mainActivity.mActionMode = mainActivity.startActionMode(LocalLyricsFragment.this.callback);
        } else
            getListView().setOnItemClickListener(standardOnClickListener);

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                LocalAdapter adapter = ((LocalAdapter) getListAdapter());
                adapter.toggle(position);
                if (mainActivity.mActionMode == null) {
                    actionModeInitialized = true;
                    mainActivity.mActionMode = mainActivity.startActionMode(LocalLyricsFragment.this.callback);
                } else if (unselectMode != (adapter.getCheckedItemCount() == adapter.getCount())) {
                    unselectMode = (adapter.getCheckedItemCount() == adapter.getCount());
                    mainActivity.mActionMode.invalidate();
                }


                return true;
            }
        });
        this.isActiveFragment = true;
        new DBContentLister().execute(this);
    }

    public void update(final ArrayList<Lyrics> results) {
        int scrollY = getListView().getScrollY();
        lyricsArray = results;
        ViewGroup container = ((ViewGroup) getListView().getParent());

        if (container != null)
            if (results.size() == 0 && container.findViewById(R.id.local_empty_database_textview) == null)
                container.addView(View.inflate(getActivity(), R.layout.local_empty_database_textview, null));
            else if (results.size() != 0)
                container.removeView(container.findViewById(R.id.local_empty_database_textview));
        setListAdapter(new LocalAdapter(getActivity(), R.layout.list_row, results));

        if (((MainActivity) getActivity()).mActionMode != null) {
            ((LocalAdapter) getListAdapter()).checkAll(false);
            if (getListAdapter().getCount() == 0)
                ((MainActivity) getActivity()).mActionMode.finish();
        }
        getListView().scrollTo(0, scrollY);
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
        if (mainActivity.focusOnFragment && actionBar != null) // focus is on Fragment
        {
            if (actionBar.getTitle() == null || !actionBar.getTitle().equals(this.getString(R.string.local_title)))
                actionBar.setTitle(R.string.local_title);
            inflater.inflate(R.menu.local, menu);
        } else
            menu.clear();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort:
                final SharedPreferences localSortOrderPreferences = getActivity().getSharedPreferences("local_sort_order", Context.MODE_PRIVATE);
                final int sortModePref = localSortOrderPreferences.getInt("mode", 0);
                final CharSequence[] sortModesArray = getResources().getStringArray(R.array.sort_modes);
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this.getActivity());
                builder1.setTitle(R.string.sort_dialog_title);
                builder1.setSingleChoiceItems(sortModesArray, sortModePref, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog1, final int mode) {
                        CharSequence[] items;
                        int sortOrder = 0;
                        items = getResources().getStringArray(R.array.AZ_sort_order);
                        if (mode == 0)
                            sortOrder = localSortOrderPreferences.getInt("order_artist", 0);
                        else if (mode == 1)
                            sortOrder = localSortOrderPreferences.getInt("order_title", 0);

                        dialog1.dismiss();
                        AlertDialog.Builder builder2 = new AlertDialog.Builder(LocalLyricsFragment.this.getActivity());
                        builder2.setTitle(R.string.sort_dialog_title);
                        builder2.setSingleChoiceItems(items, sortOrder, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog2, int order) {
                                        SharedPreferences.Editor editor = localSortOrderPreferences.edit();
                                        editor.putInt("mode", mode);
                                        switch (mode) {
                                            default:
                                                editor.putInt("order_artist", order);
                                                break;
                                            case 1:
                                                editor.putInt("order_title", order);
                                                break;
                                        }
                                        editor.apply();
                                        dialog2.dismiss();
                                        new DBContentLister().execute(LocalLyricsFragment.this);
                                    }
                                }

                        );
                        AlertDialog alert2 = builder2.create();
                        alert2.show();
                    }
                });
                AlertDialog alert1 = builder1.create();
                alert1.show();
                return true;
        }

        return false;
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        final MainActivity mainActivity = (MainActivity) getActivity();
        Animator anim = null;
        if (showTransitionAnim) {
            if (nextAnim != 0)
                anim = AnimatorInflater.loadAnimator(getActivity(), nextAnim);
            showTransitionAnim = false;
            if (anim != null)
                anim.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        if (mainActivity.drawer instanceof DrawerLayout)
                            ((DrawerLayout) mainActivity.drawer).closeDrawer(mainActivity.drawerView);
                        mainActivity.setDrawerListener(true);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                    }

                    @Override
                    public void onAnimationStart(Animator animator) {
                        mainActivity.setDrawerListener(false);
                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {
                    }
                });
        } else
            anim = AnimatorInflater.loadAnimator(getActivity(), R.animator.none);
        return anim;
    }
}