package be.geecko.QuickLyric.tasks;

import android.os.AsyncTask;
import android.os.Process;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.List;

import be.geecko.QuickLyric.MainActivity;
import be.geecko.QuickLyric.R;
import be.geecko.QuickLyric.adapter.SearchAdapter;
import be.geecko.QuickLyric.fragment.SearchFragment;
import be.geecko.QuickLyric.lyrics.Genius;
import be.geecko.QuickLyric.lyrics.Lyrics;
import be.geecko.QuickLyric.utils.OnlineAccessVerifier;

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
