package be.geecko.QuickLyric.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;

import com.android.volley.toolbox.NetworkImageView;

import be.geecko.QuickLyric.R;

public class FadeInNetworkImageView extends NetworkImageView {
    private static final int FADE_IN_TIME_MS = 250;

    public FadeInNetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        Context context = getContext();
        if (context != null) {
            if (bm == null) {
                Resources resources = context.getResources();
                BitmapDrawable bd = ((BitmapDrawable) resources.getDrawable(R.drawable.default_cover));
                if (bd != null)
                    bm = bd.getBitmap();
            }
            TransitionDrawable td = new TransitionDrawable(new Drawable[]{
                    new ColorDrawable(android.R.color.transparent),
                    new BitmapDrawable(context.getResources(), bm)
            });

            setImageDrawable(td);
            td.startTransition(FADE_IN_TIME_MS);
        }
    }

}