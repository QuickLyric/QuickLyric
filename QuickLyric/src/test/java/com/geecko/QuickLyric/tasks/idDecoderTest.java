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

import com.geecko.QuickLyric.model.Lyrics;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class idDecoderTest {

    IdDecoder idDecoder;

    @Before
    public void setUp() {
        idDecoder = new IdDecoder(null, null);
    }

    @Test
    public void testSoundHound() {
        Lyrics result = idDecoder.doInBackground("http://www.soundhound.com/?t=100712370075285435");
        assertNotNull(result);
        assertEquals("Tycho", result.getArtist());
        assertEquals("Melanine", result.getTitle());
    }

    @Test
    public void testShazam() {
        Lyrics result = idDecoder.doInBackground("http://shz.am/t54008130");
        assertNotNull(result);
        assertEquals(result.getArtist(), "Tycho");
        assertEquals(result.getTitle(), "A Walk");
    }

    @Test
    public void testGPlay() {
        Lyrics result = idDecoder.doInBackground("https://play.google.com/store/music/album?id=Bnl7gvtywqiwmfao4rmd5pecfbu&tid=song-Trdszdvkk3arz3dkuv4flo37uly");
        assertNotNull(result);
        assertEquals(result.getArtist(), "Tycho");
        assertEquals(result.getTitle(), "A Walk");
    }

}
