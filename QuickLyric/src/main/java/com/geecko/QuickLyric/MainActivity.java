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
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.geecko.QuickLyric.adapter.DrawerAdapter;
import com.geecko.QuickLyric.broadcastReceiver.MusicBroadcastReceiver;
import com.geecko.QuickLyric.fragment.LocalLyricsFragment;
import com.geecko.QuickLyric.fragment.LyricsViewFragment;
import com.geecko.QuickLyric.fragment.SearchFragment;
import com.geecko.QuickLyric.fragment.SettingsFragment;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.tasks.ParseTask;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.IdDecoder;
import com.geecko.QuickLyric.utils.ScreenSlidePagerAdapter;
import com.viewpagerindicator.CirclePageIndicator;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.geecko.QuickLyric.R.array;
import static com.geecko.QuickLyric.R.id;
import static com.geecko.QuickLyric.R.layout;
import static com.geecko.QuickLyric.R.string;

public class MainActivity extends ActionBarActivity {

    private static final String LYRICS_FRAGMENT_TAG = "LyricsViewFragment";
    private static final String SETTINGS_FRAGMENT = "SettingsFragment";
    private static final String LOCAL_LYRICS_FRAGMENT_TAG = "LocalLyricsFragment";
    private static final String SEARCH_FRAGMENT_TAG = "SearchFragment";
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
        int theme = Integer.valueOf(sharedPref.getString("pref_theme", "0"));
        setTheme(themes[theme]);
        setStatusBarColor(null);
        setNavBarColor(null);
        final FragmentManager fragmentManager = getFragmentManager();
        setContentView(layout.nav_drawer_activity);
        setSupportActionBar((android.support.v7.widget.Toolbar) findViewById(id.toolbar));

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
            ((DrawerLayout) drawer).setDrawerListener(mDrawerToggle);
        }

        drawerListener = new DrawerItemClickListener();
        drawerList.setOnItemClickListener(drawerListener);
        database = new DatabaseHelper(getApplicationContext()).getReadableDatabase();

        Intent intent = getIntent();
        String extra = intent.getStringExtra(Intent.EXTRA_TEXT);
        Lyrics receivedLyrics = getBeamedLyrics(intent);
        if (receivedLyrics != null) {
            updateLyricsFragment(0, 0, false, receivedLyrics);
        } else {
            if ("android.intent.action.SEND".equals(intent.getAction())
                    && (extra.contains("http://www.soundhound.com/")
                    || extra.contains("http://shz.am/"))) {
                new IdDecoder(this, null).execute(getIdUrl(extra));
            } else if (intent.getAction().equals("android.intent.action.VIEW")) {
                processURL(intent);
            } else if (intent.getAction().equals("com.geecko.QuickLyric.getLyrics")) {
                String[] metadata = intent.getStringArrayExtra("TAGS");
                String artist = metadata[0];
                String track = metadata[1];
                updateLyricsFragment(0, artist, track);
            } else {
                LyricsViewFragment lyricsViewFragment = (LyricsViewFragment) fragmentManager.findFragmentByTag(LYRICS_FRAGMENT_TAG);
                if (lyricsViewFragment == null)
                    lyricsViewFragment = new LyricsViewFragment();
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
            }
        }
        intent.setAction("");
        if (!getSharedPreferences("tutorial", Context.MODE_PRIVATE).getBoolean("seen", false)) {
            registerTempReceiver();
            setupDemoScreen();
        }
    }

    public Fragment getDisplayedFragment(Fragment[] fragments) {
        for (Fragment fragment : fragments) {
            if (fragment == null)
                continue;
            if ((fragment instanceof LyricsViewFragment && ((LyricsViewFragment) fragment).isActiveFragment) || (fragment instanceof SettingsFragment && ((SettingsFragment) fragment).isActiveFragment) || (fragment instanceof SearchFragment && ((SearchFragment) fragment).isActiveFragment) || (fragment instanceof LocalLyricsFragment && ((LocalLyricsFragment) fragment).isActiveFragment))
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
        if (sharedPref.getBoolean("pref_auto_refresh", false)) {
            if (getIntent() != null && getIntent().getAction() != null)
                getIntent().setAction(null);
            else { // fixme executes twice
                LyricsViewFragment lyricsViewFragment = (LyricsViewFragment) getFragmentManager()
                        .findFragmentByTag(LYRICS_FRAGMENT_TAG);
                new ParseTask().execute(lyricsViewFragment, null);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        String extra = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (action != null) {
            if (action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
                Lyrics receivedLyrics = getBeamedLyrics(intent);
                if (receivedLyrics != null)
                    updateLyricsFragment(0, 0, false, receivedLyrics);
            } else if (action.equals("android.intent.action.SEARCH"))
                search(intent.getStringExtra(SearchManager.QUERY));
            else if (action.equals("android.intent.action.SEND")
                    && (extra.contains("http://www.soundhound.com/")
                    || extra.contains("http://shz.am/"))) {
                LyricsViewFragment lyricsViewFragment = (LyricsViewFragment) getFragmentManager()
                        .findFragmentByTag(LYRICS_FRAGMENT_TAG);
                new IdDecoder(this, lyricsViewFragment).execute(getIdUrl(extra));
            } else if (action.equals("android.intent.action.VIEW"))
                processURL(intent);
            else if (action.equals("com.geecko.QuickLyric.getLyrics")) {
                String[] metadata = intent.getStringArrayExtra("TAGS");
                if (metadata != null) {
                    String artist = metadata[0];
                    String track = metadata[1];
                    LyricsViewFragment lyricsViewFragment = (LyricsViewFragment) getFragmentManager()
                            .findFragmentByTag(LYRICS_FRAGMENT_TAG);
                    lyricsViewFragment.fetchLyrics(artist, track);
                }
            }
            selectItem(0);
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
        SearchFragment sf = (SearchFragment) getFragmentManager().findFragmentByTag(SEARCH_FRAGMENT_TAG);
        if (sf != null && sf.isActiveFragment) { //focus on search
            sf.setSearchQuery(searchQuery);
            sf.refresh();
            sf.onActivityCreated(null);
        } else if (sf == null) { //searchFragment doesn't exist
            sf = new SearchFragment();
            sf.setSearchQuery(searchQuery);
            sf.refresh();
            sf.showTransitionAnim = true;
            LyricsViewFragment activeFragment = ((LyricsViewFragment) getFragmentManager()
                    .findFragmentByTag(LYRICS_FRAGMENT_TAG));
            prepareAnimations(activeFragment);
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.animator.slide_in_end, android.R.animator.fade_out)
                    .add(R.id.main_fragment_container, sf, SEARCH_FRAGMENT_TAG).hide(activeFragment).commit();
        }
    }

    private void processURL(Intent intent) {
        Uri data = intent.getData();
        String scheme = data.getScheme();//get the scheme (http,https)
        String fullPath = data.getEncodedSchemeSpecificPart();//get the full path -scheme - fragments
        String url = scheme + ":" + fullPath;
        if (url.contains("www.azlyrics.com/") ||
                url.contains("lyrics.wikia.com/") ||
                url.contains("genius.com") ||
                url.contains("j-lyric.net"))
            updateLyricsFragment(0, null, null, url);
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
    }

    @Override
    protected void onDestroy() {
        this.destroyed = true;
        if (database != null && database.isOpen())
            database.close();
        super.onDestroy();
    }

    @Override
    public boolean isDestroyed() {
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
    }

    @Override
    public void onBackPressed() {
        SearchFragment searchFragment = (SearchFragment) getFragmentManager().findFragmentByTag(SEARCH_FRAGMENT_TAG);
        if (drawer instanceof DrawerLayout && ((DrawerLayout) drawer).isDrawerOpen(drawerView))
            ((DrawerLayout) drawer).closeDrawer(drawerView);
        else if (searchFragment != null && searchFragment.isActiveFragment) {
            popSearchFragment(searchFragment);
        } else
            finish();
    }

    private void popSearchFragment(SearchFragment searchFragment) {
        if (searchFragment.searchTask != null)
            searchFragment.searchTask.cancel(true);
        searchFragment.isActiveFragment = false;
        searchFragment.showTransitionAnim = true;
        Fragment lyricsFragment = getFragmentManager().findFragmentByTag(LYRICS_FRAGMENT_TAG);
        prepareAnimations(lyricsFragment);
        getFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.animator.fade_in, R.animator.slide_out_end)
                .remove(searchFragment).show(lyricsFragment).commit();
        if (drawer instanceof DrawerLayout) {
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            ((DrawerLayout) drawer).setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
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

    public void setDrawerListener(boolean bool) {
        ((ListView) findViewById(id.drawer_list))
                .setOnItemClickListener(bool ? drawerListener : null);
    }

    private void registerTempReceiver() {
        receiver = new MusicBroadcastReceiver();
        receiver.forceAutoUpdate(true);
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
        final ScreenSlidePagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), this);
        pager.setAdapter(pagerAdapter);
        pager.setOnPageChangeListener(pagerAdapter);
        indicator.setViewPager(pager);
        indicator.setOnPageChangeListener(pagerAdapter);
        Button skipButton = (Button) rootView.findViewById(id.pager_button);
        Button arrowButton = (Button) rootView.findViewById(id.pager_arrow);
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
        if (mDrawerToggle != null) {
            mDrawerToggle.setDrawerIndicatorEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
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

    public void updateLyricsFragment(int outAnim, String... params) { // Should only be called from SearchFragment
        String artist = params[0];
        String song = params[1];
        String url = null;
        if (params.length > 2)
            url = params[2];
        LyricsViewFragment lyricsViewFragment = (LyricsViewFragment) getFragmentManager().findFragmentByTag("LyricsViewFragment");
        if (lyricsViewFragment != null) {
            SearchFragment searchFragment = (SearchFragment) getFragmentManager().findFragmentByTag(SEARCH_FRAGMENT_TAG);
            if (searchFragment != null && searchFragment.isActiveFragment)
                popSearchFragment(searchFragment);
            lyricsViewFragment.fetchLyrics(artist, song, url);
        } else {
            Lyrics lyrics = new Lyrics(Lyrics.SEARCH_ITEM);
            lyrics.setArtist(artist);
            lyrics.setTitle(song);
            lyrics.setURL(url);
            Bundle lyricsBundle = new Bundle();
            try {
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
            fragmentTransaction.add(id.main_fragment_container, lyricsViewFragment, "LyricsViewFragment");
            lyricsViewFragment.isActiveFragment = true;
            fragmentTransaction.commit();
        }
        if (drawer instanceof DrawerLayout && !mDrawerToggle.isDrawerIndicatorEnabled()) {
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            ((DrawerLayout) drawer).setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    public void updateLyricsFragment(int outAnim, int inAnim, boolean transition, Lyrics lyrics) {
        LyricsViewFragment lyricsViewFragment = (LyricsViewFragment) getFragmentManager().findFragmentByTag("LyricsViewFragment");
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(inAnim, outAnim, inAnim, outAnim);
        Fragment activeFragment = getDisplayedFragment(getActiveFragments());
        if (lyricsViewFragment != null && lyricsViewFragment.getView() != null) {
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
                fragmentTransaction.hide(activeFragment).add(id.main_fragment_container, lyricsViewFragment, "LyricsViewFragment");
            else
                fragmentTransaction.replace(id.main_fragment_container, lyricsViewFragment, "LyricsViewFragment");
        }
        fragmentTransaction.commitAllowingStateLoss();
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
                tag = LYRICS_FRAGMENT_TAG;
                newFragment = fragmentManager.findFragmentByTag(tag);
                if (newFragment == null || !(newFragment instanceof LyricsViewFragment))
                    newFragment = new LyricsViewFragment();
                ((LyricsViewFragment) newFragment).showTransitionAnim = true;
                break;
            case 1:
                tag = LOCAL_LYRICS_FRAGMENT_TAG;
                newFragment = fragmentManager.findFragmentByTag(tag);
                if (newFragment == null || !(newFragment instanceof LocalLyricsFragment))
                    newFragment = new LocalLyricsFragment();
                ((LocalLyricsFragment) newFragment).showTransitionAnim = true;
                break;
            case 2:
                tag = SETTINGS_FRAGMENT;
                newFragment = fragmentManager.findFragmentByTag(tag);
                if (newFragment == null || !(newFragment instanceof SettingsFragment))
                    newFragment = new SettingsFragment();
                ((SettingsFragment) newFragment).showTransitionAnim = true;
                break;
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
            fragmentTransaction.commit();
        }
        if (drawer instanceof DrawerLayout && (newFragment == activeFragment))
            ((DrawerLayout) drawer).closeDrawer(drawerView);
    }


    private class DrawerItemClickListener implements
            ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }
}
