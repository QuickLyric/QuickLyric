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

package com.geecko.QuickLyric;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;

import com.geecko.QuickLyric.adapter.SearchPagerAdapter;
import com.geecko.QuickLyric.provider.LyricsChart;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.LyricsSearchSuggestionsProvider;
import com.geecko.QuickLyric.utils.NightTimeVerifier;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;
import com.geecko.QuickLyric.view.MaterialSuggestionsSearchView;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity {
    public ArrayList<Class> searchProviders = new ArrayList<>();
    private String searchQuery;
    public boolean leaving;

    @SuppressWarnings("unchecked")
    private void updateSearchProviders() {
        searchProviders.clear();
        searchProviders.add(DatabaseHelper.class);
        searchProviders.add(LyricsChart.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateSearchProviders();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int[] themes = new int[]{R.style.Theme_QuickLyric, R.style.Theme_QuickLyric_Red,
                R.style.Theme_QuickLyric_Purple, R.style.Theme_QuickLyric_Indigo,
                R.style.Theme_QuickLyric_Green, R.style.Theme_QuickLyric_Lime,
                R.style.Theme_QuickLyric_Brown, R.style.Theme_QuickLyric_Dark};
        int themeNum = Integer.valueOf(sharedPref.getString("pref_theme", "0"));
        boolean nightMode = sharedPref.getBoolean("pref_night_mode", false);
        if (nightMode && NightTimeVerifier.check(this))
            setTheme(R.style.Theme_QuickLyric_Night);
        else
            setTheme(themes[themeNum]);
        setStatusBarColor(null);
        setNavBarColor(null);

        setContentView(R.layout.search_view_pager);
        Toolbar toolbar = findViewById(R.id.search_toolbar);
        setSupportActionBar(toolbar);
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription taskDescription =
                    new ActivityManager.TaskDescription
                            (null, null, toolbar.getSolidColor());
            this.setTaskDescription(taskDescription);
        }

        ViewPager viewPager = getViewPager();
        viewPager.setAdapter(new SearchPagerAdapter(
                this.getFragmentManager(), this, searchQuery));
        boolean online = OnlineAccessVerifier.check(this);
        viewPager.setCurrentItem(online ? 1 : 0);
        PagerTitleStrip titleIndicator = findViewById(R.id.pager_title_strip);
        titleIndicator.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        setSearchQuery(getIntent().getStringExtra("query"));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        if (action != null)
            switch (action) {
                case "android.intent.action.SEARCH":
                    this.searchQuery = intent.getStringExtra(SearchManager.QUERY);
                    this.refresh();
                    break;
            }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        leaving = true;
        overridePendingTransition(android.R.anim.fade_in, R.anim.slide_out_end);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (leaving)
            leaving = false;
        else
            overridePendingTransition(android.R.anim.fade_in, R.animator.fade_out);
    }

    @Override
    public void finish() {
        leaving = true;
        super.finish();
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
        ViewPager viewPager = getViewPager();
        if (viewPager != null)
            ((SearchPagerAdapter) viewPager.getAdapter()).setSearchQuery(searchQuery);
        if (searchQuery != null)
            LyricsSearchSuggestionsProvider.getInstance(getApplicationContext()).saveQuery(searchQuery);
    }

    public void refresh() {
        ViewPager viewPager = getViewPager();
        if (viewPager != null) {
            SearchPagerAdapter searchPagerAdapter = (SearchPagerAdapter) viewPager.getAdapter();
            searchPagerAdapter.setSearchQuery(searchQuery);
            searchPagerAdapter.notifyDataSetChanged();
        }
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        getMenuInflater().inflate(R.menu.menu_search, menu);
        // Get the SearchView and set the searchable configuration
        final MaterialSuggestionsSearchView materialSearchView =
                findViewById(R.id.material_search_view);
        materialSearchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {
                materialSearchView.setSuggestions(null);
                materialSearchView.requestFocus();
                materialSearchView.post(() -> ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(materialSearchView.getWindowToken(), 0));
                materialSearchView.postDelayed(() -> {
                    SearchActivity.this.searchQuery = query;
                    refresh();
                }, 90);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });

        materialSearchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
            }

            @Override
            public void onSearchViewClosed() {
                onBackPressed();
            }
        });
        materialSearchView.setMenuItem(menu.findItem(R.id.search_view));
        materialSearchView.setHint(getResources().getString(R.string.search_hint));
        materialSearchView.showSearch();
        materialSearchView.setQuery(this.searchQuery, false);
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    public ActionBar getSupportActionBar() {
        return super.getSupportActionBar();
    }

    @TargetApi(21)
    public void setStatusBarColor(Integer color) {
        if (Build.VERSION.SDK_INT >= 20) {
            if (color == null) {
                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = getTheme();
                theme.resolveAttribute(android.R.attr.colorPrimaryDark, typedValue, true);
                color = typedValue.data;
            }
            getWindow().setStatusBarColor(color);
        }
    }

    @TargetApi(21)
    public void setNavBarColor(Integer color) {
        if (Build.VERSION.SDK_INT >= 20) {
            if (color == null) {
                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = getTheme();
                theme.resolveAttribute(android.R.attr.navigationBarColor, typedValue, true);
                color = typedValue.data;
            }
            getWindow().setNavigationBarColor(color);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    public ViewPager getViewPager() {
        return (ViewPager) findViewById(R.id.search_pager);
    }
}