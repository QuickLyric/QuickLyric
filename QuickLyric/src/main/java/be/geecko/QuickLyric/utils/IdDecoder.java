package be.geecko.QuickLyric.utils;

import android.content.Context;
import android.os.AsyncTask;

import java.io.IOException;

import be.geecko.QuickLyric.lyrics.Lyrics;
import be.geecko.QuickLyric.tasks.DownloadTask;
import be.geecko.QuickLyric.tasks.ParseTask;

import static be.geecko.QuickLyric.utils.Net.getUrlAsString;

/**
 * This file is part of QuickLyric
 * Created by geecko on 15/09/14.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class IdDecoder extends AsyncTask<String, Integer, Lyrics> {
    private Context mContext;

    public IdDecoder(Context context) {
        this.mContext = context;
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
                track = title.split(" : ")[1];
                artist = title.split(" : ")[0];
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            return new Lyrics(Lyrics.ERROR);
        Lyrics res = new Lyrics(Lyrics.NO_RESULT);
        res.setArtist(artist);
        res.setTitle(track);
        return res;
    }

    @Override
    protected void onPostExecute(Lyrics lyrics) {
        super.onPostExecute(lyrics);
        if (lyrics.getFlag() == Lyrics.NO_RESULT)
            new DownloadTask().execute(mContext, lyrics.getArtist(), lyrics.getTrack(), null);
        else
            new ParseTask().execute();
    }


}
