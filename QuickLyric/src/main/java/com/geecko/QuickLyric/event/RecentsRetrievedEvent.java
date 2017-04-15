package com.geecko.QuickLyric.event;

import com.geecko.QuickLyric.model.Lyrics;

/**
 * Created by steve on 4/9/17.
 */

public class RecentsRetrievedEvent {
    public final Lyrics lyrics;

    public RecentsRetrievedEvent(Lyrics lyrics)
    {
        this.lyrics = lyrics;
    }
}
