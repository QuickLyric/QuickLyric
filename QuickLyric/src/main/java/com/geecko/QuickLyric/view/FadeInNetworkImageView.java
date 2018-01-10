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
import android.text.TextUtils;
import android.util.AttributeSet;

import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.tasks.CoverArtLoader;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;

import java.io.File;

public class FadeInNetworkImageView extends NetworkImageView {
    private static final int FADE_IN_TIME_MS = 500;
    private final DiskBasedCache mDiskCache;

    private Bitmap mLocalBitmap;
    private boolean mShowLocal;
    private Lyrics mLyrics;
    private int defaultImageCounter = 0;
    private int[] defaultImageResources = new int[] {R.drawable.empty_cover_0, R.drawable.empty_cover_1, R.drawable.empty_cover_2, R.drawable.empty_cover_3};
    private boolean defaultShown = false;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean mFirstStart;

    public FadeInNetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        File cacheDir = new File(getContext().getCacheDir(), "volley");
        mDiskCache = new DiskBasedCache(cacheDir);
        new Thread(mDiskCache::initialize).start();
    }

    public void setLocalImageBitmap(Bitmap bitmap) {
        mShowLocal = true;
        this.mLocalBitmap = bitmap;
        requestLayout();
    }

    public void setLyrics(Lyrics lyrics) {
        this.mLyrics = lyrics;
    }

    @TargetApi(21)
    @Override
    public void setImageUrl(String url, ImageLoader imageLoader) {
        mShowLocal = TextUtils.isEmpty(url);
        if (mDiskCache.get(url) == null && !OnlineAccessVerifier.check(getContext())) {
            this.setImageBitmap(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                final ConnectivityManager cm = (ConnectivityManager)
                        getContext().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        new CoverArtLoader((MainActivity) FadeInNetworkImageView.this.getActivity()).execute(mLyrics);
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
            defaultShown = true;
        } else {
            super.setImageUrl(url, imageLoader);
            if (!TextUtils.isEmpty(url))
                defaultShown = false;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setImageBitmap(Bitmap bm) {
        Context context = getContext();
        if (context != null) {
            if (bm == null) {
                if (defaultShown && defaultImageCounter > 0)
                    defaultImageCounter--;
                bm = ((BitmapDrawable) getResources().getDrawable(this.defaultImageResources[defaultImageCounter++ % defaultImageResources.length])).getBitmap();
                defaultShown = true;
            } else
                defaultShown = false;
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
        Bitmap bitmap = null;
        if (getDrawable() != null)
            bitmap = ((BitmapDrawable) ((TransitionDrawable) getDrawable()).getDrawable(1)).getBitmap();
        if (mShowLocal && mLocalBitmap != null && (bitmap == null || !(bitmap).equals(mLocalBitmap)))
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
            if (Build.VERSION.SDK_INT >= 19) {
                Matrix matrix = getImageMatrix();

                float scaleFactorWidth = getWidth() / (float) getDrawable().getIntrinsicWidth();
                float scaleFactorHeight = getHeight() / (float) getDrawable().getIntrinsicHeight();

                float scaleFactor = (scaleFactorWidth > scaleFactorHeight) ? scaleFactorWidth : scaleFactorHeight;

                matrix.setScale(scaleFactor, scaleFactor, 0, mFirstStart || defaultShown ? 0 : getDrawable().getIntrinsicHeight() * 0.62f);
                setImageMatrix(matrix);
            } else {
                setScaleType(ScaleType.CENTER_CROP);
            }
        }
        super.onDraw(canvas);
    }

    public void setFirstStart(boolean firstStart) {
        this.mFirstStart = firstStart;
    }
}
