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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.geecko.QuickLyric.lyrics.Lyrics;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagOptionSingleton;

import java.io.ByteArrayInputStream;
import java.io.File;

import static com.geecko.QuickLyric.lyrics.Lyrics.POSITIVE_RESULT;

public class Id3Reader {

    public static Bitmap getCover(Context context, String artist, String title) {
        try {
            AudioFile af = AudioFileIO.read(getFile(context, artist, title));
            TagOptionSingleton.getInstance().setAndroid(true);
            Tag tag = af.getTag();
            byte[] byteArray = tag.getFirstArtwork().getBinaryData();
            ByteArrayInputStream imageStream = new ByteArrayInputStream(byteArray);
            return BitmapFactory.decodeStream(imageStream);
        } catch (Exception e) {
            return null;
        }
    }

    public static Lyrics getLyrics(Context context, String artist, String title) {
        String text;
        try {
            AudioFile af = AudioFileIO.read(getFile(context, artist, title));
            TagOptionSingleton.getInstance().setAndroid(true);
            Tag tag = af.getTag();
            text = tag.getFirst(FieldKey.LYRICS);
            if (text.isEmpty())
                throw new NoSuchFieldException();
            text = text.replaceAll("\n", "<br/>");
        } catch (Exception e) {
            return null;
        }
        Lyrics lyrics = new Lyrics(POSITIVE_RESULT);
        lyrics.setArtist(artist);
        lyrics.setTitle(title);
        lyrics.setText(text);
        lyrics.setSource("Storage");
        return lyrics;
    }

    public static File getFile(Context context, String artist, String title) {
        Uri uri = Uri.parse("content://media/external/audio/media");
        String[] columns = new String[]{"artist", "title", "_data"};
        String[] args = new String[]{artist, title};

        Cursor cursor = context.getContentResolver().query(uri, columns, "artist=? AND title=?", args, null);
        if (cursor == null || cursor.getCount() == 0) {
            if (cursor != null)
                cursor.close();
            return null;
        }
        cursor.moveToFirst(); // TODO handle more than one file

        String path = cursor.getString(2);
        cursor.close();
        return new File(path);
    }
}
