package be.geecko.QuickLyric.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import be.geecko.QuickLyric.R;

public class SearchAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] songs;
    private final String[] artists;

    public SearchAdapter(Context context, String[] songs, String[] artists)
    {
        super(context, R.layout.list_row, songs);
        this.context = context;
        this.artists = artists;
        this.songs = songs;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_row, parent, false);
        if (rowView==null)
            return null;
        TextView title = (TextView) rowView.findViewById(R.id.title);
        title.setText(songs[position]);
        TextView artist = (TextView) rowView.findViewById(R.id.artist);
        artist.setText(artists[position]);

        return rowView;
    }

}
