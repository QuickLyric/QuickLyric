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
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.view.AnimatedExpandableListView.AnimatedExpandableListAdapter;

import java.util.ArrayList;

public class LocalAdapter extends AnimatedExpandableListAdapter {
    private final Context mContext;
    private ArrayList<ArrayList<Lyrics>> savedLyrics = null;
    static private boolean[] checkedItems;
    static private int checkedItemCount = 0;

    public LocalAdapter(Context context, ArrayList<ArrayList<Lyrics>> lyrics) {
        mContext = context;
        savedLyrics = lyrics;
        if (checkedItems == null || checkedItems.length != lyrics.size())
            checkedItems = new boolean[lyrics.size()];
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

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.group_card, parent, false);
            if (convertView == null)
                return null;
        }
        AppCompatTextView artist = (AppCompatTextView) convertView.findViewById(android.R.id.text1);
        artist.setTextColor(isExpanded ? parent.getResources().getColor(R.color.accent) : Color.BLACK);
        artist.setText(savedLyrics.get(groupPosition).get(0).getArtist());
        artist.setTypeface(null, isExpanded ? Typeface.BOLD : Typeface.NORMAL);
        ((CardView) convertView).setCardElevation(8f);
        if (Build.VERSION.SDK_INT >= 21)
            convertView.setElevation(8f);
        return convertView;
    }

    @Override
    public View getRealChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.local_child_item, parent, false);
            if (convertView == null)
                return null;
        }
        TextView title = (TextView) convertView.findViewById(R.id.child_title);
        title.setText(savedLyrics.get(groupPosition).get(childPosition).getTrack());
        ((CardView) convertView).setCardBackgroundColor(parent.getResources().getColor(R.color.expanded));
        return convertView;
    }

    public int getCount() {
        return savedLyrics.size();
    }

    @Override
    public int getRealChildrenCount(int groupPosition) {
        return savedLyrics.get(groupPosition).size();
    }

    @Override
    public int getGroupCount() {
        return savedLyrics.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return savedLyrics.get(groupPosition);
    }

    @Override
    public Lyrics getChild(int groupPosition, int childPosition) {
        return savedLyrics.get(groupPosition).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return savedLyrics.get(groupPosition).hashCode();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return savedLyrics.get(groupPosition).get(childPosition).hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}