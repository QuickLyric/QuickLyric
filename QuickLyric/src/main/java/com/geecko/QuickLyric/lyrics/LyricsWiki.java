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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import static com.geecko.QuickLyric.utils.Net.getUrlAsString;

public class LyricsWiki {
    private static final String baseUrl = "http://lyrics.wikia.com/api.php?action=lyrics&fmt=json&func=getSong&artist=%1s&song=%1s";

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
        int j;
        StringBuilder stringBuilder = new StringBuilder();
        if (url.endsWith("action=edit"))
            return new Lyrics(Lyrics.NO_RESULT);
        String[] encrypted;
        try {
            String html = getUrlAsString(url);
            String preceding = html.substring(html.indexOf("<div class='lyricbox'>"));
            String following = preceding.substring(6 + preceding.indexOf("</div>"));
            encrypted = following.substring(0, following.indexOf("<!--")).replace("<br />", "\n;").replaceAll("<.*?>", "").split(";");
        } catch (StringIndexOutOfBoundsException | IOException e) {
            e.printStackTrace();
            return new Lyrics(Lyrics.ERROR);
        }
        int i = encrypted.length;
        j = 0;
        while (j < i) {
            String s = encrypted[j];
            if (s.equals("\n"))
                stringBuilder.append("<br />");
            else if (s.startsWith("&#"))
                try {
                    stringBuilder.append((char) Integer.valueOf(s.replaceAll("&#", "")).intValue());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return new Lyrics(Lyrics.NEGATIVE_RESULT);
                }
            j++;
        }

        if (artist == null)
            artist = url.substring(24).replace("Gracenote:", "").split(":", 2)[0].replace('_', ' ');
        if (song == null)
            song = url.substring(24).replace("Gracenote:", "").split(":", 2)[1].replace('_', ' ');

        try {
            artist = URLDecoder.decode(artist,"UTF-8");
            song = URLDecoder.decode(song,"UTF-8");
        } catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }
        String text = stringBuilder.toString();
        if (text.contains("Unfortunately, we are not licensed to display the full lyrics for this song at the moment.")) {
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