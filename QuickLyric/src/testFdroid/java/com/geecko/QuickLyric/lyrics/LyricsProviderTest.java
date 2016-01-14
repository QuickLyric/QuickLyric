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

package com.geecko.QuickLyric.lyrics;

import junit.framework.Assert;

import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.ParserConfigurationException;

public class LyricsProviderTest {

    @Test
    public void testAZLyrics() {
        Lyrics lyrics = AZLyrics.fromMetaData("Isyana Sarasvati", "Tetap Dalam Jiwa");
        Assert.assertTrue(lyrics.getText().contains("terbayang"));
        Assert.assertTrue(lyrics.getText().contains("ujungnya"));
    }

    @Test
    public void testGenius() {
        Assert.assertTrue(Genius.search("Eminem").size() > 2);
        Lyrics lyrics = Genius.fromMetaData("Red Hot Chili Peppers", "Can't stop");
        Assert.assertEquals(Lyrics.POSITIVE_RESULT, lyrics.getFlag());
        Assert.assertTrue(lyrics.getText().startsWith("Can't stop"));
        Assert.assertTrue(lyrics.getText().endsWith("more than just a read through"));
    }

    @Test
    public void testLyricsWiki() {
        Assert.assertTrue(LyricWiki.search("Eminem").size() > 2);
        Lyrics lyrics = LyricWiki.fromMetaData("Red Hot Chili Peppers", "Can't stop");
        Assert.assertEquals(Lyrics.POSITIVE_RESULT, lyrics.getFlag());
        Assert.assertTrue(lyrics.getText().startsWith("Can't stop"));
        Assert.assertTrue(lyrics.getText().endsWith("more than just a read through"));
    }

    @Test
    public void testLyricsMania() {
        Lyrics lyrics = LyricsMania.fromMetaData("Red Hot Chili Peppers", "Can't stop");
        Assert.assertEquals(Lyrics.POSITIVE_RESULT, lyrics.getFlag());
        Assert.assertTrue(lyrics.getText().startsWith("Can't stop"));
        Assert.assertTrue(lyrics.getText().contains("life is more than just"));
    }

    @Test
    public void testBollywood() {
        Assert.assertTrue(Bollywood.search("Birthday Bash").size() > 0);
        Lyrics lyrics = Bollywood.fromMetaData("Alfaaz", "Birthday Bash");
        Assert.assertEquals(Lyrics.POSITIVE_RESULT, lyrics.getFlag());
        Assert.assertTrue(lyrics.getText().contains("Manne suna hai"));
        Assert.assertTrue(lyrics.getText().contains("Oh baby oh baby ek baat toh bataa"));
    }

    @Test
    public void testJLyric() {
        Assert.assertTrue(JLyric.search("Shake It Off").size() > 0);
        Lyrics lyrics = JLyric.fromMetaData("Taylor Swift", "Shake It Off");
        Assert.assertEquals(Lyrics.POSITIVE_RESULT, lyrics.getFlag());
        Assert.assertTrue(lyrics.getText().startsWith("I stay out too late"));
        Assert.assertTrue(lyrics.getText().contains("Baby we can shake shake shake.."));
    }

    @Test
    public void testLoloLyrics() {
        Lyrics lyrics = Lololyrics.fromMetaData("Basshunter", "DotA");
        Assert.assertEquals(Lyrics.POSITIVE_RESULT, lyrics.getFlag());
        Assert.assertTrue(lyrics.getText().contains("Det är"));
        Assert.assertTrue(lyrics.getText().contains("I feel you man"));
    }

    @Test
    public void testMetalArchives() {
        Lyrics lyrics = MetalArchives.fromMetaData("Iron Maiden", "Fear of the dark");
        Assert.assertEquals(Lyrics.POSITIVE_RESULT, lyrics.getFlag());
        Assert.assertTrue(lyrics.getText().startsWith("I am a man who walks alone"));
        Assert.assertTrue(lyrics.getText().endsWith("I am a man who walks alone"));
    }

    @Test
    public void testPLyrics() {
        Lyrics lyrics = PLyrics.fromMetaData("Blink 182", "I Miss You");
        Assert.assertEquals(Lyrics.POSITIVE_RESULT, lyrics.getFlag());
        Assert.assertTrue(lyrics.getText().trim().startsWith("(I miss you"));
        Assert.assertTrue(lyrics.getText().contains("The voice inside my head"));
    }

    @Test
    public void testUrbanLyrics() {
        Lyrics lyrics = UrbanLyrics.fromMetaData("Nicki Minaj", "Starships");
        Assert.assertEquals(Lyrics.POSITIVE_RESULT, lyrics.getFlag());
        Assert.assertTrue(lyrics.getText().contains("Let's go get away"));
        Assert.assertTrue(lyrics.getText().contains("Starships were meant to fly"));
        Assert.assertTrue(lyrics.getText().contains("Jump in my hoopty"));
    }

    @Test
    public void testViewLyrics() throws SAXException, NoSuchAlgorithmException, ParserConfigurationException, IOException {
        Lyrics lyrics = ViewLyrics.fromMetaData("Silversun Pickups", "The Royal We");
        Assert.assertEquals(Lyrics.POSITIVE_RESULT, lyrics.getFlag());
        Assert.assertNotNull(lyrics.getText());
        Assert.assertTrue(lyrics.isLRC());
    }
}