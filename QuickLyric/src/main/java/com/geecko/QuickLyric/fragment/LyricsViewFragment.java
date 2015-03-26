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

package com.geecko.QuickLyric.fragment;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.adapter.DrawerAdapter;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.tasks.CoverArtLoader;
import com.geecko.QuickLyric.tasks.DownloadThread;
import com.geecko.QuickLyric.tasks.ParseTask;
import com.geecko.QuickLyric.tasks.PresenceChecker;
import com.geecko.QuickLyric.tasks.WriteToDatabaseTask;
import com.geecko.QuickLyric.utils.CoverCache;
import com.geecko.QuickLyric.utils.CustomSelectionCallback;
import com.geecko.QuickLyric.utils.LyricsTextFactory;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;
import com.geecko.QuickLyric.view.FadeInNetworkImageView;
import com.geecko.QuickLyric.view.ObservableScrollView;
import com.geecko.QuickLyric.view.RefreshIcon;
import com.melnykov.fab.FloatingActionButton;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;

public class LyricsViewFragment extends Fragment implements ObservableScrollView.Callbacks, Lyrics.Callback {

    private static final int STATE_ONSCREEN = 0;
    private int mState = STATE_ONSCREEN;
    private static final int STATE_OFFSCREEN = 1;
    private static final int STATE_RETURNING = 2;
    private static BroadcastReceiver broadcastReceiver;
    public boolean lyricsPresentInDB;
    public boolean isActiveFragment = false;
    public boolean showTransitionAnim = true;
    private Lyrics mLyrics;
    private String mSearchQuery;
    private boolean mSearchFocused;
    private int minFrameRawY = 0;
    private FrameLayout mFrame;
    private ObservableScrollView mObservableScrollView;

    public LyricsViewFragment() {
    }

    public static void sendIntent(Context context, Intent intent) {
        if (broadcastReceiver != null)
            broadcastReceiver.onReceive(context, intent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mLyrics != null)
            try {
                outState.putByteArray("lyrics", mLyrics.toBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        View searchView = getActivity().findViewById(R.id.search_view);
        if (searchView instanceof SearchView) {
            outState.putString("searchQuery", ((SearchView) searchView).getQuery().toString());
            outState.putBoolean("searchFocused", searchView.hasFocus());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        setHasOptionsMenu(true);
        View layout = inflater.inflate(R.layout.lyrics_view, container, false);
        if (savedInstanceState != null)
            try {
                Lyrics l = Lyrics.fromBytes(savedInstanceState.getByteArray("lyrics"));
                if (l != null)
                    this.mLyrics = l;
                mSearchQuery = savedInstanceState.getString("searchQuery");
                mSearchFocused = savedInstanceState.getBoolean("searchFocused");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        else {
            Bundle args = getArguments();
            if (args != null)
                try {
                    Lyrics lyrics = Lyrics.fromBytes(args.getByteArray("lyrics"));
                    this.mLyrics = lyrics;
                    if (lyrics.getText() == null) {
                        String artist = lyrics.getArtist();
                        String track = lyrics.getTrack();
                        String url = lyrics.getURL();
                        fetchLyrics(artist, track, url);
                        ((RefreshIcon) layout.findViewById(R.id.refresh_fab)).startAnimation();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
        }
        if (layout != null) {
            TextSwitcher textSwitcher = (TextSwitcher) layout.findViewById(R.id.switcher);
            textSwitcher.setFactory(new LyricsTextFactory(layout.getContext()));
            ActionMode.Callback callback = new CustomSelectionCallback(getActivity());
            ((TextView) textSwitcher.getChildAt(0)).setCustomSelectionActionModeCallback(callback);
            ((TextView) textSwitcher.getChildAt(1)).setCustomSelectionActionModeCallback(callback);

            FadeInNetworkImageView cover = (FadeInNetworkImageView) layout.findViewById(R.id.cover);
            cover.setDefaultImageResId(R.drawable.no_cover);
            cover.setErrorImageResId(android.R.drawable.ic_menu_close_clear_cancel);

            mFrame = (FrameLayout) layout.findViewById(R.id.frame);
            final RefreshIcon refreshFab = (RefreshIcon) layout.findViewById(R.id.refresh_fab);
            refreshFab.setShadow(layout.findViewById(R.id.fab_shadow));
            refreshFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fetchCurrentLyrics(true);
                }
            });

            mObservableScrollView = (ObservableScrollView) layout.findViewById(R.id.scrollview);
            mObservableScrollView.setCallbacks(this);

            ((FloatingActionButton) layout.findViewById(R.id.refresh_fab)).attachToScrollView(mObservableScrollView);


            if (mLyrics == null)
                fetchCurrentLyrics(false);
            else if (mLyrics.getFlag() == Lyrics.SEARCH_ITEM) {
                startRefreshAnimation();
                if (mLyrics.getArtist() != null)
                    fetchLyrics(mLyrics.getArtist(), mLyrics.getTrack());
                ((TextView) (layout.findViewById(R.id.artist))).setText(mLyrics.getArtist());
                ((TextView) (layout.findViewById(R.id.song))).setText(mLyrics.getTrack());
            } else //Rotation, resume
                update(mLyrics, layout, false);
        }
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String artist = intent.getStringExtra("artist");
                String track = intent.getStringExtra("track");
                if (artist != null && track != null) {
                    startRefreshAnimation();
                    new ParseTask(LyricsViewFragment.this, false).execute(mLyrics);
                }
            }
        };
        return layout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (this.isHidden())
            return;

        DrawerAdapter drawerAdapter = ((DrawerAdapter) ((ListView) this.getActivity().findViewById(R.id.drawer_list)).getAdapter());
        if (drawerAdapter.getSelectedItem() != 0) {
            drawerAdapter.setSelectedItem(0);
            drawerAdapter.notifyDataSetChanged();
        }
        this.isActiveFragment = true;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            this.onViewCreated(getView(), null);
            if (mLyrics != null && mLyrics.getFlag() == Lyrics.POSITIVE_RESULT && lyricsPresentInDB)
                new PresenceChecker().execute(this, new String[]{mLyrics.getArtist(), mLyrics.getTrack()});
        } else
            this.isActiveFragment = false;
    }

    public void startRefreshAnimation() {
        if (getActivity() != null && getView() != null)
            ((RefreshIcon) getActivity().findViewById(R.id.refresh_fab)).startAnimation();
    }

    void stopRefreshAnimation() {
        RefreshIcon refreshIcon = (RefreshIcon) getActivity().findViewById(R.id.refresh_fab);
        if (refreshIcon != null)
            refreshIcon.stopAnimation();
    }

    public void fetchLyrics(String... params) {
        String artist = params[0];
        String song = params[1];
        String url = null;
        if (params.length > 2)
            url = params[2];
        this.startRefreshAnimation();
        Set<String> providersSet = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getStringSet("pref_providers", Collections.EMPTY_SET);
        DownloadThread.refreshProviders(providersSet);

        if (url == null)
            new DownloadThread(this, artist, song).start();
        else
            new DownloadThread(this, url, artist, song).start();
    }

    public void fetchCurrentLyrics(boolean showMsg) {
        if (mLyrics != null && mLyrics.getArtist() != null && mLyrics.getTrack() != null)
            new ParseTask(this, showMsg).execute(mLyrics);
        else
            new ParseTask(this, showMsg).execute((Object) null);
    }

    @TargetApi(16)
    private void beamLyrics(final Lyrics lyrics, Activity activity) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            if (lyrics.getText() != null) {
                nfcAdapter.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
                    @Override
                    public NdefMessage createNdefMessage(NfcEvent event) {
                        try {
                            byte[] payload = lyrics.toBytes(); // whatever data you want to send
                            NdefRecord record = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "application/lyrics".getBytes(), new byte[0], payload);
                            return new NdefMessage(new NdefRecord[]{
                                    record, // your data
                                    NdefRecord.createApplicationRecord("com.geecko.QuickLyric"), // the "application record"
                            });
                        } catch (IOException e) {
                            return null;
                        }
                    }
                }, activity);
            }
        }
    }

    @Override
    public void onLyricsDownloaded(Lyrics lyrics) {
        if (getActivity() != null && !((MainActivity) getActivity()).hasBeenDestroyed())
            update(lyrics, getView(), true);
    }

    public void update(Lyrics lyrics, View layout, boolean animation) {
        new CoverArtLoader().execute(lyrics, this);
        TextSwitcher textSwitcher = ((TextSwitcher) layout.findViewById(R.id.switcher));
        TextView artistTV = ((TextView) layout.findViewById(R.id.artist));
        TextView songTV = (TextView) layout.findViewById(R.id.song);
        RelativeLayout bugLayout = (RelativeLayout) layout.findViewById(R.id.error_msg);
        this.mLyrics = lyrics;
        if (SDK_INT >= ICE_CREAM_SANDWICH)
            beamLyrics(lyrics, this.getActivity());
        new PresenceChecker().execute(this, new String[]{lyrics.getArtist(), lyrics.getTrack()});

        if (lyrics.getArtist() != null)
            artistTV.setText(lyrics.getArtist());
        else
            artistTV.setText("");
        if (lyrics.getTrack() != null)
            songTV.setText(lyrics.getTrack());
        else
            songTV.setText("");
        ((FloatingActionButton) layout.findViewById(R.id.refresh_fab)).show();

        if (lyrics.getFlag() == Lyrics.POSITIVE_RESULT) {
            if (animation)
                textSwitcher.setText(Html.fromHtml(lyrics.getText()));
            else
                textSwitcher.setCurrentText(Html.fromHtml(lyrics.getText()));

            bugLayout.setVisibility(View.INVISIBLE);
            mObservableScrollView.post(new Runnable() {
                @Override
                public void run() {
                    mObservableScrollView.scrollTo(0, 0); //only useful when coming from localLyricsFragment
                    mObservableScrollView.smoothScrollTo(0, 0);
                }
            });
        } else {
            textSwitcher.setText("");
            bugLayout.setVisibility(View.VISIBLE);
            int message;
            int whyVisibility;
            if (lyrics.getFlag() == Lyrics.ERROR || !OnlineAccessVerifier.check(getActivity())) {
                message = R.string.connection_error;
                whyVisibility = TextView.GONE;
            } else {
                message = R.string.no_results;
                whyVisibility = TextView.VISIBLE;
            }
            TextView whyTextView = ((TextView) bugLayout.findViewById(R.id.bugtext_why));
            ((TextView) bugLayout.findViewById(R.id.bugtext)).setText(message);
            whyTextView.setVisibility(whyVisibility);
            whyTextView.setPaintFlags(whyTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }
        stopRefreshAnimation();
        getActivity().getIntent().setAction("");
    }

    public void showWhyPopup() {
        String title = mLyrics.getTrack();
        String artist = mLyrics.getArtist();
        new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.why_popup_title))
                .setMessage(Html.fromHtml(String.format(getString(R.string.why_popup_text), title, artist)))
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share_action:
                final Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                if (mLyrics != null && mLyrics.getURL() != null) {
                    sendIntent.putExtra(Intent.EXTRA_TEXT, mLyrics.getURL());
                    startActivity(Intent.createChooser(sendIntent, getString(R.string.share)));
                }
                return true;
            case R.id.save_action:
                if (mLyrics != null && mLyrics.getFlag() == Lyrics.POSITIVE_RESULT)
                    new WriteToDatabaseTask().execute(this, item, this.mLyrics);
                break;
        }
        return false;
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        final MainActivity mainActivity = (MainActivity) getActivity();
        Animator anim = null;
        if (showTransitionAnim) {
            if (nextAnim != 0)
                anim = AnimatorInflater.loadAnimator(getActivity(), nextAnim);
            showTransitionAnim = false;
            if (anim != null)
                anim.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        if (mainActivity.drawer instanceof DrawerLayout)
                            ((DrawerLayout) mainActivity.drawer).closeDrawer(mainActivity.drawerView);
                        mainActivity.setDrawerListener(true);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                    }

                    @Override
                    public void onAnimationStart(Animator animator) {
                        mainActivity.setDrawerListener(false);
                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {
                    }
                });
        } else
            anim = AnimatorInflater.loadAnimator(getActivity(), R.animator.none);
        return anim;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MainActivity mainActivity = (MainActivity) this.getActivity();
        ActionBar actionBar = mainActivity.getSupportActionBar();
        actionBar.setTitle(R.string.app_name);

        if (!mainActivity.focusOnFragment) // focus is on Fragment
            return;

        inflater.inflate(R.menu.lyrics, menu);
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getActivity()
                .getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.search_view);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem searchItem) {
                searchView.requestFocus();
                searchView.post(new Runnable() {
                    @Override
                    public void run() {
                        ((InputMethodManager) getActivity()
                                .getSystemService(Context.INPUT_METHOD_SERVICE))
                                .toggleSoftInput(InputMethodManager.SHOW_FORCED,
                                        InputMethodManager.HIDE_IMPLICIT_ONLY);
                    }
                });
                return true;
            }
        });
        if (mSearchQuery != null) {
            searchItem.expandActionView();
            searchView.setQuery(mSearchQuery, false);
            if (mSearchFocused)
                searchView.requestFocus();
        }
        MenuItem saveMenuItem = menu.findItem(R.id.save_action);
        if (saveMenuItem != null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            if (mLyrics != null
                    && mLyrics.getFlag() == Lyrics.POSITIVE_RESULT
                    && sharedPref.getBoolean("pref_auto_save", false)
                    && !lyricsPresentInDB) {
                lyricsPresentInDB = true;
                new WriteToDatabaseTask().execute(this, saveMenuItem, mLyrics);
            } else {
                saveMenuItem.setIcon(lyricsPresentInDB ? R.drawable.ic_trash : R.drawable.ic_save);
                saveMenuItem.setTitle(lyricsPresentInDB ? R.string.remove_action : R.string.save_action);
            }
        }
    }

    public void setCoverArt(String url, FadeInNetworkImageView cover) {
        MainActivity mainActivity = (MainActivity) LyricsViewFragment.this.getActivity();
        if (mainActivity == null)
            return;
        if (cover == null)
            cover = (FadeInNetworkImageView) mainActivity.findViewById(R.id.cover);
        if (mLyrics != null) {
            if (url != null && !url.equals(cover.getImageURL()))
                cover.setImageResource(R.drawable.no_cover);
            mLyrics.setCoverURL(url);
            if (url == null)
                url = "";
            cover.setImageUrl(url,
                    new ImageLoader(Volley.newRequestQueue(mainActivity), CoverCache.instance()));
        }
    }

    @Override
    public void onScrollChanged() {
        int cachedVerticalScrollRange = mObservableScrollView.computeVerticalScrollRange();
        int quickReturnHeight = mFrame.getMeasuredHeight();
        int rawFrameY = mFrame.getTop() - Math.min(
                cachedVerticalScrollRange - mObservableScrollView.getHeight(),
                mObservableScrollView.getScrollY());
        int frameTranslationY = 0;

        switch (mState) {
            case STATE_OFFSCREEN:
                if (rawFrameY <= minFrameRawY) {
                    minFrameRawY = rawFrameY;
                } else {
                    mState = STATE_RETURNING;
                }
                frameTranslationY = rawFrameY;
                break;

            case STATE_ONSCREEN:
                if (rawFrameY < -quickReturnHeight) {
                    mState = STATE_OFFSCREEN;
                    minFrameRawY = rawFrameY;
                }
                frameTranslationY = rawFrameY;
                break;

            case STATE_RETURNING:
                frameTranslationY = (rawFrameY - minFrameRawY) - quickReturnHeight;
                if (frameTranslationY > 0) {
                    frameTranslationY = 0;
                    minFrameRawY = rawFrameY - quickReturnHeight;
                }

                if (rawFrameY > 0) {
                    mState = STATE_ONSCREEN;
                    frameTranslationY = rawFrameY;
                }

                if (frameTranslationY < -quickReturnHeight) {
                    mState = STATE_OFFSCREEN;
                    minFrameRawY = rawFrameY;
                }
                break;
        }
        if (mObservableScrollView.getScrollY() != 0) {
            frameTranslationY += mObservableScrollView.getScrollY();
        } else {
            frameTranslationY = 0;
        }
        mFrame.setTranslationY(frameTranslationY);
    }
}
