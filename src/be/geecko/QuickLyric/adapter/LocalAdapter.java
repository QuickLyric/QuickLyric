package be.geecko.QuickLyric.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import be.geecko.QuickLyric.R;
import be.geecko.QuickLyric.lyrics.Lyrics;
import be.geecko.QuickLyric.view.CheckableLayout;

public class LocalAdapter extends ArrayAdapter<Lyrics> {
    private final Context mcontext;
    private ArrayList<Lyrics> savedLyrics = new ArrayList<Lyrics>();
    static private boolean[] checkedItems;
    static private int checkedItemCount = 0;

    public LocalAdapter(Context context, int resource, ArrayList<Lyrics> lyrics) {
        super(context, resource, lyrics);
        mcontext = context;
        savedLyrics = lyrics;
        if (checkedItems == null || checkedItems.length != lyrics.size())
            checkedItems = new boolean[lyrics.size()];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mcontext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_row, parent, false);
            if (convertView == null)
                return null;
        }
        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView artist = (TextView) convertView.findViewById(R.id.artist);
        title.setText(savedLyrics.get(position).getTrack());
        artist.setText(savedLyrics.get(position).getArtist());
        CheckableLayout row = (CheckableLayout) convertView;
        if (position < checkedItems.length && checkedItems[position] != row.isChecked())
            row.toggle();
        return convertView;
    }

    void setItemChecked(int position, boolean checked) {
        if (checked && !checkedItems[position])
            checkedItemCount++;
        else if (!checked && checkedItems[position])
            checkedItemCount--;
        if (checkedItems[position] != checked) {
            checkedItems[position] = checked;
            notifyDataSetChanged();
        }
    }

    public void checkAll(boolean checked) {
        for (int i = 0; i < savedLyrics.size(); i++)
            checkedItems[i] = checked;
        checkedItemCount = checked ? savedLyrics.size() : 0;
        notifyDataSetChanged();
    }

    public void toggle(int position) {
        setItemChecked(position, !checkedItems[position]);
    }

    public boolean isItemChecked(int position) {
        return checkedItems[position];
    }

    public int getCheckedItemCount() {
        return checkedItemCount;
    }
}