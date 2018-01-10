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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;

import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.utils.PermissionsChecker;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagOptionSingleton;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import static com.geecko.QuickLyric.model.Lyrics.POSITIVE_RESULT;

public class Id3Reader {

    public static final int REQUEST_CODE = 2;

    public static Bitmap getCover(Context context, String artist, String title, boolean requestPermission) {
        try {
            Tag tag = null;
            for (File file : getFiles(context, artist, title, requestPermission)) {
                AudioFile af = AudioFileIO.read(file);
                TagOptionSingleton.getInstance().setAndroid(true);
                tag = af.getTag();
                if (!tag.getArtworkList().isEmpty())
                    break;
            }
            if (tag.getFirstArtwork() == null)
                return null;
            byte[] byteArray = tag.getFirstArtwork().getBinaryData();
            ByteArrayInputStream imageStream = new ByteArrayInputStream(byteArray);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(imageStream, null, options);

            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            options.inJustDecodeBounds = false;
            options.inSampleSize = options.outWidth / size.x;

            return BitmapFactory.decodeStream(imageStream, null, options);
        } catch (Exception e) {
            return null;
        }
    }

    public static Lyrics getLyrics(Context context, String artist, String title, boolean requestPermission) {
        String text = null;
        try {
            for (File file : getFiles(context, artist, title, requestPermission)) {
                AudioFile af = AudioFileIO.read(file);
                TagOptionSingleton.getInstance().setAndroid(true);
                Tag tag = af.getTag();
                text = tag.getFirst(FieldKey.LYRICS);
                if (!text.isEmpty()) {
                    text = text.replaceAll("\n", "<br/>");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        if (TextUtils.isEmpty(text))
            return null;
        Lyrics lyrics = new Lyrics(POSITIVE_RESULT);
        lyrics.setArtist(artist);
        lyrics.setTitle(title);
        lyrics.setText(text);
        lyrics.setSource("Storage");
        return lyrics;
    }

    public static File getFile(Context context, String artist, String title, boolean requestPermission) {
        File[] files = getFiles(context, artist,title, requestPermission);
        return files == null || files.length == 0 ? null : files[0];
    }

    private static File[] getFiles(Context context, String artist, String title, boolean requestPermission) {
        if (!PermissionsChecker.hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (context instanceof Activity) {
                if (requestPermission) {
                    PermissionsChecker.requestPermission((Activity) context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            R.string.storage_permission_rationale,
                            Id3Reader.REQUEST_CODE);
                }
            }
            return new File[0];
        }

        if (TextUtils.isEmpty(artist) || artist.toLowerCase().contains("unknown artist") || artist.equalsIgnoreCase("Unknown"))
            artist = "<unknown>";
        if (TextUtils.isEmpty(title))
            title = "<unknown>";

        final Uri uri = Uri.parse("content://media/external/audio/media");
        final String[] columns = new String[]{"artist", "title", "_data"};
        String extensionlessTitle = title;
        if (title.contains("."))
            extensionlessTitle = title.substring(0, title.lastIndexOf("."));

        File[] output = new File[0];
        String selection = "title='" + title.replaceAll("'", "''") + "%' OR title='%" + extensionlessTitle.replaceAll("'", "''") + "%'";
        Cursor cursor = context.getContentResolver().query(uri, columns, selection, null, null);
        if (cursor == null)
            return output;

        if (cursor.getCount() > 1) {
            output = getFileArrayWithAdditionalFiltering(context, title, artist, selection);
        } else if (cursor.getCount() == 1) {
            output = getFileArray(context, cursor, selection, artist, title);
        } else {
            cursor.close();
            selection = "title LIKE '%" + title.replaceAll("'", "''") + "%' OR title LIKE '%" + extensionlessTitle.replaceAll("'", "''") + "%'";
            cursor = context.getContentResolver().query(uri, columns, selection, null, null);
            if (cursor.getCount() == 0) {
                SharedPreferences preferences = context.getSharedPreferences("current_music", Context.MODE_PRIVATE);
                output = new File[0];
                String player = preferences.getString("player", "");
                if (!TextUtils.isEmpty(player) && !onlinePlayers.contains(player)) {
                    Exception e = new IllegalStateException(String.format("No file found for: %s - %s (%s) | %s", artist, title, player, selection));
                }
            } else if (cursor.getCount() > 1)
                output = getFileArrayWithAdditionalFiltering(context, title, artist, selection);
            else
                output = getFileArray(context, cursor, selection, artist, title);
        }

        if (!cursor.isClosed())
            cursor.close();
        return output;
    }

    private static File[] getFileArray(Context context, Cursor cursor, String selection, String artist, String title) {
        File[] output = new File[cursor.getCount()];
        cursor.moveToFirst();
        if (output.length == 0) {
            SharedPreferences preferences = context.getSharedPreferences("current_music", Context.MODE_PRIVATE);
            String player = preferences.getString("player", "");
            if (!TextUtils.isEmpty(player) && !onlinePlayers.contains(player)) {
                Exception e = new IllegalStateException(String.format("No file found for: %s - %s (%s) | %s", artist, title, player, selection));
            }
        } else {
            do {
                String path = cursor.getString(2);
                output[cursor.getPosition()] = path == null ? null : new File(path);
            } while (cursor.moveToNext());
        }
        return output;
    }

    private static File[] getFileArrayWithAdditionalFiltering(Context context, String title, String artist, String selection) {
        Uri uri = Uri.parse("content://media/external/audio/media");
        String[] columns = new String[]{"artist", "title", "_data"};
        selection = "(" + selection + ") AND artist LIKE '%" + artist.replaceAll("'", "''") + "%'";
        Cursor cursor = context.getContentResolver().query(uri, columns, selection, null, null);
        File[] output = getFileArray(context, cursor, selection, artist, title);
        cursor.close();
        return output;
    }

    private static List onlinePlayers = Arrays.asList("com.spotify.music", "com.google.android.youtube",
            "com.soundcloud.android", "com.apple.android.music", "com.google.android.music", "com.bsbportal.music",
            "deezer.android.app", "com.amazon.mp3", "com.saavn.android");
}
