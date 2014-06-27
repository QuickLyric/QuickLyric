package be.geecko.QuickLyric.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Process;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import com.gracenote.mmid.MobileSDK.GNConfig;
import com.gracenote.mmid.MobileSDK.GNOperations;
import com.gracenote.mmid.MobileSDK.GNSearchResponse;
import com.gracenote.mmid.MobileSDK.GNSearchResult;
import com.gracenote.mmid.MobileSDK.GNSearchResultReady;

import java.net.URL;

import be.geecko.QuickLyric.Keys;
import be.geecko.QuickLyric.R;
import be.geecko.QuickLyric.fragment.LyricsViewFragment;
import be.geecko.QuickLyric.lyrics.AZLyrics;
import be.geecko.QuickLyric.lyrics.Lyrics;
import be.geecko.QuickLyric.lyrics.LyricsNMusic;
import be.geecko.QuickLyric.lyrics.LyricsWiki;
import be.geecko.QuickLyric.utils.OnlineAccessVerifier;

public class DownloadTask extends AsyncTask<Object, Object, Lyrics> implements GNSearchResultReady {

    private Context mContext;
    private String givenArtist;
    private String givenTrack;
    private boolean gracenoteCorrection;
    private URL searchURL;

    @Override
    protected Lyrics doInBackground(Object... params) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        Lyrics lyrics;
        mContext = (Context) params[0];
        String artist;
        String track;
        if (params.length > 4) {
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

        if (!OnlineAccessVerifier.check(mContext))
            return new Lyrics(Lyrics.ERROR);

        if (searchURL != null)
            lyrics = LyricsNMusic.direct(artist, track, searchURL);
        else if (gracenoteCorrection && givenArtist.equals(artist) && givenTrack.equals(track))
            lyrics = new Lyrics(Lyrics.NEGATIVE_RESULT);
        else
            lyrics = LyricsWiki.direct(artist, track);

        if (lyrics == null || lyrics.getFlag() == Lyrics.NO_RESULT && gracenoteCorrection || lyrics.getFlag() == Lyrics.NEGATIVE_RESULT || lyrics.getFlag() == Lyrics.ERROR)
            lyrics = LyricsNMusic.direct(artist, track);

        if (lyrics == null || lyrics.getFlag() == Lyrics.NO_RESULT && gracenoteCorrection || lyrics.getFlag() == Lyrics.NEGATIVE_RESULT || lyrics.getFlag() == Lyrics.ERROR)
            lyrics = AZLyrics.direct(artist, track);

        return lyrics;
    }

    protected void onPostExecute(Lyrics lyrics) {
        if (lyrics.getFlag() != Lyrics.POSITIVE_RESULT && !gracenoteCorrection) { //slow mode
            GNOperations.searchByText(this, GNConfig.init(Keys.gracenote, mContext.getApplicationContext()), givenArtist, null, givenTrack);
            return;
        }

        FragmentManager fm = ((ActionBarActivity) mContext).getSupportFragmentManager();
        LyricsViewFragment lyricsViewFragment = (LyricsViewFragment) fm.findFragmentByTag("LyricsViewFragment");

        if (!OnlineAccessVerifier.check(mContext))
            Toast.makeText(mContext, mContext.getString(R.string.connection_error), Toast.LENGTH_LONG).show();
        if (lyrics.getArtist() == null)
            lyrics.setArtist(givenArtist);
        if (lyrics.getTrack() == null)
            lyrics.setTitle(givenTrack);

        if (!isCancelled())
            lyricsViewFragment.update(lyrics);
    }

    @Override
    public void GNResultReady(GNSearchResult result) {
        if (!result.isFailure() && !isCancelled()) {
            GNSearchResponse response = result.getBestResponse();
            this.gracenoteCorrection = true;
            new DownloadTask().execute(mContext, response.getArtist(), response.getTrackTitle(), true, givenArtist, givenTrack);
        }
    }
}