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
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.widget.DrawerLayout;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextSwitcher;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.fragment.LyricsViewFragment;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.view.RefreshIcon;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagOptionSingleton;

import java.io.File;
import java.io.IOException;

public class Id3Writer extends AsyncTask<Object, Object, Object> {

    private final Context mContext;
    private final LyricsViewFragment fragment;

    public Id3Writer(LyricsViewFragment lyricsViewFragment) {
        this.fragment = lyricsViewFragment;
        this.mContext = lyricsViewFragment.getActivity();
    }

    @Override
    public void onPreExecute() {
        MainActivity activity = (MainActivity) mContext;

        ((DrawerLayout) activity.drawer).setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        fragment.enablePullToRefresh(true);
        activity.findViewById(R.id.refresh_fab).setEnabled(true);
        ((RefreshIcon) activity.findViewById(R.id.refresh_fab)).show();
        activity.invalidateOptionsMenu();

        TextSwitcher textSwitcher = ((TextSwitcher) activity.findViewById(R.id.switcher));
        EditText newLyrics = (EditText) activity.findViewById(R.id.edit_lyrics);

        textSwitcher.setCurrentText(newLyrics.getText().toString());
        textSwitcher.setVisibility(View.VISIBLE);
        newLyrics.setVisibility(View.GONE);
    }

    @Override
    protected Object doInBackground(Object... params) {
        Lyrics editedLyrics = (Lyrics) params[0];
        File musicFile = (File) params[1];

        try {
            AudioFile af = AudioFileIO.read(musicFile);
            TagOptionSingleton.getInstance().setAndroid(true);
            Tag tags = af.getTag();
            tags.setField(FieldKey.ARTIST, editedLyrics.getArtist());
            tags.setField(FieldKey.TITLE, editedLyrics.getTrack());
            tags.setField(FieldKey.LYRICS, editedLyrics.getText());
            af.setTag(tags);
            AudioFileIO.write(af);
        } catch (CannotReadException | IOException | ReadOnlyFileException
                | TagException | InvalidAudioFrameException | NullPointerException e) {
            e.printStackTrace();
        } catch (CannotWriteException e) {
            e.printStackTrace(); // TODO: check Android 4.4 Kitkat
        }

        return null;
    }
}
