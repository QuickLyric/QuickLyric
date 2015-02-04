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
