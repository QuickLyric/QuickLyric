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
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.view.AnimatedExpandableListView;
import com.geecko.QuickLyric.view.AnimatedExpandableListView.AnimatedExpandableListAdapter;

import java.util.ArrayList;
import java.util.HashMap;

public class LocalAdapter extends AnimatedExpandableListAdapter {
    private final AnimatedExpandableListView megaListView;
    private ArrayList<ArrayList<Lyrics>> savedLyrics = null;
    private LayoutInflater inflater;
    private HashMap<String, Long> mGroupIDs = new HashMap<>();
    private View.OnTouchListener mTouchListener;

    public LocalAdapter(Context context, ArrayList<ArrayList<Lyrics>> lyrics,
                        View.OnTouchListener touchListener, AnimatedExpandableListView listView) {
        savedLyrics = lyrics;
        inflater = LayoutInflater.from(context);
        mTouchListener = touchListener;
        megaListView = listView;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        GroupViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.group_card, parent, false);
            holder = new GroupViewHolder();
            holder.artist = (TextView) convertView.findViewById(android.R.id.text1);
            holder.textColor = holder.artist.getCurrentTextColor();
            convertView.setTag(holder);
        } else
            holder = (GroupViewHolder) convertView.getTag();
        holder.artist.setTextColor(isExpanded ? parent.getResources().getColor(R.color.accent) : holder.textColor);
        holder.artist.setText(getChild(groupPosition, 0).getArtist());
        holder.artist.setTypeface(null, isExpanded ? Typeface.BOLD : Typeface.NORMAL);
        return convertView;
    }

    @Override
    public View getRealChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ChildViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.local_child_item, parent, false);
            holder = new ChildViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.child_title);
            convertView.setTag(holder);
        } else
            holder = (ChildViewHolder) convertView.getTag();
        holder.title.setText(getChild(groupPosition, childPosition).getTrack());
        convertView.setOnTouchListener(mTouchListener);
        holder.groupPosition = groupPosition;
        holder.lyrics = getChild(groupPosition, childPosition);
        return convertView;
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
    public ArrayList<Lyrics> getGroup(int groupPosition) {
        ArrayList<Lyrics> group = savedLyrics.size() > 0 ? savedLyrics.get(groupPosition) : null;
        if (group != null && group.size() > 0)
            mGroupIDs.put(group.get(0).getArtist(), (long) group.get(0).getArtist().hashCode());
        return group;
    }

    @Override
    public Lyrics getChild(int groupPosition, int childPosition) {
        return getGroup(groupPosition).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        String artist = getChild(groupPosition, 0).getArtist();
        return mGroupIDs.get(artist);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return getChild(groupPosition, childPosition).hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public boolean remove(int groupPosition, View viewToRemove) {
        int childPosition = getGroup(groupPosition).indexOf(((ChildViewHolder) viewToRemove.getTag()).lyrics);
        boolean result = getGroup(groupPosition).remove(childPosition) != null;
        if (result) {
            if (getGroup(groupPosition).size() != 0 || !remove(groupPosition))
                notifyDataSetInvalidated();
        }
        return result;
    }

    public boolean remove(int groupPosition) {
        boolean result = savedLyrics.remove(groupPosition) != null;
        if (result) {
            if (getGroupCount() > groupPosition)
                megaListView.collapseGroup(groupPosition);
            notifyDataSetChanged();
        }
        return result;
    }

    public class ChildViewHolder {
        TextView title;
        public int groupPosition;
        public Lyrics lyrics;
    }

    public class GroupViewHolder {
        TextView artist;
        int textColor;
    }
}