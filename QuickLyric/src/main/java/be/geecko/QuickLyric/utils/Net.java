package be.geecko.QuickLyric.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This file is part of QuickLyric
 * Created by geecko on 15/09/14.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
