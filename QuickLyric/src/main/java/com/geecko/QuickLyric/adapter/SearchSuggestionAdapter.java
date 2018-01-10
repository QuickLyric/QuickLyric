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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.utils.LyricsSearchSuggestionsProvider;
import com.geecko.QuickLyric.view.MaterialSuggestionsSearchView;

import java.lang.reflect.Field;


public class SearchSuggestionAdapter extends com.miguelcatalan.materialsearchview.SearchAdapter {

    private final Context mContext;

    public SearchSuggestionAdapter(Context context, String[] suggestions, Drawable suggestionIcon, boolean ellipsize) {
        super(context, suggestions, suggestionIcon, ellipsize);
        this.mContext = context;
    }

    public SearchSuggestionAdapter(Context context, String[] suggestions) {
        super(context, suggestions);
        this.mContext = context;
    }


    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final RelativeLayout suggestionRow = (RelativeLayout) super.getView(position, convertView, parent);
        TypedValue suggestionTextColor = new TypedValue();
        suggestionRow.getContext().getTheme()
                .resolveAttribute(android.R.attr.textColorSecondaryInverse, suggestionTextColor, true);
        ((TextView)suggestionRow.getChildAt(0)).setTextColor(suggestionTextColor.data);

        if (suggestionRow.getChildCount() < 3) {
            final MaterialSuggestionsSearchView searchView = parent.getRootView().findViewById(R.id.material_search_view);
            ImageButton removeButton = new ImageButton(suggestionRow.getContext());
            removeButton.setImageDrawable(searchView.getCloseIcon());
            TypedValue background = new TypedValue();
            suggestionRow.getContext().getTheme()
                    .resolveAttribute(R.attr.selectableItemBackground, background, true);
            removeButton.setBackgroundResource(background.resourceId);
            removeButton.setClickable(true);
            removeButton.setOnClickListener(v -> {
                removeSuggestion(((TextView) suggestionRow
                        .findViewById(com.miguelcatalan.materialsearchview.R.id.suggestion_text)).getText()
                        .toString()
                );
                searchView.refreshSuggestions();
            });

            RelativeLayout.LayoutParams lp =
                    new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                lp.addRule(RelativeLayout.ALIGN_PARENT_END, removeButton.getId());
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, removeButton.getId());
            suggestionRow.addView(removeButton, 2, lp);
            int padding = suggestionRow.getResources().getDimensionPixelSize(R.dimen.search_icon_padding);
            removeButton.setPadding(padding, 0, padding, 0);
        }
        suggestionRow.setOnClickListener(v -> {
            MaterialSuggestionsSearchView searchView = parent.getRootView().findViewById(R.id.material_search_view);
            searchView.setQuery((String) getItem(position), true);
        });
        return suggestionRow;
    }

    private void removeSuggestion(String suggestion) {
        LyricsSearchSuggestionsProvider.getInstance(mContext).deleteQuery(suggestion);
        try {
            Field suggestions = com.miguelcatalan.materialsearchview.SearchAdapter.class
                    .getDeclaredField("suggestions");
            suggestions.setAccessible(true);
            LyricsSearchSuggestionsProvider.getInstance(mContext).getHistory();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
