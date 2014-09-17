package be.geecko.QuickLyric.lyrics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import static be.geecko.QuickLyric.utils.Net.getUrlAsString;

public class LyricsWiki {
    private static final String baseUrl = "http://lyrics.wikia.com/api.php?action=lyrics&fmt=json&func=getSong&artist=%1s&song=%1s";

    public static Lyrics fromMetaData(String artist, String song) {
        if ((artist == null) || (song == null))
            return new Lyrics(Lyrics.ERROR);
        String encodedArtist = null;
        String encodedSong = null;
        URL url = null;
        try {
            encodedArtist = URLEncoder.encode(artist, "UTF-8");
            encodedSong = URLEncoder.encode(song, "UTF-8");
            url = new URL(new JSONObject(getUrlAsString(new URL(String.format(baseUrl, encodedArtist, encodedSong))).replace("song = ", "")).getString("url"));
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
            artist = url.substring(24).split(":")[0].replace('_', ' ');
        if (song == null)
            song = url.substring(24).split(":")[1].replace('_', ' ');
        String text = stringBuilder.toString();
        if (text.contains("Unfortunately, we are not licensed to display the full lyrics for this song at the moment."))
            return new Lyrics(Lyrics.NEGATIVE_RESULT);
        else if (text.equals("") || text.length() < 3)
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