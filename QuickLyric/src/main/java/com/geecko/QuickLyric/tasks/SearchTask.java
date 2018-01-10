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

package com.geecko.QuickLyric.tasks;

import android.os.AsyncTask;
import android.os.Process;
import android.widget.Toast;

import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.SearchActivity;
import com.geecko.QuickLyric.fragment.SearchFragment;
import com.geecko.QuickLyric.provider.LyricsChart;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.geecko.QuickLyric.provider.LyricsChart.search;

public class SearchTask extends AsyncTask<Object, Object, List<Lyrics>> {

    private WeakReference<SearchFragment> searchFragment;
    private String searchQuery;

    @Override
    @SuppressWarnings("unchecked")
    protected List<Lyrics> doInBackground(Object... params) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        searchQuery = (String) params[0];
        searchFragment = new WeakReference<>((SearchFragment) params[1]);
        int position = (Integer) params[2];
        SearchActivity searchActivity = (SearchActivity) searchFragment.get().getActivity();
        if (searchActivity == null)
            return null;

        List<Lyrics> results;
        do
            results = doSearch(searchActivity.searchProviders.get(position));
        while (results == null && !isCancelled() &&  searchFragment != null &&
                searchFragment.get().getActivity() != null &&
                (OnlineAccessVerifier.check(searchFragment.get().getActivity()) ||
                position == 0)); // DatabaseHelper

        return results;
    }

    protected void onPostExecute(List<Lyrics> results) {
        if (searchFragment.get().getActivity() == null)
            return;
        ((SearchActivity) searchFragment.get().getActivity()).setSearchQuery(searchQuery);
        if (results == null && searchFragment.get().getActivity() != null &&
                !OnlineAccessVerifier.check(searchFragment.get().getActivity()))
            Toast.makeText(searchFragment.get().getActivity(),
                    searchFragment.get().getString(R.string.connection_error),
                    Toast.LENGTH_LONG).show();
        else if (searchFragment.get().getActivity() != null) {
            if (results == null)
                results = new ArrayList<>(0);
            searchFragment.get().setResults(results);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Lyrics> doSearch(Class provider) {
        String s = provider.getSimpleName();
        if (s.equals(DatabaseHelper.class.getSimpleName())) {
            return DatabaseHelper.getInstance(searchFragment.get().getActivity()).search(searchQuery);
        } else if (s.equals(LyricsChart.class.getSimpleName())) {
            return search(searchQuery);
        } else {
            try {
                return (List<Lyrics>) provider.getMethod("search", String.class).invoke(null, searchQuery);
            } catch (Exception ignored) {
                ignored.printStackTrace();
                return null;
            }
        }
    }

}
