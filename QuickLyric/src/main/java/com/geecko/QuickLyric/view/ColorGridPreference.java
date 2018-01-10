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
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;

import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.SettingsActivity;

import java.lang.reflect.Method;

public class ColorGridPreference extends ListPreference {

    private AppCompatDialog mDialog;
    public static String originalValue = null;

    @SuppressWarnings("unused")
    public ColorGridPreference(Context context) {
        super(context);
    }

    @SuppressWarnings("unused")
    public ColorGridPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public AppCompatDialog getDialog() {
        return mDialog;
    }

    @Override
    public void showDialog(Bundle state) {
        if (getEntryValues() == null) {
            throw new IllegalStateException(
                    "ColorGridPreference requires an entryValues array.");
        }

        if (originalValue == null)
            originalValue = getValue();
        final int preselect = findIndexOfValue(getValue());
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(getDialogTitle())
                .setIcon(getDialogIcon())
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    onDialogClosed(false);
                    dialog.cancel();
                })
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    onDialogClosed(true);
                    dialog.dismiss();
                })
                .setView(R.layout.theme_dialog);
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
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.getWindow().getAttributes().windowAnimations = android.R.anim.accelerate_interpolator;
        if (state != null)
            mDialog.onRestoreInstanceState(state);
        mDialog.show();

        GridLayout gridLayout = mDialog.findViewById(R.id.grid);
        gridLayout.getChildAt(preselect).setSelected(true);
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            gridLayout.getChildAt(i).setOnClickListener(v -> ((SettingsActivity) getContext()).selectTheme(v));
        }
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View view = super.getView(convertView, parent);

        boolean containsCoin = false;
        for (int i = 0; !containsCoin && i < ((ViewGroup) view).getChildCount(); i++) {
            View child = ((ViewGroup) view).getChildAt(i);
            if (child instanceof ImageView && "coin".equals(child.getTag()))
                containsCoin = true;
        }

        return view;
    }

    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(false);
        if (!positiveResult)
            ((SettingsActivity) getContext()).selectTheme(Integer.parseInt(originalValue), false);
        else
            originalValue = getValue();
    }

    @Override
    public void onActivityDestroy() {
        super.onActivityDestroy();
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();
    }
}
