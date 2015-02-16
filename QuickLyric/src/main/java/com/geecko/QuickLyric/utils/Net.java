package com.geecko.QuickLyric.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This file is part of QuickLyric
 * Created by geecko
 *
 * QuickLyric is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QuickLyric is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with QuickLyric.  If not, see <http://www.gnu.org/licenses/>.
 */
public class Net {

    public static String getUrlAsString(String paramURL) throws IOException {
        return getUrlAsString(new URL(paramURL));
    }

    public static String getUrlAsString(URL paramURL)
            throws IOException {

        HttpURLConnection localHttpURLConnection = (HttpURLConnection) paramURL.openConnection();
        localHttpURLConnection.setRequestMethod("GET");
        localHttpURLConnection.setReadTimeout(15000);
        localHttpURLConnection.setUseCaches(false);
        localHttpURLConnection.connect();
        InputStreamReader localInputStreamReader = new InputStreamReader(localHttpURLConnection.getInputStream());
        BufferedReader localBufferedReader = new BufferedReader(localInputStreamReader);
        StringBuilder localStringBuilder = new StringBuilder();
        while (true) {
            String str = localBufferedReader.readLine();
            if (str == null)
                break;
            localStringBuilder.append(str).append("\n");
        }
        localInputStreamReader.close();
        return localStringBuilder.toString();
    }
}
