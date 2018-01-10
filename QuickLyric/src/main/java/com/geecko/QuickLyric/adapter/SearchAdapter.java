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

package com.geecko.QuickLyric.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.geecko.QuickLyric.R;

import java.lang.ref.WeakReference;

public class SearchAdapter extends ArrayAdapter<String> {
    private final String[] songs;
    private final String[] artists;
    private final WeakReference<Activity> activityReference;

    public SearchAdapter(Context context, String[] songs, String[] artists, WeakReference<Activity> activityWeakReference) {
        super(context, R.layout.list_row, songs);
        this.artists = artists;
        this.songs = songs;
        this.activityReference = activityWeakReference;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) activityReference.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_row, parent, false);
            viewHolder.title = convertView.findViewById(R.id.row_title);
            viewHolder.artist = convertView.findViewById(R.id.row_artist);
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
