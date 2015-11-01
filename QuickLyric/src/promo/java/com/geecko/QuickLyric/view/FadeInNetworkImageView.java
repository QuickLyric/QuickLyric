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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

public class FadeInNetworkImageView extends NetworkImageView {
    private static final int FADE_IN_TIME_MS = 500;

    private Bitmap  mLocalBitmap;
    private boolean mShowLocal;

    public FadeInNetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setLocalImageBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            mShowLocal = true;
        }
        this.mLocalBitmap = bitmap;
        requestLayout();
    }

    @Override
    public void setImageUrl(String url, ImageLoader imageLoader) {
        mShowLocal = false;
        super.setImageUrl(url, imageLoader);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        Context context = getContext();
        if (context != null) {
            Resources resources = context.getResources();
            int overlayColor = 0xFFF;
            if (bm != null) {
                Palette coverPalette = Palette.generate(bm);
//              int muted = coverPalette.getLightMutedColor(resources.getColor(R.color.selected));
                Palette.Swatch swatch = coverPalette.getVibrantSwatch();
                if (swatch == null || swatch.getPopulation() < 200) {
                    swatch = coverPalette.getLightMutedSwatch();
                    if (swatch == null || swatch.getPopulation() < 200)
                        swatch = coverPalette.getMutedSwatch();
                    if (swatch == null)
                        return;
                }
                // DST = Disk, SRC = Artwork
                overlayColor = swatch.getRgb();
            }
            TransitionDrawable td = new TransitionDrawable(new Drawable[]{
                    new ColorDrawable(overlayColor),
                    new ColorDrawable(overlayColor)
            });

            setImageDrawable(td);
            td.startTransition(FADE_IN_TIME_MS);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        super.onLayout(changed, left, top, right, bottom);
        if (mShowLocal) {
            setImageBitmap(mLocalBitmap);
        }
    }
}