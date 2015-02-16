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

package com.geecko.QuickLyric.tasks;

import android.os.AsyncTask;
import android.os.Process;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.geecko.QuickLyric.adapter.SearchAdapter;
import com.geecko.QuickLyric.fragment.SearchFragment;
import com.geecko.QuickLyric.lyrics.Genius;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;

import java.util.List;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;

public class SearchTask extends AsyncTask<Object, Object, List<Lyrics>> {

    private SearchFragment searchFragment;

    @Override
    protected List<Lyrics> doInBackground(Object... params) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        String keyword = (String) params[0];
        searchFragment = (SearchFragment) params[1];
        if (searchFragment == null || !OnlineAccessVerifier.check(searchFragment.getActivity()))
            return null;

        List<Lyrics> results;
        do
            results = Genius.search(keyword);
        while (results == null && !isCancelled() && searchFragment.isActiveFragment);
        return results;
    }

    protected void onPostExecute(final List<Lyrics> results) {
        if (results == null || !searchFragment.isActiveFragment)
            return;
        String[] mSongsArray = new String[results.size()];
        String[] mArtistsArray = new String[mSongsArray.length];
        int i = 0;
        for (Lyrics l : results) {
            mSongsArray[i] = l.getTrack();
            mArtistsArray[i++] = l.getArtist();
        }

        final MainActivity mainActivity = ((MainActivity) searchFragment.getActivity());
        searchFragment.setListAdapter(new SearchAdapter(searchFragment.getActivity(), mSongsArray, mArtistsArray));
        searchFragment.setResults(results);
        if (searchFragment.getListView() != null)
            searchFragment.getListView().setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Lyrics l = results.get(position);
                    mainActivity.updateLyricsFragment(R.animator.slide_out_end, l.getArtist(), l.getTrack(), l.getURL());
                }
            });
        searchFragment.setListShown(true);
    }

}
