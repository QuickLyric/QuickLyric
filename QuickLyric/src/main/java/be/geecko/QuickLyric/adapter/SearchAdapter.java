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

    public SearchAdapter(Context context, String[] songs, String[] artists) {
        super(context, R.layout.list_row, songs);
        this.context = context;
        this.artists = artists;
        this.songs = songs;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_row, parent, false);
            viewHolder.title = (TextView) convertView.findViewById(R.id.title);
            viewHolder.artist = (TextView) convertView.findViewById(R.id.artist);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.title.setText(songs[position]);
        viewHolder.artist.setText(artists[position]);

        return convertView;
    }

    static class ViewHolder {
        private TextView title;
        private TextView artist;
    }

}
