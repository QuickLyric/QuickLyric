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

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.utils.ColorUtils;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.view.AnimatedExpandableListView;
import com.geecko.QuickLyric.view.AnimatedExpandableListView.AnimatedExpandableListAdapter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

public class LocalAdapter extends AnimatedExpandableListAdapter {
    private final AnimatedExpandableListView megaListView;
    private final int expandedColor;
    private LayoutInflater inflater;
    private String[] mArtists;
    private final WeakHashMap<String, Lyrics[]> mCache;
    private HashMap<String, Long> mGroupIDs = new HashMap<>();
    private View.OnTouchListener mTouchListener;

    public LocalAdapter(Context context, String[] artists, View.OnTouchListener touchListener,
                        AnimatedExpandableListView listView) {
        this.mArtists = artists;
        mCache = new WeakHashMap<>(4);
        inflater = LayoutInflater.from(context);
        mTouchListener = touchListener;
        megaListView = listView;

        expandedColor = ColorUtils.getAccentColor(context);
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
        holder.artist.setTextColor(isExpanded ? expandedColor : holder.textColor);
        holder.artist.setText(mArtists[groupPosition]);
        holder.artist.setTypeface(null, isExpanded ? Typeface.BOLD : Typeface.NORMAL);
        convertView.setAlpha(1f);
        if (convertView.getTranslationX() == convertView.getWidth())
            convertView.setTranslationX(0f);
        return convertView;
    }

    @Override
    public View getRealChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ChildViewHolder holder;
        if (convertView == null || !(convertView.getTag() instanceof ChildViewHolder)) {
            convertView = inflater.inflate(R.layout.local_child_item, parent, false);
            holder = new ChildViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.child_title);
            convertView.setTag(holder);
        } else
            holder = (ChildViewHolder) convertView.getTag();
        holder.lyrics = getChild(groupPosition, childPosition);
        holder.title.setText(holder.lyrics.getTitle());
        convertView.setOnTouchListener(mTouchListener);
        holder.groupPosition = groupPosition;
        convertView.setAlpha(1f);
        convertView.setTranslationX(0f);
        return convertView;
    }

    @Override
    public int getRealChildrenCount(int groupPosition) {
        return getGroup(groupPosition).length;
    }

    @Override
    public int getGroupCount() {
        return mArtists.length;
    }

    @Override
    public Lyrics[] getGroup(int groupPosition) {
        String artistName = mArtists[groupPosition];
        if (mCache.containsKey(artistName))
            return mCache.get(artistName);
        Lyrics[] results = DatabaseHelper.getInstance(megaListView.getContext()).getLyricsByArtist(artistName);
        mGroupIDs.put(artistName, (long) artistName.hashCode());
        mCache.put(artistName, results);
        return results;
    }

    @Override
    public Lyrics getChild(int groupPosition, int childPosition) {
        return getGroup(groupPosition)[childPosition];
    }

    @Override
    public long getGroupId(int groupPosition) {
        String artist = mArtists[groupPosition];
        if (mGroupIDs.containsKey(artist))
            return mGroupIDs.get(artist);
        else {
            long id = artist.hashCode();
            mGroupIDs.put(artist, id);
            return id;
        }
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return getChild(groupPosition, childPosition).hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public void removeArtistFromCache(String artist) {
        if (mCache.containsKey(artist)) {
            int count = mCache.get(artist).length;
            mCache.remove(artist);
            if (count <= 1) {
                int j = 0;
                String[] newArtists = new String[mArtists.length - 1];
                for (String str : mArtists) {
                    if (artist.equals(str))
                        continue;
                    newArtists[j++] = str;
                }
                mArtists = newArtists;
            }
            notifyDataSetChanged();
        }
    }

    public void addArtist(String artist) {
        mCache.remove(artist);
        if (!Arrays.asList(mArtists).contains(artist)) {
            String[] newArtists = Arrays.copyOf(mArtists, mArtists.length + 1);
            newArtists[newArtists.length - 1] = artist;
            List artistsList = Arrays.asList(newArtists);
            Collections.sort(artistsList, String.CASE_INSENSITIVE_ORDER);
            this.mArtists = (String[]) artistsList.toArray();
        }
        notifyDataSetChanged();
    }

    public View.OnTouchListener getItemOnTouchListener() {
        return mTouchListener;
    }

    public int getGroupPosition(String artist) {
        for (int i = 0; i < mArtists.length; i++) {
            if (mArtists[i].equalsIgnoreCase(artist))
                return i;
        }
        return -1;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
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
