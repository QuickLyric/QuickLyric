package be.geecko.QuickLyric.tasks;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

import be.geecko.QuickLyric.Keys;
import be.geecko.QuickLyric.fragment.LyricsViewFragment;
import be.geecko.QuickLyric.lyrics.Lyrics;

import static be.geecko.QuickLyric.utils.Net.getUrlAsString;

public class CoverArtLoader extends AsyncTask<Object, Object, String> {

    private LyricsViewFragment lyricsViewFragment;

    @Override
    protected String doInBackground(Object... objects) {
        Lyrics lyrics = (Lyrics) objects[0];
        lyricsViewFragment = (LyricsViewFragment) objects[1];
        String url = lyrics.getCoverURL();

        if (url == null) {
            try {
                String html = getUrlAsString(new URL(String.format("http://ws.audioscrobbler.com/2.0/?method=track.getInfo&api_key=%s&artist=%s&track=%s&format=json", Keys.lastFM, URLEncoder.encode(lyrics.getArtist(), "UTF-8"), URLEncoder.encode(lyrics.getTrack(), "UTF-8"))));
                JSONObject json = new JSONObject(html);
                url = json.getJSONObject("track").getJSONObject("album").getJSONArray("image").getJSONObject(2).getString("#text");
                if (url.contains("noimage"))
                    url = null;
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    @Override
    protected void onPostExecute(String url) {
        lyricsViewFragment.setCoverArt(url, null);
    }
}