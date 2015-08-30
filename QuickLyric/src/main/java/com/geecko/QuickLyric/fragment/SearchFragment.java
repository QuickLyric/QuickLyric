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

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.SearchActivity;
import com.geecko.QuickLyric.adapter.SearchAdapter;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.tasks.SearchTask;

import java.util.List;

public class SearchFragment extends ListFragment {

    private String searchQuery;
    public List<Lyrics> results;
    private boolean refresh = false;
    private ViewGroup errorView;
    private static int errorVisibility = View.INVISIBLE;
    public SearchTask searchTask;
    public SearchActivity searchTabs;
    private int searchProvider;
    public int position;

    @Override
    public void onActivityCreated(Bundle bundle) {
        setRetainInstance(true);
        super.onActivityCreated(bundle);

        ListView listView = getListView();
        View fragmentView = getView();
        TypedValue typedValue = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
        if (fragmentView != null)
            fragmentView.setBackgroundColor(typedValue.data);

        if (searchQuery == null || searchQuery.equals("")) {  // bug
            getActivity().onBackPressed();
        } else if (listView.getAdapter() == null || refresh) { //refresh or empty list
            if (searchTask != null)
                searchTask.cancel(true);
            refresh = false;
            search(searchQuery);
        } else if (this.results != null) //orientation change
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Lyrics lyrics = results.get(position);
                    Intent activityResult = new Intent();
                    activityResult.putExtra("lyrics", lyrics);
                    getActivity().setResult(Activity.RESULT_OK, activityResult);
                    getActivity().finish();
                    getActivity().overridePendingTransition(R.anim.fade_in, R.anim.slide_out_end);
                }
            });
    }

    public void setSearchProvider(int searchProvider) {
        this.searchProvider = searchProvider;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String getSearchQuery() {
        return this.searchQuery;
    }

    public void setSearchTabs(SearchActivity searchActivity) {
        this.searchTabs = searchActivity;
    }

    public void refresh() {
        this.refresh = true;
    }

    public void setResults(List<Lyrics> results) {
        this.results = results;
        if (getView() != null && getListView() != null) {
            LayoutInflater inflater = (LayoutInflater)
                    getActivity().getSystemService(MainActivity.LAYOUT_INFLATER_SERVICE);
            errorView = (ViewGroup)
                    inflater.inflate(R.layout.error_msg, getListView(), false);

            if (getView() != null && errorView != null) {
                if (errorView.getParent() == null)
                    ((ViewGroup) getView()).addView(errorView);
            }

            getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Lyrics lyrics = SearchFragment.this.results.get(position);
                    Intent activityResult = getActivity().getIntent();
                    activityResult = activityResult == null ? new Intent() : activityResult;
                    activityResult.putExtra("lyrics", lyrics);
                    getActivity().setResult(Activity.RESULT_OK, activityResult);
                    getActivity().finish();
                    getActivity().overridePendingTransition(android.R.anim.fade_in, R.anim.slide_out_end);
                }
            });
            ViewGroup parent = ((ViewGroup) getListView().getParent());
            parent.removeView(parent.findViewById(R.id.error_msg));
            setListShown(true);
            if (errorView != null) {
                if (results.size() == 0) {
                    errorView.setVisibility(View.VISIBLE);
                    errorVisibility = View.VISIBLE;
                } else {
                    errorVisibility = View.INVISIBLE;
                    errorView.setVisibility(errorVisibility);
                }
            }
            String[] songsArray = new String[results.size()];
            String[] artistsArray = new String[results.size()];
            int i = 0;
            for (Lyrics l : results) {
                songsArray[i] = l.getTrack();
                artistsArray[i++] = l.getArtist();
            }
            setListAdapter(new SearchAdapter(searchTabs, songsArray, artistsArray));
        }
        ((SearchPagerAdapter) searchTabs.viewPager.getAdapter()).registerFragment(position, this);
    }

    protected void search(String searchQuery) {
        if (getView() != null)
            this.setListShown(false);
        if (errorView != null) {
            errorView.setVisibility(View.INVISIBLE);
            errorVisibility = View.INVISIBLE;
        }
        searchTask = new SearchTask();
        searchTask.execute(searchQuery, this, searchProvider);
    }
}