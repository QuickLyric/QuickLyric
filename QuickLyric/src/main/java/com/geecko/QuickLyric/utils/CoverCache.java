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

package com.geecko.QuickLyric.utils;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.toolbox.ImageLoader;

public class CoverCache implements ImageLoader.ImageCache {

    private static LruCache<String, Bitmap> mMemoryCache;

    private static CoverCache coverCache;

    private CoverCache(){
        // Get the Max available memory
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key, Bitmap bitmap){
                return bitmap.getRowBytes() * bitmap.getHeight();
            }
        };
    }

    public static CoverCache instance(){
        if(coverCache == null){
            coverCache = new CoverCache();
        }
        return coverCache;
    }

    @Override
    public Bitmap getBitmap(String key) {
        return mMemoryCache.get(key);
    }

    @Override
    public void putBitmap(String key, Bitmap arg1) {
        if(getBitmap(key) == null){
            mMemoryCache.put(key, arg1);
        }
    }

}