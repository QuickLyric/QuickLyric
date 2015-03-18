package com.geecko.QuickLyric.utils;

import android.content.Context;
import android.os.AsyncTask;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.fragment.LyricsViewFragment;
import com.geecko.QuickLyric.lyrics.Lyrics;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

import static com.geecko.QuickLyric.utils.Net.getUrlAsString;

/**
 * This file is part of QuickLyric
 * Created by geecko
 * <p/>
 * QuickLyric is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
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
        String artist;
        String track;
        if (url.contains("http://www.soundhound.com/")) {
            try { // todo switch to Jsoup
                String html = getUrlAsString(url);
                int preceding = html.indexOf("root.App.trackDa") + 19;
                int following = html.substring(preceding).indexOf(";");
                String data = html.substring(preceding, preceding + following);
                JSONObject jsonData = new JSONObject(data);
                artist = jsonData.getString("artist_display_name");
                track = jsonData.getString("track_name");
            } catch (IOException | JSONException e) {
                e.printStackTrace(); // todo test offline
                return new Lyrics(Lyrics.ERROR);
            }

        } else if (url.contains("http://shz.am/")) {
            try {
                Document doc = Jsoup.connect(url.trim()).get();
                track = doc.getElementsByAttribute("data-track-title").text();
                artist = doc.getElementsByAttribute("data-track-artist").text();
            } catch (IOException e) {
                e.printStackTrace();
                return new Lyrics(Lyrics.ERROR);
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
        if (lyricsViewFragment != null) {
            if (lyrics.getArtist() == null && lyrics.getTrack() == null) {
                lyricsViewFragment.onLyricsDownloaded(lyrics);
                return;
            }
            lyricsViewFragment.startRefreshAnimation();
            lyricsViewFragment.fetchLyrics(lyrics.getArtist(), lyrics.getTrack());
        } else
            ((MainActivity) mContext).updateLyricsFragment(0, lyrics.getArtist(), lyrics.getTrack());
    }
}