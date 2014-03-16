package be.geecko.QuickLyric.lyrics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class LyricsWiki {
    private static final String baseUrl = "http://lyrics.wikia.com/api.php?action=lyrics&fmt=json&func=getSong&artist=%1s&song=%1s";

    public static Lyrics direct(String artist, String song) {
        if ((artist == null) || (song == null))
            return new Lyrics(Lyrics.ERROR);
        int j;
        StringBuilder stringBuilder = new StringBuilder();
        URL url;
        try {
            String str1 = URLEncoder.encode(artist, "UTF-8");
            String str2 = URLEncoder.encode(song, "UTF-8");
            url = new URL(new JSONObject(Lyrics.getUrlAsString(new URL(String.format(baseUrl, str1, str2))).replace("song = ", "")).getString("url"));
            if (url.toExternalForm().endsWith("action=edit"))
                return new Lyrics(Lyrics.NO_RESULT);
            try {
                String html = Lyrics.getUrlAsString(url);
                String preceding = html.substring(html.indexOf("<div class='lyricbox'>"));
                String following = preceding.substring(6 + preceding.indexOf("</div>"));
                String[] encrypted = following.substring(0, following.indexOf("<!--")).replace("<br />", "\n;").replaceAll("<.*?>", "").split(";");
                int i = encrypted.length;
                j = 0;
                while (j < i) {
                    String s = encrypted[j];
                    if (s.equals("\n"))
                        stringBuilder.append("<br />");
                    else
                        try {
                            stringBuilder.append((char) Integer.valueOf(s.replaceAll("&#", "")).intValue());
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            return new Lyrics(Lyrics.NEGATIVE_RESULT);
                        }

                    j++;
                }
            }
            catch (StringIndexOutOfBoundsException e)
            {
                return new Lyrics(Lyrics.ERROR);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return new Lyrics(Lyrics.ERROR);
        } catch (IOException e) {
            e.printStackTrace();
            return new Lyrics(Lyrics.ERROR);
        } catch (JSONException e) {
            e.printStackTrace();
            return new Lyrics(Lyrics.ERROR);
        }
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
            lyrics.setURL(url.toExternalForm());
            return lyrics;
        }
    }

}