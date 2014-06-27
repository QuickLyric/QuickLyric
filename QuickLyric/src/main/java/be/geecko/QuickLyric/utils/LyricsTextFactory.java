package be.geecko.QuickLyric.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import be.geecko.QuickLyric.R;

public class LyricsTextFactory implements ViewSwitcher.ViewFactory {

    private final Context mContext;

    public LyricsTextFactory(Context context) {
        this.mContext = context;
    }

    @Override
    public View makeView() {
        TextView t = new TextView(mContext);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        Configuration c = mContext.getResources().getConfiguration();
        if (c.fontScale > 1.0)
            t.setTypeface(Typeface.createFromAsset(mContext.getAssets(), "fonts/opendyslexic.otf"));
        else
            t.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        t.setTextColor(Color.parseColor("#222222"));
        t.setLineSpacing(8, 1);
        setSelectable(t);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, mContext.getResources().getDimension(R.dimen.txt_size));
        return t;
    }

    @SuppressLint("newAPI")
    public void setSelectable(TextView t){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            t.setTextIsSelectable(true);
    }

}
