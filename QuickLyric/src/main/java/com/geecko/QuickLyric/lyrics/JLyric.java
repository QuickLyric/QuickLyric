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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;

public class JLyric {
    private static final String baseUrl = "http://search.j-lyric.net/index.php?ct=0&ca=0&kl=&cl=0&ka=%1s&kt=%1s";

    public static Lyrics fromMetaData(String artist, String song) {
        if ((artist == null) || (song == null))
            return new Lyrics(Lyrics.ERROR);

        String encodedArtist;
        String encodedSong;
        String url;

        try {
            encodedArtist = URLEncoder.encode(artist, "UTF-8");
            encodedSong = URLEncoder.encode(song, "UTF-8");
            Document searchPage = Jsoup.connect(String.format(baseUrl, encodedArtist, encodedSong)).get();
            Elements artistBlocks = searchPage.body().select("div#lyricList");

            //@todo give all results
            if(artistBlocks.first() == null)
                return new Lyrics(Lyrics.NO_RESULT);
            url = artistBlocks.first().select("div.title a").attr("href");

        } catch (IOException e) {
            e.printStackTrace();
            return new Lyrics(Lyrics.ERROR);
        }

        return fromURL(url, artist, song);
    }

    public static Lyrics fromURL(String url, String artist, String song) {
        String text;

        try {
            Document lyricsPage = Jsoup.connect(url).get();
            text = lyricsPage.select("p#lyricBody").html();
            if (artist == null)
                artist = lyricsPage.select("div.body")
                        .get(0).child(0).child(0).child(0).child(0).child(0).text();
            if (song == null)
                song = lyricsPage.select("div.caption").get(0).child(0).text();
        } catch (IOException e) {
            e.printStackTrace();
            return new Lyrics(Lyrics.ERROR);
        }

        if(text == null) {
            return new Lyrics(Lyrics.ERROR);
        } else {
            Lyrics lyrics = new Lyrics(Lyrics.POSITIVE_RESULT);
            lyrics.setArtist(artist);
            lyrics.setTitle(song);
            lyrics.setText(text);
            lyrics.setSource("J-Lyric");
            lyrics.setURL(url);
            return lyrics;
        }

    }

}