package be.geecko.QuickLyric.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import be.geecko.QuickLyric.R;

public class DrawerAdapter extends ArrayAdapter<String> {

    private final String[] stringArray;
    private final Drawable[] drawableArray;
    private int selectedItem;

    public DrawerAdapter(Context context, String[] strings) {
        super(context, R.id.drawerlist_row_text, strings);
        this.stringArray = strings;
        Drawable drawable1 = context.getResources().getDrawable(R.drawable.ic_lyrics);
        Drawable drawable2 = context.getResources().getDrawable(R.drawable.ic_menu_mic);
        Drawable drawable3 = context.getResources().getDrawable(R.drawable.ic_menu_sdcard);
        Drawable drawable4 = context.getResources().getDrawable(R.drawable.ic_menu_settings);
        this.drawableArray = new Drawable[]{drawable1, drawable2, drawable3, drawable4};
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textview = (TextView) convertView;
        if (textview == null || textview.getId() != position) {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            textview = (TextView) inflater.inflate(R.layout.drawer_row, null);
            if (textview != null) {
                textview.setId(position);
                textview.setText(stringArray[position]);
                textview.setCompoundDrawablesWithIntrinsicBounds(drawableArray[position], null, null, null);
            }
        }
        if (textview != null) {
            Typeface roboto = Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Light.ttf");
            Typeface robotoBold = Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Bold.ttf");
            textview.setTextColor(Color.parseColor("#505050"));
            if (position == selectedItem && (textview.getTypeface() != robotoBold)) {
                ((ListView) parent).setSelectionFromTop(position, textview.getTop());
                textview.setTypeface(robotoBold);
            } else if (position != selectedItem && textview.getTypeface() != roboto)
                textview.setTypeface(roboto);
            return textview;
        } else
            return null;
    }

    public void setSelectedItem(int position) {
        selectedItem = position;
    }

    public int getSelectedItem() {
        return selectedItem;
    }

}
