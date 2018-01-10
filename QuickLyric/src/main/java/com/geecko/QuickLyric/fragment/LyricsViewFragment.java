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

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.session.MediaSessionManager;
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.geecko.QuickLyric.App;
import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.adapter.DrawerAdapter;
import com.geecko.QuickLyric.broadcastReceiver.MusicBroadcastReceiver;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.services.NotificationListenerService;
import com.geecko.QuickLyric.tasks.CoverArtLoader;
import com.geecko.QuickLyric.tasks.DownloadThread;
import com.geecko.QuickLyric.tasks.Id3Reader;
import com.geecko.QuickLyric.tasks.Id3Writer;
import com.geecko.QuickLyric.tasks.ParseTask;
import com.geecko.QuickLyric.tasks.PresenceChecker;
import com.geecko.QuickLyric.tasks.RomanizeAsyncTask;
import com.geecko.QuickLyric.tasks.WriteToDatabaseTask;
import com.geecko.QuickLyric.utils.AnimatorActionListener;
import com.geecko.QuickLyric.utils.ColorUtils;
import com.geecko.QuickLyric.utils.CoverCache;
import com.geecko.QuickLyric.utils.CustomSelectionCallback;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.LyricsTextFactory;
import com.geecko.QuickLyric.utils.MediaControllerCallback;
import com.geecko.QuickLyric.utils.NightTimeVerifier;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;
import com.geecko.QuickLyric.utils.PermissionsChecker;
import com.geecko.QuickLyric.utils.RatingUtils;
import com.geecko.QuickLyric.utils.RomanizeUtil;
import com.geecko.QuickLyric.utils.WhiteListUtil;
import com.geecko.QuickLyric.view.ControllableAppBarLayout;
import com.geecko.QuickLyric.view.FadeInNetworkImageView;
import com.geecko.QuickLyric.view.LrcView;
import com.geecko.QuickLyric.view.MaterialSuggestionsSearchView;
import com.geecko.QuickLyric.view.RefreshIcon;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.squareup.leakcanary.RefWatcher;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import static com.geecko.QuickLyric.R.menu.lyrics;

public class LyricsViewFragment extends Fragment implements Lyrics.Callback, SwipeRefreshLayout.OnRefreshListener, PresenceChecker.PresenceCheckerCallback, RomanizeAsyncTask.RomanisationCallback, ParseTask.ParseCallback {

    private BroadcastReceiver broadcastReceiver;
    public boolean lyricsPresentInDB;
    public boolean isActiveFragment = false;
    public boolean showTransitionAnim = true;
    private boolean warningShown;
    private Lyrics mLyrics;
    private String mSearchQuery;
    private boolean mSearchFocused;
    private NestedScrollView mScrollView;
    private boolean startEmpty = false;
    public boolean manualUpdateLock;
    private SwipeRefreshLayout mRefreshLayout;
    private Thread mLrcThread;
    private boolean mExpandedSearchView;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean threadCancelled;
    private boolean mHasBeenRotated;
    private Object sessionListener = null;
    private MediaControllerCallback controllerCallback = null;

    public static final String UPDATE_LYRICS_ACTION = "QUICKLYRIC_UPDATE_LYRICS_ACTION";

    public LyricsViewFragment() {
    }

    public static void sendIntent(Context context, Intent intent) {
        context.sendBroadcast(intent);
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

        EditText editedLyrics = getActivity().findViewById(R.id.edit_lyrics);
        if (editedLyrics.getVisibility() == View.VISIBLE) {
            EditText editedTitle = getActivity().findViewById(R.id.song);
            EditText editedArtist = getActivity().findViewById(R.id.artist);
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
                        String track = lyrics.getTitle();
                        String url = lyrics.getURL();
                        fetchLyrics(true, null, 0L, artist, track, url);
                        mRefreshLayout = layout.findViewById(R.id.refresh_layout);
                        startRefreshAnimation();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
        }
        if (layout != null) {
            Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();

            boolean screenOn = PreferenceManager
                    .getDefaultSharedPreferences(getActivity()).getBoolean("pref_force_screen_on", false);

            ViewSwitcher viewSwitcher = layout.findViewById(R.id.switcher);
            ActionMode.Callback callback = new CustomSelectionCallback(getActivity());
            setSelectionCallbackOnAllDescendingTVs(viewSwitcher, callback);
            viewSwitcher.setKeepScreenOn(screenOn);
            layout.findViewById(R.id.lrc_view).setKeepScreenOn(screenOn);

            EditText artistTV = getActivity().findViewById(R.id.artist);
            EditText songTV = getActivity().findViewById(R.id.song);

            if (args != null && args.containsKey("editedLyrics")) {
                EditText editedLyrics = layout.findViewById(R.id.edit_lyrics);
                viewSwitcher.setVisibility(View.GONE);
                editedLyrics.setVisibility(View.VISIBLE);
                songTV.setInputType(InputType.TYPE_CLASS_TEXT);
                artistTV.setInputType(InputType.TYPE_CLASS_TEXT);
                songTV.setBackgroundResource(R.drawable.abc_textfield_search_material);
                artistTV.setBackgroundResource(R.drawable.abc_textfield_search_material);
                editedLyrics.setText(args.getCharSequence("editedLyrics"), TextView.BufferType.EDITABLE);
                songTV.setText(args.getCharSequence("editedTitle"), TextView.BufferType.EDITABLE);
                artistTV.setText(args.getCharSequence("editedArtist"), TextView.BufferType.EDITABLE);
            }

            artistTV.setTypeface(LyricsTextFactory.FontCache.get("regular", getActivity()));
            songTV.setTypeface(LyricsTextFactory.FontCache.get("medium", getActivity()));

            final RefreshIcon refreshFab = getActivity().findViewById(R.id.refresh_fab);
            refreshFab.setOnClickListener(v -> {
                if (!mRefreshLayout.isRefreshing())
                    fetchCurrentLyrics(true, true);
            });

            if (args != null)
                refreshFab.setEnabled(args.getBoolean("refreshFabEnabled", true));

            mScrollView = layout.findViewById(R.id.scrollview);
            mRefreshLayout = layout.findViewById(R.id.refresh_layout);

            mRefreshLayout.setColorSchemeResources(ColorUtils.getPrimaryColorResource(getActivity()), ColorUtils.getAccentColorResource(getActivity()));
            float offset = getResources().getDisplayMetrics().density * 64;
            mRefreshLayout.setProgressViewEndTarget(true, (int) offset);
            mRefreshLayout.setOnRefreshListener(this);

            final ImageButton editTagsButton = getActivity().findViewById(R.id.edit_tags_btn);

            View.OnClickListener startEditClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startEditTagsMode();
                    final View.OnClickListener startEditClickListener = this;
                    editTagsButton.setOnClickListener(v1 -> {
                        exitEditTagsMode();
                        editTagsButton.setOnClickListener(startEditClickListener);
                    });
                }
            };
            editTagsButton.setOnClickListener(startEditClickListener);

            if (mLyrics == null) {
                if (!startEmpty)
                    fetchCurrentLyrics(false, true);
            } else if (mLyrics.getFlag() == Lyrics.SEARCH_ITEM) {
                mRefreshLayout = layout.findViewById(R.id.refresh_layout);
                startRefreshAnimation();
                if (mLyrics.getArtist() != null)
                    fetchLyrics(true, null, 0L, mLyrics.getArtist(), mLyrics.getTitle());
                ((TextView) (getActivity().findViewById(R.id.artist))).setText(mLyrics.getArtist());
                ((TextView) (getActivity().findViewById(R.id.song))).setText(mLyrics.getTitle());
            } else //Rotation, resume
                update(mLyrics, layout, false);
        }
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    manualUpdateLock = false;
                    String artist = intent.getStringExtra("artist");
                    String track = intent.getStringExtra("track");
                    if (artist != null && track != null && mRefreshLayout.isEnabled()) {
                        startRefreshAnimation();
                        new ParseTask(LyricsViewFragment.this, getActivity(), false, true, true).execute();
                    }
                }
            };
            getActivity().registerReceiver(broadcastReceiver, new IntentFilter(UPDATE_LYRICS_ACTION));
        }
        return layout;
    }

    private void setSelectionCallbackOnAllDescendingTVs(ViewGroup viewGroup, ActionMode.Callback callback) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View v = viewGroup.getChildAt(i);
            if (v instanceof TextView)
                ((TextView) v).setCustomSelectionActionModeCallback(callback);
            else if (v instanceof ViewGroup)
                setSelectionCallbackOnAllDescendingTVs((ViewGroup) v, callback);
        }
    }

    private void startEditTagsMode() {
        ImageButton editButton = getActivity().findViewById(R.id.edit_tags_btn);
        editButton.setImageResource(R.drawable.ic_edit_anim);
        ((Animatable) editButton.getDrawable()).start();

        ((DrawerLayout) ((MainActivity) getActivity()).drawer).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mRefreshLayout.setEnabled(false);
        getActivity().findViewById(R.id.refresh_fab).setEnabled(false);
        ((RefreshIcon) getActivity().findViewById(R.id.refresh_fab)).hide();
        ((Toolbar) getActivity().findViewById(R.id.toolbar)).getMenu().clear();

        ViewSwitcher viewSwitcher = getActivity().findViewById(R.id.switcher);
        EditText songTV = getActivity().findViewById(R.id.song);
        TextView artistTV = getActivity().findViewById(R.id.artist);

        EditText newLyrics = getActivity().findViewById(R.id.edit_lyrics);
        newLyrics.setTypeface(LyricsTextFactory.FontCache.get("light", getActivity()));
        newLyrics.setText(Html.fromHtml(TextUtils.isEmpty(mLyrics.getText()) ? "" : mLyrics.getText()), TextView.BufferType.EDITABLE);

        viewSwitcher.setVisibility(View.GONE);
        newLyrics.setVisibility(View.VISIBLE);

        songTV.setInputType(InputType.TYPE_CLASS_TEXT);
        artistTV.setInputType(InputType.TYPE_CLASS_TEXT);
        songTV.setBackgroundResource(R.drawable.abc_textfield_search_material);
        artistTV.setBackgroundResource(R.drawable.abc_textfield_search_material);


        if (songTV.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void exitEditTagsMode() {
        ((ImageButton) getActivity().findViewById(R.id.edit_tags_btn)).setImageResource(R.drawable.ic_done_anim);
        Drawable editIcon = ((ImageButton) getActivity().findViewById(R.id.edit_tags_btn)).getDrawable();
        ((Animatable) editIcon).start();

        if (getActivity().getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm.isAcceptingText())
                imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }

        EditText songTV = getActivity().findViewById(R.id.song);
        EditText artistTV = getActivity().findViewById(R.id.artist);
        EditText newLyrics = getActivity().findViewById(R.id.edit_lyrics);

        songTV.setInputType(InputType.TYPE_NULL);
        artistTV.setInputType(InputType.TYPE_NULL);
        songTV.setBackgroundColor(Color.TRANSPARENT);
        artistTV.setBackgroundColor(Color.TRANSPARENT);

        String txt = mLrcThread == null ? null : mLyrics.getText();
        if (txt == null)
            txt = "";

        File musicFile = Id3Reader.getFile(getActivity(), mLyrics.getOriginalArtist(), mLyrics.getOriginalTitle(), true);

        if (!mLyrics.getArtist().equals(artistTV.getText().toString())
                || !mLyrics.getTitle().equals(songTV.getText().toString())
                || !Html.fromHtml(txt).toString().equals(newLyrics.getText().toString())) {
            mLyrics.setArtist(artistTV.getText().toString());
            mLyrics.setTitle(songTV.getText().toString());
            mLyrics.setText(newLyrics.getText().toString().replaceAll("\n", "<br/>"));
            if (PermissionsChecker.requestPermission(getActivity(),
                    "android.permission.WRITE_EXTERNAL_STORAGE", 0, Id3Writer.REQUEST_CODE))
                new Id3Writer(this).execute(mLyrics, musicFile);
        } else
            new Id3Writer(this).onPreExecute();
        update(mLyrics, getView(), false);
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
                new PresenceChecker(this).execute(getActivity(), new String[]{mLyrics.getArtist(), mLyrics.getTitle(),
                        mLyrics.getOriginalArtist(), mLyrics.getOriginalTitle()});
        } else
            this.isActiveFragment = false;
    }

    public void startRefreshAnimation() {
        if (mRefreshLayout == null)
            if (getActivity() != null && getView() != null)
                mRefreshLayout = getActivity().findViewById(R.id.refresh_layout);
        if (mRefreshLayout != null)
            mRefreshLayout.post(() -> {
                if (!mRefreshLayout.isRefreshing())
                    mRefreshLayout.setRefreshing(true);
            });
        if (getView() != null) {
            int offsetY = 0;
            if (getView().findViewById(R.id.feedback_prompt) != null)
                offsetY = getView().findViewById(R.id.feedback_prompt).getMeasuredHeight();
            else if (getView().findViewById(R.id.good_xp_prompt) != null)
                offsetY = getView().findViewById(R.id.good_xp_prompt).getMeasuredHeight();
            else if (getView().findViewById(R.id.nls_warning) != null)
                offsetY = getView().findViewById(R.id.nls_warning).getMeasuredHeight();
            mRefreshLayout.setTranslationY(offsetY);
        }
    }

    public void stopRefreshAnimation() {
        if (mRefreshLayout == null)
            if (getActivity() != null && getView() != null)
                mRefreshLayout = getActivity().findViewById(R.id.refresh_layout);
        if (mRefreshLayout != null)
            mRefreshLayout.post(() -> mRefreshLayout.setRefreshing(false));
        mRefreshLayout.postDelayed(() -> mRefreshLayout.setTranslationY(0), 300);
    }

    public void fetchLyrics(boolean requestPermission, String player, long duration, String... params) {
        if (getActivity() == null)
            return;

        String artist = params[0];
        String title = params[1];
        String url = null;
        if (params.length > 2)
            url = params[2];
        startRefreshAnimation();

        Lyrics lyrics = null;
        File musicFile = null;
        if (artist != null && title != null) {
            if (url == null && (getActivity().getSharedPreferences("intro_slides", Context.MODE_PRIVATE).getBoolean("seen", false))
                    && (mLyrics == null || mLyrics.getFlag() != Lyrics.POSITIVE_RESULT ||
                    !("Storage".equals(mLyrics.getSource())
                            && mLyrics.getArtist().equalsIgnoreCase(artist)
                            && mLyrics.getTitle().equalsIgnoreCase(title)) &&
                            PermissionsChecker.hasPermission(getActivity(), "android.permission.READ_EXTERNAL_STORAGE")
            ))
                    lyrics = Id3Reader.getLyrics(getActivity(), artist, title, requestPermission);

            if (lyrics == null)
                lyrics = DatabaseHelper.getInstance(getActivity()).get(new String[]{artist, title});

            if (lyrics == null)
                lyrics = DatabaseHelper.getInstance(getActivity()).get(DownloadThread.correctTags(artist, title));
        } else {
            musicFile = Id3Reader.getFile(getActivity(), artist, title, requestPermission);
            if (url == null && musicFile == null) {
                if (mLyrics == null || mLyrics.getFlag() != Lyrics.POSITIVE_RESULT)
                    showFirstStart();
                return;
            }
        }
        boolean prefLRC = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getBoolean("pref_lrc", true);
        if (lyrics == null && OnlineAccessVerifier.check(getActivity())) {
            DownloadThread.LRC = prefLRC;

            if (mLyrics == null) {
                TextView artistTV = getActivity().findViewById(R.id.artist);
                TextView songTV = getActivity().findViewById(R.id.song);
                artistTV.setText(artist);
                songTV.setText(title);
            }

            if (musicFile == null)
                musicFile = Id3Reader.getFile(getActivity(), artist, title, requestPermission);

            if (url == null)
                new DownloadThread(new WeakReference<>(this), player, 0L, musicFile, artist, title).start();
            else
                new DownloadThread(new WeakReference<>(this), player, duration, null, url, artist, title).start();

        } else if (lyrics != null)
            onLyricsDownloaded(lyrics);
        else {
            lyrics = new Lyrics(Lyrics.ERROR);
            lyrics.setArtist(artist);
            lyrics.setTitle(title);
            onLyricsDownloaded(lyrics);
        }
    }

    public void fetchCurrentLyrics(boolean showMsg, boolean requestPermission) {
        manualUpdateLock = false;
        getActivity().findViewById(R.id.edit_tags_btn).setEnabled(false);
        new ParseTask(this, getActivity(), showMsg, requestPermission, false).execute();
    }

    @TargetApi(16)
    private void beamLyrics(final Lyrics lyrics, Activity activity) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            if (lyrics.getText() != null) {
                nfcAdapter.setNdefPushMessageCallback(event -> {
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

    @SuppressLint("SetTextI18n")
    public void update(final Lyrics lyrics, View layout, boolean animation) {
        File musicFile = null;
        Bitmap cover = null;
        if (PermissionsChecker.hasPermission(getActivity(), "android.permission.READ_EXTERNAL_STORAGE")) {
            musicFile = Id3Reader.getFile(getActivity(), lyrics.getOriginalArtist(), lyrics.getOriginalTitle(), true);
            cover = Id3Reader.getCover(getActivity(), lyrics.getArtist(), lyrics.getTitle(), true);
        }
        setCoverArt(cover, null, false);
        boolean artCellDownload =
                Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .getString("pref_artworks", "0")) == 0;
        if (cover == null)
            new CoverArtLoader((MainActivity) getActivity()).execute(lyrics, null, artCellDownload || OnlineAccessVerifier.isConnectedWifi(getActivity()));
        getActivity().findViewById(R.id.edit_tags_btn).setEnabled(true);
        getActivity().findViewById(R.id.edit_tags_btn)
                .setVisibility(musicFile == null || !musicFile.canWrite() || lyrics.isLRC()
                        || Id3Reader.getLyrics(getActivity(), lyrics.getArtist(), lyrics.getTitle(), true) == null
                        ? View.GONE : View.VISIBLE);
        ViewSwitcher viewSwitcher = layout.findViewById(R.id.switcher);
        LrcView lrcView = layout.findViewById(R.id.lrc_view);
        View v = getActivity().findViewById(R.id.tracks_msg);
        if (v != null)
            ((ViewGroup) v.getParent()).removeView(v);
        TextView artistTV = getActivity().findViewById(R.id.artist);
        TextView songTV = getActivity().findViewById(R.id.song);
        final TextView id3TV = layout.findViewById(R.id.source_tv);
        TextView writerTV = layout.findViewById(R.id.writer_tv);
        TextView copyrightTV = layout.findViewById(R.id.copyright_tv);
        RelativeLayout bugLayout = layout.findViewById(R.id.error_msg);

        boolean sameLyrics = mLyrics != null && lyrics != null && mLyrics.getArtist() != null && mLyrics.getTitle() != null
                && mLyrics.getArtist().equals(lyrics.getArtist()) && mLyrics.getTitle().equals(lyrics.getTitle());

        this.mLyrics = lyrics;
        beamLyrics(lyrics, this.getActivity());
        new PresenceChecker(this).execute(getActivity(), new String[]{lyrics.getArtist(), lyrics.getTitle(),
                lyrics.getOriginalArtist(), lyrics.getOriginalTitle()});

        if (lyrics.getArtist() != null)
            artistTV.setText(lyrics.getArtist());
        else
            artistTV.setText("");
        if (lyrics.getTitle() != null)
            songTV.setText(lyrics.getTitle());
        else
            songTV.setText("");
        if (lyrics.getCopyright() != null) {
            copyrightTV.setText("Copyright: " + lyrics.getCopyright());
        } else {
            copyrightTV.setText("");
        }
        if (!TextUtils.isEmpty(lyrics.getWriter())) {
            if (lyrics.getWriter().contains(","))
                writerTV.setText("Writers:\n" + lyrics.getWriter());
            else
                writerTV.setText("Writer:" + lyrics.getWriter());
            writerTV.setVisibility(View.VISIBLE);
        } else {
            writerTV.setText("");
            writerTV.setVisibility(View.GONE);
        }
        if (isActiveFragment)
            ((RefreshIcon) getActivity().findViewById(R.id.refresh_fab)).show();
        EditText newLyrics = getActivity().findViewById(R.id.edit_lyrics);
        if (newLyrics != null)
            newLyrics.setText("");

        if (lyrics.getFlag() == Lyrics.POSITIVE_RESULT) {
            if (!lyrics.isLRC()) {
                viewSwitcher.setVisibility(View.VISIBLE);
                lrcView.setVisibility(View.GONE);
                String[] paragraphs = new String[] {Html.fromHtml(lyrics.getText()).toString()};
                View[] lyricsViews = new View[paragraphs.length + 2];
                int viewsIndex = 0;
                LyricsTextFactory factory = new LyricsTextFactory(getActivity());

                for (int i = 0; i < paragraphs.length; i++) {
                    TextView paragraph = (TextView) factory.makeView();
                    paragraph.setText(paragraphs[i]);
                    lyricsViews[viewsIndex++] = paragraph;
                    if (i == paragraphs.length - 1) {
                        break;
                    }
                }

                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                LinearLayout linearLayout = new LinearLayout(getActivity());
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                linearLayout.setLayoutParams(lp);

                for (View lyricsView : lyricsViews) {
                    if (lyricsView != null) {
                        linearLayout.addView(lyricsView);
                        LinearLayout.LayoutParams layoutParams;
                        if (lyricsView.getLayoutParams() != null && lyricsView.getLayoutParams() instanceof LinearLayout.LayoutParams)
                            layoutParams = (LinearLayout.LayoutParams) lyricsView.getLayoutParams();
                        else
                            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                        layoutParams.setMargins(0,  0, 0, 25 * (int) getResources().getDimension(R.dimen.dp));
                        lyricsView.setLayoutParams(layoutParams);
                    }
                }
                if (viewSwitcher.getChildCount() > 1)
                    viewSwitcher.removeViewAt(0);

                viewSwitcher.addView(linearLayout);
                if (animation)
                    viewSwitcher.showNext();
                else {
                    Animation in = viewSwitcher.getInAnimation();
                    Animation out = viewSwitcher.getOutAnimation();
                    viewSwitcher.setInAnimation(null);
                    viewSwitcher.setOutAnimation(null);
                    viewSwitcher.showNext();
                    viewSwitcher.setInAnimation(in);
                    viewSwitcher.setOutAnimation(out);
                }
            } else {
                viewSwitcher.setVisibility(View.GONE);
                lrcView.setVisibility(View.VISIBLE);
                lrcView.setOriginalLyrics(lyrics);
                lrcView.setSourceLrc(lyrics.getText());
                if (isActiveFragment)
                    ((ControllableAppBarLayout) getActivity().findViewById(R.id.appbar)).expandToolbar(true);
                if (getActivity() != null && (!PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_lrc", true))) {
                    final Lyrics staticLyrics = lrcView.getStaticLyrics();
                    getActivity().runOnUiThread(() -> update(staticLyrics, layout, true));
                } else
                    updateLRC();
            }

            RatingUtils.trackSuccess(getActivity());

            bugLayout.setVisibility(View.INVISIBLE);
            id3TV.setVisibility(View.VISIBLE);
            id3TV.setMovementMethod(LinkMovementMethod.getInstance());
            if ("Storage".equals(lyrics.getSource())) {
                SpannableString text = new SpannableString(getString(R.string.from_id3));
                text.setSpan(new UnderlineSpan(), 1, text.length() - 1, 0);
                id3TV.setText(text);
                id3TV.setOnClickListener(v1 -> ((MainActivity) getActivity()).id3PopUp(id3TV));
            } else {
                id3TV.setOnClickListener(null);
                SpannableString text = new SpannableString("Lyrics licensed & provided by LyricFind");
                int start = text.toString().indexOf("LyricFind");
                text.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.lyricfind.com")));
                    }
                }, start, start + 9, 0);
                text.setSpan(new UnderlineSpan(), start, start + 9, 0);
                id3TV.setText(text);
            }
            mScrollView.post(() -> {
                mScrollView.scrollTo(0, 0); //only useful when coming from localLyricsFragment
                mScrollView.smoothScrollTo(0, 0);
            });
            if (RatingUtils.shouldPromptGoodXP(getActivity()))
                showGoodXPPrompt(layout);
        } else {
            if (viewSwitcher.getChildCount() > 1)
                viewSwitcher.removeViewAt(0);
            viewSwitcher.addView(new View(getActivity()));
            viewSwitcher.showNext();
            viewSwitcher.setVisibility(View.INVISIBLE);
            lrcView.setVisibility(View.INVISIBLE);
            bugLayout.setVisibility(View.VISIBLE);
            int message = -1;
            int whyVisibility;
            int letUsKnowVisibility;
            if (lyrics.getFlag() == Lyrics.ERROR || !OnlineAccessVerifier.check(getActivity())) {
                switch (lyrics.getErrorCode()) {
                    default:
                        message = R.string.connection_error;
                        break;
                    case 540:
                        message = R.string.client_version_error;
                        break;
                    case 504:
                    case 800:
                    case 900:
                }
                whyVisibility = TextView.GONE;
                letUsKnowVisibility = TextView.VISIBLE;
            } else {
                message = R.string.no_results;
                boolean storageGranted = PermissionsChecker.hasPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
                whyVisibility = storageGranted ? TextView.GONE :
                        PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getBoolean("never_ask_checked", false) ?
                                TextView.GONE : TextView.VISIBLE;
                letUsKnowVisibility = TextView.GONE;
                updateSearchView(false, lyrics.getTitle(), false);
            }
            if (lyrics.getFlag() != Lyrics.ERROR)
                RatingUtils.trackFail(getActivity());
            if (!warningShown && RatingUtils.shouldPromptFeedback(getActivity())) {
                showFeedbackPrompt(layout);
            }
            TextView storageTextView = bugLayout.findViewById(R.id.bugtext_storage);
            TextView letUsKnowTextView = bugLayout.findViewById(R.id.bugtext_letusknow);
            if (message != -1)
                ((TextView) bugLayout.findViewById(R.id.bugtext)).setText(message);
            else
                ((TextView) bugLayout.findViewById(R.id.bugtext)).setText("Code: " + lyrics.getErrorCode());
            storageTextView.setVisibility(whyVisibility);
            storageTextView.setPaintFlags(storageTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            letUsKnowTextView.setPaintFlags(letUsKnowTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            letUsKnowTextView.setVisibility(letUsKnowVisibility);
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

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        setCoverArt(bd != null ? bd.getBitmap() : null, null, true);
        ViewSwitcher viewSwitcher = getActivity().findViewById(R.id.switcher);
        if (viewSwitcher.getChildCount() > 1)
            viewSwitcher.removeViewAt(0);
        viewSwitcher.addView(new View(getActivity()));
        viewSwitcher.showNext();

        int themeNum = Integer.valueOf(sharedPref.getString("pref_theme", "0"));
        if (themeNum > 0 && themeNum != 7) {
            TypedValue darkColorValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(R.attr.colorPrimaryDark, darkColorValue, true);
            ((FadeInNetworkImageView) getActivity().findViewById(R.id.cover))
                    .setColorFilter(darkColorValue.data, PorterDuff.Mode.OVERLAY);
        }

        getActivity().findViewById(R.id.error_msg).setVisibility(View.INVISIBLE);
        ((TextView) getActivity().findViewById(R.id.artist)).setText("");
        ((TextView) getActivity().findViewById(R.id.song)).setText("");
        getActivity().findViewById(R.id.top_gradient).setVisibility(View.INVISIBLE);
        getActivity().findViewById(R.id.bottom_gradient).setVisibility(View.INVISIBLE);
        getActivity().findViewById(R.id.edit_tags_btn).setVisibility(View.INVISIBLE);
    }

    private void changeTypefaceForAllChildren(ViewGroup viewGroup, String typeface) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup)
                changeTypefaceForAllChildren((ViewGroup) child, typeface);
            else if (child instanceof TextView) {
                ((TextView) child).setTypeface(LyricsTextFactory.FontCache.get(typeface, getActivity()));
            }
        }
    }

    public void checkPreferencesChanges() {
        boolean screenOn = PreferenceManager
                .getDefaultSharedPreferences(getActivity()).getBoolean("pref_force_screen_on", false);
        boolean dyslexic = PreferenceManager
                .getDefaultSharedPreferences(getActivity()).getBoolean("pref_opendyslexic", false);

        ViewSwitcher switcher = getActivity().findViewById(R.id.switcher);
        View lrcView = getActivity().findViewById(R.id.lrc_view);

        if (switcher != null) {
            switcher.setKeepScreenOn(screenOn);
            if (switcher.getCurrentView() != null && switcher.getCurrentView() instanceof ViewGroup) {
                changeTypefaceForAllChildren((ViewGroup) switcher.getCurrentView(), dyslexic ? "dyslexic" : "light");
            }
        }

        if (lrcView != null)
            lrcView.setKeepScreenOn(screenOn);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        TypedValue outValue = new TypedValue();
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.getTheme().resolveAttribute(R.attr.themeName, outValue, false);
        if ("Night".equals(outValue.string) != NightTimeVerifier.check(getActivity()) ||
                mainActivity.themeNum != Integer.valueOf(sharedPrefs.getString("pref_theme", "0"))) {
            getActivity().finish();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setAction("android.intent.action.MAIN");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().overridePendingTransition(0, 0);
        }
    }

    public void enablePullToRefresh(boolean enabled) {
        mRefreshLayout.setEnabled(enabled && !isInEditMode());
    }

    public boolean isInEditMode() {
        return getActivity().findViewById(R.id.edit_lyrics).getVisibility() == View.VISIBLE;
    }

    @Override
    public void onRefresh() {
        fetchCurrentLyrics(true, true);
    }

    public String getSource() {
        return mLyrics != null ? mLyrics.getSource() : null;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
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
            case R.id.action_search:
                MaterialSuggestionsSearchView suggestionsSearchView =
                        getActivity().findViewById(R.id.material_search_view);
                if (suggestionsSearchView.isSearchOpen())
                    ((ControllableAppBarLayout) getActivity().findViewById(R.id.appbar))
                            .expandToolbar(true);
                break;
            case R.id.save_action:
                if (mLyrics != null && mLyrics.getFlag() == Lyrics.POSITIVE_RESULT)
                    new WriteToDatabaseTask().execute(this, item, this.mLyrics);
                break;
            case R.id.resync_action:
                MainActivity.resync(getActivity());
                break;
            case R.id.convert_action:
                if (mLyrics.isLRC()) {
                    LrcView lrcView = getActivity().findViewById(R.id.lrc_view);
                    if (lrcView != null && lrcView.dictionnary != null)
                        update(lrcView.getStaticLyrics(), getView(), true);
                } else
                    update(DatabaseHelper.getInstance(getActivity())
                            .get(new String[]{mLyrics.getArtist(), mLyrics.getTitle(),
                                    mLyrics.getOriginalArtist(), mLyrics.getOriginalTitle()}), getView(), true);
                break;
            case R.id.romanize_action:
                if (mRefreshLayout.isRefreshing())
                    break;
                if (RomanizeUtil.detectIdeographic(mLyrics.getText())) {
                    if (RomanizeUtil.isRomanizerInstalled(getActivity())) {
                        Lyrics lyrics = mLyrics;
                        new RomanizeAsyncTask(getActivity(), this).execute(lyrics);
                        startRefreshAnimation();
                    } else {
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.romanizer_prompt_title)
                                .setMessage(R.string.romanizer_prompt_msg).setIcon(R.drawable.splash_icon)
                                .setCancelable(true)
                                .setPositiveButton("Google Play", (dialogInterface, i) -> getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.quicklyric.romanizer")))).show();
                    }
                } else
                    update(DatabaseHelper.getInstance(getActivity())
                            .get(new String[]{mLyrics.getArtist(), mLyrics.getTitle(),
                                    mLyrics.getOriginalArtist(), mLyrics.getOriginalTitle()}), getView(), true);
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
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null)
            return;
        CollapsingToolbarLayout toolbarLayout =
                mainActivity.findViewById(R.id.toolbar_layout);
        toolbarLayout.setTitle(getString(R.string.app_name));

        if (((DrawerLayout) mainActivity.drawer) // drawer is locked
                .getDrawerLockMode(mainActivity.drawerView) == DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            return;

        inflater.inflate(lyrics, menu);
        // Get the SearchView and set the searchable configuration
        final MaterialSuggestionsSearchView materialSearchView = mainActivity.findViewById(R.id.material_search_view);
        materialSearchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {
                materialSearchView.setSuggestions(null);
                materialSearchView.requestFocus();
                materialSearchView.post(() -> ((InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(materialSearchView.getWindowToken(), 0));
                materialSearchView.postDelayed(() -> {
                    ((MainActivity) getActivity()).search(query);
                    materialSearchView.setSuggestions(null);
                }, 90);
                mExpandedSearchView = false;
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!materialSearchView.hasSuggestions())
                    materialSearchView.setSuggestions(materialSearchView.getHistory());
                return true;
            }
        });

        materialSearchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                if (getActivity() == null)
                    return;
                ((ControllableAppBarLayout) getActivity().findViewById(R.id.appbar))
                        .expandToolbar(true);
                mExpandedSearchView = true;
            }

            @Override
            public void onSearchViewClosed() {
                mExpandedSearchView = false;
            }
        });

        final Resources resources = getResources();
        final int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        int statusBarHeight;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP)
            statusBarHeight = 0;
        else if (resourceId > 0)
            statusBarHeight = resources.getDimensionPixelSize(resourceId);
        else
            statusBarHeight = (int) Math.ceil((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 24 : 25) * resources.getDisplayMetrics().density);
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) materialSearchView.getLayoutParams();
        lp.setMargins(lp.leftMargin, statusBarHeight, lp.rightMargin, lp.bottomMargin);
        materialSearchView.setLayoutParams(lp);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        materialSearchView.setMenuItem(searchItem);

        if (!materialSearchView.isSearchOpen() && mExpandedSearchView) {
            materialSearchView.showSearch();
            mExpandedSearchView = false;
        } else if (!mExpandedSearchView)
            materialSearchView.closeSearch();

        materialSearchView.setHint(getString(R.string.search_hint));
        if (mSearchQuery != null && !mSearchQuery.equals("")) {
            searchItem.expandActionView();
            materialSearchView.setQuery(mSearchQuery, false);
            if (mSearchFocused)
                materialSearchView.requestFocus();
            else
                materialSearchView.clearFocus();
            mSearchQuery = null;
        }
        Lyrics storedLyrics = mLyrics == null ? null :
                DatabaseHelper.getInstance(getActivity()).get(new String[]{
                        mLyrics.getArtist(),
                        mLyrics.getTitle(),
                        mLyrics.getOriginalArtist(),
                        mLyrics.getOriginalTitle()});


        MenuItem saveMenuItem = menu.findItem(R.id.save_action);
        if (saveMenuItem != null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            if (mLyrics == null)
                saveMenuItem.setVisible(false);
            else if (mLyrics.getFlag() == Lyrics.POSITIVE_RESULT
                    && sharedPref.getBoolean("pref_auto_save", true)) {
                if (storedLyrics == null || (mLyrics.isLRC() && !storedLyrics.isLRC())) {
                    lyricsPresentInDB = true;
                    new WriteToDatabaseTask().execute(this, saveMenuItem, mLyrics);
                }
                saveMenuItem.setVisible(false);
            } else {
                saveMenuItem.setIcon(lyricsPresentInDB ? R.drawable.ic_trash : R.drawable.ic_save);
                saveMenuItem.setTitle(lyricsPresentInDB ? R.string.remove_action : R.string.save_action);
            }
        }
        MenuItem resyncMenuItem = menu.findItem(R.id.resync_action);
        MenuItem convertMenuItem = menu.findItem(R.id.convert_action);
        MenuItem romanizeMenuItem = menu.findItem(R.id.romanize_action);
        if (resyncMenuItem != null)
            resyncMenuItem.setVisible(mLyrics != null && mLyrics.isLRC() && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT);
        if (convertMenuItem != null) {
            Lyrics stored = mLyrics == null || mLyrics.isLRC() ? null : storedLyrics;
            convertMenuItem.setVisible((mLyrics != null && (mLyrics.isLRC())) || (stored != null && stored.isLRC()));
            convertMenuItem.setTitle(stored == null ? R.string.full_text_action : R.string.pref_lrc);
        }
        if (romanizeMenuItem != null && mLyrics != null && mLyrics.getText() != null && mLyrics.getFlag() == Lyrics.POSITIVE_RESULT) {
            boolean isIdeographic = RomanizeUtil.detectIdeographic(mLyrics.getText());
            romanizeMenuItem.setVisible(isIdeographic ||
                    (storedLyrics != null && RomanizeUtil.detectIdeographic(storedLyrics.getText())));
            romanizeMenuItem.setTitle(isIdeographic ? R.string.romanize : R.string.reset);
        } else {
            romanizeMenuItem.setVisible(false);
        }
        if (Runtime.getRuntime().maxMemory() / 1000 / 1000 < 128)
            romanizeMenuItem.setVisible(false);

        MenuItem shareMenuItem = menu.findItem(R.id.share_action);
        if (shareMenuItem != null)
            shareMenuItem.setVisible(false);//mLyrics != null && mLyrics.getFlag() == Lyrics.POSITIVE_RESULT && mLyrics.getURL() != null);
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean introSeen = getActivity().getSharedPreferences("intro_slides", Context.MODE_PRIVATE).getBoolean("seen", false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getActivity() != null && introSeen) {
            getView().postDelayed(() -> {
                if (getActivity() != null) {
                    if (!NotificationListenerService.isAppScrobbling(getActivity())) {
                        showBrokenScrobblingWarning();
                    } else if (warningShown)
                        removePrompt(getView().findViewById(R.id.nls_warning), false);
                }
            }, 2100);
        }
        if (!warningShown && RatingUtils.shouldPromptFeedback(getActivity()) && getView() != null) {
            showFeedbackPrompt(getView());
        }
        if (mLyrics != null && mLyrics.isLRC()) {
            this.mHasBeenRotated = true;
        }
    }

    private void showFeedbackPrompt(View layout) {
        boolean feedbackPromptAdded = layout.findViewById(R.id.send_feedback) != null;
        if (warningShown || PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("feedback_prompt_closed", false) || feedbackPromptAdded)
            return;
        final ViewGroup feedbackPrompt = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.bad_xp_prompt, (ViewGroup) layout, false);
        feedbackPrompt.findViewById(R.id.ic_close).setOnClickListener(view -> {
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("feedback_prompt_closed", true).apply();
            removePrompt(feedbackPrompt, true);
        });
        feedbackPrompt.findViewById(R.id.send_feedback).setOnClickListener(view -> {
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("feedback_prompt_closed", true).apply();
            MainActivity.startFeedbackActivity(getActivity(), true);
            removePrompt(feedbackPrompt, false);
        });
        ((ViewGroup) layout).addView(feedbackPrompt);

        feedbackPrompt.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mRefreshLayout.getProgressViewEndOffset() == 0)
                    mRefreshLayout.setProgressViewOffset(true, 0, feedbackPrompt.getMeasuredHeight());
                feedbackPrompt.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }

    private void removePrompt(final View view, boolean animate) {
        if (getView() == null)
            return;
        if (animate) {
            view.animate().translationXBy(view.getMeasuredWidth()).setInterpolator(new AccelerateInterpolator())
                    .setDuration(350).setListener(new AnimatorActionListener(() -> ((ViewGroup) getView()).removeView(view), AnimatorActionListener.ActionType.END));
        } else
            ((ViewGroup) getView()).removeView(view);
        mRefreshLayout.setProgressViewOffset(true, 0, mRefreshLayout.getProgressCircleDiameter() / 2);
    }

    private void showGoodXPPrompt(View layout) {
        boolean feedbackPromptAdded = layout.findViewById(R.id.send_feedback) != null;
        boolean goodXPPromptAdded = layout.findViewById(R.id.love) != null;
        if (warningShown || PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("has_rated", false)
                || goodXPPromptAdded || feedbackPromptAdded
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !NotificationListenerService.isAppScrobbling(getActivity())))
            return;
        final ViewGroup goodXPPrompt = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.good_xp_prompt, (ViewGroup) layout, false);
        goodXPPrompt.findViewById(R.id.love).setOnClickListener(view -> {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.thanks)
                    .setMessage(R.string.goodXP_rate_prompt)
                    .setPositiveButton(R.string.rate_us, (dialog, i) -> {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.geecko.QuickLyric")));
                        dialog.dismiss();
                        Toast.makeText(getActivity(), R.string.thanks, Toast.LENGTH_SHORT).show();
                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("has_rated", true).apply();
                    })
                    .setNeutralButton(android.R.string.no, (dialog, i) -> {
                        dialog.dismiss();
                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("has_rated", true).apply();
                    })
                    .setOnCancelListener(dialogInterface -> PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("has_rated", false).apply())
                    .setCancelable(true)
                    .setIcon(R.drawable.icon)
                    .show();
            removePrompt(goodXPPrompt, true);
        });
        goodXPPrompt.findViewById(R.id.hate).setOnClickListener(view -> {
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("has_rated", true).apply();
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.thanks_feedback)
                    .setMessage(R.string.please_improve_ql)
                    .setPositiveButton(R.string.send_feedback, (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        MainActivity.startFeedbackActivity(getActivity(), true);
                    })
                    .setNegativeButton(android.R.string.no, (dialogInterface, i) -> dialogInterface.dismiss())
                    .setCancelable(true)
                    .setIcon(R.drawable.icon)
                    .show();
            removePrompt(goodXPPrompt, true);
        });
        ((ViewGroup) layout).addView(goodXPPrompt);
        goodXPPrompt.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mRefreshLayout.getProgressViewEndOffset() == 0)
                    mRefreshLayout.setProgressViewOffset(true, 0, goodXPPrompt.getMeasuredHeight());
                goodXPPrompt.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }

    @RequiresApi(21)
    public void showBrokenScrobblingWarning() {
        if (controllerCallback == null)
            controllerCallback = new MediaControllerCallback(null);
        if (NotificationListenerService.isListeningAuthorized(getActivity()))
            MediaControllerCallback.registerFallbackControllerCallback(getActivity(), controllerCallback);

        String[] manufacturers = new String[]{"XIAOMI", "HUAWEI", "HONOR", "LETV"};
        final boolean canFix = Arrays.asList(manufacturers).contains(Build.BRAND.toUpperCase());
        if (canFix && !PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("nls_warning_removed", false)) {
            final ViewGroup nlsWarning = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.nls_warning, (ViewGroup) getView(), false);
            AppCompatButton button = nlsWarning.findViewById(R.id.fix_it);
            button.setText(R.string.fix_it);
            button.setOnClickListener(view -> {
                if (!WhiteListUtil.openBootSpecialMenu(getActivity())) {
                    MainActivity.startFeedbackActivity(getActivity(), true);
                }
                warningShown = false;
                removePrompt(nlsWarning, false);
            });
            AppCompatImageButton closeButton = nlsWarning.findViewById(R.id.ic_nls_warning_close);
            closeButton.setOnClickListener(view -> {
                removePrompt(nlsWarning, true);
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("nls_warning_removed", true).apply();
            });
            ((ViewGroup) getView()).addView(nlsWarning);
            warningShown = true;
            nlsWarning.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (mRefreshLayout.getProgressViewEndOffset() == 0)
                        mRefreshLayout.setProgressViewOffset(true, 0, nlsWarning.getMeasuredHeight());
                    nlsWarning.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        unregisterUpdateBroadcastReceiver();
        threadCancelled = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && networkCallback != null)
            try {
                ((ConnectivityManager) getActivity().getApplicationContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE)).unregisterNetworkCallback(networkCallback);
            } catch (IllegalArgumentException ignored) {
            }
        if (Build.VERSION.SDK_INT >= 21 && sessionListener != null) {
            ((MediaSessionManager) getActivity().getSystemService(Context.MEDIA_SESSION_SERVICE))
                    .removeOnActiveSessionsChangedListener((OnActiveSessionsChangedListener) sessionListener);
        }
        super.onDestroy();
        RefWatcher refWatcher = App.getRefWatcher(getActivity());
        refWatcher.watch(this);
    }

    private void unregisterUpdateBroadcastReceiver() {
        if (broadcastReceiver != null && getActivity() != null) {
            try {
                getActivity().unregisterReceiver(broadcastReceiver);
            } catch (IllegalArgumentException ignored) {
            } finally {
                broadcastReceiver = null;
            }
        }
    }

    public void setCoverArt(String url, FadeInNetworkImageView coverView) {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null)
            return;
        mainActivity.findViewById(R.id.top_gradient).setVisibility(View.VISIBLE);
        mainActivity.findViewById(R.id.bottom_gradient).setVisibility(View.VISIBLE);
        if (coverView == null)
            coverView = mainActivity.findViewById(R.id.cover);
        if (url == null)
            url = "";
        if (mLyrics != null) {
            mLyrics.setCoverURL(url);
            coverView.setLyrics(mLyrics);
        }
        coverView.clearColorFilter();
        if (url.startsWith("/")) {
            coverView.setImageBitmap(BitmapFactory.decodeFile(url));
        } else {
            coverView.setImageUrl(url,
                    new ImageLoader(Volley.newRequestQueue(mainActivity), CoverCache.instance()));
            if (!url.isEmpty() && mLyrics != null && mLyrics.getFlag() == Lyrics.POSITIVE_RESULT)
                DatabaseHelper.getInstance(getActivity()).updateCover(mLyrics.getArtist(), mLyrics.getTitle(), url);
        }
    }

    public void setCoverArt(Bitmap cover, FadeInNetworkImageView coverView, boolean firstStart) {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null)
            return;
        if (coverView == null)
            coverView = mainActivity.findViewById(R.id.cover);
        if (coverView != null) {
            coverView.setLocalImageBitmap(cover);
            coverView.clearColorFilter();
            coverView.setFirstStart(firstStart);
        }
        getActivity().findViewById(R.id.top_gradient).setVisibility(View.VISIBLE);
        getActivity().findViewById(R.id.bottom_gradient).setVisibility(View.VISIBLE);
    }

    public void expandToolbar() {
        ((ControllableAppBarLayout) getActivity().findViewById(R.id.appbar)).expandToolbar(true);
    }

    public void updateLRC() {
        if (mLrcThread == null || !mLrcThread.isAlive()) {
            mLrcThread = new Thread(lrcUpdater);
            mLrcThread.start();
        }
    }

    public void startEmpty(boolean startEmpty) {
        this.startEmpty = startEmpty;
    }

    private Runnable lrcUpdater = () -> {
            if (threadCancelled)
                return;
            boolean ran = false;
            if (getActivity() == null)
                return;
            long[] position = new long[] {MediaControllerCallback.getActiveControllerPosition(getActivity())};
            SharedPreferences preferences = getActivity().getSharedPreferences("current_music", Context.MODE_PRIVATE);
            long duration = preferences.getLong("duration", -1);
            String player = preferences.getString("player", "");
            final LrcView[] lrcView = {LyricsViewFragment.this.getActivity().findViewById(R.id.lrc_view)};

            if (lrcView[0] != null) {
                boolean songIsTooShort = duration > 0 && lrcView[0].getLastLinePosition() > duration;
                boolean youtubeSongIsTooLong = player.contains("youtube") && duration - 60000 > lrcView[0].getLastLinePosition();
                if (getActivity() != null) {
                    if ((position[0] == -1 || !PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_lrc", true))
                            || songIsTooShort || youtubeSongIsTooLong) {
                        final Lyrics staticLyrics = lrcView[0].getStaticLyrics();
                        getActivity().runOnUiThread(() -> update(staticLyrics, getView(), true));
                        return;
                    } else {
                        getActivity().runOnUiThread(() -> {
                            Activity activity = LyricsViewFragment.this.getActivity();
                            if (activity != null)
                                ((LrcView) activity.findViewById(R.id.lrc_view))
                                        .changeCurrent(MediaControllerCallback.getActiveControllerPosition(activity));
                        });
                    }
                }
            }

            MusicBroadcastReceiver.forceAutoUpdate(true);
            while (getActivity() != null &&
                    preferences.getString("track", "").equalsIgnoreCase(mLyrics.getOriginalTitle()) &&
                    preferences.getString("artist", "").equalsIgnoreCase(mLyrics.getOriginalArtist()) &&
                    preferences.getBoolean("playing", true)) {
                if (threadCancelled)
                    return;
                ran = true;
                position[0] = MediaControllerCallback.getActiveControllerPosition(getActivity());
                getActivity().runOnUiThread(() -> {
                    if (lrcView[0] == null || mHasBeenRotated) {
                        lrcView[0] = LyricsViewFragment.this.getActivity().findViewById(R.id.lrc_view);
                        mHasBeenRotated = false;
                    }
                    if (lrcView[0] != null)
                        lrcView[0].changeCurrent(position[0]);
                });
                //String time = String.valueOf((position / 60000)) + " min ";
                //time += String.valueOf((position / 1000) % 60) + " sec";
                //Log.i("QuickLyric", time);
                //Log.d("QuickLyric", "Playing:"+preferences.getBoolean("playing", true));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            MusicBroadcastReceiver.forceAutoUpdate(true);
            if (preferences.getBoolean("playing", true) && ran && mLyrics.isLRC() && getActivity() != null
                    && lrcView[0].getVisibility() == View.VISIBLE) {
                fetchCurrentLyrics(false, true);
            }
    };

    public void updateSearchView(boolean collapsed, String query, boolean focused) {
        final MaterialSuggestionsSearchView materialSearchView = getActivity().findViewById(R.id.material_search_view);
        if (materialSearchView != null && materialSearchView.hasFocus())
            return;
        this.mExpandedSearchView = !collapsed;
        if (query != null)
            this.mSearchQuery = query;
        this.mSearchFocused = focused;
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onPresenceChecked(boolean present) {
        if (lyricsPresentInDB != present) {
            lyricsPresentInDB = present;
            if (getActivity() != null)
                getActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onLyricsRomanized(Lyrics result) {
        update(result, getView(), true);
    }

    @Override
    public void onMetadataParsed(String[] metadata, long duration, boolean showMsg, boolean requestPermission, boolean noDoubleBroadcast) {
        if (getActivity() != null) {
            if (mLyrics != null && metadata != null
                    && metadata[0] != null && metadata[0].equalsIgnoreCase(mLyrics.getOriginalArtist())
                    && metadata[1] != null && metadata[1].equalsIgnoreCase(mLyrics.getOriginalTitle())
                    && (!"Storage".equals(mLyrics.getSource()) || ("Storage".equals(mLyrics.getSource()) && noDoubleBroadcast))
                    && mLyrics.getFlag() == Lyrics.POSITIVE_RESULT) {
                if (showMsg) {
                    Toast.makeText(getActivity(), getString(R.string.no_refresh), Toast.LENGTH_LONG).show();
                    RatingUtils.trackFailedToRefresh(getActivity());
                }
                stopRefreshAnimation();
                getActivity().findViewById(R.id.edit_tags_btn).setEnabled(true);
                if (mLyrics.isLRC())
                    updateLRC();
            } else {
                requestPermission &= getActivity().getSharedPreferences("intro_slides", Context.MODE_PRIVATE).getBoolean("seen", false);
                fetchLyrics(requestPermission, metadata.length > 2 ? metadata[2] : null, duration, metadata[0], metadata[1]);
            }
        }
    }

    public void setNetworkCallback(ConnectivityManager.NetworkCallback networkCallback) {
        this.networkCallback = networkCallback;
    }
}