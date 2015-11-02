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


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.geecko.QuickLyric.adapter.DrawerAdapter;
import com.geecko.QuickLyric.broadcastReceiver.MusicBroadcastReceiver;
import com.geecko.QuickLyric.fragment.LocalLyricsFragment;
import com.geecko.QuickLyric.fragment.LyricsViewFragment;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.tasks.DBContentLister;
import com.geecko.QuickLyric.tasks.Id3Writer;
import com.geecko.QuickLyric.tasks.IdDecoder;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.LyricsSearchSuggestionsProvider;
import com.geecko.QuickLyric.utils.NightTimeVerifier;
import com.geecko.QuickLyric.utils.RefreshButtonBehavior;
import com.geecko.QuickLyric.utils.ScreenSlidePagerAdapter;
import com.geecko.QuickLyric.utils.Spotify;
import com.geecko.QuickLyric.view.LrcView;
import com.geecko.QuickLyric.view.RefreshIcon;
import com.viewpagerindicator.CirclePageIndicator;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.geecko.QuickLyric.R.array;
import static com.geecko.QuickLyric.R.id;
import static com.geecko.QuickLyric.R.layout;
import static com.geecko.QuickLyric.R.string;

public class MainActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener {

    private static final String LYRICS_FRAGMENT_TAG = "LyricsViewFragment";
    private static final String SETTINGS_FRAGMENT = "SettingsFragment";
    private static final String LOCAL_LYRICS_FRAGMENT_TAG = "LocalLyricsFragment";
    public static final String SEARCH_FRAGMENT_TAG = "SearchFragment";
    public View drawer;
    public View drawerView;
    public DrawerItemClickListener drawerListener;
    public boolean focusOnFragment = true;
    public ActionMode mActionMode;
    public SQLiteDatabase database;
    public ActionBarDrawerToggle mDrawerToggle;
    private Fragment displayedFragment;
    private MusicBroadcastReceiver receiver;
    private boolean receiverRegistered = false;
    private boolean destroyed = false;
    private AlertDialog mLinksDialog;

    private static void prepareAnimations(Fragment nextFragment) {
        if (nextFragment != null) {
            Class fragmentClass = ((Object) nextFragment).getClass();
            try {
                fragmentClass.getDeclaredField("showTransitionAnim").setBoolean(nextFragment, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int[] themes = new int[]{R.style.Theme_QuickLyric, R.style.Theme_QuickLyric_Dark};
        int themeNum = Integer.valueOf(sharedPref.getString("pref_theme", "0"));
        boolean nightMode = sharedPref.getBoolean("pref_night_mode", false);
        if (nightMode && NightTimeVerifier.check(this))
            setTheme(R.style.Theme_QuickLyric_Night);
        else
            setTheme(themes[themeNum]);
        setStatusBarColor(null);
        setNavBarColor(null);
        final FragmentManager fragmentManager = getFragmentManager();
        setContentView(layout.nav_drawer_activity);
        setSupportActionBar((android.support.v7.widget.Toolbar) findViewById(id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(string.app_name);

        AppBarLayout appBarLayout = (AppBarLayout) findViewById(id.appbar);
        appBarLayout.addOnOffsetChangedListener(this);

        /** Drawer setup */
        final ListView drawerList = (ListView) findViewById(id.drawer_list);
        DrawerAdapter drawerAdapter = new DrawerAdapter(this, this.getResources().getStringArray(array.nav_items));
        drawerList.setAdapter(drawerAdapter);
        drawerView = this.findViewById(id.left_drawer);
        drawer = this.findViewById(id.drawer_layout);
        if (drawer instanceof DrawerLayout) { // if phone
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            mDrawerToggle = new ActionBarDrawerToggle(this, (DrawerLayout) drawer, string.drawer_open_desc, string.drawer_closed_desc) {

                /**
                 * Called when a drawer has settled in a completely open state.
                 */
                public void onDrawerOpened(View drawerView) {
                    focusOnFragment = false;
                    if (mActionMode != null)
                        mActionMode.finish();
                    MainActivity.this.invalidateOptionsMenu(); // onPrepareOptionsMenu()
                }

                /**
                 * Called when a drawer has settled in a completely closed state.
                 */
                public void onDrawerClosed(View view) {
                    focusOnFragment = true;
                    MainActivity.this.invalidateOptionsMenu(); // onPrepareOptionsMenu()
                }
            };
            Resources.Theme theme = getTheme();
            TypedValue darkColorValue = new TypedValue();
            theme.resolveAttribute(R.attr.colorPrimaryDark, darkColorValue, true);
            ((DrawerLayout) drawer).setStatusBarBackgroundColor(darkColorValue.data);
            ((DrawerLayout) drawer).setDrawerListener(mDrawerToggle);
        }

        drawerListener = new DrawerItemClickListener();
        drawerList.setOnItemClickListener(drawerListener);
        database = new DatabaseHelper(getApplicationContext()).getReadableDatabase();
        DatabaseHelper.setDatabase(database);
        if (database != null && DatabaseHelper.getColumnsCount(database) <= 6)
            DatabaseHelper.addMissingColumns(database);

        Intent intent = getIntent();
        String extra = intent.getStringExtra(Intent.EXTRA_TEXT);
        Lyrics receivedLyrics = getBeamedLyrics(intent);
        if (receivedLyrics != null) {
            updateLyricsFragment(0, 0, false, receivedLyrics);
        } else
            switch (intent.getAction()) {
                case "com.geecko.QuickLyric.getLyrics":
                    Log.v("geecko", "breadcrumb");
                    String[] metadata = intent.getStringArrayExtra("TAGS");
                    String artist = metadata[0];
                    String track = metadata[1];
                    updateLyricsFragment(0, artist, track);
                    break;
                case "android.intent.action.SEND":
                    new IdDecoder(this, init(fragmentManager, true)).execute(getIdUrl(extra));
                    break;
                default:
                    init(fragmentManager, false);
            }
        if (!getSharedPreferences("tutorial", Context.MODE_PRIVATE).getBoolean("seen", false)) {
            registerTempReceiver();
            setupDemoScreen();
        }
    }

    private LyricsViewFragment init(FragmentManager fragmentManager, boolean startEmpty) {
        LyricsViewFragment lyricsViewFragment = (LyricsViewFragment) fragmentManager.findFragmentByTag(LYRICS_FRAGMENT_TAG);
        if (lyricsViewFragment == null || lyricsViewFragment.isDetached())
            lyricsViewFragment = new LyricsViewFragment();
        lyricsViewFragment.startEmpty(startEmpty);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.slide_in_end, R.animator.slide_out_start, R.animator.slide_in_start, R.animator.slide_out_end);
        if (!lyricsViewFragment.isAdded()) {
            fragmentTransaction.add(id.main_fragment_container, lyricsViewFragment, LYRICS_FRAGMENT_TAG);
        }

        Fragment[] activeFragments = getActiveFragments();
        displayedFragment = getDisplayedFragment(activeFragments);

        for (Fragment fragment : activeFragments)
            if (fragment != null) {
                if (fragment != displayedFragment && !fragment.isHidden()) {
                    fragmentTransaction.hide(fragment);
                    fragment.onHiddenChanged(true);
                } else if (fragment == displayedFragment)
                    fragmentTransaction.show(fragment);
            }
        fragmentTransaction.commit();
        return lyricsViewFragment;
    }

    public Fragment getDisplayedFragment(Fragment[] fragments) {
        for (Fragment fragment : fragments) {
            if (fragment == null)
                continue;
            if ((fragment instanceof LyricsViewFragment && ((LyricsViewFragment) fragment).isActiveFragment)
                    || (fragment instanceof LocalLyricsFragment && ((LocalLyricsFragment) fragment).isActiveFragment))
                return fragment;
        }
        return fragments[0];
    }

    public Fragment[] getActiveFragments() {
        FragmentManager fragmentManager = this.getFragmentManager();
        Fragment[] fragments = new Fragment[4];
        fragments[0] = fragmentManager.findFragmentByTag(LYRICS_FRAGMENT_TAG);
        fragments[1] = fragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG);
        fragments[2] = fragmentManager.findFragmentByTag(SETTINGS_FRAGMENT);
        fragments[3] = fragmentManager.findFragmentByTag(LOCAL_LYRICS_FRAGMENT_TAG);
        return fragments;
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    public ActionBar getSupportActionBar() {
        return super.getSupportActionBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        App.activityResumed();
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, ((Object) this).getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
            try {
                ndef.addDataType("application/lyrics");
            } catch (IntentFilter.MalformedMimeTypeException e) {
                return;
            }
            IntentFilter[] intentFiltersArray = new IntentFilter[]{ndef,};
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, null);
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        LyricsViewFragment lyricsViewFragment = (LyricsViewFragment) getFragmentManager()
                .findFragmentByTag(LYRICS_FRAGMENT_TAG);
        if (lyricsViewFragment != null) {
            if ((sharedPref.getBoolean("pref_auto_refresh", false) || lyricsViewFragment.isLRC()) &&
                    (getIntent() == null || getIntent().getAction() == null ||
                            getIntent().getAction().equals(""))) {
                // fixme executes twice?
                if (!"Storage".equals(lyricsViewFragment.getSource())
                        && !lyricsViewFragment.searchResultLock)
                    lyricsViewFragment.fetchCurrentLyrics(false);
                lyricsViewFragment.checkPreferencesChanges();
            }
        }
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(id.appbar);
        appBarLayout.removeOnOffsetChangedListener(this);
        appBarLayout.addOnOffsetChangedListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        String extra = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (action != null) {
            switch (action) {
                case NfcAdapter.ACTION_NDEF_DISCOVERED:
                    Lyrics receivedLyrics = getBeamedLyrics(intent);
                    if (receivedLyrics != null)
                        updateLyricsFragment(0, 0, false, receivedLyrics);
                    break;
                case "android.intent.action.SEARCH":
                    search(intent.getStringExtra(SearchManager.QUERY));
                    break;
                case "android.intent.action.SEND":
                    LyricsViewFragment lyricsViewFragment = (LyricsViewFragment) getFragmentManager()
                            .findFragmentByTag(LYRICS_FRAGMENT_TAG);
                    new IdDecoder(this, lyricsViewFragment).execute(getIdUrl(extra));
                    selectItem(0);
                    break;
                case "android.intent.action.VIEW":
                    if (intent.getDataString().contains("spotify"))
                        Spotify.onCallback(intent, this);
                    break;
                case "com.geecko.QuickLyric.getLyrics":
                    String[] metadata = intent.getStringArrayExtra("TAGS");
                    if (metadata != null) {
                        String artist = metadata[0];
                        String track = metadata[1];
                        LyricsViewFragment lyricsFragment = (LyricsViewFragment) getFragmentManager()
                                .findFragmentByTag(LYRICS_FRAGMENT_TAG);
                        lyricsFragment.fetchLyrics(artist, track);
                        selectItem(0);
                    }
                    break;
                case "com.geecko.QuickLyric.updateDBList":
                    updateDBList();
                    break;
            }
        }
    }

    @TargetApi(14)
    private Lyrics getBeamedLyrics(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        if (rawMsgs != null && rawMsgs.length > 0) {
            NdefMessage msg = (NdefMessage) rawMsgs[0];
            // record 0 contains the MIME type, record 1 is the AAR, if present
            NdefRecord[] records = msg.getRecords();
            if (records.length > 0) {
                try {
                    return Lyrics.fromBytes(records[0].getPayload());
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private void search(String searchQuery) {
        android.os.SystemClock.sleep(75); // fixme, keyboard animation slows down the transition
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                LyricsSearchSuggestionsProvider.AUTHORITY, LyricsSearchSuggestionsProvider.MODE);
        suggestions.saveRecentQuery(searchQuery, null);
        Intent searchIntent = new Intent(this, SearchActivity.class);
        searchIntent.putExtra("query", searchQuery);
        startActivityForResult(searchIntent, 55);
        overridePendingTransition(R.anim.slide_in_end, R.anim.fade_out);
    }

    private void updateDBList() {
        LocalLyricsFragment localLyricsFragment =
                (LocalLyricsFragment) getFragmentManager().findFragmentByTag(LOCAL_LYRICS_FRAGMENT_TAG);
        if (localLyricsFragment != null && localLyricsFragment.isActiveFragment)
            new DBContentLister(localLyricsFragment).execute();
        else
            selectItem(1);
    }

    private String getIdUrl(String extra) {
        final Pattern urlPattern = Pattern.compile(
                "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
                        + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
                        + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

        Matcher matcher = urlPattern.matcher(extra);
        if (matcher.find())
            return extra.substring(matcher.start(), matcher.end());
        else
            return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        invalidateOptionsMenu();
        LyricsViewFragment lyricsViewFragment =
                (LyricsViewFragment) getFragmentManager().findFragmentByTag(LYRICS_FRAGMENT_TAG);
        if (requestCode == 77) {
            lyricsViewFragment.checkPreferencesChanges();
        } else if (resultCode == RESULT_OK && requestCode == 55) {
            Lyrics results = (Lyrics) data.getSerializableExtra("lyrics");
            updateLyricsFragment(R.animator.slide_out_end, results.getArtist(), results.getTrack(), results.getURL());
            lyricsViewFragment.searchResultLock = true;
        }
        lyricsViewFragment.updateSearchView(true, null, false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        App.activityPaused();
        if (receiver != null && receiverRegistered) {
            unregisterReceiver(receiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
            receiverRegistered = false;
        }
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null)
            nfcAdapter.disableForegroundDispatch(this);
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(id.appbar);
        appBarLayout.removeOnOffsetChangedListener(this);
    }

    @Override
    protected void onDestroy() {
        this.destroyed = true;
        if (database != null && database.isOpen())
            database.close();
        super.onDestroy();
    }

    public boolean hasBeenDestroyed() {
        return this.destroyed;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (displayedFragment instanceof LyricsViewFragment) {
            DrawerAdapter drawerAdapter = (DrawerAdapter)
                    ((ListView) findViewById(id.drawer_list)).getAdapter();
            if (drawerAdapter.getSelectedItem() != 0) {
                drawerAdapter.setSelectedItem(0);
                drawerAdapter.notifyDataSetChanged();
            }
        }
        if (!RefreshButtonBehavior.visible)
            findViewById(id.refresh_fab).setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (drawer instanceof DrawerLayout && ((DrawerLayout) drawer).isDrawerOpen(drawerView))
            ((DrawerLayout) drawer).closeDrawer(drawerView);
        else
            finish();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null)
            mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null)
            mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        LyricsViewFragment lyricsViewFragment =
                ((LyricsViewFragment) getFragmentManager().findFragmentByTag(LYRICS_FRAGMENT_TAG));
        if (lyricsViewFragment != null)
            lyricsViewFragment.enablePullToRefresh(i == 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LocalLyricsFragment.REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LocalLyricsFragment localLyricsFragment = (LocalLyricsFragment)
                            getFragmentManager().findFragmentByTag(LOCAL_LYRICS_FRAGMENT_TAG);
                    localLyricsFragment.showScanDialog();
                } else {
                    Toast.makeText(this, string.scan_error_permission_denied, Toast.LENGTH_LONG).show();
                }
                break;
            case Id3Writer.REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, string.id3_write_permission_error, Toast.LENGTH_LONG).show();
                } else {
                    String message = getString(string.id3_write_error) + " " + getString(string.permission_denied);
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                }
        }
    }

    public void setDrawerListener(boolean bool) {
        ((ListView) findViewById(id.drawer_list))
                .setOnItemClickListener(bool ? drawerListener : null);
    }

    private void registerTempReceiver() {
        receiver = new MusicBroadcastReceiver();
        MusicBroadcastReceiver.forceAutoUpdate(true);
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction("com.android.music.metachanged");
        intentfilter.addAction("com.htc.music.metachanged");
        intentfilter.addAction("com.miui.player.metachanged");
        intentfilter.addAction("com.real.IMP.metachanged");
        intentfilter.addAction("com.sonyericsson.music.metachanged");
        intentfilter.addAction("com.rdio.android.playstatechanged");
        intentfilter.addAction("com.samsung.sec.android.MusicPlayer.metachanged");
        intentfilter.addAction("com.sec.android.app.music.metachanged");
        intentfilter.addAction("com.nullsoft.winamp.metachanged");
        intentfilter.addAction("com.amazon.mp3.metachanged");
        intentfilter.addAction("com.rhapsody.metachanged");
        intentfilter.addAction("com.maxmpz.audioplayer.metachanged");
        intentfilter.addAction("com.real.IMP.metachanged");
        intentfilter.addAction("com.andrew.apollo.metachanged");
        intentfilter.addAction("fm.last.android.metachanged");
        intentfilter.addAction("com.adam.aslfms.notify.playstatechanged");
        intentfilter.addAction("net.jjc1138.android.scrobbler.action.MUSIC_STATUS");
        intentfilter.addAction("com.spotify.music.metadatachanged");
        registerReceiver(receiver, intentfilter);
        this.receiverRegistered = true;
    }

    private void setupDemoScreen() {
        ViewGroup rootView = (ViewGroup) findViewById(id.root_view);
        getLayoutInflater().inflate(layout.tutorial_view, rootView);
        ViewPager pager = (ViewPager) findViewById(id.pager);
        CirclePageIndicator indicator = (CirclePageIndicator) findViewById(id.indicator);
        final ScreenSlidePagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(getFragmentManager(), this);
        pager.setAdapter(pagerAdapter);
        pager.addOnPageChangeListener(pagerAdapter);
        indicator.setViewPager(pager);
        pager.setCurrentItem(pagerAdapter.rightToLeft ? pagerAdapter.getCount() - 1 : 0);
        indicator.setOnPageChangeListener(pagerAdapter);
        Button skipButton = (Button) rootView.findViewById(id.pager_button);
        ImageButton arrowButton = (ImageButton) rootView.findViewById(id.pager_arrow);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pagerAdapter.exitAction();
            }
        });
        arrowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pagerAdapter.nextAction();
            }
        });
        if (mDrawerToggle != null)
            ((DrawerLayout) drawer).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        focusOnFragment = false;
        invalidateOptionsMenu();
    }

    @TargetApi(21)
    public void setStatusBarColor(Integer color) {
        if (Build.VERSION.SDK_INT >= 20) {
            if (color == null) {
                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = getTheme();
                theme.resolveAttribute(android.R.attr.statusBarColor, typedValue, true);
                color = typedValue.data;
            }
            getWindow().setStatusBarColor(color);
        }
    }

    @TargetApi(21)
    public void setNavBarColor(Integer color) {
        if (Build.VERSION.SDK_INT >= 20) {
            if (color == null) {
                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = getTheme();
                theme.resolveAttribute(android.R.attr.navigationBarColor, typedValue, true);
                color = typedValue.data;
            }
            getWindow().setNavigationBarColor(color);
        }
    }

    public void updateLyricsFragment(int outAnim, String... params) { // Should only be called from SearchFragment or IdDecoder
        String artist = params[0];
        String song = params[1];
        String url = null;
        if (params.length > 2)
            url = params[2];
        LyricsViewFragment lyricsViewFragment = (LyricsViewFragment)
                getFragmentManager().findFragmentByTag(LYRICS_FRAGMENT_TAG);
        if (lyricsViewFragment != null)
            lyricsViewFragment.fetchLyrics(artist, song, url);
        else {
            Lyrics lyrics = new Lyrics(Lyrics.SEARCH_ITEM);
            lyrics.setArtist(artist);
            lyrics.setTitle(song);
            lyrics.setURL(url);
            Bundle lyricsBundle = new Bundle();
            try {
                if (artist != null && song != null)
                    lyricsBundle.putByteArray("lyrics", lyrics.toBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            lyricsViewFragment = new LyricsViewFragment();
            lyricsViewFragment.setArguments(lyricsBundle);

            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.slide_in_start, outAnim, R.animator.slide_in_start, outAnim);
            Fragment activeFragment = getDisplayedFragment(getActiveFragments());
            if (activeFragment != null) {
                prepareAnimations(activeFragment);
                fragmentTransaction.hide(activeFragment);
            }
            fragmentTransaction.add(id.main_fragment_container, lyricsViewFragment, LYRICS_FRAGMENT_TAG);
            lyricsViewFragment.isActiveFragment = true;
            fragmentTransaction.commit();
        }
        if (drawer instanceof DrawerLayout && !mDrawerToggle.isDrawerIndicatorEnabled()) {
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            ((DrawerLayout) drawer).setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    public void updateLyricsFragment(int outAnim, int inAnim, boolean transition, Lyrics lyrics) {
        LyricsViewFragment lyricsViewFragment = (LyricsViewFragment) getFragmentManager().findFragmentByTag(LYRICS_FRAGMENT_TAG);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(inAnim, outAnim, inAnim, outAnim);
        Fragment activeFragment = getDisplayedFragment(getActiveFragments());
        if (lyricsViewFragment != null && lyricsViewFragment.getView() != null) {
            SharedPreferences preferences = getSharedPreferences("current_music", Context.MODE_PRIVATE);
            String artist = preferences.getString("artist", null);
            String track = preferences.getString("track", null);
            if (lyrics.isLRC() && !(lyrics.getOriginalArtist().equals(artist) && lyrics.getOriginalTrack().equals(track))) {
                LrcView parser = new LrcView(this, null);
                parser.setOriginalLyrics(lyrics);
                parser.setSourceLrc(lyrics.getText());
                lyrics = parser.getStaticLyrics();
            }
            lyricsViewFragment.update(lyrics, lyricsViewFragment.getView(), true);
            if (transition) {
                fragmentTransaction.hide(activeFragment).show(lyricsViewFragment);
                prepareAnimations(activeFragment);
                prepareAnimations(lyricsViewFragment);
            }
        } else {
            Bundle lyricsBundle = new Bundle();
            try {
                lyricsBundle.putByteArray("lyrics", lyrics.toBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            lyricsViewFragment = new LyricsViewFragment();
            lyricsViewFragment.setArguments(lyricsBundle);
            if (!(activeFragment instanceof LyricsViewFragment) && activeFragment != null)
                fragmentTransaction.hide(activeFragment).add(id.main_fragment_container, lyricsViewFragment, LYRICS_FRAGMENT_TAG);
            else
                fragmentTransaction.replace(id.main_fragment_container, lyricsViewFragment, LYRICS_FRAGMENT_TAG);
        }
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void collapseToolbar() {
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(id.appbar);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
        if (behavior != null) {
            behavior.onNestedFling((CoordinatorLayout) (findViewById(id.coordinator_layout)),
                    appBarLayout, null, 0, 10000, true);
        }
    }

    private void showRefreshFab(boolean show) {
        RefreshIcon refreshFAB = (RefreshIcon) findViewById(id.refresh_fab);
        if (show)
            refreshFAB.show();
        else
            refreshFAB.hide();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle != null)
            return (mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item));
        else
            return super.onOptionsItemSelected(item);
    }

    // Swaps fragments from the drawer
    private void selectItem(int position) {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment newFragment;
        String tag;
        switch (position) {
            default:
                // Lyrics
                tag = LYRICS_FRAGMENT_TAG;
                newFragment = fragmentManager.findFragmentByTag(tag);
                if (newFragment == null || !(newFragment instanceof LyricsViewFragment))
                    newFragment = new LyricsViewFragment();
                ((LyricsViewFragment) newFragment).showTransitionAnim = true;
                break;
            case 1:
                // Saved Lyrics
                tag = LOCAL_LYRICS_FRAGMENT_TAG;
                newFragment = fragmentManager.findFragmentByTag(tag);
                if (newFragment == null || !(newFragment instanceof LocalLyricsFragment))
                    newFragment = new LocalLyricsFragment();
                ((LocalLyricsFragment) newFragment).showTransitionAnim = true;
                break;
            case 2:
                // Separator
                return;
            case 3:
                // Settings
                if (drawer instanceof DrawerLayout)
                    ((DrawerLayout) drawer).closeDrawer(drawerView);
                drawer.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivityForResult(settingsIntent, 77);
                    }
                }, 250);
                return;
            case 4:
                // Feedback
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"quicklyricapp@gmail.com"});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Issues with QuickLyric");
                if (emailIntent.resolveActivity(getPackageManager()) != null) {
                    Toast.makeText(this, R.string.pref_issues_sum, Toast.LENGTH_LONG).show();
                    startActivity(emailIntent);
                }
                if (drawer instanceof DrawerLayout)
                    ((DrawerLayout) drawer).closeDrawer(drawerView);
                return;
            case 5:
                // About Dialog
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setView(getLayoutInflater().inflate(layout.links_dialog, (ViewGroup) drawerView.getRootView(), false));
                dialog.setCancelable(true);
                mLinksDialog = dialog.create();
                mLinksDialog.show();
                if (drawer instanceof DrawerLayout)
                    ((DrawerLayout) drawer).closeDrawer(drawerView);
                return;
        }

        Fragment activeFragment = getDisplayedFragment(getActiveFragments());
        prepareAnimations(activeFragment);

        // Insert the fragment by replacing any existing fragment
        if (newFragment != activeFragment) {
            if (mActionMode != null)
                mActionMode.finish();
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.slide_in_start, R.animator.slide_out_start, R.animator.slide_in_start, R.animator.slide_out_start);
            fragmentTransaction.hide(activeFragment);
            if (newFragment.isAdded())
                fragmentTransaction.show(newFragment);
            else
                fragmentTransaction.add(id.main_fragment_container, newFragment, tag);
            ((CollapsingToolbarLayout) findViewById(R.id.toolbar_layout)).setCollapsedTitleTextColor(Color.WHITE);
            fragmentTransaction.commit();

            if (activeFragment instanceof LyricsViewFragment) {
                showRefreshFab(false);
                collapseToolbar();
            } else if (newFragment instanceof LyricsViewFragment)
                showRefreshFab(true);
        }
        if (drawer instanceof DrawerLayout && (newFragment == activeFragment))
            ((DrawerLayout) drawer).closeDrawer(drawerView);
    }

    public void whyPopUp(View view) {
        LyricsViewFragment lyricsViewFragment = ((LyricsViewFragment) getFragmentManager().findFragmentByTag(LYRICS_FRAGMENT_TAG));
        if (lyricsViewFragment != null && !lyricsViewFragment.isDetached())
            lyricsViewFragment.showWhyPopup();
    }

    @SuppressWarnings("unused")
    public void id3PopUp(View view) {
        Toast.makeText(this, string.ignore_id3_toast, Toast.LENGTH_LONG).show();
    }

    public void facebookLink(View view) {
        openLink("https://www.facebook.com/QuickLyric");
        mLinksDialog.dismiss();
    }

    public void twitterLink(View view) {
        openLink("https://www.twitter.com/QuickLyric");
        mLinksDialog.dismiss();
    }

    public void googleLink(View view) {
        openLink("https://plus.google.com/communities/115504114424315189412");
        mLinksDialog.dismiss();
    }

    public void githubLink(View view) {
        openLink("https://github.com/geecko86/QuickLyric");
        mLinksDialog.dismiss();
    }

    private void openLink(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    public void aboutApp(View view) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setView(getLayoutInflater().inflate(R.layout.about_dialog, (ViewGroup) drawerView.getRootView(), false));
        dialog.create().show();
    }

    @SuppressLint("InlinedApi")
    public void resync(MenuItem item) {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null,
                // Use the music stream.
                AudioManager.STREAM_SYSTEM,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        am.abandonAudioFocus(null);
    }

    private class DrawerItemClickListener implements
            ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }
}
