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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AppCompatDelegate;
import android.util.Base64;

import com.birbit.android.jobqueue.JobManager;
import com.geecko.QuickLyric.lyrics.QuickLyricAPI;
import com.geecko.QuickLyric.services.LyricsOverlayService;
import com.geecko.QuickLyric.utils.CertificateUtils;
import com.geecko.QuickLyric.utils.PowerUserChecker;
import com.github.javiersantos.piracychecker.PiracyChecker;
import com.github.javiersantos.piracychecker.enums.InstallerID;
import com.github.javiersantos.piracychecker.enums.PiracyCheckerCallback;
import com.github.javiersantos.piracychecker.enums.PiracyCheckerError;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import java.security.MessageDigest;
import java.util.Collections;
import java.util.TreeSet;

import javax.net.ssl.TrustManager;

public class App extends MultiDexApplication implements Application.ActivityLifecycleCallbacks {

    public static final boolean playStoreVariant = true;
    public static boolean pirated = false;
    public FirebaseAnalytics fireAnalyticsInstance;
    public JobManager jobManager;

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

        playStoreVariant = BuildConfig.FLAVOR.equals("play");
        super.onCreate();
        refWatcher = LeakCanary.install(this);
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        visibleActivities.add(activity.getLocalClassName());
        if (App.isAppVisible()) {
            Intent showIntent = new Intent(getApplicationContext(), LyricsOverlayService.class);
            showIntent.setAction(LyricsOverlayService.HIDE_FLOATING_ACTION);
            getApplicationContext().startService(showIntent);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        visibleActivities.remove(activity.getLocalClassName());
        if (!App.isAppVisible() && PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("pref_overlay", false) &&
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
