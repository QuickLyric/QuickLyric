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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Lyrics implements Serializable {

    private String mTitle;
    private String mArtist;
    private String mOriginalTitle;
    private String mOriginalArtist;
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

    public String getOriginalTrack() {
        return mOriginalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.mOriginalTitle = originalTitle;
    }

    public String getArtist() {
        return mArtist;
    }

    public void setArtist(String artist) {
        this.mArtist = artist;
    }

    public String getOriginalArtist() {
        return mOriginalArtist;
    }

    public void setOriginalArtist(String originalArtist) {
        this.mOriginalArtist = originalArtist;
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

    @Override
    public boolean equals(Object object) {
        return object instanceof Lyrics && this.getURL().equals(((Lyrics) object).getURL());
    }
}
