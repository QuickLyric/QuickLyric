package com.geecko.QuickLyric.lyrics;

import com.geecko.QuickLyric.Keys;
import com.geecko.QuickLyric.annotations.Reflection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.regex.Pattern;

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
public class Genius {

    @Reflection
    public static final String domain = "genius.com";

    public static ArrayList<Lyrics> search(String query) {
        ArrayList<Lyrics> results = new ArrayList<>();
        query = Normalizer.normalize(query, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        JSONObject response = null;
        try {
            URL queryURL = new URL(String.format("http://api.genius.com/search?q=%s", URLEncoder.encode(query, "UTF-8")));
            Connection connection = Jsoup.connect(queryURL.toExternalForm())
                    .header("Authorization", "Bearer " + Keys.GENIUS)
                    .ignoreContentType(true);
            Document document = connection.get();
            response = new JSONObject(document.text());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        try {
            if (response == null || response.getJSONObject("meta").getInt("status") != 200)
                return results;
            JSONArray hits = response.getJSONObject("response").getJSONArray("hits");

            int processed = 0;
            while (processed < hits.length()) {
                JSONObject song = hits.getJSONObject(processed).getJSONObject("result");
                String artist = song.getJSONObject("primary_artist").getString("name");
                String title = song.getString("title");
                String url = "http://genius.com/songs/" + song.getInt("id");
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

    @Reflection
    public static Lyrics fromMetaData(String originalArtist, String originalTitle) {
        String urlArtist = Normalizer.normalize(originalArtist, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String urlTitle = Normalizer.normalize(originalTitle, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        urlArtist = urlArtist.replaceAll("[^a-zA-Z0-9\\s+]", "").replaceAll("&", "and")
                .trim().replaceAll("[\\s+]", "-");
        urlTitle = urlTitle.replaceAll("[^a-zA-Z0-9\\s+]", "").replaceAll("&", "and")
                .trim().replaceAll("[\\s+]", "-");
        String url = String.format("http://genius.com/%s-%s-lyrics", urlArtist, urlTitle);
        return fromURL(url, originalArtist, originalTitle);
    }

    public static Lyrics fromURL(String url, String artist, String title) {
        Document lyricsPage;
        String text;
        try {
            lyricsPage = Jsoup.connect(url).get();
            Elements lyricsDiv = lyricsPage.select("div.lyrics");
            if (lyricsDiv.isEmpty())
                throw new StringIndexOutOfBoundsException();
            else
                text = Jsoup.clean(lyricsDiv.html(), Whitelist.none().addTags("br")).trim();
        } catch (HttpStatusException e) {
            return new Lyrics(Lyrics.NO_RESULT);
        } catch (IOException | StringIndexOutOfBoundsException e) {
            e.printStackTrace();
            return new Lyrics(Lyrics.ERROR);
        }
        if (artist == null) {
            title = lyricsPage.getElementsByClass("text_title").get(0).text();
            artist = lyricsPage.getElementsByClass("text_artist").get(0).text();
        }
        Lyrics result = new Lyrics(Lyrics.POSITIVE_RESULT);
        if ("[Instrumental]".equals(text))
            result = new Lyrics(Lyrics.NEGATIVE_RESULT);
        Pattern pattern = Pattern.compile("\\[.+\\]");
        StringBuilder builder = new StringBuilder();
        for (String line : text.split("<br> ")) {
            if (!pattern.matcher(line.replaceAll("\\s","")).matches())
                builder.append(line).append("<br/>");
        }
        builder.delete(builder.length() - 5, builder.length());
        result.setArtist(artist);
        result.setTitle(title);
        result.setText(builder.toString());
        result.setURL(url);
        result.setSource("Genius");
        return result;
    }

}