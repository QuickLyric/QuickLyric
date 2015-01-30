package be.geecko.QuickLyric.lyrics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;

import static be.geecko.QuickLyric.utils.Net.getUrlAsString;

/**
 * This file is part of QuickLyric
 * Created by geecko on 29/01/15.
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
public class Genius {

    public static ArrayList<Lyrics> search(String query) {
        ArrayList<Lyrics> results = new ArrayList<>();
        query = Normalizer.normalize(query, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        JSONObject response = null;
        try {
            URL queryURL = new URL(String.format("http://api.genius.com/search?q=%s", URLEncoder.encode(query, "UTF-8")));
            response = new JSONObject(getUrlAsString(queryURL));
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
            html = getUrlAsString(url);
        } catch (IOException e) {
            return new Lyrics(Lyrics.ERROR);
        }
        String cut = html.substring(html.indexOf("<div class=\"lyrics_container\">"));
        cut = cut.substring(cut.indexOf("<p>") + 3, cut.indexOf("</div>"));
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