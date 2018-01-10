/*
 * *
 *  * This file is part of QuickLyric
 *  * Copyright © 2017 QuickLyric SPRL
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

package com.geecko.QuickLyric.provider;

import android.text.TextUtils;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.StringRequest;
import com.geecko.QuickLyric.BuildConfig;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.utils.Chromaprint;
import com.geecko.QuickLyric.utils.Net;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.util.ArrayList;


@SuppressWarnings("unused")
public class LyricsChart {

    public static final String domain = "api.chartlyrics.com";

    public static Lyrics fromMetaData(String originalArtist, String originalTitle, boolean lrc, Chromaprint.Fingerprint fingerprint, String player) {
        String json;
        try {
            String url = "http://api.chartlyrics.com/apiv1.asmx/SearchLyricDirect?"
                    + "artist=" + URLEncoder.encode(originalArtist, "UTF-8")
                    + "&song=" + URLEncoder.encode(originalTitle, "UTF-8");
            json = Net.getUrlAsString(url);
            return fromXml(json, originalArtist, originalTitle);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            Lyrics lyrics = new Lyrics(Lyrics.NO_RESULT);
            lyrics.setArtist(originalArtist);
            lyrics.setTitle(originalTitle);
            return lyrics;
        } catch (Exception e) {
            e.printStackTrace();
            Lyrics error = new Lyrics(Lyrics.ERROR);
            if (e instanceof InvalidKeyException)
                error.setErrorCode(800);
            else if (e instanceof ArrayIndexOutOfBoundsException)
                error.setErrorCode(900);
            return error;
        }
    }

    public static Lyrics fromURL(String url, String originalArtist, String originalTitle, boolean lrc) {
        Lyrics lyrics = new Lyrics(Lyrics.ERROR);
        try {
            lyrics = fromXml(Net.getUrlAsString(url));
        } catch (Exception e) {
            if (!BuildConfig.DEBUG && !(e instanceof IOException)) {
                e.printStackTrace();
            }
            if (e instanceof InvalidKeyException)
                lyrics.setErrorCode(800);
            else if (e instanceof ArrayIndexOutOfBoundsException)
                lyrics.setErrorCode(900);
        }
        return lyrics;
    }

    public static ArrayList<Lyrics> search(String query) {
        ArrayList<Lyrics> results = new ArrayList<>();
        try {
            String url = "http://api.chartlyrics.com/apiv1.asmx/SearchLyricText?lyricText=";
            url += URLEncoder.encode(query, "UTF-8");
            Document doc = Jsoup.parse(url, null);
            Elements elements = doc.getElementsByTag("SearchLyricResult");
            for (Element element : elements) {
                String id = element.getElementsByTag("TrackId").get(0).text();
                String checksum = element.getElementsByTag("TrackChecksum").get(0).text();
                Lyrics lyrics = new Lyrics(Lyrics.SEARCH_ITEM);
                lyrics.setArtist(element.getElementsByTag("artist").get(0).text());
                lyrics.setTitle(element.getElementsByTag("song").get(0).text());
                lyrics.setURL("http://api.chartlyrics.com/apiv1.asmx/GetLyric?lyricId=" + id + "&lyricCheckSum=" + checksum);
                results.add(lyrics);
            }
            return results;
        } catch (Exception e) {
            if (!BuildConfig.DEBUG && !(e instanceof IOException)) {
                e.printStackTrace();
            }
        }

        return new ArrayList<>();
    }

    public static Lyrics fromXml(String xmlString, String... originalMetadata) {
        if (TextUtils.isEmpty(xmlString))
            return new Lyrics(Lyrics.ERROR);
        Document doc = Jsoup.parse(xmlString);
        Element element = doc.getElementsByTag("GetLyricResult").first();
        String id = element.getElementsByTag("TrackId").get(0).text();
        String checksum = element.getElementsByTag("LyricChecksum").get(0).text();
        Lyrics lyrics = new Lyrics(Lyrics.POSITIVE_RESULT);
        lyrics.setArtist(element.getElementsByTag("LyricArtist").get(0).text());
        lyrics.setTitle(element.getElementsByTag("LyricSong").get(0).text());
        lyrics.setURL("http://api.chartlyrics.com/apiv1.asmx/GetLyric?lyricId=" + id + "&lyricCheckSum=" + checksum);
        boolean hasOriginalMetadata = originalMetadata != null && originalMetadata.length > 0;
        String originalArtist = hasOriginalMetadata ? originalMetadata[0] : null;
        String originalTitle = hasOriginalMetadata ? originalMetadata[1] : null;

        if (TextUtils.isEmpty(lyrics.getArtist()))
            lyrics.setArtist(originalArtist);
        else
            lyrics.setOriginalArtist(originalArtist);
        if (TextUtils.isEmpty(lyrics.getTitle()))
            lyrics.setTitle(originalTitle);
        else
            lyrics.setOriginalTitle(originalTitle);
        lyrics.setText(element.getElementsByTag("Lyric").get(0).html());
        lyrics.setSource(domain);
        return lyrics;
    }

    public static com.android.volley.Request getVolleyRequest(boolean lrc, Listener<String> listener,
                                                              ErrorListener errorListener, Chromaprint.Fingerprint fingerprint, String... args) throws Exception {
        String url = String.format("http://api.chartlyrics.com/apiv1.asmx/SearchLyricDirect?artist=%s&song=%s", URLEncoder.encode(args[0], "UTF-8"), URLEncoder.encode(args[1], "UTF-8"));
        StringRequest request = new StringRequest(com.android.volley.Request.Method.GET, url, listener, errorListener);
        request.setRetryPolicy(new DefaultRetryPolicy(10000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        return request;
    }

}
