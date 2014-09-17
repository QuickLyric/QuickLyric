package be.geecko.QuickLyric.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Process;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import java.net.URL;

import be.geecko.QuickLyric.MainActivity;
import be.geecko.QuickLyric.R;
import be.geecko.QuickLyric.fragment.LyricsViewFragment;
import be.geecko.QuickLyric.lyrics.AZLyrics;
import be.geecko.QuickLyric.lyrics.Lyrics;
import be.geecko.QuickLyric.lyrics.LyricsNMusic;
import be.geecko.QuickLyric.lyrics.LyricsWiki;
import be.geecko.QuickLyric.utils.OnlineAccessVerifier;

public class DownloadTask extends AsyncTask<Object, Object, Lyrics> {

    private Context mContext;
    private String givenArtist;
    private String givenTrack;
    private boolean gracenoteCorrection;

    @Override
    protected Lyrics doInBackground(Object... params) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        Lyrics lyrics;
        mContext = (Context) params[0];
        String artist = null;
        String track = null;
        URL searchURL = null;
        String url = null;

        if (params.length == 2)
            url = (String) params[1];
        else if (params.length > 4) {
            gracenoteCorrection = (Boolean) params[3];
            artist = (String) params[1];
            track = (String) params[2];
            givenArtist = (String) params[4];
            givenTrack = (String) params[5];
        } else {
            artist = (String) params[1];
            track = (String) params[2];
            givenArtist = artist;
            givenTrack = track;
            if (params.length > 3)
                searchURL = (URL) params[3];
        }

        if (url != null) {
            if (url.contains("http://www.azlyrics.com/"))
                lyrics = AZLyrics.fromURL(url, null, null);
            else if (url.contains("lyrics.wikia.com/"))
                lyrics = LyricsWiki.fromURL(url, null, null);
            else
                lyrics = LyricsNMusic.fromURL(url, null, null);
        } else {
            if (!OnlineAccessVerifier.check(mContext))
                return new Lyrics(Lyrics.ERROR);
            if (searchURL != null)
                lyrics = LyricsNMusic.fromURL(searchURL.toExternalForm(), artist, track);
            else if (gracenoteCorrection && givenArtist.equals(artist) && givenTrack.equals(track))
                lyrics = new Lyrics(Lyrics.NEGATIVE_RESULT);
            else
                lyrics = LyricsWiki.fromMetaData(artist, track);

            if (lyrics == null || lyrics.getFlag() == Lyrics.NO_RESULT && gracenoteCorrection || lyrics.getFlag() == Lyrics.NEGATIVE_RESULT || lyrics.getFlag() == Lyrics.ERROR)
                lyrics = LyricsNMusic.fromMetaData(artist, track);

            if (lyrics == null || lyrics.getFlag() == Lyrics.NO_RESULT && gracenoteCorrection || lyrics.getFlag() == Lyrics.NEGATIVE_RESULT || lyrics.getFlag() == Lyrics.ERROR)
                lyrics = AZLyrics.fromMetaData(artist, track);
        }
        return lyrics;
    }

    protected void onPostExecute(Lyrics lyrics) {
/*        if (lyrics.getFlag() != Lyrics.POSITIVE_RESULT && !gracenoteCorrection) { // fixme: slow mode
            GNOperations.searchByText(this, GNConfig.init(Keys.gracenote, mContext.getApplicationContext()), givenArtist, null, givenTrack);
            return;
        }
*/

        FragmentManager fm = ((ActionBarActivity) mContext).getSupportFragmentManager();
        LyricsViewFragment lyricsViewFragment = (LyricsViewFragment) fm.findFragmentByTag("LyricsViewFragment");

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