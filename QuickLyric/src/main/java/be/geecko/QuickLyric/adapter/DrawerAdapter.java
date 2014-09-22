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
        Drawable drawable2 = context.getResources().getDrawable(R.drawable.ic_menu_sdcard);
        Drawable drawable3 = context.getResources().getDrawable(R.drawable.ic_menu_settings);
        this.drawableArray = new Drawable[]{drawable1, drawable2, drawable3};
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null || convertView.getId() != position) {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.drawer_row, null);
            if (convertView != null) {
                TextView textView = (TextView) convertView;
                convertView.setId(position);
                textView.setText(stringArray[position]);
                textView.setCompoundDrawablesWithIntrinsicBounds(drawableArray[position], null, null, null);
            }
        }
        if (convertView != null) {
            TextView textView = (TextView) convertView;
            Typeface roboto = Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Light.ttf");
            Typeface robotoBold = Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Bold.ttf");
            textView.setTextColor(Color.parseColor("#505050"));
            if (position == selectedItem && (textView.getTypeface() != robotoBold)) {
                ((ListView) parent).setSelectionFromTop(position, convertView.getTop());
                textView.setTypeface(robotoBold);
            } else if (position != selectedItem && textView.getTypeface() != roboto)
                textView.setTypeface(roboto);
            return convertView;
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
