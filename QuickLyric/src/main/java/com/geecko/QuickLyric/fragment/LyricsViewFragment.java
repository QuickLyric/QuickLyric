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
import android.app.Fragment;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.InputType;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.adapter.DrawerAdapter;
import com.geecko.QuickLyric.broadcastReceiver.MusicBroadcastReceiver;
import com.geecko.QuickLyric.lyrics.Lyrics;
import com.geecko.QuickLyric.tasks.CoverArtLoader;
import com.geecko.QuickLyric.tasks.DownloadThread;
import com.geecko.QuickLyric.tasks.Id3Reader;
import com.geecko.QuickLyric.tasks.Id3Writer;
import com.geecko.QuickLyric.tasks.ParseTask;
import com.geecko.QuickLyric.tasks.PresenceChecker;
import com.geecko.QuickLyric.tasks.WriteToDatabaseTask;
import com.geecko.QuickLyric.utils.CoverCache;
import com.geecko.QuickLyric.utils.CustomSelectionCallback;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.LyricsTextFactory;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;
import com.geecko.QuickLyric.utils.PermissionsChecker;
import com.geecko.QuickLyric.view.ControllableAppBarLayout;
import com.geecko.QuickLyric.view.FadeInNetworkImageView;
import com.geecko.QuickLyric.view.LrcView;
import com.geecko.QuickLyric.view.RefreshIcon;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;

public class LyricsViewFragment extends Fragment implements Lyrics.Callback, SwipeRefreshLayout.OnRefreshListener {

    private static BroadcastReceiver broadcastReceiver;
    public boolean lyricsPresentInDB;
    public boolean isActiveFragment = false;
    public boolean showTransitionAnim = true;
    private Lyrics mLyrics;
    private String mSearchQuery;
    private boolean mSearchFocused;
    private NestedScrollView mScrollView;
    private Activity mActivity;
    private MenuItem searchItem;
    private boolean startEmtpy = false;
    public boolean searchResultLock;
    private SwipeRefreshLayout mRefreshLayout;

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

        outState.putBoolean("refreshFabEnabled", getActivity().findViewById(R.id.refresh_fab).isEnabled());

        EditText editedLyrics = (EditText) getActivity().findViewById(R.id.edit_lyrics);
        if (editedLyrics.getVisibility() == View.VISIBLE) {
            EditText editedTitle = (EditText) getActivity().findViewById(R.id.song);
            EditText editedArtist = (EditText) getActivity().findViewById(R.id.artist);
            outState.putCharSequence("editedLyrics", editedLyrics.getText());
            outState.putCharSequence("editedTitle", editedTitle.getText());
            outState.putCharSequence("editedArtist", editedArtist.getText());
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
                    if (lyrics != null && lyrics.getText() == null && lyrics.getArtist() != null) {
                        String artist = lyrics.getArtist();
                        String track = lyrics.getTrack();
                        String url = lyrics.getURL();
                        fetchLyrics(artist, track, url);
                        ((SwipeRefreshLayout) layout.findViewById(R.id.refresh_layout)).setRefreshing(true);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
        }
        if (layout != null) {
            Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();

            boolean screenOn = PreferenceManager
                    .getDefaultSharedPreferences(getActivity()).getBoolean("pref_force_screen_on", false);

            TextSwitcher textSwitcher = (TextSwitcher) layout.findViewById(R.id.switcher);
            textSwitcher.setFactory(new LyricsTextFactory(layout.getContext()));
            ActionMode.Callback callback = new CustomSelectionCallback(getActivity());
            ((TextView) textSwitcher.getChildAt(0)).setCustomSelectionActionModeCallback(callback);
            ((TextView) textSwitcher.getChildAt(1)).setCustomSelectionActionModeCallback(callback);
            textSwitcher.setKeepScreenOn(screenOn);
            layout.findViewById(R.id.lrc_view).setKeepScreenOn(screenOn);

            if (args != null && args.containsKey("editedLyrics")) {
                EditText editedLyrics = (EditText) layout.findViewById(R.id.edit_lyrics);
                EditText artistTV = (EditText) getActivity().findViewById(R.id.artist);
                EditText songTV = (EditText) getActivity().findViewById(R.id.song);
                textSwitcher.setVisibility(View.GONE);
                editedLyrics.setVisibility(View.VISIBLE);
                songTV.setInputType(InputType.TYPE_CLASS_TEXT);
                artistTV.setInputType(InputType.TYPE_CLASS_TEXT);
                songTV.setBackgroundResource(R.drawable.abc_textfield_search_material);
                artistTV.setBackgroundResource(R.drawable.abc_textfield_search_material);
                editedLyrics.setText(args.getCharSequence("editedLyrics"), TextView.BufferType.EDITABLE);
                songTV.setText(args.getCharSequence("editedTitle"), TextView.BufferType.EDITABLE);
                artistTV.setText(args.getCharSequence("editedArtist"), TextView.BufferType.EDITABLE);
            }

            TextView id3TV = (TextView) layout.findViewById(R.id.id3_tv);
            SpannableString text = new SpannableString(id3TV.getText());
            text.setSpan(new UnderlineSpan(), 1, text.length() - 1, 0);
            id3TV.setText(text);

            final RefreshIcon refreshFab = (RefreshIcon) getActivity().findViewById(R.id.refresh_fab);
            refreshFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fetchCurrentLyrics(true);
                }
            });
            if (args != null)
                refreshFab.setEnabled(args.getBoolean("refreshFabEnabled"));

            mScrollView = (NestedScrollView) layout.findViewById(R.id.scrollview);
            mRefreshLayout = (SwipeRefreshLayout) layout.findViewById(R.id.refresh_layout);
            TypedValue primaryColor = new TypedValue();
            getActivity().getTheme().resolveAttribute(R.attr.colorPrimary, primaryColor, true);
            mRefreshLayout.setColorSchemeResources(primaryColor.resourceId, R.color.accent);
            float offset = getResources().getDisplayMetrics().density * 64;
            mRefreshLayout.setProgressViewEndTarget(true, (int) offset);
            mRefreshLayout.setOnRefreshListener(this);

            final ImageButton editTagsButton = (ImageButton) getActivity().findViewById(R.id.edit_tags_btn);

            View.OnClickListener startEditClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startEditTagsMode();
                    final View.OnClickListener startEditClickListener = this;
                    editTagsButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            exitEditTagsMode();
                            editTagsButton.setOnClickListener(startEditClickListener);
                        }
                    });
                }
            };
            editTagsButton.setOnClickListener(startEditClickListener);

            if (mLyrics == null) {
                if (!startEmtpy)
                    fetchCurrentLyrics(false);
            } else if (mLyrics.getFlag() == Lyrics.SEARCH_ITEM) {
                startRefreshAnimation();
                if (mLyrics.getArtist() != null)
                    fetchLyrics(mLyrics.getArtist(), mLyrics.getTrack());
                ((TextView) (getActivity().findViewById(R.id.artist))).setText(mLyrics.getArtist());
                ((TextView) (getActivity().findViewById(R.id.song))).setText(mLyrics.getTrack());
            } else //Rotation, resume
                update(mLyrics, layout, false);
        }
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                searchResultLock = false;
                String artist = intent.getStringExtra("artist");
                String track = intent.getStringExtra("track");
                if (artist != null && track != null && mRefreshLayout.isEnabled()) {
                    startRefreshAnimation();
                    new ParseTask(LyricsViewFragment.this, false, true).execute(mLyrics);
                }
            }
        };
        return layout;
    }

    private void startEditTagsMode() {
        ImageButton editButton = (ImageButton) getActivity().findViewById(R.id.edit_tags_btn);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            editButton.setImageResource(R.drawable.ic_edit_anim);
            ((Animatable) editButton.getDrawable()).start();
        } else
            editButton.setImageResource(R.drawable.ic_done);

        ((DrawerLayout) ((MainActivity) getActivity()).drawer).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mRefreshLayout.setEnabled(false);
        getActivity().findViewById(R.id.refresh_fab).setEnabled(false);
        ((RefreshIcon) getActivity().findViewById(R.id.refresh_fab)).hide();
        ((Toolbar) getActivity().findViewById(R.id.toolbar)).getMenu().clear();

        TextSwitcher textSwitcher = ((TextSwitcher) getActivity().findViewById(R.id.switcher));
        EditText songTV = (EditText) getActivity().findViewById(R.id.song);
        TextView artistTV = ((TextView) getActivity().findViewById(R.id.artist));

        EditText newLyrics = (EditText) getActivity().findViewById(R.id.edit_lyrics);
        newLyrics.setText(((TextView) textSwitcher.getCurrentView()).getText(), TextView.BufferType.EDITABLE);

        textSwitcher.setVisibility(View.GONE);
        newLyrics.setVisibility(View.VISIBLE);

        songTV.setInputType(InputType.TYPE_CLASS_TEXT);
        artistTV.setInputType(InputType.TYPE_CLASS_TEXT);
        songTV.setBackgroundResource(R.drawable.abc_textfield_search_material);
        artistTV.setBackgroundResource(R.drawable.abc_textfield_search_material);


        if (songTV.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void exitEditTagsMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ((ImageButton) getActivity().findViewById(R.id.edit_tags_btn)).setImageResource(R.drawable.ic_done_anim);
            Drawable editIcon = ((ImageButton) getActivity().findViewById(R.id.edit_tags_btn)).getDrawable();
            ((Animatable) editIcon).start();
        } else
            ((ImageButton) getActivity().findViewById(R.id.edit_tags_btn)).setImageResource(R.drawable.ic_edit);

        if (getActivity().getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm.isAcceptingText())
                imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }

        EditText songTV = (EditText) getActivity().findViewById(R.id.song);
        EditText artistTV = ((EditText) getActivity().findViewById(R.id.artist));
        EditText newLyrics = (EditText) getActivity().findViewById(R.id.edit_lyrics);

        songTV.setInputType(InputType.TYPE_NULL);
        artistTV.setInputType(InputType.TYPE_NULL);
        songTV.setBackgroundColor(Color.TRANSPARENT);
        artistTV.setBackgroundColor(Color.TRANSPARENT);

        String txt = mLyrics.getText();
        if (txt == null)
            txt = "";

        File musicFile = Id3Reader.getFile(getActivity(), mLyrics.getOriginalArtist(), mLyrics.getOriginalTrack());

        if (!mLyrics.getArtist().equals(artistTV.getText().toString())
                || !mLyrics.getTrack().equals(songTV.getText().toString())
                || !Html.fromHtml(txt).toString().equals(newLyrics.getText().toString())) {
            mLyrics.setArtist(artistTV.getText().toString());
            mLyrics.setTitle(songTV.getText().toString());
            mLyrics.setText(newLyrics.getText().toString().replaceAll("\n", "<br/>"));
            if (PermissionsChecker.requestPermission(getActivity(),
                    "android.permission.WRITE_EXTERNAL_STORAGE", 0, Id3Writer.REQUEST_CODE))
                new Id3Writer(this).execute(mLyrics, musicFile);
        } else
            new Id3Writer(this).onPreExecute();
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
        if (mRefreshLayout == null)
            if (getActivity() != null && getView() != null)
                mRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.refresh_layout);
        if (mRefreshLayout != null)
            mRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mRefreshLayout.setRefreshing(true);
                }
            });
    }

    public void stopRefreshAnimation() {
        if (mRefreshLayout == null)
            if (getActivity() != null && getView() != null)
                mRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.refresh_layout);
        if (mRefreshLayout != null)
            mRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mRefreshLayout.setRefreshing(false);
                }
            });
    }

    public void fetchLyrics(String... params) {
        String artist = params[0];
        String title = params[1];
        String url = null;
        if (params.length > 2)
            url = params[2];
        startRefreshAnimation();

        Lyrics lyrics;
        if (artist != null && title != null) {
            lyrics = DatabaseHelper.get(((MainActivity) getActivity()).database, new String[]{artist, title});
            if (lyrics == null)
                lyrics = DatabaseHelper.get(((MainActivity) getActivity()).database, new String[]{artist, title});
            if (lyrics == null)
                lyrics = DatabaseHelper.get(((MainActivity) getActivity()).database, DownloadThread.correctTags(artist, title));

            if (lyrics == null && url == null &&
                    (getActivity().getSharedPreferences("tutorial", Context.MODE_PRIVATE).getBoolean("seen", false))
                    && (mLyrics == null || mLyrics.getFlag() != Lyrics.POSITIVE_RESULT ||
                    !("Storage".equals(mLyrics.getSource())
                            && mLyrics.getArtist().equalsIgnoreCase(artist)
                            && mLyrics.getTrack().equalsIgnoreCase(title))
            ))
                lyrics = Id3Reader.getLyrics(getActivity(), artist, title);
        } else {
            showFirstStart();
            return;
        }
        if (lyrics != null)
            onLyricsDownloaded(lyrics);
        else if (OnlineAccessVerifier.check(getActivity())) {
            Set<String> providersSet = PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getStringSet("pref_providers", Collections.<String>emptySet());
            DownloadThread.refreshProviders(providersSet);

            if (mLyrics == null) {
                TextView artistTV = (TextView) getActivity().findViewById(R.id.artist);
                TextView songTV = (TextView) getActivity().findViewById(R.id.song);
                artistTV.setText(artist);
                songTV.setText(title);
            }

            if (url == null)
                new DownloadThread(this, artist, title).start();
            else
                new DownloadThread(this, url, artist, title).start();
        } else {
            lyrics = new Lyrics(Lyrics.ERROR);
            lyrics.setArtist(artist);
            lyrics.setTitle(title);
            onLyricsDownloaded(lyrics);
        }
    }

    public void fetchCurrentLyrics(boolean showMsg) {
        searchResultLock = false;
        getActivity().findViewById(R.id.edit_tags_btn).setVisibility(View.GONE);
        if (mLyrics != null && mLyrics.getArtist() != null && mLyrics.getTrack() != null)
            new ParseTask(this, showMsg, false).execute(mLyrics);
        else
            new ParseTask(this, showMsg, false).execute((Object) null);
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
        if (getActivity() != null && !((MainActivity) getActivity()).hasBeenDestroyed() && getView() != null)
            update(lyrics, getView(), true);
        else
            mLyrics = lyrics;
    }

    public void update(Lyrics lyrics, View layout, boolean animation) {
        File musicFile = null;
        Bitmap cover = null;
        if (PermissionsChecker.hasPermission(getActivity(), "android.permission.READ_EXTERNAL_STORAGE")) {
            musicFile = Id3Reader.getFile(getActivity(), lyrics.getOriginalArtist(), lyrics.getOriginalTrack());
            cover = Id3Reader.getCover(getActivity(), lyrics.getArtist(), lyrics.getTrack());
        }
        setCoverArt(cover, null);
        if (cover == null)
            new CoverArtLoader().execute(lyrics, this);
        if (!lyrics.isLRC())
            getActivity().findViewById(R.id.edit_tags_btn).setVisibility(musicFile == null ? View.GONE : View.VISIBLE);
        TextSwitcher textSwitcher = ((TextSwitcher) layout.findViewById(R.id.switcher));
        LrcView lrcView = (LrcView) layout.findViewById(R.id.lrc_view);
        View v = getActivity().findViewById(R.id.tracks_msg);
        if (v != null)
            ((ViewGroup) v.getParent()).removeView(v);
        TextView artistTV = (TextView) getActivity().findViewById(R.id.artist);
        TextView songTV = (TextView) getActivity().findViewById(R.id.song);
        TextView id3TV = (TextView) layout.findViewById(R.id.id3_tv);
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
        if (isActiveFragment)
            ((RefreshIcon) getActivity().findViewById(R.id.refresh_fab)).show();
        EditText newLyrics = (EditText) getActivity().findViewById(R.id.edit_lyrics);
        if (newLyrics != null)
            newLyrics.setText("");

        if (lyrics.getFlag() == Lyrics.POSITIVE_RESULT) {
            if (!lyrics.isLRC()) {
                textSwitcher.setVisibility(View.VISIBLE);
                lrcView.setVisibility(View.GONE);
                if (animation)
                    textSwitcher.setText(Html.fromHtml(lyrics.getText()));
                else
                    textSwitcher.setCurrentText(Html.fromHtml(lyrics.getText()));
            } else {
                textSwitcher.setVisibility(View.GONE);
                lrcView.setVisibility(View.VISIBLE);
                lrcView.setSourceLrc(lyrics.getText());
                if (isActiveFragment)
                    ((ControllableAppBarLayout) getActivity().findViewById(R.id.appbar)).expandToolbar(true);
                new Thread(lrcUpdater).start();
            }

            bugLayout.setVisibility(View.INVISIBLE);
            if ("Storage".equals(lyrics.getSource()))
                id3TV.setVisibility(View.VISIBLE);
            else
                id3TV.setVisibility(View.GONE);
            mScrollView.post(new Runnable() {
                @Override
                public void run() {
                    mScrollView.scrollTo(0, 0); //only useful when coming from localLyricsFragment
                    mScrollView.smoothScrollTo(0, 0);
                }
            });
        } else {
            textSwitcher.setText("");
            textSwitcher.setVisibility(View.INVISIBLE);
            lrcView.setVisibility(View.INVISIBLE);
            bugLayout.setVisibility(View.VISIBLE);
            int message;
            int whyVisibility;
            if (lyrics.getFlag() == Lyrics.ERROR || !OnlineAccessVerifier.check(getActivity())) {
                message = R.string.connection_error;
                whyVisibility = TextView.GONE;
            } else {
                message = R.string.no_results;
                whyVisibility = TextView.VISIBLE;
                if (searchItem != null) {
                    SearchView searchView = (SearchView) searchItem.getActionView();
                    if (!searchItem.isActionViewExpanded())
                        searchItem.expandActionView();
                    searchView.setQuery(lyrics.getTrack(), false);
                    searchView.clearFocus();
                }
            }
            TextView whyTextView = ((TextView) bugLayout.findViewById(R.id.bugtext_why));
            ((TextView) bugLayout.findViewById(R.id.bugtext)).setText(message);
            whyTextView.setVisibility(whyVisibility);
            whyTextView.setPaintFlags(whyTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            id3TV.setVisibility(View.GONE);
        }
        stopRefreshAnimation();
        getActivity().getIntent().setAction("");
        getActivity().invalidateOptionsMenu();
    }

    private void showFirstStart() {
        stopRefreshAnimation();
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup parent = (ViewGroup) ((ViewGroup) getActivity().findViewById(R.id.scrollview)).getChildAt(0);
        if (parent.findViewById(R.id.tracks_msg) == null)
            inflater.inflate(R.layout.no_tracks, parent);

        TypedValue typedValue = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.firstLaunchCoverDrawable, typedValue, true);
        int firstLaunchBGid = typedValue.resourceId;
        @SuppressWarnings("deprecation")
        BitmapDrawable bd = ((BitmapDrawable) getResources().getDrawable(firstLaunchBGid));

        setCoverArt(bd != null ? bd.getBitmap() : null, null);
        ((TextSwitcher) getActivity().findViewById(R.id.switcher)).setText("");
        getActivity().findViewById(R.id.error_msg).setVisibility(View.INVISIBLE);
        ((TextView) getActivity().findViewById(R.id.artist)).setText("");
        ((TextView) getActivity().findViewById(R.id.song)).setText("");
        getActivity().findViewById(R.id.top_gradient).setVisibility(View.INVISIBLE);
        getActivity().findViewById(R.id.bottom_gradient).setVisibility(View.INVISIBLE);
    }

    public void checkPreferencesChanges() {
        boolean screenOn = PreferenceManager
                .getDefaultSharedPreferences(getActivity()).getBoolean("pref_force_screen_on", false);
        boolean dyslexic = PreferenceManager
                .getDefaultSharedPreferences(getActivity()).getBoolean("pref_opendyslexic", false);

        TextSwitcher switcher = (TextSwitcher) getActivity().findViewById(R.id.switcher);
        View lrcView = getActivity().findViewById(R.id.lrc_view);

        if (switcher != null) {
            switcher.setKeepScreenOn(screenOn);
            if (switcher.getCurrentView() != null)
                ((TextView) switcher.getCurrentView()).setTypeface(
                        LyricsTextFactory.FontCache.get(dyslexic ? "dyslexic" : "normal", getActivity())
                );
            View nextView = switcher.getNextView();
            if (nextView != null) {
                ((TextView) nextView).setTypeface(
                        LyricsTextFactory.FontCache.get(dyslexic ? "dyslexic" : "normal", getActivity())
                );
            }
        }
        if (lrcView != null)
            lrcView.setKeepScreenOn(screenOn);
    }

    public void showWhyPopup() {
        String title = mLyrics.getTrack();
        String artist = mLyrics.getArtist();
        new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.why_popup_title))
                .setMessage(String.format(String.valueOf(Html.fromHtml(getString(R.string.why_popup_text))),
                        title, artist))
                .show();
    }

    public void enablePullToRefresh(boolean enabled) {
        mRefreshLayout.setEnabled(enabled && !isInEditMode());
    }

    public boolean isInEditMode() {
        return getActivity().findViewById(R.id.edit_lyrics).getVisibility() == View.VISIBLE;
    }

    @Override
    public void onRefresh() {
        fetchCurrentLyrics(true);
    }

    public String getSource() {
        return mLyrics.getSource();
    }

    public boolean isLRC() {
        return mLyrics.isLRC();
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
                    if (!mLyrics.isLRC())
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
        MainActivity mainActivity = (MainActivity) this.mActivity;
        CollapsingToolbarLayout toolbarLayout =
                (CollapsingToolbarLayout) mainActivity.findViewById(R.id.toolbar_layout);
        toolbarLayout.setTitle(getString(R.string.app_name));

        if (((DrawerLayout) mainActivity.drawer) // drawer is locked
                .getDrawerLockMode(mainActivity.drawerView) == DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            return;

        inflater.inflate(R.menu.lyrics, menu);
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) this.mActivity
                .getSystemService(Context.SEARCH_SERVICE);
        searchItem = menu.findItem(R.id.search_view);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(this.mActivity.getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint(getString(R.string.search_hint));
        searchItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem searchItem) {
                searchView.requestFocus();
                searchView.post(new Runnable() {
                    @Override
                    public void run() {
                        ((InputMethodManager) mActivity
                                .getSystemService(Context.INPUT_METHOD_SERVICE))
                                .toggleSoftInput(InputMethodManager.SHOW_FORCED,
                                        InputMethodManager.HIDE_IMPLICIT_ONLY);
                    }
                });
                return true;
            }
        });
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                ((ControllableAppBarLayout) getActivity().findViewById(R.id.appbar)).expandToolbar(true);
                ((CollapsingToolbarLayout) getActivity().findViewById(R.id.toolbar_layout))
                        .setCollapsedTitleTextColor(Color.TRANSPARENT);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                ((CollapsingToolbarLayout) getActivity().findViewById(R.id.toolbar_layout))
                        .setCollapsedTitleTextColor(Color.WHITE);
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
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.mActivity);
            if (mLyrics != null
                    && mLyrics.getFlag() == Lyrics.POSITIVE_RESULT
                    && sharedPref.getBoolean("pref_auto_save", false)
                    && !lyricsPresentInDB) {
                lyricsPresentInDB = true;
                if (!mLyrics.isLRC())
                    new WriteToDatabaseTask().execute(this, saveMenuItem, mLyrics);
            } else {
                saveMenuItem.setIcon(lyricsPresentInDB ? R.drawable.ic_trash : R.drawable.ic_save);
                saveMenuItem.setTitle(lyricsPresentInDB ? R.string.remove_action : R.string.save_action);
            }
            if (mLyrics != null)
                saveMenuItem.setVisible(!mLyrics.isLRC());
        }
        MenuItem shareMenuItem = menu.findItem(R.id.share_action);
        if (shareMenuItem != null)
            shareMenuItem.setVisible(mLyrics != null && mLyrics.getFlag() == Lyrics.POSITIVE_RESULT && mLyrics.getURL() != null);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    public void setCoverArt(String url, FadeInNetworkImageView coverView) {
        MainActivity mainActivity = (MainActivity) LyricsViewFragment.this.mActivity;
        if (mainActivity == null)
            return;
        mainActivity.findViewById(R.id.top_gradient).setVisibility(View.VISIBLE);
        mainActivity.findViewById(R.id.bottom_gradient).setVisibility(View.VISIBLE);
        if (coverView == null)
            coverView = (FadeInNetworkImageView) mainActivity.findViewById(R.id.cover);
        if (mLyrics != null) {
            mLyrics.setCoverURL(url);
            if (url == null)
                url = "";
            coverView.setImageUrl(url,
                    new ImageLoader(Volley.newRequestQueue(mainActivity), CoverCache.instance()));
        }
    }

    public void setCoverArt(Bitmap cover, FadeInNetworkImageView coverView) {
        MainActivity mainActivity = (MainActivity) LyricsViewFragment.this.mActivity;
        if (mainActivity == null)
            return;
        if (coverView == null)
            coverView = (FadeInNetworkImageView) mainActivity.findViewById(R.id.cover);
        if (coverView != null)
            coverView.setLocalImageBitmap(cover);
        getActivity().findViewById(R.id.top_gradient).setVisibility(View.VISIBLE);
        getActivity().findViewById(R.id.bottom_gradient).setVisibility(View.VISIBLE);
    }

    public void startEmpty(boolean startEmpty) {
        this.startEmtpy = startEmpty;
    }

    private Runnable lrcUpdater = new Runnable() {
        @Override
        public void run() {
            LrcView lrcView = ((LrcView) LyricsViewFragment.this.getActivity().findViewById(R.id.lrc_view));
            if (lrcView == null || getActivity() == null)
                return;
            SharedPreferences preferences = getActivity().getSharedPreferences("current_music", Context.MODE_PRIVATE);
            while (!lrcView.isFinished()) {
                long position = preferences.getLong("position", 0);
                long startTime = preferences.getLong("startTime", System.currentTimeMillis());
                long distance = System.currentTimeMillis() - startTime;
                if (preferences.getBoolean("playing", true))
                    position += distance;
                lrcView.changeCurrent(position);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            MusicBroadcastReceiver.forceAutoUpdate(true);
        }
    };

    public void collapseSearchView() {
        searchItem.collapseActionView();
    }
}
