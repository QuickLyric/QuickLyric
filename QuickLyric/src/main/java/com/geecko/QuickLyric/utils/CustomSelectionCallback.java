package com.geecko.QuickLyric.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;

/**
 * This file is part of QuickLyric
 * Copyright © 2017 QuickLyric SPRL
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mActivity.setStatusBarColor(actionMode ? mActivity.getResources()
                            .getColor(R.color.action_dark) : null);
            mActivity.setNavBarColor(actionMode ? mActivity.getResources()
                    .getColor(R.color.action) : null);
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        changeThemeColors(true);
        if (menu.size() > 0) {
            menu.add(0, 9876, Menu.NONE, R.string.share);
            menu.findItem(9876).setIcon(R.drawable.ic_menu_share);
            menu.findItem(9876).setOnMenuItemClickListener(item -> {
                View focus = mActivity.getCurrentFocus();
                if (focus instanceof TextView) {
                    CharSequence selection = ((TextView) focus).getText().subSequence(((TextView) focus)
                            .getSelectionStart(), ((TextView) focus).getSelectionEnd());
                    final Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.setType("text/plain");
                    sendIntent.putExtra(Intent.EXTRA_TEXT, selection.toString());
                    mActivity.startActivity(Intent.createChooser(sendIntent, mActivity.getString(R.string.share)));
                    return true;
                }
                return false;
            });
        }
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
