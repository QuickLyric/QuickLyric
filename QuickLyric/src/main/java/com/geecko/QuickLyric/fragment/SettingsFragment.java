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

package com.geecko.QuickLyric.fragment;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.View;

import com.cgollner.unclouded.preferences.SwitchPreferenceCompat;
import com.geecko.QuickLyric.App;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.broadcastReceiver.MusicBroadcastReceiver;
import com.geecko.QuickLyric.services.LyricsOverlayService;
import com.geecko.QuickLyric.utils.play.Premium;
import com.squareup.leakcanary.RefWatcher;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

public class SettingsFragment extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener, TimePickerDialog.OnTimeSetListener, DialogInterface.OnCancelListener {

    private static final String NIGHT_START_TIME_DIALOG_TAG = "StartPickerDialog";
    private static final String NIGHT_END_TIME_DIALOG_TAG = "EndPickerDialog";
    int[] themes = new int[]{R.string.defaut_theme, R.string.red_theme,
            R.string.purple_theme, R.string.indigo_theme, R.string.green_theme,
            R.string.lime_theme, R.string.brown_theme, R.string.dark_theme};

    private int[] nightTimeStart = new int[]{42, 0};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRetainInstance(true);
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int themeNum = Integer.valueOf(sharedPref.getString("pref_theme", "0"));

        findPreference("pref_theme").setSummary(themes[themeNum]);
        findPreference("pref_theme").setOnPreferenceChangeListener(this);
        findPreference("pref_opendyslexic").setOnPreferenceChangeListener(this);
        findPreference("pref_night_mode").setOnPreferenceChangeListener(this);
        findPreference("pref_notifications").setOnPreferenceChangeListener(this);
        findPreference("pref_providers").setOnPreferenceChangeListener(this);
        findPreference("pref_overlay").setOnPreferenceChangeListener(this);
        findPreference("pref_overlay_behavior").setOnPreferenceChangeListener(this);
        findPreference("pref_overlay_behavior").setEnabled(isOverlayEnabled);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case "pref_theme":
                findPreference("pref_theme").setSummary(themes[Integer.valueOf((String) newValue)]);
                break;
            case "pref_notifications":
                if (newValue.equals("0")) {
                    ((NotificationManager) getActivity()
                            .getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
                } else
                    broadcast();
                break;
            case "pref_night_mode":
                if ((Boolean) newValue) {
                    boolean twentyFourHourStyle = DateFormat.is24HourFormat(getActivity());
                    TimePickerDialog tpd = TimePickerDialog
                            .newInstance(this, 21, 0, twentyFourHourStyle);
                    tpd.setOnCancelListener(this);
                    tpd.setTitle(getActivity().getString(R.string.nighttime_start_dialog_title));
                    tpd.show(getFragmentManager(), NIGHT_START_TIME_DIALOG_TAG);
                } else {
                    this.onCancel(null);
                }
                break;
            case "pref_overlay":
                findPreference("pref_overlay_behavior").setEnabled((Boolean) newValue);
                findPreference("pref_notifications").setEnabled(!((Boolean) newValue));
                findPreference("pref_notifications").setSummary((Boolean) newValue ?
                        R.string.pref_notification_sum_disabled : R.string.pref_notifications_sum);
                if ((Boolean) newValue) {
                    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(pref.getKey(), (Boolean) newValue).apply();
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(getActivity())) {
                        broadcast();
                    } else {
                        final Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getActivity().getPackageName()));
                        getActivity().startActivity(intent);
                    }
                } else {
                    LyricsOverlayService.removeCustomFloatingView(getActivity());
                }
                break;
            case "pref_overlay_behavior":
                LyricsOverlayService.removeCustomFloatingView(getActivity());
                break;
        }
        return true;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (this.isHidden())
            return;
        TypedValue typedValue = new TypedValue();
        view.getContext().getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
        view.setBackgroundDrawable(new ColorDrawable(typedValue.data));
    }

    @Override
    public void onTimeSet(RadialPickerLayout radialPickerLayout, int h, int min) {
        if (nightTimeStart[0] >= 25) {
            nightTimeStart = new int[]{h, min};
            boolean twentyFourHourStyle = DateFormat.is24HourFormat(getActivity());
            TimePickerDialog tpd = TimePickerDialog
                    .newInstance(this, 6, 0, twentyFourHourStyle);
            tpd.setOnCancelListener(this);
            tpd.setTitle(getActivity().getString(R.string.nighttime_end_dialog_title));
            tpd.show(getFragmentManager(), NIGHT_END_TIME_DIALOG_TAG);
        } else {
            SharedPreferences current = getActivity().getSharedPreferences("night_time", Context.MODE_PRIVATE);
            current.edit().putInt("startHour", nightTimeStart[0])
                    .putInt("startMinute", nightTimeStart[1])
                    .putInt("endHour", h)
                    .putInt("endMinute", min)
                    .apply();

            nightTimeStart[0] = 42;
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden)
            this.onViewCreated(getView(), null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = App.getRefWatcher(getActivity());
        refWatcher.watch(this);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        SharedPreferences current = getActivity().getSharedPreferences("night_time", Context.MODE_PRIVATE);
        current.edit().putInt("startHour", 42)
                .putInt("endHour", 45)
                .apply();
        ((SwitchPreferenceCompat) findPreference("pref_night_mode")).setChecked(false);
    }

    private void broadcast() {
        Intent localIntent = new Intent("com.android.music.metachanged");
        SharedPreferences current = getActivity().getSharedPreferences("current_music", Context.MODE_PRIVATE);
        String artist = current.getString("artist", "Michael Jackson");
        String track = current.getString("track", "Bad");
        boolean playing = current.getBoolean("playing", false);
        long position = current.getLong("position", -1L);
        localIntent.putExtra("artist", artist);
        localIntent.putExtra("track", track);
        localIntent.putExtra("playing", playing);
        localIntent.putExtra("duration", 600);
        if (position != -1)
            localIntent.putExtra("position", position);
        new MusicBroadcastReceiver().onReceive(getActivity(), localIntent);
    }
}
