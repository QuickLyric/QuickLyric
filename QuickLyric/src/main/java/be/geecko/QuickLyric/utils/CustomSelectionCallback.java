package be.geecko.QuickLyric.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import be.geecko.QuickLyric.R;

/**
 * This file is part of QuickLyric
 * Created by geecko on 31/12/14.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class CustomSelectionCallback implements ActionMode.Callback {
    Activity mActivity;

    public CustomSelectionCallback(Activity activity) {
        this.mActivity = activity;
    }

    @TargetApi(21)
    private void actionModeStatusBar(boolean actionMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mActivity.getWindow().setStatusBarColor
                    (mActivity.getResources()
                            .getColor(actionMode ? R.color.action_dark : R.color.primary_dark));
            mActivity.getWindow().setNavigationBarColor
                    (mActivity.getResources()
                            .getColor(actionMode ? R.color.action : R.color.primary));
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        actionModeStatusBar(true);
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
        actionModeStatusBar(false);
    }
}
