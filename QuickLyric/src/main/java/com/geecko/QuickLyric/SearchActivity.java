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

package com.geecko.QuickLyric;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;

import com.geecko.QuickLyric.lyrics.Genius;
import com.geecko.QuickLyric.lyrics.LyricWiki;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.NightTimeVerifier;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;
import com.geecko.QuickLyric.utils.SearchPagerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {
    public ArrayList<Class> searchProviders = new ArrayList<>();
    public ViewPager viewPager;
    private String searchQuery;

    @SuppressWarnings("unchecked")
    private void updateSearchProviders(Context context) {
        searchProviders.clear();
        searchProviders.add(DatabaseHelper.class);
        searchProviders.add(LyricWiki.class);
        searchProviders.add(Genius.class);

        Set<String> providersSet = PreferenceManager.getDefaultSharedPreferences(context)
                .getStringSet("pref_providers", Collections.<String>emptySet());
        if (providersSet != null)
            for (String name : providersSet)
                try {
                    Class provider = Class.forName("com.geecko.QuickLyric.lyrics." + name);
                    if (provider.getMethod("search", String.class) != null)
                        searchProviders.add(provider);
                } catch (Exception ignored) {
                }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateSearchProviders(this);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int[] themes = new int[]{R.style.Theme_QuickLyric, R.style.Theme_QuickLyric_Dark};
        int themeNum = Integer.valueOf(sharedPref.getString("pref_theme", "0"));
        boolean nightMode = sharedPref.getBoolean("pref_night_mode", false);
        if (nightMode && NightTimeVerifier.check(this))
            setTheme(R.style.Theme_QuickLyric_Night);
        else
            setTheme(themes[themeNum]);
        setStatusBarColor(null);
        setNavBarColor(null);

        setContentView(R.layout.search_view_pager);
        Toolbar toolbar = (Toolbar) findViewById(R.id.search_toolbar);
        setSupportActionBar(toolbar);
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);
        viewPager = (ViewPager) findViewById(R.id.search_pager);
        viewPager.setAdapter(new SearchPagerAdapter(
                this.getFragmentManager(), this, searchQuery));
        boolean online = OnlineAccessVerifier.check(this);
        viewPager.setCurrentItem(online ? 1 : 0);
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

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
        if (viewPager != null)
            ((SearchPagerAdapter) viewPager.getAdapter()).setSearchQuery(searchQuery);
    }

    public void refresh() {
        if (viewPager != null) {
            SearchPagerAdapter searchPagerAdapter = (SearchPagerAdapter) viewPager.getAdapter();
            searchPagerAdapter.setSearchQuery(searchQuery);
            searchPagerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        getMenuInflater().inflate(R.menu.menu_search, menu);
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search_view).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setQuery(searchQuery, false);
        searchView.setMaxWidth(99999); //fixme?
        return true;
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
}