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
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.adapter.DrawerAdapter;
import com.geecko.QuickLyric.adapter.LocalAdapter;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.services.BatchDownloaderService;
import com.geecko.QuickLyric.tasks.DBContentLister;
import com.geecko.QuickLyric.tasks.WriteToDatabaseTask;
import com.geecko.QuickLyric.view.AnimatedExpandableListView;

import java.util.ArrayList;

public class LocalLyricsFragment extends Fragment {

    // TODO: maybe replace the AnimatedExpandableListView with a RecyclerView
    // This would potentially make it easier for thumbnails
    // Replace action mode with swipe to remove
    // TODO: Move back to ListFragment https://stackoverflow.com/questions/4274519/android-how-to-avoid-header-from-scrolling-in-listview-android
    // TODO: Animate group indicator (with AnimatedStateListDrawable <animated-selector>)
    // TODO cleanup
    // TODO: Test batch download

    private final ExpandableListView.OnChildClickListener standardOnClickListener =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                    final MainActivity mainActivity = (MainActivity) getActivity();
                    megaListView.setOnItemClickListener(null); // prevents bug on double tap
                    mainActivity.updateLyricsFragment(R.animator.slide_out_start, R.animator.slide_in_start,
                            true, lyricsArray.get(groupPosition).get(childPosition));
                    return true;
                }
            };

    public boolean showTransitionAnim = true;
    public boolean isActiveFragment = false;
    public ArrayList<ArrayList<Lyrics>> lyricsArray = null;
    private boolean unselectMode;
    private AnimatedExpandableListView megaListView;
    private ProgressBar progressBar;
    private final ExpandableListView.OnChildClickListener actionOnClickListener = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            LocalAdapter adapter = ((LocalAdapter) megaListView.getAdapter());
            adapter.toggle(0);
            if (unselectMode != (adapter.getCheckedItemCount() == adapter.getCount())) {
                unselectMode = (adapter.getCheckedItemCount() == adapter.getCount());
                ((MainActivity) getActivity()).mActionMode.invalidate();
            }
            return true;
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
            megaListView.setOnChildClickListener(actionOnClickListener);
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
            LocalAdapter adapter = (LocalAdapter) megaListView.getAdapter();
            switch (menuItem.getItemId()) {
                case R.id.action_delete:
                    ArrayList<Lyrics> delendam = new ArrayList<>();
                    for (int i = 0; i < lyricsArray.size(); i++) /*
                        if (adapter.isItemChecked(i)) fixme
                            delendam.add(lyricsArray.get(i));
                            */
                        if (delendam.size() > 0) {
                            Lyrics[] delendamArray = new Lyrics[delendam.size()];
                            delendamArray = delendam.toArray(delendamArray);
                            new WriteToDatabaseTask(LocalLyricsFragment.this)
                                    .execute(LocalLyricsFragment.this, null, delendamArray);
                        }
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
                ((LocalAdapter) megaListView.getAdapter()).checkAll(false);
                ((MainActivity) getActivity()).mActionMode = null;
                megaListView.setOnChildClickListener(standardOnClickListener);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        setHasOptionsMenu(true);
        FrameLayout layout = (FrameLayout) inflater.inflate(R.layout.local_listview, container, false);
        megaListView = (AnimatedExpandableListView) layout.findViewById(R.id.expandable_listview);
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
            megaListView.setDivider(new ColorDrawable(Color.parseColor("#cccccc")));
            megaListView.setDividerHeight(0);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                megaListView.setSelector(R.drawable.abc_list_selector_disabled_holo_dark);
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

        megaListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {

            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                if (megaListView.isGroupExpanded(groupPosition))
                    megaListView.collapseGroupWithAnimation(groupPosition);
                else
                    megaListView.expandGroupWithAnimation(groupPosition);
                return true;
            }
        });

        final float scale = getResources().getDisplayMetrics().density;
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int left = size.x - Math.round(90 * scale);
        int right = size.x - Math.round(50 * scale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            megaListView.setIndicatorBoundsRelative(left, right);
        megaListView.setIndicatorBounds(left, right);

        if (actionModeInitialized && mainActivity.mActionMode == null) {
            megaListView.setOnChildClickListener(actionOnClickListener);
            mainActivity.mActionMode = mainActivity.startActionMode(LocalLyricsFragment.this.callback);
        } else
            megaListView.setOnChildClickListener(standardOnClickListener);

        megaListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            // fixme
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                LocalAdapter adapter = ((LocalAdapter) megaListView.getAdapter());
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
        new DBContentLister(this).execute();
    }

    public void update(final ArrayList<ArrayList<Lyrics>> results) {
        if (getView() == null)
            return;
        int scrollY = megaListView.getScrollY();
        lyricsArray = results;
        ViewGroup container = ((ViewGroup) megaListView.getParent());

        if (container != null)
            if (results.size() == 0 && container.findViewById(R.id.local_empty_database_textview) == null)
                container.addView(View.inflate(getActivity(), R.layout.local_empty_database_textview, null));
            else if (results.size() != 0)
                container.removeView(container.findViewById(R.id.local_empty_database_textview));
        megaListView.setAdapter(new LocalAdapter(getActivity(), results));

        if (((MainActivity) getActivity()).mActionMode != null) {
            ((LocalAdapter) megaListView.getAdapter()).checkAll(false);
            if (megaListView.getAdapter().getCount() == 0)
                ((MainActivity) getActivity()).mActionMode.finish();
        }
        megaListView.scrollTo(0, scrollY);
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

    public void scrollUp() {
        int size = (megaListView.getExpandableListAdapter()).getGroupCount();
        for (int i = 0; i < size; i++)
            if (megaListView.isGroupExpanded(i))
                megaListView.collapseGroup(i);
        megaListView.setSelection(0);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MainActivity mainActivity = (MainActivity) this.getActivity();
        ActionBar actionBar = (mainActivity).getSupportActionBar();
        CollapsingToolbarLayout toolbarLayout =
                (CollapsingToolbarLayout) mainActivity.findViewById(R.id.toolbar_layout);
        if (mainActivity.focusOnFragment && actionBar != null) // focus is on Fragment
        {
            if (actionBar.getTitle() == null || !actionBar.getTitle().equals(this.getString(R.string.local_title)))
                toolbarLayout.setTitle(getString(R.string.app_name));
            inflater.inflate(R.menu.local, menu);
        } else
            menu.clear();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
                showScanDialog();
                break;
            case R.id.action_sort:
                final SharedPreferences localSortOrderPreferences = getActivity().getSharedPreferences("local_sort_order", Context.MODE_PRIVATE);
                final int sortModePref = localSortOrderPreferences.getInt("mode", -1);
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
                            sortOrder = localSortOrderPreferences.getInt("order_artist", -1);
                        else if (mode == 1)
                            sortOrder = localSortOrderPreferences.getInt("order_title", -1);

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
                                        new DBContentLister(LocalLyricsFragment.this).execute();
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

    public void showScanDialog() {
        CharSequence[] items = getResources().getStringArray(R.array.URI_labels);
        AlertDialog.Builder choiceBuilder = new AlertDialog.Builder(getActivity());
        choiceBuilder
                .setTitle(R.string.content_providers_title)
                .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface choiceDialog, int i) {
                        String[] values = getResources().getStringArray(R.array.URI_values);
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
                        final int count = countCursor.getCount();
                        final int time = (int) Math.ceil(count / 500f);
                        countCursor.close();
                        choiceDialog.dismiss();
                        String prompt = getResources()
                                .getQuantityString(R.plurals.scan_dialog, count > 1 ? 2 : 1);
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
                .create().show();
    }

    public void setListShown(boolean visible) {
        if (megaListView != null) {
            progressBar.startAnimation(AnimationUtils.loadAnimation(
                    getActivity(), visible ? android.R.anim.fade_out : android.R.anim.fade_in));
            megaListView.startAnimation(AnimationUtils.loadAnimation(
                    getActivity(), visible ? android.R.anim.fade_out : android.R.anim.fade_in));
            progressBar.setVisibility(visible ? View.GONE : View.VISIBLE);
            megaListView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
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