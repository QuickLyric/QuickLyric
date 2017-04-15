package com.geecko.QuickLyric.model;

/**
 * Created by steve on 4/7/17.
 */

public class Track {
    public final String mTitle;
    public final String mArtist;

    public Track(String title, String artist)
    {
        mTitle = title;
        mArtist = artist;
    }

    @Override
    public boolean equals(Object other)
    {
        Track oth = (Track) other;
        return
                this.mTitle.equals(oth.mTitle) && this.mArtist.equals(oth.mArtist);
    }
}
