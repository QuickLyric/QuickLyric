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

package com.geecko.QuickLyric.tasks;

import android.os.AsyncTask;
import android.os.Build;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.utils.Net;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CoverArtLoader extends AsyncTask<Object, Object, String> {

    private MainActivity mActivity;

    @Override
    protected String doInBackground(Object... objects) {
        Lyrics lyrics = (Lyrics) objects[0];
        mActivity = (MainActivity) objects[1];
        String url = lyrics.getCoverURL();
        boolean online = objects.length >= 3 && (Boolean) objects[2];
        boolean secondTry = objects.length >= 4 && (Boolean) objects[3];

        File artworksDir = new File(mActivity.getCacheDir(), "artworks");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && (artworksDir.exists() || artworksDir.mkdirs())) {
            long size = 0;
            List<File> files = new ArrayList<>(Arrays.asList(artworksDir.listFiles()));
            for (File file : files) {
                size += file.length() / 1024;
            }
            File artworkFile = new File(artworksDir, lyrics.getOriginalArtist() + lyrics.getOriginalTrack() + ".png");
            if (size > 20000L) {
                File[] sortedFiles = new File[files.size() / 2];

                for (int i = 0; i < files.size() / 2; i++) {
                    sortedFiles[i] = Collections.min(files, new Comparator<File>() {
                        @Override
                        public int compare(File file1, File file2) {
                            return (int) (file1.lastModified() - file2.lastModified());
                        }
                    });
                    files.remove(sortedFiles[i]);
                }
                for (File file : sortedFiles) {
                    if (file != null && !file.getName().equals(artworkFile.getName()))
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                }
            }
            if (artworkFile.exists() && artworkFile.length() > 0) {
                return artworkFile.getAbsoluteFile().getAbsolutePath();
            }
        }
        if (url == null && online) {
            try {
                String requestURL = String.format(
                        "https://itunes.apple.com/search?term=%s+%s&entity=song&media=music",
                        URLEncoder.encode(lyrics.getArtist(), "UTF-8"),
                        URLEncoder.encode(lyrics.getTitle(), "UTF-8"));
                String txt = Net.getUrlAsString(new URL(requestURL));
                JSONObject json = new JSONObject(txt);
                JSONArray results = json.getJSONArray("results");
                JSONObject result = results.getJSONObject(0);
                url = result.getString("artworkUrl60").replace("60x60bb.jpg", "1000x1000bb.jpg");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException ignored) {
                if (!secondTry) {
                    lyrics.setArtist(lyrics.getOriginalArtist());
                    lyrics.setTitle(lyrics.getOriginalTrack());
                    return doInBackground(lyrics, mActivity, online, Boolean.TRUE);
                }
            }
        }
        return url;
    }

    @Override
    protected void onPostExecute(String url) {
        if (mActivity != null && !mActivity.hasBeenDestroyed())
            mActivity.updateArtwork(url);
    }
}