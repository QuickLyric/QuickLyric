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

package com.geecko.QuickLyric.lyrics;

import com.geecko.QuickLyric.annotations.Reflection;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;

import static com.geecko.QuickLyric.lyrics.Lyrics.ERROR;
import static com.geecko.QuickLyric.lyrics.Lyrics.NEGATIVE_RESULT;
import static com.geecko.QuickLyric.lyrics.Lyrics.NO_RESULT;
import static com.geecko.QuickLyric.lyrics.Lyrics.POSITIVE_RESULT;
import static com.geecko.QuickLyric.lyrics.Lyrics.SEARCH_ITEM;
import static com.geecko.QuickLyric.utils.Net.getUrlAsString;

public class LyricWiki {

    @Reflection
    public static final String domain = "lyrics.wikia.com";
    private static final String baseUrl =
            "http://lyrics.wikia.com/api.php?action=lyrics&fmt=json&func=getSong&artist=%1s&song=%1s";
    private static final String baseAPIUrl =
            "http://lyrics.wikia.com/wikia.php?controller=LyricsApi&method=getSong&artist=%1s&song=%2s";
    private static final String baseSearchUrl =
            "http://lyrics.wikia.com/Special:Search?search=%s&fulltext=Search";

    @Reflection
    public static ArrayList<Lyrics> search(String query) {
        ArrayList<Lyrics> results = new ArrayList<>();
        query = query + " song";
        query = Normalizer.normalize(query, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        try {
            URL queryURL = new URL(String.format(baseSearchUrl, URLEncoder.encode(query, "UTF-8")));
            Document searchpage = Jsoup.connect(queryURL.toExternalForm()).get();
            Elements searchResults = searchpage.getElementsByClass("Results");
            if (searchResults.size() >= 1) {
                searchResults = searchResults.get(0).getElementsByClass("result");
                for (Element searchResult : searchResults) {
                    String[] tags = searchResult.getElementsByTag("h1").text().split(":");
                    if (tags.length != 2) continue;
                    String url = searchResult.getElementsByTag("a").attr("href");
                    Lyrics lyrics = new Lyrics(SEARCH_ITEM);
                    lyrics.setArtist(tags[0]);
                    lyrics.setTitle(tags[1]);
                    lyrics.setURL(url);
                    lyrics.setSource(domain);
                    results.add(lyrics);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return results;
    }

    @Reflection
    public static Lyrics fromMetaData(String artist, String title) {
        if ((artist == null) || (title == null))
            return new Lyrics(ERROR);
        String originalArtist = artist;
        String originalTitle = title;
        String url = null;
        try {
            String encodedArtist = URLEncoder.encode(artist, "UTF-8");
            String encodedSong = URLEncoder.encode(title, "UTF-8");
            JSONObject json = new JSONObject(getUrlAsString(new URL(
                    String.format(baseUrl, encodedArtist, encodedSong))).replace("song = ", ""));
            url = URLDecoder.decode(json.getString("url"), "UTF-8");
            artist = json.getString("artist");
            title = json.getString("song");
            encodedArtist = URLEncoder.encode(artist, "UTF-8");
            encodedSong = URLEncoder.encode(title, "UTF-8");
            json = new JSONObject(getUrlAsString
                    (new URL(String.format(baseAPIUrl, encodedArtist, encodedSong)))
            ).getJSONObject("result");
            Lyrics lyrics = new Lyrics(POSITIVE_RESULT);
            lyrics.setArtist(artist);
            lyrics.setTitle(title);
            lyrics.setText(json.getString("lyrics").replaceAll("\n", "<br />"));
            lyrics.setURL(url);
            lyrics.setOriginalArtist(originalArtist);
            lyrics.setOriginalTitle(originalTitle);
            return lyrics;
        } catch (JSONException e) {
            return new Lyrics(NO_RESULT);
        } catch (IOException e) {
            return url == null ? new Lyrics(ERROR) : fromURL(url, originalArtist, originalTitle);
        }
    }

    public static Lyrics fromURL(String url, String artist, String song) {
        if (url.endsWith("action=edit")) {
            return new Lyrics(NO_RESULT);
        }
        String text;
        try {
            //url = URLDecoder.decode(url, "utf-8");
            Document lyricsPage = Jsoup.connect(url).get();
            Element lyricbox = lyricsPage.select("div.lyricBox").get(0);
            lyricbox.after(lyricbox.childNode(0));
            String lyricsHtml = lyricbox.html();
            text = lyricsHtml.substring(0, lyricsHtml.indexOf("<!--"))
                    .replaceAll("<.*?>", "")
                    .replaceAll("\n", "<br />");
            if (text.contains("&#"))
                text = Parser.unescapeEntities(text, true);
        } catch (IndexOutOfBoundsException | IOException e) {
            e.printStackTrace();
            return new Lyrics(ERROR);
        }

        if (artist == null)
            artist = url.substring(24).replace("Gracenote:", "").split(":", 2)[0].replace('_', ' ');
        if (song == null)
            song = url.substring(24).replace("Gracenote:", "").split(":", 2)[1].replace('_', ' ');

        try {
            artist = URLDecoder.decode(artist, "UTF-8");
            song = URLDecoder.decode(song, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (text.contains("Unfortunately, we are not licensed to display the full lyrics for this song at the moment.")
                || text.equals("Instrumental <br />")) {
            Lyrics result = new Lyrics(NEGATIVE_RESULT);
            result.setArtist(artist);
            result.setTitle(song);
            return result;
        } else if (text.equals("") || text.length() < 3)
            return new Lyrics(NO_RESULT);
        else {
            Lyrics lyrics = new Lyrics(POSITIVE_RESULT);
            lyrics.setArtist(artist);
            lyrics.setTitle(song);
            lyrics.setText(text);
            lyrics.setSource("LyricsWiki");
            lyrics.setURL(url);
            return lyrics;
        }
    }

}