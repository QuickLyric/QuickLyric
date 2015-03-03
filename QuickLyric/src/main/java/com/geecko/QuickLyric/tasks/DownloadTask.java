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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Process;
import android.widget.Toast;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.lyrics.AZLyrics;
import com.geecko.QuickLyric.lyrics.Genius;
import com.geecko.QuickLyric.lyrics.JLyric;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.lyrics.LyricsWiki;
import com.geecko.QuickLyric.utils.LastFMCorrection;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;

public class DownloadTask extends AsyncTask<Object, Object, Lyrics> {

    private Context mContext;
    private String givenArtist;
    private String givenTrack;
    private boolean correction;
    public boolean interruptible = true;

    @Override
    protected Lyrics doInBackground(Object... params) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        Lyrics lyrics = null;
        mContext = (Context) params[0];
        String artist = null;
        String track = null;
        String searchURL = null;
        String url = null;

        if (params.length == 2)
            url = (String) params[1];
        else if (params.length > 4) {
            correction = (Boolean) params[3];
            artist = (String) params[1];
            track = (String) params[2];
            givenArtist = (String) params[4];
            givenTrack = (String) params[5];
        } else {
            givenArtist = artist = (String) params[1];
            givenTrack = track = (String) params[2];
            if (params.length > 3)
                searchURL = (String) params[3];
        }

        if (url != null) {
            if (url.contains("http://www.azlyrics.com/"))
                lyrics = AZLyrics.fromURL(url, null, null);
            else if (url.contains("lyrics.wikia.com/"))
                lyrics = LyricsWiki.fromURL(url, null, null);
            else if (url.contains("genius.com"))
                lyrics = Genius.fromURL(url, null, null);
            else if (url.contains("j-lyric.net"))
                lyrics = JLyric.fromURL(url, null, null);
        } else {
            if (!OnlineAccessVerifier.check(mContext))
                return new Lyrics(Lyrics.ERROR);
            if (searchURL != null)
                lyrics = Genius.fromURL(searchURL, artist, track);
            else {
                if (correction) {
                    String[] corrections = LastFMCorrection.getCorrection(artist, track);
                    if (corrections[0] != null)
                        artist = corrections[0];
                    if (corrections[1] != null)
                        track = corrections[1];
                    if (!givenArtist.equals(artist) || !givenTrack.equals(track))
                        lyrics = LyricsWiki.fromMetaData(artist, track);
                } else
                    lyrics = LyricsWiki.fromMetaData(artist, track);
            }

            if (lyrics == null || lyrics.getFlag() == Lyrics.NO_RESULT && correction ||
                    lyrics.getFlag() == Lyrics.NEGATIVE_RESULT || lyrics.getFlag() == Lyrics.ERROR)
                lyrics = Genius.fromMetaData(artist, track);

            if (lyrics == null || lyrics.getFlag() == Lyrics.NO_RESULT && correction ||
                    lyrics.getFlag() == Lyrics.NEGATIVE_RESULT || lyrics.getFlag() == Lyrics.ERROR)
                lyrics = AZLyrics.fromMetaData(artist, track);

            if (lyrics == null || lyrics.getFlag() == Lyrics.NO_RESULT && correction ||
                    lyrics.getFlag() == Lyrics.NEGATIVE_RESULT || lyrics.getFlag() == Lyrics.ERROR)
                lyrics = JLyric.fromMetaData(artist, track);
        }
        if (givenArtist != null && givenTrack != null) {
            lyrics.setOriginalArtist(givenArtist);
            lyrics.setOriginalTitle(givenTrack);
        }
        return lyrics;
    }

    protected void onPostExecute(Lyrics lyrics) {
        if (lyrics.getFlag() != Lyrics.POSITIVE_RESULT && !correction) {
            String correctedArtist = givenArtist.replaceAll("\\(.*\\)", "")
                    .replaceAll(" \\- .*", "").trim();
            String correctedTrack = givenTrack.replaceAll("\\(.*\\)", "")
                    .replaceAll("\\[.*\\]", "").replaceAll(" \\- .*", "").trim();
            new DownloadTask().execute(mContext, correctedArtist, correctedTrack, true,
                    givenArtist, givenTrack);
            return;
        }

        if (!OnlineAccessVerifier.check(mContext))
            Toast.makeText(mContext, mContext.getString(R.string.connection_error), Toast.LENGTH_LONG).show();
        if (lyrics.getArtist() == null)
            lyrics.setArtist(givenArtist);
        if (lyrics.getTrack() == null)
            lyrics.setTitle(givenTrack);

        if (!isCancelled())
            ((MainActivity) mContext).updateLyricsFragment(0, 0, false, lyrics);
    }
}