/*
 * *
 *  * This file is part of QuickLyric
 *  * Created by maximko, edited by geecko
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

//import android.util.Log;

import com.geecko.QuickLyric.annotations.Reflection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.net.URLEncoder;

@Reflection
public class Lololyrics {

    private static final String baseUrl = "http://api.lololyrics.com/0.5/getLyric?artist=%1s&track=%1s&rawutf8=1";
    public static final String domain = "www.lololyrics.com/";

    @Reflection
    public static Lyrics fromMetaData(String artist, String song) {
        if ((artist == null) || (song == null))
            return new Lyrics(Lyrics.ERROR);

        try {
            String encodedArtist = URLEncoder.encode(artist, "UTF-8");
            String encodedSong = URLEncoder.encode(song, "UTF-8");

            String url = String.format(baseUrl, encodedArtist, encodedSong);

            String body = Jsoup.connect(url).execute().body();
            Document lololyrics = Jsoup.parse(body.replaceAll("(\\n)", "<br />"));

            Element loloResult = lololyrics.select("result").first();

            if (loloResult.select("status") == null || !"OK".equals(loloResult.select("status").text()))
                return new Lyrics(Lyrics.NO_RESULT);

            if (loloResult.select("response").hasText()) {
                Lyrics lyrics = new Lyrics(Lyrics.POSITIVE_RESULT);
                lyrics.setArtist(artist);
                lyrics.setTitle(song);
                String text = Parser.unescapeEntities(loloResult.select("response").html(), true);
                lyrics.setText(text);
                lyrics.setSource(domain);

                if (loloResult.select("cover").hasText())
                    lyrics.setCoverURL(loloResult.select("cover").text());

                String weburl = loloResult.select("url").html();
                lyrics.setURL(weburl);
                return lyrics;
            } else {
                return new Lyrics(Lyrics.NO_RESULT);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return new Lyrics(Lyrics.ERROR);
        }
    }

    // TODO handle lololyrics.com url
    public static Lyrics fromURL(String url, String artist, String song) {
        /** We can't transform generic lololyrics url to API url.
         Also we can't get artist name and song title from Lololyrics API. **/
        return new Lyrics(Lyrics.NO_RESULT);
    }
}