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
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.utils.Net;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

public class CoverArtLoader extends AsyncTask<Object, Object, String> {

    private MainActivity mActivity;

    @Override
    protected String doInBackground(Object... objects) {
        Lyrics lyrics = (Lyrics) objects[0];
        mActivity = (MainActivity) objects[1];
        String url = lyrics.getCoverURL();
        boolean secondTry = objects.length > 2;

        if (url == null) {
            try {
                String requestURL = String.format(
                        "https://itunes.apple.com/search?term=%s+%s&entity=song&media=music",
                        URLEncoder.encode(lyrics.getArtist(), "UTF-8"),
                        URLEncoder.encode(lyrics.getTrack(), "UTF-8"));
                String txt = Net.getUrlAsString(new URL(requestURL));
                JSONObject json = new JSONObject(txt);
                JSONArray results = json.getJSONArray("results");
                JSONObject result = results.getJSONObject(0);
                url = result.getString("artworkUrl60").replace("60x60bb.jpg", "600x600bb.jpg");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException ignored) {
                if (!secondTry) {
                    lyrics.setArtist(lyrics.getOriginalArtist());
                    lyrics.setTitle(lyrics.getTrack());
                    return doInBackground(lyrics, mActivity, Boolean.TRUE);
                }
            }
        }
        return url;
    }

    @Override
    protected void onPostExecute(String url) {
        if (mActivity != null && !mActivity.hasBeenDestroyed())
            mActivity.updateArtwork(url);
    }
}