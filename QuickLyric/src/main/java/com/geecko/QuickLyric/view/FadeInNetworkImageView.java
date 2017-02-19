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

package com.geecko.QuickLyric.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.AttributeSet;

import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.tasks.CoverArtLoader;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;

import java.io.File;

public class FadeInNetworkImageView extends NetworkImageView {
    private static final int FADE_IN_TIME_MS = 500;
    private final DiskBasedCache mDiskCache;

    private Bitmap mLocalBitmap;
    private boolean mShowLocal;
    private Lyrics mLyrics;
    private ConnectivityManager.NetworkCallback networkCallback;

    public FadeInNetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        File cacheDir = new File(getContext().getCacheDir(), "volley");
        mDiskCache = new DiskBasedCache(cacheDir);
        new Thread(new Runnable() {
            @Override
            public void run() {
                mDiskCache.initialize();
            }
        }).start();
    }

    public void setLocalImageBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            mShowLocal = true;
        }
        this.mLocalBitmap = bitmap;
        requestLayout();
    }

    public void setLyrics(Lyrics lyrics) {
        this.mLyrics = lyrics;
    }

    @TargetApi(21)
    @Override
    public void setImageUrl(String url, ImageLoader imageLoader) {
        mShowLocal = false;
        if (mDiskCache.get(url) == null && !OnlineAccessVerifier.check(getContext())) {
            this.setImageBitmap(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                final ConnectivityManager cm = (ConnectivityManager)
                        getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                            new CoverArtLoader().execute(mLyrics, FadeInNetworkImageView.this.getActivity());
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && networkCallback != null)
                                try {
                                    cm.unregisterNetworkCallback(networkCallback);
                                } catch (IllegalArgumentException ignored) {
                                }
                    }
                };
                NetworkRequest request = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
                cm.registerNetworkCallback(request, networkCallback);
            }
        } else {
            super.setImageUrl(url, imageLoader);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setImageBitmap(Bitmap bm) {
        Context context = getContext();
        if (context != null) {
            Resources resources = context.getResources();
            TransitionDrawable td = new TransitionDrawable(new Drawable[]{
                    new ColorDrawable(resources.getColor(android.R.color.transparent)),
                    new BitmapDrawable(context.getResources(), bm)
            });
            setImageDrawable(td);
            td.startTransition(FADE_IN_TIME_MS);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed)
            super.onLayout(true, left, top, right, bottom);
        Bitmap bitmap = null;
        if (getDrawable() != null)
            bitmap = ((BitmapDrawable) ((TransitionDrawable) getDrawable()).getDrawable(1)).getBitmap();
        if (mShowLocal && (bitmap == null || !(bitmap).equals(mLocalBitmap)))
            setImageBitmap(mLocalBitmap);
    }

    private Activity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getDrawable() != null) {
            Matrix matrix = getImageMatrix();

            float scaleFactorWidth = getWidth() / (float) getDrawable().getIntrinsicWidth();
            float scaleFactorHeight = getHeight() / (float) getDrawable().getIntrinsicHeight();

            float scaleFactor = (scaleFactorWidth > scaleFactorHeight) ? scaleFactorWidth : scaleFactorHeight;

            matrix.setScale(scaleFactor, scaleFactor, 0, getDrawable().getIntrinsicHeight() * 0.12f);
            setImageMatrix(matrix);
        }
        super.onDraw(canvas);
    }
}
