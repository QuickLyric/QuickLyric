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
import com.geecko.QuickLyric.utils.Net;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

import static com.geecko.QuickLyric.lyrics.Lyrics.*;

@Reflection
public class MetalArchives {

    public static final String domain = "metal-archives.com";

    @Reflection
    public static Lyrics fromMetaData(String artist, String title) {
        String baseURL = "http://www.metal-archives.com/search/ajax-advanced/searching/songs/?bandName=%s&songTitle=%s&releaseType[]=1&exactSongMatch=1&exactBandMatch=1";
        String urlArtist = artist.replaceAll("\\s","+");
        String urlTitle = title.replaceAll("\\s","+");
        String url;
        String text;
        try {
            String response = Net.getUrlAsString(String.format(baseURL, urlArtist, urlTitle));
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray track = jsonResponse.getJSONArray("aaData").getJSONArray(0);
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < track.length(); i++)
                builder.append(track.getString(i));
            Document trackDocument = Jsoup.parse(builder.toString());
            url = trackDocument.getElementsByTag("a").get(1).attr("href");
            String id = trackDocument.getElementsByClass("viewLyrics").get(0).id().substring(11);
            text = Jsoup.connect("http://www.metal-archives.com/release/ajax-view-lyrics/id/"+id)
                    .get().body().html();
        } catch (IOException e) {
            return new Lyrics(ERROR);
        } catch (JSONException e) {
            return new Lyrics(NO_RESULT);
        }
        Lyrics lyrics = new Lyrics(POSITIVE_RESULT);
        lyrics.setArtist(artist);
        lyrics.setTitle(title);
        lyrics.setText(text);
        lyrics.setSource(domain);
        lyrics.setURL(url);

        return lyrics;
    }

    @Reflection
    public static Lyrics fromURL(String url, String artist, String title){
        // TODO: support metal-archives URL
        return new Lyrics(NO_RESULT);
    }

}
