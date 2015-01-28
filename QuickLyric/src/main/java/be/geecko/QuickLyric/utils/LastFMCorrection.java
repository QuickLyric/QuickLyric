package be.geecko.QuickLyric.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

import be.geecko.QuickLyric.Keys;

import static be.geecko.QuickLyric.utils.Net.getUrlAsString;

/**
 * This file is part of QuickLyric
 * Created by geecko on 29/09/14.
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

public class LastFMCorrection {
    public static String[] getCorrection(String artist, String track) {
        if ((artist == null) || (track == null))
            return new String[]{null, null};
        String encodedArtist;
        String encodedSong;
        try {
            encodedArtist = URLEncoder.encode(artist, "UTF-8");
            encodedSong = URLEncoder.encode(track, "UTF-8");
            String baseUrl = "http://ws.audioscrobbler.com/2.0/?method=track.getcorrection&artist=%s&track=%s&api_key=%s&format=json";
            JSONObject json = new JSONObject(getUrlAsString(new URL(
                    String.format(baseUrl, encodedArtist, encodedSong, Keys.lastFM))))
                    .getJSONObject("corrections").getJSONObject("correction");
            artist = json.getJSONObject("track").getJSONObject("artist").getString("name");
            track = json.getJSONObject("track").getString("name");
            return new String[]{artist, track};
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        return new String[]{null, null};
    }
}
