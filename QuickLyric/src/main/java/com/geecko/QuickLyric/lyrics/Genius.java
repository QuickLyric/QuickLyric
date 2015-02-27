package com.geecko.QuickLyric.lyrics;

import com.geecko.QuickLyric.utils.Net;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;

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
public class Genius {

    public static ArrayList<Lyrics> search(String query) {
        ArrayList<Lyrics> results = new ArrayList<>();
        query = Normalizer.normalize(query, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        JSONObject response = null;
        try {
            URL queryURL = new URL(String.format("http://api.genius.com/search?q=%s", URLEncoder.encode(query, "UTF-8")));
            response = new JSONObject(Net.getUrlAsString(queryURL));
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        try {
            if (response == null || response.getJSONObject("meta").getInt("status") != 200)
                return null;
            JSONArray hits = response.getJSONObject("response").getJSONArray("hits");

            int processed = 0;
            while (processed < hits.length()) {
                JSONObject song = hits.getJSONObject(processed).getJSONObject("result");
                String artist = song.getJSONObject("primary_artist").getString("name");
                String urlArtist = song.getJSONObject("primary_artist").getString("url");
                urlArtist = urlArtist.substring(urlArtist.lastIndexOf("/artists/") + 9);
                String title = song.getString("title");
                String urlTitle = title.replace(' ', '-');
                String url = String.format("http://genius.com/%s-%s-lyrics", urlArtist, urlTitle);
                Lyrics l = new Lyrics(Lyrics.SEARCH_ITEM);
                l.setArtist(artist);
                l.setTitle(title);
                l.setURL(url);
                l.setSource("Genius");
                results.add(l);
                processed++;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return results;
    }

    public static Lyrics fromMetaData(String originalArtist, String originalTitle) {
        String urlArtist = Normalizer.normalize(originalArtist, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String urlTitle = Normalizer.normalize(originalTitle, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        urlArtist = urlArtist.replaceAll("[^a-zA-Z0-9\\s]", "").trim().replaceAll("\\s+", "-");
        urlTitle = urlTitle.replaceAll("[^a-zA-Z0-9\\s]", "").trim().replaceAll("\\s+", "-");
        String url = String.format("http://genius.com/%s-%s-lyrics", urlArtist, urlTitle);
        return fromURL(url, originalArtist, originalTitle);
    }

    public static Lyrics fromURL(String url, String artist, String title) {
        String html;
        try {
            html = Net.getUrlAsString(url);
        } catch (IOException e) {
            return new Lyrics(Lyrics.ERROR);
        }
        String cut = html.substring(html.indexOf("<div class=\"lyrics_container\">"));
        cut = cut.substring(cut.indexOf("<p>") + 3);
        cut = cut.substring(0, cut.indexOf("</div>"));
        String text = cut.substring(0, cut.lastIndexOf("</p>"));
        text = text.replaceAll("</?a[^>]*>", "").trim();
        if (artist == null) {
            cut = html.substring(html.indexOf("var TRACKING_DATA = ") + 20);
            try {
                JSONObject trackingData = new JSONObject(cut.substring(0, cut.indexOf("//]]>")));
                artist = trackingData.getString("Primary Arist");
                title = trackingData.getString("Title");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Lyrics result = new Lyrics(Lyrics.POSITIVE_RESULT);
        result.setArtist(artist);
        result.setTitle(title);
        result.setText(text);
        result.setURL(url);
        result.setSource("Genius");
        return result;
    }

}