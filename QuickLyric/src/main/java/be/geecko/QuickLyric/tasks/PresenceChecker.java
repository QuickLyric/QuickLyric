package be.geecko.QuickLyric.tasks;

import android.os.AsyncTask;

import be.geecko.QuickLyric.fragment.LyricsViewFragment;
import be.geecko.QuickLyric.utils.DatabaseHelper;

public class PresenceChecker extends AsyncTask<Object, Void, Boolean> {
    private LyricsViewFragment lyricsViewFragment;

    @Override
    protected Boolean doInBackground(Object... params) {
        lyricsViewFragment = (LyricsViewFragment) params[0];
        String[] metaData = (String[]) params[1];
        return (DatabaseHelper.presenceCheck(new DatabaseHelper(lyricsViewFragment.getActivity()).getReadableDatabase(), metaData));
    }

    @Override
    protected void onPostExecute(Boolean present) {
        if (lyricsViewFragment.lyricsPresentInDB != present) {
            lyricsViewFragment.lyricsPresentInDB = present;
            lyricsViewFragment.getActivity().invalidateOptionsMenu();
        }
    }
}
