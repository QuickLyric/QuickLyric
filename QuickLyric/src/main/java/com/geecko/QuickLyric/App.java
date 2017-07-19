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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.geecko.QuickLyric.services.LyricsOverlayService;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import java.util.TreeSet;

public class App extends Application implements Application.ActivityLifecycleCallbacks {

    public static final boolean playStoreVariant = BuildConfig.FLAVOR.equals("play");

    public static RefWatcher getRefWatcher(Context context) {
        App app = (App) context.getApplicationContext();
        return app.refWatcher;
    }

    private RefWatcher refWatcher;

    private static TreeSet<String> visibleActivities = new TreeSet<>();

    public static boolean isMainActivityVisible() {
        return visibleActivities.contains("MainActivity");
    }

    public static boolean isAppVisible() {
        return visibleActivities.size() > 0;
    }

    @Override
    public void onCreate() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }

        super.onCreate();
        refWatcher = LeakCanary.install(this);
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        visibleActivities.add(activity.getLocalClassName());
        if (App.isAppVisible() && LyricsOverlayService.isRunning()) {
            Intent showIntent = new Intent(getApplicationContext(), LyricsOverlayService.class);
            showIntent.setAction(LyricsOverlayService.HIDE_FLOATING_ACTION);
            getApplicationContext().startService(showIntent);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        visibleActivities.remove(activity.getLocalClassName());
        if (!App.isAppVisible() && LyricsOverlayService.isRunning() && PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("pref_overlay", false) &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(activity))) {
            Intent showIntent = new Intent(getApplicationContext(), LyricsOverlayService.class);
            showIntent.setAction(LyricsOverlayService.SHOW_FLOATING_ACTION);
            getApplicationContext().startService(showIntent);
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
