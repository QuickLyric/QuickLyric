package be.geecko.QuickLyric.lyrics;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;

public class Lyrics implements Serializable {

    private String mTitle;
    private String mArtist;
    private String mSourceUrl;
    private String mCoverURL;
    private String mLyrics;
    private String mSource;
    private final int mFlag;
    public static final int NO_RESULT = -2;
    public static final int NEGATIVE_RESULT = -1;
    public static final int POSITIVE_RESULT = 1;
    public static final int ERROR = -3;
    public static final int SEARCH_ITEM = 2;

    public Lyrics(int flag) {
        this.mFlag = flag;
    }

    public String getTrack() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getArtist() {
        return mArtist;
    }

    public void setArtist(String artist) {
        this.mArtist = artist;
    }

    public String getURL() {
        return mSourceUrl;
    }

    public void setURL(String uRL) {
        this.mSourceUrl = uRL;
    }

    public String getCoverURL() {
        return mCoverURL;
    }

    public void setCoverURL(String coverURL) {
        this.mCoverURL = coverURL;
    }

    public String getText() {
        return mLyrics;
    }

    public void setText(String lyrics) {
        this.mLyrics = lyrics;
    }

    public String getSource() {
        return mSource;
    }

    public int getFlag() {
        return mFlag;
    }

    public void setSource(String mSource) {
        this.mSource = mSource;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.close();
        } finally {
            bos.close();
        }
        return bos.toByteArray();
    }

    public static Lyrics fromBytes(byte[] data) throws IOException, ClassNotFoundException {
        if (data == null)
            return null;
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return (Lyrics) is.readObject();
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

    @Override
    public boolean equals(Object object) {
        return object instanceof Lyrics && this.getURL().equals(((Lyrics) object).getURL());
    }
}
