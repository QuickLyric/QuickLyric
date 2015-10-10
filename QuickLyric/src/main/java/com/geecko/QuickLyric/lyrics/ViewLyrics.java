/*
 * *
 *  * This file is part of QuickLyric
 *  * Created by PedroHLC, modified by geecko
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

import android.text.TextUtils;

import com.geecko.QuickLyric.utils.Net;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static com.geecko.QuickLyric.lyrics.Lyrics.NEGATIVE_RESULT;
import static com.geecko.QuickLyric.lyrics.Lyrics.POSITIVE_RESULT;
import static com.geecko.QuickLyric.lyrics.Lyrics.SEARCH_ITEM;

public class ViewLyrics {

    /*
     * Needed data
     */
    private static final String url = "http://search.crintsoft.com/searchlyrics.htm";
    //ACTUAL: http://search.crintsoft.com/searchlyrics.htm
    //CLASSIC: http://www.viewlyrics.com:1212/searchlyrics.htm

    private static final String clientUserAgent = "MiniLyrics4Android";
    //NORMAL: MiniLyrics <version> for <player>
    //EXAMPLE: MiniLyrics 7.6.44 for Windows Media Player
    //MOBILE: MiniLyrics4Android

    private static final String clientTag = "client=\"ViewLyricsOpenSearcher\"";
    //NORMAL: MiniLyrics
    //MOBILE: MiniLyricsForAndroid

    private static final String searchQueryBase = "<?xml version='1.0' encoding='utf-8' ?><searchV1 artist=\"%s\" title=\"%s\" OnlyMatched=\"1\" %s/>";

    private static final String searchQueryPage = " RequestPage='%d'";

    private static final byte[] magickey = "Mlv1clt4.0".getBytes();

	/*
     * Search function
	 */

    public static Lyrics fromMetaData(String artist, String title) throws IOException, NoSuchAlgorithmException, SAXException, ParserConfigurationException {
        ArrayList<Lyrics> results =
                searchQuery(
                        String.format(searchQueryBase, artist, title, clientTag +
                                String.format(searchQueryPage, 0)) // Create XMLQuery String
                );
        if (results.size() == 0)
            return new Lyrics(NEGATIVE_RESULT);
        String url = results.get(0).getURL();

        if (url.endsWith("txt") || (!results.get(0).getTrack().equalsIgnoreCase(title) &&
                !results.get(0).getArtist().equalsIgnoreCase(artist)))
            return new Lyrics(NEGATIVE_RESULT);
        Lyrics result = new Lyrics(POSITIVE_RESULT);
        result.setTitle(title);
        result.setArtist(artist);
        result.setLRC(url.endsWith("lrc"));
        result.setText(Net.getUrlAsString(url));
        result.setSource(clientUserAgent);

        return result;
    }

    @SuppressWarnings("resource")
    private static ArrayList<Lyrics> searchQuery(String searchQuery) throws ClientProtocolException, IOException, NoSuchAlgorithmException, SAXException, ParserConfigurationException {
        // Create Client
        DefaultHttpClient client = new DefaultHttpClient();
        HttpPost request = new HttpPost(url);

        // Define HEADER
        request.setHeader("User-Agent", clientUserAgent);
        client.getParams().setBooleanParameter("http.protocol.expect-continue", true);

        // Define POST Entity as a magic encoded version of XMLQuery
        request.setEntity(new ByteArrayEntity(assembleQuery(searchQuery.getBytes("UTF-8"))));


        // Send Request
        HttpResponse response = client.execute(request);

        // Get the response
        BufferedReader rd = new BufferedReader
                (new InputStreamReader(response.getEntity().getContent(), "ISO_8859_1"));

        // Get full result
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[8192];
        int read;
        while ((read = rd.read(buffer, 0, buffer.length)) > 0) {
            builder.append(buffer, 0, read);
        }
        String full = builder.toString();

        // Decrypt, parse, store, and return the result list
        return parseResultXML(decryptResultXML(full));
    }

	/*
     * Add MD5 and Encrypts Search Query
	 */

    public static byte[] assembleQuery(byte[] value) throws NoSuchAlgorithmException, IOException {
        // Create the variable POG to be used in a dirt code
        byte[] pog = new byte[value.length + magickey.length]; //TODO Give a better name then POG

        // POG = XMLQuery + Magic Key
        System.arraycopy(value, 0, pog, 0, value.length);
        System.arraycopy(magickey, 0, pog, value.length, magickey.length);

        // POG is hashed using MD5
        byte[] pog_md5 = MessageDigest.getInstance("MD5").digest(pog);

        //TODO Thing about using encryption or k as 0...
        // Prepare encryption key
        int j = 0;
        for (int i = 0; i < value.length; i++) {
            j += value[i];
        }
        int k = (byte) (j / value.length);

        // Value is encrypted
        for (int m = 0; m < value.length; m++)
            value[m] = (byte) (k ^ value[m]);

        // Prepare result code
        ByteArrayOutputStream result = new ByteArrayOutputStream();

        // Write Header
        result.write(0x02);
        result.write(k);
        result.write(0x04);
        result.write(0x00);
        result.write(0x00);
        result.write(0x00);

        // Write Generated MD5 of POG problaby to be used in a search cache
        result.write(pog_md5);

        // Write encrypted value
        result.write(value);

        // Return magic encoded query
        return result.toByteArray();
    }

	/*
	 * Decrypts only the XML from the entire result
	 */

    public static String decryptResultXML(String value) {
        // Get Magic key value
        char magickey = value.charAt(1);

        // Prepare output
        ByteArrayOutputStream neomagic = new ByteArrayOutputStream();

        // Decrypts only the XML
        for (int i = 22; i < value.length(); i++)
            neomagic.write((byte) (value.charAt(i) ^ magickey));

        // Return value
        return neomagic.toString();
    }

	/*
	 * Create the ArrayList<LyricInfo>
	 */

    private static String readStrFromAttr(Element elem, String attr, String def) {
        String data = elem.getAttribute(attr);
        try {
            if (!TextUtils.isEmpty(data))
                return data;
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return def;
    }

    public static ArrayList<Lyrics> parseResultXML(String resultXML) throws SAXException, IOException, ParserConfigurationException {
        // Create array for storing the results
        ArrayList<Lyrics> availableLyrics = new ArrayList<Lyrics>();

        // Parse XML
        ByteArrayInputStream resultBA = new ByteArrayInputStream(resultXML.getBytes("UTF-8"));
        Element resultRootElem = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resultBA).getDocumentElement();

        String server_url = readStrFromAttr(resultRootElem, "server_url", "http://www.viewlyrics.com/");

        NodeList resultItemList = resultRootElem.getElementsByTagName("fileinfo");
        for (int i = 0; i < resultItemList.getLength(); i++) {
            Element itemElem = (Element) resultItemList.item(i);
            Lyrics item = new Lyrics(SEARCH_ITEM);

            item.setURL(server_url + readStrFromAttr(itemElem, "link", ""));
            item.setArtist(readStrFromAttr(itemElem, "artist", ""));
            item.setTitle(readStrFromAttr(itemElem, "title", ""));
            //item.setLyricsFileName(readStrFromAttr(itemElem, "filename", ""));
            //itemInfo.setFType(readIntFromAttr(itemElem, "file_type", 0));
            //itemInfo.setMatchVal(readFloatFromAttr(itemElem, "match_value", 0.0F));
            //itemInfo.setTimeLenght(readIntFromAttr(itemElem, "timelength", 0));


            availableLyrics.add(item);
        }

        return availableLyrics;
    }


}
