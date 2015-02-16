package com.geecko.QuickLyric.utils;

import android.content.Context;
import android.os.AsyncTask;

import java.io.IOException;

import com.geecko.QuickLyric.fragment.LyricsViewFragment;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.tasks.DownloadTask;
import com.geecko.QuickLyric.tasks.ParseTask;

import static com.geecko.QuickLyric.utils.Net.getUrlAsString;

/**
 * This file is part of QuickLyric
 * Created by geecko
 *
 * QuickLyric is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QuickLyric is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with QuickLyric.  If not, see <http://www.gnu.org/licenses/>.
 */
public class IdDecoder extends AsyncTask<String, Integer, Lyrics> {
    private Context mContext;
    private LyricsViewFragment lyricsViewFragment;

    public IdDecoder(Context context, LyricsViewFragment lyricsFragment) {
        this.mContext = context;
        this.lyricsViewFragment = lyricsFragment;
    }

    @Override
    protected Lyrics doInBackground(String... strings) {
        String url = strings[0];
        String artist = null;
        String track = null;
        if (url.contains("http://www.soundhound.com/")) {
            try {
                String html = getUrlAsString(url);
                int preceding = html.indexOf("<title>SoundHound") + 20;
                int following = html.substring(preceding).indexOf("</title>");
                String title = html.substring(preceding, preceding + following);
                track = title.split(" by ")[0];
                artist = title.split(" by ")[1];
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (url.contains("http://shz.am/")) {
            try {
                String html = getUrlAsString(url);
                int preceding = html.indexOf("<title>") + 7;
                int following = html.substring(preceding).indexOf("</title>");
                String title = html.substring(preceding, preceding + following);
                artist = title.split(" - ")[0];

                preceding = html.indexOf("\"og:title\"") + 20;
                following = html.substring(preceding).indexOf("\"");
                track = html.substring(preceding, preceding + following);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            return new Lyrics(Lyrics.ERROR);
        Lyrics res = new Lyrics(Lyrics.SEARCH_ITEM);
        res.setArtist(artist);
        res.setTitle(track);
        return res;
    }

    @Override
    protected void onPostExecute(Lyrics lyrics) {
        super.onPostExecute(lyrics);
        if (lyrics.getFlag() == Lyrics.SEARCH_ITEM) {
            lyricsViewFragment.startRefreshAnimation();
            if (lyricsViewFragment.currentDownload != null &&
                    lyricsViewFragment.currentDownload.getStatus() != Status.FINISHED)
                lyricsViewFragment.currentDownload.cancel(true);
            new DownloadTask().execute(mContext, lyrics.getArtist(), lyrics.getTrack(), null);
        } else
            new ParseTask().execute();
    }
}