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

package com.geecko.QuickLyric.tasks;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.TextSwitcher;
import android.widget.Toast;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.fragment.LyricsViewFragment;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.utils.PermissionsChecker;
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

public class Id3Writer extends AsyncTask<Object, Object, Boolean> {

    public static final int REQUEST_CODE = 1;
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
    protected Boolean doInBackground(Object... params) {
        Lyrics editedLyrics = (Lyrics) params[0];
        File musicFile = (File) params[1];
        boolean failed = false;

        if (musicFile != null)
            try {
                AudioFile af = AudioFileIO.read(musicFile);
                TagOptionSingleton.getInstance().setAndroid(true);
                Tag tags = af.getTag();
                tags.setField(FieldKey.ARTIST, editedLyrics.getArtist());
                tags.setField(FieldKey.TITLE, editedLyrics.getTitle());
                tags.setField(FieldKey.LYRICS, Html.fromHtml(editedLyrics.getText()).toString());
                af.setTag(tags);
                AudioFileIO.write(af);
            } catch (CannotReadException | IOException | ReadOnlyFileException | TagException
                    | InvalidAudioFrameException | NullPointerException | CannotWriteException e) {
                e.printStackTrace();
                failed = true;
            }
            
        return failed;
    }

    @Override
    protected void onPostExecute(Boolean failed) {
        if (failed) {
            Toast.makeText(mContext, R.string.id3_write_error, Toast.LENGTH_LONG).show();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && PermissionsChecker.hasPermission((Activity) mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                Toast.makeText(mContext, R.string.id3_write_permission_error, Toast.LENGTH_LONG).show();
        } else
            Toast.makeText(mContext, R.string.id3_write_success, Toast.LENGTH_LONG).show();
    }
}
