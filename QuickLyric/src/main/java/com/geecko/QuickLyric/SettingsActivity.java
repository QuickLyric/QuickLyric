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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.widget.GridLayout;

import com.geecko.QuickLyric.fragment.SettingsFragment;
import com.geecko.QuickLyric.utils.NightTimeVerifier;
import com.geecko.QuickLyric.view.ColorGridPreference;

public class SettingsActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        setContentView(R.layout.settings_activity);
        setStatusBarColor(null);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            toolbar.setElevation(8f);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);

        if ((getIntent().getExtras() != null && !getIntent().getExtras().getString("openedDialog", "").isEmpty()) ||
                ColorGridPreference.originalValue != null && Integer.parseInt(ColorGridPreference.originalValue) != themeNum) {
            ((ColorGridPreference) ((SettingsFragment) getFragmentManager().findFragmentByTag("SettingsFragment"))
                    .findPreference("pref_theme")).showDialog(null);
            getIntent().putExtra("openedDialog", "");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription taskDescription =
                    new ActivityManager.TaskDescription
                            (null, null, ((ColorDrawable) toolbar.getBackground()).getColor());
            this.setTaskDescription(taskDescription);
        }
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

    public int getSelectedTheme() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return Integer.valueOf(sharedPref.getString("pref_theme", "0"));
    }

    public void selectTheme(View view) {
        GridLayout grid = (GridLayout) view.getParent();
        int selection = grid.indexOfChild(view);
        for (int i = 0; i < grid.getChildCount(); i++)
            grid.getChildAt(i).setSelected(false);
        view.setSelected(true);
        selectTheme(selection, true);
    }

    public void selectTheme(int selection, boolean showDialog) {
        int selectedTheme = getSelectedTheme();
        if (selectedTheme == selection)
            return;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit().putString("pref_theme", String.valueOf(selection)).apply();
        Intent relaunch = new Intent(SettingsActivity.this, SettingsActivity.class);
        if (showDialog)
            relaunch.putExtra("openedDialog", "themeSelector");
        finish();
        startActivity(relaunch);
        overridePendingTransition(0, 0);
    }
}
