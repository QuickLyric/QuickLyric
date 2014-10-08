package be.geecko.QuickLyric;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import be.geecko.QuickLyric.adapter.DrawerAdapter;
import be.geecko.QuickLyric.fragment.LocalLyricsFragment;
import be.geecko.QuickLyric.fragment.LyricsViewFragment;
import be.geecko.QuickLyric.fragment.SearchFragment;
import be.geecko.QuickLyric.fragment.SettingsFragment;
import be.geecko.QuickLyric.lyrics.Lyrics;
import be.geecko.QuickLyric.tasks.DownloadTask;
import be.geecko.QuickLyric.utils.DatabaseHelper;
import be.geecko.QuickLyric.utils.IdDecoder;

import static be.geecko.QuickLyric.R.array;
import static be.geecko.QuickLyric.R.drawable;
import static be.geecko.QuickLyric.R.id;
import static be.geecko.QuickLyric.R.layout;
import static be.geecko.QuickLyric.R.string;

public class MainActivity extends ActionBarActivity {
    // TODO batch saving lyrics from Google Music / Storage (Note : make sure it's easy to go through 10k+ songs in LocalLyricsFragment) (Note2: Make sure I'm allowed to do that)

    // fixme short podcasts? e.g. Tech News Tonight

    private static final String LYRICS_FRAGMENT_TAG = "LyricsViewFragment";
    private static final String MUSIC_ID_FRAGMENT_TAG = "MusicIDFragment";
    private static final String SETTINGS_FRAGMENT = "SettingsFragment";
    private static final String LOCAL_LYRICS_FRAGMENT_TAG = "LocalLyricsFragment";
    private static final String SEARCH_FRAGMENT_TAG = "SearchFragment";
    public View drawer;
    public View drawerView;
    public boolean focusOnFragment = true;
    public ActionMode mActionMode;
    public SQLiteDatabase database;
    private ActionBarDrawerToggle mDrawerToggle;
    private Fragment displayedFragment;

    private static void prepareAnimations(Fragment nextFragment) {
        Class fragmentClass = ((Object) nextFragment).getClass();
        try {
            fragmentClass.getDeclaredField("showTransitionAnim").setBoolean(nextFragment, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final FragmentManager fragmentManager = getSupportFragmentManager();
        setContentView(layout.nav_drawer_activity);
        this.getSupportActionBar().setDisplayUseLogoEnabled(true);

        /** Drawer setup */
        final ListView mDrawerList = (ListView) findViewById(id.drawer_list);
        DrawerAdapter drawerAdapter = new DrawerAdapter(this, this.getResources().getStringArray(array.nav_items));
        mDrawerList.setAdapter(drawerAdapter);
        drawerView = this.findViewById(id.left_drawer);
        drawer = this.findViewById(id.drawer_layout);
        if (drawer instanceof DrawerLayout) { // if phone
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            mDrawerToggle = new ActionBarDrawerToggle(this, (DrawerLayout) drawer, drawable.ic_drawer, string.drawer_open_desc, string.drawer_closed_desc) {

                /**
                 * Called when a drawer has settled in a completely open state.
                 */
                public void onDrawerOpened(View drawerView) {
                    focusOnFragment = false;
                    if (mActionMode != null)
                        mActionMode.finish();
                    MainActivity.this.supportInvalidateOptionsMenu(); // onPrepareOptionsMenu()
                }

                /**
                 * Called when a drawer has settled in a completely closed state.
                 */
                public void onDrawerClosed(View view) {
                    focusOnFragment = true;
                    MainActivity.this.supportInvalidateOptionsMenu(); // onPrepareOptionsMenu()
                }
            };
            ((DrawerLayout) drawer).setDrawerListener(mDrawerToggle);
        }

        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        database = new DatabaseHelper(getApplicationContext()).getReadableDatabase();

        setupSearchBox(fragmentManager);

        LyricsViewFragment lyricsViewFragment = (LyricsViewFragment) fragmentManager.findFragmentByTag(LYRICS_FRAGMENT_TAG);
        if (lyricsViewFragment == null)
            lyricsViewFragment = new LyricsViewFragment();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("pref_animations", true))
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_end, R.anim.slide_out_start, R.anim.slide_in_start, R.anim.slide_out_end);
        if (!lyricsViewFragment.isAdded()) {
            fragmentTransaction.add(id.main_fragment_container, lyricsViewFragment, LYRICS_FRAGMENT_TAG);
        }

        /** ugly */
        Fragment[] activeFragments = getActiveFragments();
        displayedFragment = getDisplayedFragment(activeFragments);
        /** ugly */

        for (Fragment fragment : activeFragments)
            if (fragment != null) {
                if (fragment != displayedFragment && !fragment.isHidden()) {
                    fragmentTransaction.hide(fragment);
                    fragment.onHiddenChanged(true);
                } else if (fragment == displayedFragment)
                    fragmentTransaction.show(fragment);
            }
        Intent intent = getIntent();
        String extra = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (intent.getAction().equals("android.intent.action.SEND")
                && (extra.contains("http://www.soundhound.com/")
                || extra.contains("http://shz.am/"))) {
            new IdDecoder(this, lyricsViewFragment).execute(getIdUrl(extra));
        } else if (intent.getAction().equals("android.intent.action.VIEW")) {
            processURL(intent);
        } else {
            Lyrics receivedLyrics = getBeamedLyrics(intent);
            if (receivedLyrics == null)
                fragmentTransaction.commit();
            else
                updateLyricsFragment(0, 0, false, receivedLyrics);
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
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        Fragment[] fragments = new Fragment[5];
        fragments[0] = fragmentManager.findFragmentByTag(LYRICS_FRAGMENT_TAG);
        fragments[1] = fragmentManager.findFragmentByTag(MUSIC_ID_FRAGMENT_TAG);
        fragments[2] = fragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG);
        fragments[3] = fragmentManager.findFragmentByTag(SETTINGS_FRAGMENT);
        fragments[4] = fragmentManager.findFragmentByTag(LOCAL_LYRICS_FRAGMENT_TAG);
        return fragments;
    }

    @TargetApi(14)
    @Override
    protected void onResume() {
        super.onResume();
        App.activityResumed();
        if (Build.VERSION.SDK_INT >= 14) {
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
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        String extra = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (action != null)
            if (Build.VERSION.SDK_INT >= 14 && action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED))
                getBeamedLyrics(intent);
            else if (intent.getAction().equals("android.intent.action.SEND")
                    && (extra.contains("http://www.soundhound.com/")
                    || extra.contains("http://shz.am/"))) {
                LyricsViewFragment lyricsViewFragment = (LyricsViewFragment) getSupportFragmentManager().findFragmentByTag(LYRICS_FRAGMENT_TAG);
                new IdDecoder(this, lyricsViewFragment).execute(getIdUrl(extra));
            } else if (action.equals("android.intent.action.VIEW"))
                processURL(intent);
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

    private void processURL(Intent intent) {
        Uri data = intent.getData();
        String scheme = data.getScheme();//get the scheme (http,https)
        String fullPath = data.getEncodedSchemeSpecificPart();//get the full path -scheme - fragments
        String url = scheme + ":" + fullPath;
        if (url.contains("www.azlyrics.com/") ||
                url.contains("lyrics.wikia.com/") ||
                url.contains("lyricsnmusic.com"))
            new DownloadTask().execute(this, url);
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

    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        super.onPause();
        App.activityPaused();
        if (Build.VERSION.SDK_INT >= 14) {
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
            if (nfcAdapter != null)
                nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onDestroy() {
        if (database != null && database.isOpen())
            database.close();
        super.onDestroy();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (displayedFragment instanceof LyricsViewFragment) {
            DrawerAdapter drawerAdapter = (DrawerAdapter) ((ListView) findViewById(id.drawer_list)).getAdapter();
            if (drawerAdapter.getSelectedItem() != 0) {
                drawerAdapter.setSelectedItem(0);
                drawerAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onBackPressed() {
        SearchFragment searchFragment = (SearchFragment) getSupportFragmentManager().findFragmentByTag(SEARCH_FRAGMENT_TAG);
        if (drawer instanceof DrawerLayout && ((DrawerLayout) drawer).isDrawerOpen(drawerView))
            ((DrawerLayout) drawer).closeDrawer(drawerView);
        else if (getSupportFragmentManager().getBackStackEntryCount() != 0 && searchFragment != null && searchFragment.isActiveFragment) {
            searchFragment.showTransitionAnim = true;
            FragmentManager.BackStackEntry backStackEntry = getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1);
            prepareAnimations(getSupportFragmentManager().findFragmentByTag(backStackEntry.getName()));
            getSupportFragmentManager().popBackStack();
        } else
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

    private void setupSearchBox(final FragmentManager fragmentManager) {
        EditText searchBox = (EditText) this.findViewById(id.searchBox);
        searchBox.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView tv, int keycode, KeyEvent event) {
                if (event == null)
                    return false;
                final CharSequence boxText = tv.getText();
                if (null != boxText && (boxText.length() > 0) && keycode == KeyEvent.KEYCODE_UNKNOWN && event.getAction() == KeyEvent.ACTION_DOWN) {
                    SearchFragment sf = (SearchFragment) fragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG);
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    if (PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("pref_animations", true))
                        fragmentTransaction.setCustomAnimations(R.anim.slide_in_start, R.anim.slide_out_start, R.anim.slide_in_start, R.anim.slide_out_end);
                    String searchQuery = boxText.toString().replaceAll("(?!\")\\p{Punct}", " ").replaceAll("\\s+", " ");
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(tv.getWindowToken(), 0);
                    imm.showSoftInputFromInputMethod(tv.getWindowToken(), 0);
                    if (drawer instanceof DrawerLayout)
                        ((DrawerLayout) drawer).closeDrawer(drawerView);
                    tv.setText("");
                    if (sf != null && sf.isActiveFragment) { //focus on search
                        sf.setSearchQuery(searchQuery);
                        sf.refresh();
                        sf.onActivityCreated(null);
                    } else if (sf == null) { //focus on something else but no searchfragment
                        sf = new SearchFragment();
                        sf.setSearchQuery(searchQuery);
                        sf.refresh();
                        Fragment activeFragment = getDisplayedFragment(getActiveFragments());
                        prepareAnimations(activeFragment);
                        fragmentTransaction.add(R.id.main_fragment_container, sf, "SearchFragment").hide(activeFragment).addToBackStack(activeFragment.getTag()).commit();
                    } else { //already created a searchfragment
                        sf.setSearchQuery(searchQuery);
                        sf.refresh();
                        sf.showTransitionAnim = true;
                        Fragment activeFragment = getDisplayedFragment(getActiveFragments());
                        prepareAnimations(activeFragment);
                        fragmentTransaction.show(sf).hide(activeFragment).addToBackStack(activeFragment.getTag()).commit();
                        sf.onActivityCreated(null);
                    }
                    return true;
                } else
                    return false;
            }
        });
    }

    public void updateLyricsFragment(int outAnim, String... params) {
        String artist = params[0];
        String song = params[1];
        String url = null;
        if (params.length > 2)
            url = params[2];
        LyricsViewFragment lyricsViewFragment = (LyricsViewFragment) getSupportFragmentManager().findFragmentByTag("LyricsViewFragment");
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("pref_animations", true))
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_start, outAnim);
        Fragment activeFragment = getDisplayedFragment(getActiveFragments());
        prepareAnimations(activeFragment);
        if (lyricsViewFragment != null) {
            fragmentTransaction.hide(activeFragment).show(lyricsViewFragment);
            lyricsViewFragment.fetchLyrics(artist, song, url);
        } else {
            Lyrics lyrics = new Lyrics(Lyrics.SEARCH_ITEM);
            lyrics.setArtist(artist);
            lyrics.setTitle(song);
            Bundle lyricsBundle = new Bundle();
            try {
                lyricsBundle.putByteArray("lyrics", lyrics.toBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            lyricsViewFragment = new LyricsViewFragment();
            lyricsViewFragment.setArguments(lyricsBundle);
            fragmentTransaction.hide(activeFragment).add(id.main_fragment_container, lyricsViewFragment, "LyricsViewFragment");
        }
        //lyricsViewFragment.showTransitionAnim = true;
        lyricsViewFragment.isActiveFragment = true;
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void updateLyricsFragment(int outAnim, int inAnim, boolean transition, Lyrics lyrics) {
        LyricsViewFragment lyricsViewFragment = (LyricsViewFragment) getSupportFragmentManager().findFragmentByTag("LyricsViewFragment");
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("pref_animations", true))
            fragmentTransaction.setCustomAnimations(inAnim, outAnim);
        Fragment activeFragment = getDisplayedFragment(getActiveFragments());
        if (lyricsViewFragment != null) {
            lyricsViewFragment.update(lyrics, lyricsViewFragment.getView());
            if (transition) {
                fragmentTransaction.hide(activeFragment).show(lyricsViewFragment);
                prepareAnimations(activeFragment);
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
        //lyricsViewFragment.showTransitionAnim = true;
        fragmentTransaction.commit();
        lyricsViewFragment.isActiveFragment = true;
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
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment newFragment;
        String tag;
        switch (position) {
            default:
                tag = "LyricsViewFragment";
                newFragment = fragmentManager.findFragmentByTag(tag);
                if (newFragment == null || !(newFragment instanceof LyricsViewFragment))
                    newFragment = new LyricsViewFragment();
                ((LyricsViewFragment) newFragment).showTransitionAnim = true;
                break;
            case 1:
                tag = "LocalLyricsFragment";
                newFragment = fragmentManager.findFragmentByTag(tag);
                if (newFragment == null || !(newFragment instanceof LocalLyricsFragment))
                    newFragment = new LocalLyricsFragment();
                ((LocalLyricsFragment) newFragment).showTransitionAnim = true;
                break;
            case 2:
                tag = "SettingsFragment";
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
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_animations", true))
                fragmentTransaction.setCustomAnimations(R.anim.slide_in_start, R.anim.slide_out_start, R.anim.slide_in_start, R.anim.slide_out_start);
            fragmentTransaction.hide(activeFragment);
            if (newFragment.isAdded())
                fragmentTransaction.show(newFragment);
            else
                fragmentTransaction.add(id.main_fragment_container, newFragment, tag);
            fragmentTransaction.commit();
        }
        if (drawer instanceof DrawerLayout && (newFragment == activeFragment || !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_animations", true)))
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
