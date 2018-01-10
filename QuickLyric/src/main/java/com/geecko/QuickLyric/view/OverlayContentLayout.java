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

package com.geecko.QuickLyric.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.geecko.QuickLyric.App;
import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.broadcastReceiver.MusicBroadcastReceiver;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.services.LyricsOverlayService;
import com.geecko.QuickLyric.tasks.DownloadThread;
import com.geecko.QuickLyric.tasks.Id3Reader;
import com.geecko.QuickLyric.tasks.ParseTask;
import com.geecko.QuickLyric.tasks.PresenceChecker;
import com.geecko.QuickLyric.tasks.RomanizeAsyncTask;
import com.geecko.QuickLyric.tasks.WriteToDatabaseTask;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.LaunchesCounter;
import com.geecko.QuickLyric.utils.LyricsTextFactory;
import com.geecko.QuickLyric.utils.MediaControllerCallback;
import com.geecko.QuickLyric.utils.NightTimeVerifier;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;
import com.geecko.QuickLyric.utils.PermissionsChecker;
import com.geecko.QuickLyric.utils.RatingUtils;
import com.geecko.QuickLyric.utils.RomanizeUtil;

import java.lang.ref.WeakReference;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class OverlayContentLayout extends LinearLayout implements Toolbar.OnMenuItemClickListener, PresenceChecker.PresenceCheckerCallback,
        RomanizeAsyncTask.RomanisationCallback, Lyrics.Callback, ParseTask.ParseCallback {

    private View bugLayout;
    private LrcView lrcView;
    private Toolbar toolbar;
    private ViewSwitcher lyricsSwitcher;
    private ViewSwitcher viewSwitcher;
    private NonFocusableNestedScrollView scrollview;

    private OnLongClickListener menuItemLongClickListener = view -> true;
    private ConnectivityManager.NetworkCallback networkCallback;
    public boolean lyricsPresentInDB;
    private boolean mRefreshing;
    private Thread mLrcThread;

    public OverlayContentLayout(Context context, @Nullable AttributeSet attrs) {
        super(context instanceof ContextThemeWrapper ? ((ContextThemeWrapper) context).getBaseContext() : context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        toolbar = findViewById(R.id.overlay_toolbar);
        viewSwitcher = findViewById(R.id.overlay_flipper);
        bugLayout = findViewById(R.id.error_msg);
        ViewGroup lyricsContent = findViewById(R.id.lyrics_content);
        scrollview = findViewById(R.id.scrollview);
        lyricsSwitcher = lyricsContent.findViewById(R.id.switcher);
        lrcView = lyricsContent.findViewById(R.id.lrc_view);

        toolbar.inflateMenu(R.menu.overlay_lyrics);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
    }

    private int getSelectedTheme() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        int[] themes = new int[]{R.style.Theme_QuickLyric, R.style.Theme_QuickLyric_Red,
                R.style.Theme_QuickLyric_Purple, R.style.Theme_QuickLyric_Indigo,
                R.style.Theme_QuickLyric_Green, R.style.Theme_QuickLyric_Lime,
                R.style.Theme_QuickLyric_Brown, R.style.Theme_QuickLyric_Dark};

        int selectedTheme;
        int themeNum = Integer.valueOf(sharedPref.getString("pref_theme", "0"));
        boolean nightMode = sharedPref.getBoolean("pref_night_mode", false);
        if (nightMode && NightTimeVerifier.check(getContext()))
            selectedTheme = R.style.Theme_QuickLyric_Night;
        else
            selectedTheme = themes[themeNum];
        return selectedTheme;
    }

    private Lyrics getLyrics() {
        Lyrics output = LyricsOverlayService.getLyrics();
        output = output == null ? new Lyrics(Lyrics.NO_RESULT) : output;
        return output;
    }

    public void setLyrics(Lyrics lyrics) {
        LyricsOverlayService.setLyrics(lyrics);
    }

    private void refreshToolbar(Menu menu) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        menu.findItem(R.id.resync_action).setVisible(getLyrics().isLRC());
        menu.findItem(R.id.convert_action).setVisible(getLyrics().isLRC());
        menu.findItem(R.id.save_action).setVisible(!sharedPreferences.getBoolean("pref_auto_save", true));
        menu.findItem(R.id.action_vote).setVisible("user-submission".equals(getLyrics().getSource()));
        MenuItem romanizeMenuItem = menu.findItem(R.id.romanize_action); // .setVisible(RomanizeUtil.detectIdeographic(getLyrics().getText()));

        if (romanizeMenuItem != null && getLyrics() != null && getLyrics().getText() != null && getLyrics().getFlag() == Lyrics.POSITIVE_RESULT) {
            boolean isIdeographic = RomanizeUtil.detectIdeographic(getLyrics().getText());
            Lyrics storedLyrics = null;
            if (!isIdeographic) {
                storedLyrics = getLyrics() == null ? null :
                        DatabaseHelper.getInstance(getContext()).get(new String[]{
                                getLyrics().getArtist(),
                                getLyrics().getTitle(),
                                getLyrics().getOriginalArtist(),
                                getLyrics().getOriginalTitle()});
            }
            romanizeMenuItem.setVisible(isIdeographic ||
                    (storedLyrics != null && RomanizeUtil.detectIdeographic(storedLyrics.getText())));
            romanizeMenuItem.setTitle(isIdeographic ? R.string.romanize : R.string.reset);

            if (getLyrics().getFlag() == Lyrics.POSITIVE_RESULT
                    && sharedPreferences.getBoolean("pref_auto_save", true)) {
                if (storedLyrics == null || (getLyrics().isLRC() && !getLyrics().isLRC())) {
                    lyricsPresentInDB = true;
                    new WriteToDatabaseTask().execute(this, menu.findItem(R.id.save_action), getLyrics());
                }
                menu.findItem(R.id.save_action).setVisible(false);
            }
        } else {
            romanizeMenuItem.setVisible(false);
        }

        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View toolbarChild = toolbar.getChildAt(i);
            if (toolbarChild instanceof ActionMenuView) {
                ViewGroup actionBarContainer = (ViewGroup) toolbarChild;
                for (int j = 0; j < actionBarContainer.getChildCount(); j++) {
                    View v = actionBarContainer.getChildAt(j);
                    v.setOnLongClickListener(menuItemLongClickListener);
                }
                break;
            }
        }
    }

    public void setNetworkCallback(ConnectivityManager.NetworkCallback networkCallback) {
        this.networkCallback = networkCallback;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("SetTextI18n")
    protected void update(Lyrics lyrics, boolean animation) {
        final TextView id3TV = findViewById(R.id.source_tv);
        TextView writerTV = findViewById(R.id.writer_tv);
        TextView copyrightTV = findViewById(R.id.copyright_tv);

        new PresenceChecker(this).execute(getContext(), new String[]{lyrics.getArtist(), lyrics.getTitle(),
                lyrics.getOriginalArtist(), lyrics.getOriginalTitle()});

        if (lyrics == null)
            lyrics = new Lyrics(Lyrics.ERROR);

        boolean sameLyrics = getLyrics() != null && getLyrics().getArtist() != null && getLyrics().getTitle() != null
                && getLyrics().getArtist().equals(lyrics.getArtist()) && getLyrics().getTitle().equals(lyrics.getTitle());
        setLyrics(lyrics);
        toolbar.setTitle(lyrics.getTitle());
        toolbar.setSubtitle(lyrics.getArtist());
        copyrightTV.setText(lyrics.getCopyright() != null ? "Copyright: " + lyrics.getCopyright() : "");
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

        if (lyrics.getFlag() == Lyrics.POSITIVE_RESULT) {
            notifyLyricsDisplayed(lyrics, OnlineAccessVerifier.check(getContext()));
            if (!lyrics.isLRC()) {
                lyricsSwitcher.setVisibility(View.VISIBLE);
                lrcView.setVisibility(View.GONE);
                String[] paragraphs = Html.fromHtml(lyrics.getText()).toString().split("(\\s*\\n\\s*){2,}");
                View[] lyricsViews = new View[paragraphs.length + 2];
                int viewsIndex = 0;
                int adsCount = 0;
                LyricsTextFactory factory = new LyricsTextFactory(new ContextThemeWrapper(getContext(), getSelectedTheme()), false);

                for (int i = 0; i < paragraphs.length; i++) {
                    TextView paragraph = (TextView) factory.makeView();
                    paragraph.setText(paragraphs[i]);
                    lyricsViews[viewsIndex++] = paragraph;
                    if (i == paragraphs.length - 1)
                        break;
                }

                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                LinearLayout linearLayout = new LinearLayout(getContext());
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
                        layoutParams.setMargins(0, 0, 0, 25 * (int) getResources().getDimension(R.dimen.dp));
                        lyricsView.setLayoutParams(layoutParams);
                    }
                }
                if (lyricsSwitcher.getChildCount() > 1)
                    lyricsSwitcher.removeViewAt(0);
                lyricsSwitcher.addView(linearLayout);
                if (animation)
                    lyricsSwitcher.showNext();
                else {
                    Animation in = lyricsSwitcher.getInAnimation();
                    Animation out = lyricsSwitcher.getOutAnimation();
                    lyricsSwitcher.setInAnimation(null);
                    lyricsSwitcher.setOutAnimation(null);
                    lyricsSwitcher.showNext();
                    lyricsSwitcher.setInAnimation(in);
                    lyricsSwitcher.setOutAnimation(out);
                }
            } else {
                lyricsSwitcher.setVisibility(View.GONE);
                lrcView.setVisibility(View.VISIBLE);
                lrcView.setOriginalLyrics(lyrics);
                lrcView.setSourceLrc(lyrics.getText());
                updateLRC();
            }

            bugLayout.setVisibility(View.INVISIBLE);
            id3TV.setVisibility(View.VISIBLE);
            id3TV.setMovementMethod(LinkMovementMethod.getInstance());
            if ("Storage".equals(lyrics.getSource())) {
                SpannableString text = new SpannableString(getResources().getString(R.string.from_id3));
                text.setSpan(new UnderlineSpan(), 1, text.length() - 1, 0);
                id3TV.setText(text);
            } else {
                id3TV.setOnClickListener(null);
                SpannableString text = new SpannableString("Lyrics licensed & provided by LyricFind");
                int start = text.toString().indexOf("LyricFind");
                text.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.lyricfind.com"));
                        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                        getContext().startActivity(intent);
                    }
                }, start, start + 9, 0);
                text.setSpan(new UnderlineSpan(), start, start + 9, 0);
                id3TV.setText(text);
            }
            RatingUtils.trackSuccess(getContext());

            scrollview.post(() -> {
                scrollview.scrollTo(0, 0); //only useful when coming from localLyricsFragment
                scrollview.smoothScrollTo(0, 0);
            });
        } else {
            if (lyricsSwitcher.getChildCount() > 1)
                lyricsSwitcher.removeViewAt(0);
            lyricsSwitcher.addView(new View(getContext()));
            lyricsSwitcher.showNext();
            lyricsSwitcher.setVisibility(View.INVISIBLE);
            lrcView.setVisibility(View.INVISIBLE);
            bugLayout.setVisibility(View.VISIBLE);
            int message = -1;
            if (lyrics.getFlag() == Lyrics.ERROR || !OnlineAccessVerifier.check(getContext())) {
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
                notifyLyricsError(lyrics);
            } else {
                message = R.string.no_results;
                notifyLyricsNotFound(lyrics);
            }
            if (message != -1)
                ((TextView) bugLayout.findViewById(R.id.bugtext)).setText(message);
            else
                ((TextView) bugLayout.findViewById(R.id.bugtext)).setText("Code: " + lyrics.getErrorCode());
            id3TV.setVisibility(View.GONE);
            RatingUtils.trackFail(getContext());
        }

        refreshToolbar(toolbar.getMenu());
        stopRefreshAnimation();
    }

    private void notifyLyricsDisplayed(Lyrics lyrics, boolean online) {
        SharedPreferences preferences = getContext().getSharedPreferences("current_music", Context.MODE_PRIVATE);
        Bundle bundle = new Bundle();
        String artist = lyrics.getArtist();
        String track = lyrics.getTitle();
        bundle.putString("artist", artist);
        bundle.putString("title", track);
        bundle.putBoolean("online", online);
        bundle.putBoolean("acoustid_used", lyrics.wasAcoustIDUsed());
        bundle.putBoolean("has_fingerprint", !TextUtils.isEmpty(lyrics.getAudiofingerprint()));
        bundle.putBoolean("has_storage_access", PermissionsChecker.hasPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE));

        String player = preferences.getString("player", null);
        String currentTrack = preferences.getString("track", null);
        String currentArtist = preferences.getString("artist", null);
        if (player != null && track.equalsIgnoreCase(currentTrack) && artist.equalsIgnoreCase(currentArtist))
            bundle.putString("player", player);
    }

    private void notifyLyricsNotFound(Lyrics lyrics) {
        SharedPreferences preferences = getContext().getSharedPreferences("current_music", Context.MODE_PRIVATE);
        String artist = lyrics.getArtist();
        String track = lyrics.getTitle();
        Bundle bundle = new Bundle();
        bundle.putString("artist", artist);
        bundle.putString("title", track);
        bundle.putBoolean("acoustid_used", lyrics.wasAcoustIDUsed());
        bundle.putBoolean("has_fingerprint", !TextUtils.isEmpty(lyrics.getAudiofingerprint()));
        bundle.putBoolean("has_storage_access", PermissionsChecker.hasPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE));

        String player = preferences.getString("player", null);
        String currentTrack = preferences.getString("track", null);
        String currentArtist = preferences.getString("artist", null);
        if (player != null && track.equalsIgnoreCase(currentTrack) && artist.equalsIgnoreCase(currentArtist))
            bundle.putString("player", player);
    }

    private void notifyLyricsError(Lyrics lyrics) {
        SharedPreferences preferences = getContext().getSharedPreferences("current_music", Context.MODE_PRIVATE);
        String artist = lyrics.getArtist();
        String track = lyrics.getTitle();
        Bundle bundle = new Bundle();
        bundle.putString("artist", artist);
        bundle.putString("title", track);
        bundle.putBoolean("acoustid_used", lyrics.wasAcoustIDUsed());
        bundle.putBoolean("has_fingerprint", !TextUtils.isEmpty(lyrics.getAudiofingerprint()));
        bundle.putBoolean("has_storage_access", PermissionsChecker.hasPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE));

        String player = preferences.getString("player", null);
        String currentTrack = preferences.getString("track", null);
        String currentArtist = preferences.getString("artist", null);
        if (player != null && track.equalsIgnoreCase(currentTrack) && artist.equalsIgnoreCase(currentArtist))
            bundle.putString("player", player);
    }

    private void startRefreshAnimation() {
        bugLayout.setVisibility(GONE);
        if (!(viewSwitcher.getCurrentView() instanceof ProgressBar))
            viewSwitcher.showNext();
        this.mRefreshing = true;
    }

    private void stopRefreshAnimation() {
        if (viewSwitcher.getCurrentView() instanceof ProgressBar)
            viewSwitcher.showNext();
        this.mRefreshing = false;
    }

    public void fetchLyrics(String player, long duration, String... params) {
        String artist = params[0];
        String title = params[1];
        String url = null;
        if (params.length > 2)
            url = params[2];
        startRefreshAnimation();

        Lyrics lyrics = null;
        if (artist != null && title != null) {
            if (url == null &&
                    (getContext().getSharedPreferences("intro_slides", Context.MODE_PRIVATE).getBoolean("seen", false))
                    && (getLyrics() == null || getLyrics().getFlag() != Lyrics.POSITIVE_RESULT ||
                    !("Storage".equals(getLyrics().getSource())
                            && artist.equalsIgnoreCase(getLyrics().getArtist())
                            && title.equalsIgnoreCase(getLyrics().getTitle()))) &&
                    PermissionsChecker.hasPermission(getContext(), "android.permission.READ_EXTERNAL_STORAGE"))
                lyrics = Id3Reader.getLyrics(getContext(), artist, title, true);

            if (lyrics == null && !(player != null && player.contains("youtube")))
                lyrics = DatabaseHelper.getInstance(getContext()).get(new String[]{artist, title});

            if (lyrics == null && !(player != null && player.contains("youtube")))
                lyrics = DatabaseHelper.getInstance(getContext()).get(DownloadThread.correctTags(artist, title));
        }
        boolean prefLRC = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean("pref_lrc", true);
        if (lyrics == null && OnlineAccessVerifier.check(getContext())) {
            DownloadThread.LRC = prefLRC;

            toolbar.setTitle(title);
            toolbar.setSubtitle(artist);

            boolean positionAvailable = MediaControllerCallback.getActiveControllerPosition(getContext()) != -1;

            if (url == null)
                new DownloadThread(new WeakReference<>(this), player, duration, Id3Reader.getFile(getContext(), artist, title, true), artist, title).start();
            else
                new DownloadThread(new WeakReference<>(this), player, 0L, null, url, artist, title).start();

        } else if (lyrics != null)
            onLyricsDownloaded(lyrics);
        else {
            lyrics = new Lyrics(Lyrics.ERROR);
            lyrics.setArtist(artist);
            lyrics.setTitle(title);
            onLyricsDownloaded(lyrics);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.open_app_action:
                Intent mainApp = new Intent(getContext(), MainActivity.class);
                mainApp.setAction("android.intent.action.MAIN");
                mainApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                getContext().startActivity(mainApp);
                ((LyricsOverlayService) getTag()).onBackpressed();
                break;
            case R.id.share_action:
                final Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                if (getLyrics() != null && getLyrics().getURL() != null) {
                    sendIntent.putExtra(Intent.EXTRA_TEXT, getLyrics().getURL());
                    Intent intent = Intent.createChooser(sendIntent, getContext().getResources().getString(R.string.share));
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(intent);
                }
                return true;
            case R.id.save_action:
                if (getLyrics() != null && getLyrics().getFlag() == Lyrics.POSITIVE_RESULT)
                    new WriteToDatabaseTask().execute(this, item, getLyrics());
                break;
            case R.id.action_vote:
                if (getLyrics() != null && "user-submission".equals(getLyrics().getSource())) {
                    AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                            .setTitle(R.string.user_submission_dialog_title)
                            .setSingleChoiceItems(R.array.vote_options, -1, (dialog, which) -> {
                                if (which == 0) {
                                    // TODO Submit
                                    Toast.makeText(getContext(), R.string.lyrics_saved, Toast.LENGTH_SHORT).show();
                                    getLyrics().setSource("approved");
                                    refreshToolbar(toolbar.getMenu());
                                } else if (which == 1) {
                                    getLyrics().setSource("disapproved");
                                    refreshToolbar(toolbar.getMenu());
                                    RatingUtils.trackFail(getContext());
                                    RatingUtils.trackFail(getContext());
                                }
                                dialog.dismiss();
                            }).create();
                    alertDialog.getWindow().setType(Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    alertDialog.show();
                }
                break;
            case R.id.resync_action:
                MainActivity.resync(getContext());
                break;
            case R.id.convert_action:
                if (getLyrics().isLRC()) {
                    if (lrcView != null && lrcView.dictionnary != null) {
                        Lyrics staticLyrics = lrcView.getStaticLyrics();
                        if (staticLyrics != null)
                            update(staticLyrics, true);
                    }
                } else {
                    Lyrics lyrics = DatabaseHelper.getInstance(getContext()).get(new String[]{getLyrics().getArtist(), getLyrics().getTitle(),
                            getLyrics().getOriginalArtist(), getLyrics().getOriginalTitle()});
                    if (lyrics != null)
                        update(lyrics, true);
                }
                break;
            case R.id.romanize_action:
                if (RomanizeUtil.detectIdeographic(getLyrics().getText())) {
                    if (RomanizeUtil.isRomanizerInstalled(getContext())) {
                        Lyrics lyrics = getLyrics();
                        startRefreshAnimation();
                        new RomanizeAsyncTask(getContext(), this).execute(lyrics);
                    } else {
                        AlertDialog dialog = new AlertDialog.Builder(getContext())
                                .setTitle(R.string.romanizer_prompt_title)
                                .setMessage(R.string.romanizer_prompt_msg).setIcon(R.drawable.splash_icon)
                                .setCancelable(true)
                                .setPositiveButton("Google Play", (dialogInterface, i) -> {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.quicklyric.romanizer"));
                                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                                    getContext().startActivity(intent);
                                }).create();
                        dialog.getWindow().setType(Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                        dialog.show();
                    }
                } else {
                    Lyrics lyrics = DatabaseHelper.getInstance(getContext())
                            .get(new String[]{getLyrics().getArtist(), getLyrics().getTitle(),
                                    getLyrics().getOriginalArtist(), getLyrics().getOriginalTitle()});
                    if (lyrics != null)
                        update(lyrics, true);
                }
                break;
        }
        return false;
    }

    private void reopenOverlay() {
        if (!App.isMainActivityVisible()) {
            getContext().startService(new Intent(getContext().getApplicationContext(), LyricsOverlayService.class)
                    .setAction(LyricsOverlayService.CLICKED_FLOATING_ACTION));
        }
    }

    public void updateLRC() {
        if (mLrcThread == null || !mLrcThread.isAlive()) {
            mLrcThread = new Thread(lrcUpdater);
            mLrcThread.start();
        }
    }

    @Override
    public void onPresenceChecked(boolean present) {
        if (lyricsPresentInDB != present) {
            lyricsPresentInDB = present;
            if (toolbar != null)
                refreshToolbar(toolbar.getMenu());
        }
    }

    private Runnable lrcUpdater = new Runnable() {
        @Override
        public void run() {
            if (getWindowToken() == null)
                return;
            if (OverlayContentLayout.this == null)
                return;
            SharedPreferences preferences = OverlayContentLayout.this.getContext()
                    .getSharedPreferences("current_music", Context.MODE_PRIVATE);
            final long[] position = new long[] {MediaControllerCallback.getActiveControllerPosition(getContext())};
            long duration = preferences.getLong("duration", -1);
            String player = preferences.getString("player", "");
            if (lrcView == null)
                OverlayContentLayout.this.findViewById(R.id.lrc_view);

            if (lrcView != null) {
                boolean songIsTooShort = duration > 0 && lrcView.getLastLinePosition() > duration;
                boolean youtubeSongIsTooLong = player.contains("youtube") && duration - 60000 > lrcView.getLastLinePosition();
                if (OverlayContentLayout.this != null && (position[0] == -1 || !PreferenceManager.getDefaultSharedPreferences(lrcView.getContext()).getBoolean("pref_lrc", true))
                        || songIsTooShort || youtubeSongIsTooLong) {
                    final Lyrics staticLyrics = lrcView.getStaticLyrics();
                    runOnUiThread(() -> update(staticLyrics, true));
                    return;
                } else {
                    runOnUiThread(() -> {
                        if (lrcView != null)
                            lrcView.changeCurrent(position[0]);
                    });
                }
            }

            MusicBroadcastReceiver.forceAutoUpdate(true);
            while (preferences.getString("track", "").equalsIgnoreCase(getLyrics().getOriginalTitle()) &&
                    preferences.getString("artist", "").equalsIgnoreCase(getLyrics().getOriginalArtist()) &&
                    preferences.getBoolean("playing", true)) {
                if (getWindowToken() == null)
                    return;
                position[0] = MediaControllerCallback.getActiveControllerPosition(getContext());
                runOnUiThread(() -> {
                    if (lrcView == null)
                        lrcView = findViewById(R.id.lrc_view);
                    if (lrcView != null)
                        lrcView.changeCurrent(position[0]);
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
        }
    };

    private static void runOnUiThread(Runnable runnable) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }

    @Override
    public void onLyricsRomanized(Lyrics result) {
        update(result, true);
    }

    @Override
    public void onLyricsDownloaded(Lyrics lyrics) {
        update(lyrics, true);
    }

    @Override
    public void onMetadataParsed(String[] metadata, long duration, final boolean showMsg, boolean requestPermission, boolean noDoubleBroadcast) {
        if (metadata[0] == null)
            metadata[0] = "";
        if (metadata[1] == null)
            metadata[1] = "";
        if (getLyrics() != null && metadata[0].equalsIgnoreCase(getLyrics().getOriginalArtist())
                && metadata[1].equalsIgnoreCase(getLyrics().getOriginalTitle())
                && (!"Storage".equals(getLyrics().getSource()) || ("Storage".equals(getLyrics().getSource()) && noDoubleBroadcast))
                && getLyrics().getFlag() == Lyrics.POSITIVE_RESULT) {
            // No need to refresh
            stopRefreshAnimation();
            if (getLyrics().isLRC()) {
                if (!((LrcView)findViewById(R.id.lrc_view)).hasLyrics())
                    update(getLyrics(), false);
                updateLRC();
            }
        } else {
            fetchLyrics(metadata.length > 2 ? metadata[2] : null, duration, metadata[0], metadata[1]);
        }
    }

    public void onOpened() {
        LyricsOverlayService service = (LyricsOverlayService) getTag();
        if (!service.launchCountRaised) {
            LaunchesCounter.increaseLaunchCount(getContext(), true);
            service.launchCountRaised = true;
        }
    }

    public void unregisterNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && networkCallback != null)
            try {
                ((ConnectivityManager) getContext().getApplicationContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE)).unregisterNetworkCallback(networkCallback);
            } catch (IllegalArgumentException ignored) {
            }

    }
}
