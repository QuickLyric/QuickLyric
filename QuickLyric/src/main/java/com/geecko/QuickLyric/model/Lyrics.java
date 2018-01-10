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

package com.geecko.QuickLyric.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Lyrics implements Serializable, Parcelable {

    private String mTitle;
    private String mArtist;
    private String mOriginalTitle;
    private String mOriginalArtist;
    private String mSourceUrl;
    private String mCoverURL;
    private String mCopyright;
    private String mWriter;
    private String mText;
    private String mSource;
    private String audiofingerprint;
    private boolean mLRC = false;
    private boolean mReported = false;
    private final int mFlag;
    private boolean mAcoustIDUsed;
    private int mErrCode;
    public static final int NO_RESULT = -2;
    public static final int NEGATIVE_RESULT = -1;
    public static final int POSITIVE_RESULT = 1;
    public static final int ERROR = -3;
    public static final int SEARCH_ITEM = 2;

    public interface Callback {
        void onLyricsDownloaded(Lyrics lyrics);
    }

    public Lyrics(int flag) {
        this.mFlag = flag;
    }

    protected Lyrics(Parcel in) {
        mTitle = in.readString();
        mArtist = in.readString();
        mOriginalTitle = in.readString();
        mOriginalArtist = in.readString();
        mSourceUrl = in.readString();
        mCoverURL = in.readString();
        mCopyright = in.readString();
        mWriter = in.readString();
        mText = in.readString();
        mSource = in.readString();
        mLRC = in.readByte() != 0;
        mReported = in.readByte() != 0;
        mAcoustIDUsed = in.readByte() != 0;
        mErrCode = in.readInt();
        mFlag = in.readInt();
    }

    public static final Creator<Lyrics> CREATOR = new Creator<Lyrics>() {
        @Override
        public Lyrics createFromParcel(Parcel in) {
            return new Lyrics(in);
        }

        @Override
        public Lyrics[] newArray(int size) {
            return new Lyrics[size];
        }
    };

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getOriginalTitle() {
        if (mOriginalTitle != null)
            return mOriginalTitle;
        else
            return mTitle;
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
        if (mOriginalArtist != null)
            return mOriginalArtist;
        else
            return mArtist;
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

    public String getCopyright() {
        return mCopyright;
    }

    public void setCopyright(String copyright) {
        if ("\"\"".equals(copyright))
            copyright = null;
        this.mCopyright = copyright;
    }

    public String getWriter() {
        return this.mWriter;
    }

    public void setWriter(String writer) {
        if ("\"\"".equals(writer))
            writer = null;
        this.mWriter = writer;
    }

    public String getText() {
        return mText;
    }

    public void setText(String lyrics) {
        this.mText = lyrics;
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

    public void setLRC(boolean LRC) {
        this.mLRC = LRC;
    }

    public boolean isLRC() {
        return this.mLRC;
    }

    public boolean isReported() {
        return mReported;
    }

    public void setReported(boolean reported) {
        this.mReported = reported;
    }

    public int getErrorCode() {
        return mErrCode;
    }

    public void setErrorCode(int mErrCode) {
        this.mErrCode = mErrCode;
    }

    public void setAudiofingerprint(String audiofingerprint) {
        this.audiofingerprint = audiofingerprint;
    }

    public String getAudiofingerprint() {
        return audiofingerprint;
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

    public boolean wasAcoustIDUsed() {
        return mAcoustIDUsed;
    }

    public void setAcoustIDUsed(boolean acoustIDUsed) {
        this.mAcoustIDUsed = acoustIDUsed;
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
        boolean isLyrics = object instanceof Lyrics;
        if (isLyrics && (this.getURL() != null) && ((Lyrics) object).getURL() != null)
            return this.getURL().equals(((Lyrics) object).getURL());
        else if (isLyrics) {
            Lyrics other = (Lyrics) object;
            boolean result = this.getText().equals(other.getText());
            result &= this.getFlag() == other.getFlag();
            result &= this.getSource().equals(other.getSource());
            result &= this.getArtist().equals(other.getArtist());
            result &= this.getTitle().equals(other.getTitle());
            return result;
        }
        else
            return false;
    }

    @Override
    public int hashCode() {
        // Potential issue with the Birthday Paradox when we hash over 50k lyrics
        return this.getURL() != null ? this.getURL().hashCode() :
                (""+this.getOriginalArtist()+this.getOriginalTitle()+this.getSource()).hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTitle);
        dest.writeString(mArtist);
        dest.writeString(mOriginalTitle);
        dest.writeString(mOriginalArtist);
        dest.writeString(mSourceUrl);
        dest.writeString(mCoverURL);
        dest.writeString(mCopyright);
        dest.writeString(mWriter);
        dest.writeString(mText);
        dest.writeString(mSource);
        dest.writeByte((byte) (mLRC ? 1 : 0));
        dest.writeByte((byte) (mReported ? 1 : 0));
        dest.writeByte((byte) (mAcoustIDUsed ? 1 : 0));
        dest.writeInt(mErrCode);
        dest.writeInt(mFlag);
    }
}
