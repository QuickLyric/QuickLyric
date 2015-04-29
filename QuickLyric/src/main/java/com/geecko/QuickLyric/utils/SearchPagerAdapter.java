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

package com.geecko.QuickLyric.utils;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;

import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.fragment.SearchFragment;
import com.geecko.QuickLyric.SearchActivity;

public class SearchPagerAdapter extends FragmentStatePagerAdapter implements ViewPager.OnPageChangeListener {

    private SearchFragment[] fragments = new SearchFragment[5];
    private String searchQuery;
    private SearchActivity searchTabs;
    public int selectedPage = 0;

    public SearchPagerAdapter(FragmentManager fm, SearchActivity searchActivity, String query) {
        super(fm);
        this.searchQuery = query;
        this.searchTabs = searchActivity;
    }

    @Override
    public Fragment getItem(int position) {
        if (fragments[position] != null && fragments[position].getSearchQuery().equals(this.searchQuery)) {
            if (fragments[position].results.size() == 0)
                fragments[position].setResults(fragments[position].results);
            else
                return fragments[position];
        }
        SearchFragment searchFragment = new SearchFragment();
        searchFragment.setSearchProvider(position);
        searchFragment.setSearchQuery(searchQuery);
        searchFragment.setSearchTabs(searchTabs);
        searchFragment.position = position;
        return searchFragment;
    }

    @Override
    public int getItemPosition(Object fragment) {
        if (((SearchFragment) fragment).getSearchQuery().equals(this.searchQuery))
            return POSITION_UNCHANGED;
        else
            return POSITION_NONE;
    }

    public void registerFragment(int position, SearchFragment fragment) {
        fragments[position] = fragment;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        this.selectedPage = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public CharSequence getPageTitle(int position) {
        try {
            Class provider = searchTabs.searchProviders.get(position);
            if (provider == DatabaseHelper.class)
                return searchTabs.getString(R.string.local_title);
            return provider.getSimpleName();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public int getCount() {
        return searchTabs.searchProviders.size();
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }
}