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

package com.geecko.QuickLyric.utils;

import com.geecko.QuickLyric.Keys;

import junit.framework.Assert;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpotifyTest {

    @Test
    public void testSpotifyKey() {
        int statusCode = 0;
        String redirectURI = "quicklyric://spotify/callback";
        try {
            URL queryURL = new URL(String.format("https://accounts.spotify.com/authorize?client_id=%s&response_type=code&redirect_uri=%s", Keys.SPOTIFY_PUBLIC, URLEncoder.encode(redirectURI, "UTF-8")));
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(queryURL)
                    .build();
            Response response = client.newCall(request).execute();
            statusCode = response.code();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(200, statusCode);
    }
}
