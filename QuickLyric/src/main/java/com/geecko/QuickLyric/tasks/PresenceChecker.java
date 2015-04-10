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

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.fragment.LyricsViewFragment;
import com.geecko.QuickLyric.utils.DatabaseHelper;

public class PresenceChecker extends AsyncTask<Object, Void, Boolean> {
    private LyricsViewFragment lyricsViewFragment;

    @Override
    protected Boolean doInBackground(Object... params) {
        lyricsViewFragment = (LyricsViewFragment) params[0];
        String[] metaData = (String[]) params[1];
        return lyricsViewFragment != null &&
                lyricsViewFragment.getActivity() != null &&
                ((MainActivity) lyricsViewFragment.getActivity()).database != null &&
                DatabaseHelper.presenceCheck(
                        ((MainActivity) lyricsViewFragment.getActivity()).database,
                        metaData
                );
    }

    @Override
    protected void onPostExecute(Boolean present) {
        if (lyricsViewFragment != null && lyricsViewFragment.lyricsPresentInDB != present) {
            lyricsViewFragment.lyricsPresentInDB = present;
            lyricsViewFragment.getActivity().invalidateOptionsMenu();
        }
    }
}
