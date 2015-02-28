package com.geecko.QuickLyric.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;

/**
 * This file is part of QuickLyric
 * Created by geecko
 *
 * QuickLyric is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QuickLyric is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with QuickLyric.  If not, see <http://www.gnu.org/licenses/>.
 */
public class CustomSelectionCallback implements ActionMode.Callback {
    MainActivity mActivity;

    public CustomSelectionCallback(Activity activity) {
        this.mActivity = (MainActivity) activity;
    }

    @TargetApi(21)
    private void changeThemeColors(boolean actionMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mActivity.setStatusBarColor(actionMode ? mActivity.getResources()
                            .getColor(R.color.action_dark) : null);
            mActivity.setNavBarColor(actionMode ? mActivity.getResources()
                    .getColor(R.color.action) : null);
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        changeThemeColors(true);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        changeThemeColors(false);
    }
}
