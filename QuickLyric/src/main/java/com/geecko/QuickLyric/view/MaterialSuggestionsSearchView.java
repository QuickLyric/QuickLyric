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

package com.geecko.QuickLyric.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

import com.geecko.QuickLyric.utils.LyricsSearchSuggestionsProvider;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

public class MaterialSuggestionsSearchView extends MaterialSearchView {

    private LyricsSearchSuggestionsProvider suggestionsProvider;

    public MaterialSuggestionsSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MaterialSuggestionsSearchView(Context context) {
        super(context);
        init();
    }

    @SuppressWarnings("deprecation")
    private void init() {
        this.suggestionsProvider = new LyricsSearchSuggestionsProvider(getContext());
        setSubmitOnClick(true);
        LyricsSearchSuggestionsProvider.setDatabase(this.suggestionsProvider.getWritableDatabase());
       /*
        Resources.Theme theme = getContext().getTheme();
        TypedValue textColor = new TypedValue();
        TypedValue hintColor = new TypedValue();
        TypedValue searchBarColor = new TypedValue();
        theme.resolveAttribute(android.R.attr.textColorPrimary, textColor, true);
        theme.resolveAttribute(android.R.attr.textColorSecondaryInverse, hintColor, true);
        theme.resolveAttribute(android.R.attr.colorBackground, searchBarColor, true);
        setTextColor(textColor.data);
        setHintTextColor(hintColor.data);
        setBackgroundColor(searchBarColor.data);
        ((ImageView)findViewById(com.miguelcatalan.materialsearchview.R.id.action_up_btn))
                .setColorFilter(hintColor.data, PorterDuff.Mode.SRC_IN); */
    }

    public boolean hasSuggestions() {
        return ((ListView)findViewById(com.miguelcatalan.materialsearchview.R.id.suggestion_list))
                .getAdapter() != null;
    }

    public String[] getHistory() {
        return this.suggestionsProvider.getHistory();
        // todo close db
    }
}
