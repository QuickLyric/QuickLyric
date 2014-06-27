package be.geecko.QuickLyric.utils;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import com.android.volley.toolbox.ImageLoader;

public class CoverCache extends LruCache<String, Bitmap> implements ImageLoader.ImageCache {

    public CoverCache() {
        this((int) (Runtime.getRuntime().maxMemory() / 1024) / 8);
    }

    public CoverCache(int sizeInKiloBytes) {
        super(sizeInKiloBytes);
    }

    public Bitmap getBitmap(String key) {
        return get(key);
    }

    public void putBitmap(String url, Bitmap bitmap) {
        put(url, bitmap);
    }
}