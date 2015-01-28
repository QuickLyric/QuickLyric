package be.geecko.QuickLyric.utils;

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