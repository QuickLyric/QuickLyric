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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import static com.geecko.QuickLyric.utils.Net.getUrlAsString;

public class LyricsWiki {

    @Reflection
    public static final String domain = "lyrics.wikia.com";
    private static final String baseUrl = "http://lyrics.wikia.com/api.php?action=lyrics&fmt=json&func=getSong&artist=%1s&song=%1s";

    @Reflection
    public static Lyrics fromMetaData(String artist, String song) {
        if ((artist == null) || (song == null))
            return new Lyrics(Lyrics.ERROR);
        String encodedArtist;
        String encodedSong;
        URL url;
        try {
            encodedArtist = URLEncoder.encode(artist, "UTF-8");
            encodedSong = URLEncoder.encode(song, "UTF-8");
            JSONObject json = new JSONObject(getUrlAsString(new URL(
                    String.format(baseUrl, encodedArtist, encodedSong))).replace("song = ", ""));
            url = new URL(json.getString("url"));
            artist = json.getString("artist");
            song = json.getString("song");
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return new Lyrics(Lyrics.ERROR);
        }
        return fromURL(url.toExternalForm(), artist, song);
    }

    public static Lyrics fromURL(String url, String artist, String song) {
        if (url.endsWith("action=edit"))
            return new Lyrics(Lyrics.NO_RESULT);
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
            return new Lyrics(Lyrics.ERROR);
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
            Lyrics result = new Lyrics(Lyrics.NEGATIVE_RESULT);
            result.setArtist(artist);
            result.setTitle(song);
            return result;
        } else if (text.equals("") || text.length() < 3)
            return new Lyrics(Lyrics.NO_RESULT);
        else {
            Lyrics lyrics = new Lyrics(Lyrics.POSITIVE_RESULT);
            lyrics.setArtist(artist);
            lyrics.setTitle(song);
            lyrics.setText(text);
            lyrics.setSource("LyricsWiki");
            lyrics.setURL(url);
            return lyrics;
        }
    }

}