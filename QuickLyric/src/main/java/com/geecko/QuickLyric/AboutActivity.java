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

package com.geecko.QuickLyric;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.geecko.QuickLyric.adapter.IntroScreenSlidePagerAdapter;
import com.geecko.QuickLyric.utils.NightTimeVerifier;
import com.viewpagerindicator.CirclePageIndicator;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

public class AboutActivity extends AppCompatActivity {

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
        TypedValue primaryColor = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimary, primaryColor, true);
        setStatusBarColor(null);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle(R.string.pref_about);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            toolbar.setElevation(8f);
        toolbar.setBackgroundColor(primaryColor.data);
        toolbar.setTitleTextColor(Color.WHITE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription taskDescription =
                    new ActivityManager.TaskDescription
                            (null, null, primaryColor.data);
            this.setTaskDescription(taskDescription);
        }

        View.OnClickListener productTourAction = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupDemoScreen();
            }
        };

        Element productTourElement = new Element().setTitle(getString(R.string.about_product_tour));
        productTourElement.setOnClickListener(productTourAction);
        Element crowdinElement = new Element().setTitle(getString(R.string.about_crowdin));
        crowdinElement.setIntent(
                new Intent(Intent.ACTION_VIEW, Uri.parse("https://crowdin.com/project/quicklyric"))
        );
        Element ossLicensesElement = new Element().setTitle("Open Source Licenses").setTag("OSSLicenses");
        ossLicensesElement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebView webView = new WebView(AboutActivity.this);
                String data = getResources().getString(R.string.open_source_librairies_licenses);
                webView.loadData(data, "text/html; charset=utf-8", "UTF-8");
                new AlertDialog.Builder(AboutActivity.this).setView(webView).show();
            }
        });
        Element tosElement = new Element().setTitle(getString(R.string.about_read_ToS));
        tosElement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebView webView = new WebView(AboutActivity.this);
                String data = getResources().getString(R.string.QL_EULA);
                webView.loadData(data, "text/html; charset=utf-8", "UTF-8");
                new AlertDialog.Builder(AboutActivity.this).setView(webView).show();
            }
        });

        View aboutView = new AboutPage(this)
                .setDescription("QuickLyric is made with love in Brussels, Belgium.") // FixMe
                .addEmail("contact@QuickLyric.be")
                .addFacebook("QuickLyric")
                .addGitHub("geecko86/QuickLyric")
                .addPlayStore("test")
                .addTwitter("QuickLyric")
                .addWebsite("http://www.quicklyric.be")
                .setImage(R.drawable.icon)
                .addItem(productTourElement)
                .addItem(crowdinElement)
                .addItem(ossLicensesElement)
                .addItem(tosElement)
                .create();
        aboutView.setLayoutParams(new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.addView(toolbar);
        linearLayout.addView(aboutView);
        setContentView(linearLayout);

        final Drawable upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_material);
        upArrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);
    }

    private void setupDemoScreen() {
        ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content).getRootView();
        getLayoutInflater().inflate(R.layout.tutorial_view, (ViewGroup) rootView.getChildAt(0));
        final ViewPager pager = (ViewPager) findViewById(R.id.pager);
        CirclePageIndicator indicator = (CirclePageIndicator) findViewById(R.id.indicator);
        final IntroScreenSlidePagerAdapter pagerAdapter = new IntroScreenSlidePagerAdapter(getFragmentManager(), this);
        pager.setAdapter(pagerAdapter);
        pager.addOnPageChangeListener(pagerAdapter);
        indicator.setViewPager(pager);
        pager.setCurrentItem(pagerAdapter.rightToLeft ? pagerAdapter.getCount() - 1 : 0);
        indicator.setOnPageChangeListener(pagerAdapter);
        Button skipButton = (Button) rootView.findViewById(R.id.pager_button);
        ImageButton arrowButton = (ImageButton) rootView.findViewById(R.id.pager_arrow);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
                    pagerAdapter.exitAction();
                else
                    pager.setCurrentItem(pagerAdapter.getCount() - 1);
            }
        });
        arrowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pagerAdapter.nextAction();
            }
        });
    }

    @TargetApi(21)
    public void setStatusBarColor(Integer color) {
        if (Build.VERSION.SDK_INT >= 21) {
            if (color == null) {
                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = getTheme();
                theme.resolveAttribute(android.R.attr.colorPrimaryDark, typedValue, true);
                color = typedValue.data;
            }
            getWindow().setStatusBarColor(color);
        }
    }
}
