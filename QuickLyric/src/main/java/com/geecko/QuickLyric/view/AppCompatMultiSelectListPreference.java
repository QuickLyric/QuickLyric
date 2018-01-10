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

package com.geecko.QuickLyric.view;

import android.content.Context;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.util.AttributeSet;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AppCompatMultiSelectListPreference extends MultiSelectListPreference {

    private AppCompatDialog mDialog;
    private List<String> mSelectedItems = new ArrayList<>();  // Where we track the selected items

    public AppCompatMultiSelectListPreference(Context context) {
        super(context);
    }

    public AppCompatMultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public AppCompatDialog getDialog() {
        return mDialog;
    }

    @Override
    protected void showDialog(Bundle state) {
        if (getEntries() == null || getEntryValues() == null) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array.");
        }

        boolean[] preselect = new boolean[getEntryValues().length];
        for (String s : getValues()) {
            int index = findIndexOfValue(s);
            if (index != -1) {
                preselect[index] = true;
                mSelectedItems.add(s);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(getDialogTitle())
                .setIcon(getDialogIcon())
                .setNegativeButton(getNegativeButtonText(), (dialog, which) -> dialog.dismiss())
                .setPositiveButton(getPositiveButtonText(), (dialog, which) -> {
                    dialog.dismiss();
                    if (callChangeListener(mSelectedItems))
                        setValues(new HashSet<>(mSelectedItems));
                })
                .setMultiChoiceItems(getEntries(), preselect, (dialog, which, isChecked) -> {
                    String value = String.valueOf(getEntryValues()[which]);
                    if (isChecked)
                        mSelectedItems.add(value);
                    else if (mSelectedItems.contains(value))
                        mSelectedItems.remove(value);
                });

        PreferenceManager pm = getPreferenceManager();
        try {
            Method method = pm.getClass().getDeclaredMethod(
                    "registerOnActivityDestroyListener",
                    PreferenceManager.OnActivityDestroyListener.class);
            method.setAccessible(true);
            method.invoke(pm, this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mDialog = builder.create();
        if (state != null)
            mDialog.onRestoreInstanceState(state);
        mDialog.show();
    }

    @Override
    public void onActivityDestroy() {
        super.onActivityDestroy();
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();
    }
}
