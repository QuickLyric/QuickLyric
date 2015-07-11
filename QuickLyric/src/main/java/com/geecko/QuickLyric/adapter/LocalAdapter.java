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

package com.geecko.QuickLyric.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.view.CheckableLayout;

import java.util.ArrayList;

public class LocalAdapter extends ArrayAdapter<Lyrics> {
    private final Context mContext;
    private ArrayList<Lyrics> savedLyrics = new ArrayList<>();
    static private boolean[] checkedItems;
    static private int checkedItemCount = 0;

    public LocalAdapter(Context context, int resource, ArrayList<Lyrics> lyrics) {
        super(context, resource, lyrics);
        mContext = context;
        savedLyrics = lyrics;
        if (checkedItems == null || checkedItems.length != lyrics.size())
            checkedItems = new boolean[lyrics.size()];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_row, parent, false);
            if (convertView == null)
                return null;
        }
        TextView title = (TextView) convertView.findViewById(R.id.row_title);
        TextView artist = (TextView) convertView.findViewById(R.id.row_artist);
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