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
import android.app.ListFragment;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.adapter.DrawerAdapter;
import com.geecko.QuickLyric.adapter.SearchAdapter;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.tasks.SearchTask;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;

import java.util.List;

public class SearchFragment extends ListFragment {

    public boolean showTransitionAnim = true;
    private String searchQuery;
    private List<Lyrics> results;
    private boolean refresh = false;
    private ViewGroup errorView;
    private static int errorVisibility = View.INVISIBLE;
    public boolean isActiveFragment = false;
    public SearchTask searchTask;

    public SearchFragment() {
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        setRetainInstance(true);
        setHasOptionsMenu(true);
        super.onActivityCreated(bundle);

        ListView listView = getListView();
        View fragmentView = getView();
        if (fragmentView != null)
            fragmentView.setBackgroundResource(R.color.fragment_background);

        if (searchQuery == null || searchQuery.equals("")) // bug
        {
            getActivity().onBackPressed();
        } else if (listView.getAdapter() == null || refresh) //refresh or empty list
        {
            if (searchTask != null)
                searchTask.cancel(true);
            refresh = false;
            search(searchQuery);
        } else if (this.results != null) //orientation change
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Lyrics l = results.get(position);
                    ((MainActivity) SearchFragment.this.getActivity()).updateLyricsFragment(R.animator.slide_out_end, l.getArtist(), l.getTrack());
                }
            });
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (isHidden())
            return;
        DrawerAdapter drawerAdapter = ((DrawerAdapter) ((ListView) this.getActivity().findViewById(R.id.drawer_list)).getAdapter());
        this.isActiveFragment = true;
        if (drawerAdapter.getSelectedItem() != 6) {
            drawerAdapter.setSelectedItem(6);
            drawerAdapter.notifyDataSetChanged();
        }
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
        ActionBar actionBar = mainActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        inflater.inflate(R.menu.menu_search, menu);
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getActivity()
                .getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search_view).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setQuery(searchQuery, false);
        searchView.setMaxWidth(99999); //fixme?
        if (mainActivity.mDrawerToggle != null) {
            mainActivity.mDrawerToggle.setDrawerIndicatorEnabled(false);
            ((DrawerLayout) mainActivity.drawer).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public void refresh() {
        this.refresh = true;
    }

    public void setResults(List<Lyrics> results) {
        this.results = results;
        if (errorView != null) {
            if (results.size() == 0) {
                errorView.setVisibility(View.VISIBLE);
                errorVisibility = View.VISIBLE;
            } else {
                errorVisibility = View.INVISIBLE;
                errorView.setVisibility(errorVisibility);
            }
        }
    }

    private void search(String searchQuery) {
        this.setListShown(false);
        if (errorView != null) {
            errorView.setVisibility(View.INVISIBLE);
            errorVisibility = View.INVISIBLE;
        }
        if (!OnlineAccessVerifier.check(this.getActivity())) {
            LayoutInflater inflater = (LayoutInflater) this.getActivity().getSystemService(MainActivity.LAYOUT_INFLATER_SERVICE);
            errorView = (ViewGroup) inflater.inflate(R.layout.error_msg, this.getListView(), false);
            Toast.makeText(this.getActivity(), this.getString(R.string.connection_error), Toast.LENGTH_LONG).show();
            ViewGroup layout = null;
            if (this.getView() != null) {
                layout = (ViewGroup) getListView().getParent();
            }
            if (layout != null && errorView != null) {
                errorVisibility = View.VISIBLE;
                errorView.setVisibility(errorVisibility);
                if (errorView.getParent() == null)
                    layout.addView(errorView);
            }
            setListShown(true);
            if (results != null && results.size() > 0) {
                results.clear();
                ((SearchAdapter) getListAdapter()).notifyDataSetChanged();
            }
        } else {
            searchTask = new SearchTask();
            searchTask.execute(searchQuery, this);
        }
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

}