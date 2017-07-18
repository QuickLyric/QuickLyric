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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.broadcastReceiver.MusicBroadcastReceiver;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.services.NotificationListenerService;
import com.geecko.QuickLyric.tasks.DownloadThread;
import com.geecko.QuickLyric.tasks.Id3Reader;
import com.geecko.QuickLyric.tasks.ParseTask;
import com.geecko.QuickLyric.tasks.PresenceChecker;
import com.geecko.QuickLyric.tasks.RomanizeAsyncTask;
import com.geecko.QuickLyric.tasks.WriteToDatabaseTask;
import com.geecko.QuickLyric.utils.DatabaseHelper;
import com.geecko.QuickLyric.utils.LyricsTextFactory;
import com.geecko.QuickLyric.utils.NightTimeVerifier;
import com.geecko.QuickLyric.utils.OnlineAccessVerifier;
import com.geecko.QuickLyric.utils.RomanizeUtil;
import com.google.firebase.analytics.FirebaseAnalytics;

import static com.google.android.gms.internal.zzail.runOnUiThread;


public class OverlayContentLayout extends LinearLayout implements Toolbar.OnMenuItemClickListener, PresenceChecker.PresenceCheckerCallback,
        RomanizeAsyncTask.RomanisationCallback, Lyrics.Callback, ParseTask.ParseCallback {

    private View bugLayout;
    private LrcView lrcView;
    private Toolbar toolbar;
    private View lyricsContent;
    private TextSwitcher textSwitcher;
    private ViewSwitcher viewFlipper;
    private NonFocusableNestedScrollView scrollview;

    private OnLongClickListener menuItemLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            return true;
        }
    };
    private Lyrics mLyrics;
    private boolean lyricsPresentInDB;
    private Thread mLrcThread;

    public OverlayContentLayout(Context context, @Nullable AttributeSet attrs) {
        super(context instanceof ContextThemeWrapper ? ((ContextThemeWrapper) context).getBaseContext() : context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        toolbar = findViewById(R.id.overlay_toolbar);
        viewFlipper = findViewById(R.id.overlay_flipper);
        bugLayout = findViewById(R.id.error_msg);
        lyricsContent = findViewById(R.id.lyrics_content);
        scrollview = findViewById(R.id.scrollview);
        textSwitcher = lyricsContent.findViewById(R.id.switcher);
        lrcView = lyricsContent.findViewById(R.id.lrc_view);
        textSwitcher.setFactory(new LyricsTextFactory(new ContextThemeWrapper(getContext(), getSelectedTheme())));
        ((TextView) textSwitcher.getChildAt(0)).setTextIsSelectable(false);
        ((TextView) textSwitcher.getChildAt(1)).setTextIsSelectable(false);

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
        int themeNum = Premium.isPremium(getContext()) ? Integer.valueOf(sharedPref.getString("pref_theme", "0")) : 0;
        boolean nightMode = sharedPref.getBoolean("pref_night_mode", false);
        if (nightMode && Premium.isPremium(getContext()) && NightTimeVerifier.check(getContext()))
            selectedTheme = R.style.Theme_QuickLyric_Night;
        else
            selectedTheme = themes[themeNum];
        return selectedTheme;
    }

    private Lyrics getLyrics() {
        Lyrics output = ((LyricsOverlayService) getTag()).getLyrics();
        output = output == null ? new Lyrics(Lyrics.NO_RESULT) : output;
        return output;
    }

    public void setLyrics(Lyrics lyrics) {
        ((LyricsOverlayService) getTag()).setLyrics(lyrics);
    }

    private void refreshToolbar(Menu menu) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        menu.findItem(R.id.resync_action).setVisible(mLyrics.isLRC());
        menu.findItem(R.id.convert_action).setVisible(mLyrics.isLRC());
        menu.findItem(R.id.save_action).setVisible(!sharedPreferences.getBoolean("pref_auto_save", true));
        // menu.findItem(R.id.action_vote).setVisible("user-submission".equals(mLyrics.getSource())); FIXME
        menu.findItem(R.id.romanize_action).setVisible(RomanizeUtil.detectIdeographic(mLyrics.getText()));

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            View v = toolbar.findViewById(item.getItemId());
            if (v != null) {
                ViewGroup vg = (ViewGroup) v.getParent();
                for (int j = 0; j < vg.getChildCount(); j++) {
                    View subView = vg.getChildAt(j);
                    subView.setOnLongClickListener(menuItemLongClickListener);
                }
                break;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("SetTextI18n")
    private void update(Lyrics lyrics, boolean animation) {
        final TextView id3TV = findViewById(R.id.source_tv);
        TextView writerTV = findViewById(R.id.writer_tv);
        TextView copyrightTV = findViewById(R.id.copyright_tv);

        new PresenceChecker(this).execute(getContext(), new String[]{lyrics.getArtist(), lyrics.getTitle(),
                lyrics.getOriginalArtist(), lyrics.getOriginalTrack()});

        if (lyrics == null)
            lyrics = new Lyrics(Lyrics.ERROR);
        mLyrics = lyrics;
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
            //TODO: Firebase Event
            //TODO: Report view
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
                        getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.lyricfind.com")));
                    }
                }, start, start + 9, 0);
                text.setSpan(new UnderlineSpan(), start, start + 9, 0);
                id3TV.setText(text);
            }

            scrollview.post(new Runnable() {
                @Override
                public void run() {
                    scrollview.scrollTo(0, 0); //only useful when coming from localLyricsFragment
                    scrollview.smoothScrollTo(0, 0);
                }
            });
            showAdView(true);
        } else {
            textSwitcher.setText("");
            textSwitcher.setVisibility(View.INVISIBLE);
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
            } else {
                message = R.string.no_results;
                // TODO: firebase
            }
            if (message != -1)
                ((TextView) bugLayout.findViewById(R.id.bugtext)).setText(message);
            else
                ((TextView) bugLayout.findViewById(R.id.bugtext)).setText("Code: " + lyrics.getErrorCode());
            id3TV.setVisibility(View.GONE);
        }

        refreshToolbar(toolbar.getMenu());
        stopRefreshAnimation();
    }

    private void startRefreshAnimation() {
        bugLayout.setVisibility(GONE);
        if (!(viewFlipper.getCurrentView() instanceof ProgressBar))
            viewFlipper.showNext();
    }

    private void stopRefreshAnimation() {
        if (viewFlipper.getCurrentView() instanceof ProgressBar)
            viewFlipper.showNext();
    }

    private void showAdView(boolean b) {
        //TODO
    }

    public void fetchLyrics(String... params) {
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
                    && (mLyrics == null || mLyrics.getFlag() != Lyrics.POSITIVE_RESULT ||
                    !("Storage".equals(mLyrics.getSource())
                            && mLyrics.getArtist().equalsIgnoreCase(artist)
                            && mLyrics.getTitle().equalsIgnoreCase(title))
            ))
                lyrics = Id3Reader.getLyrics(getContext(), artist, title);

            if (lyrics == null)
                lyrics = DatabaseHelper.getInstance(getContext()).get(new String[]{artist, title});

            if (lyrics == null)
                lyrics = DatabaseHelper.getInstance(getContext()).get(DownloadThread.correctTags(artist, title));
        }
        boolean prefLRC = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean("pref_lrc", true);
        if (lyrics == null && OnlineAccessVerifier.check(getContext())) {
            DownloadThread.LRC = prefLRC;

            toolbar.setTitle(title);
            toolbar.setSubtitle(artist);

            SharedPreferences preferences = getContext().getSharedPreferences("current_music", Context.MODE_PRIVATE);
            boolean positionAvailable = preferences.getLong("position", 0) != -1;

            if (url == null)
                new DownloadThread(this, positionAvailable, artist, title).start();
            else
                new DownloadThread(this, positionAvailable, url, artist, title).start();

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
            case R.id.share_action:
                final Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                if (mLyrics != null && mLyrics.getURL() != null) {
                    sendIntent.putExtra(Intent.EXTRA_TEXT, mLyrics.getURL());
                    getContext().startActivity(Intent.createChooser(sendIntent, getContext().getResources().getString(R.string.share)));
                }
                return true;
            case R.id.save_action:
                if (mLyrics != null && mLyrics.getFlag() == Lyrics.POSITIVE_RESULT)
                    new WriteToDatabaseTask().execute(this, item, this.mLyrics);
                break;
            case R.id.action_vote:
                if (mLyrics != null && "user-submission".equals(mLyrics.getSource())) {
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.user_submission_dialog_title)
                            .setSingleChoiceItems(R.array.vote_options, -1, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == 0) {
                                        // TODO Submit
                                        Toast.makeText(getContext(), R.string.lyrics_saved, Toast.LENGTH_SHORT).show();
                                        mLyrics.setSource("approved");
                                        FirebaseAnalytics.getInstance(getContext()).logEvent("crowdsourced_lyrics_good", new Bundle());
                                        refreshToolbar(toolbar.getMenu());
                                    } else if (which == 1) {
                                        mLyrics.setSource("disapproved");
                                        refreshToolbar(toolbar.getMenu());
                                        FirebaseAnalytics.getInstance(getContext()).logEvent("crowdsourced_lyrics_bad", new Bundle());
                                    }
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
                break;
            case R.id.resync_action:
                MainActivity.resync(getContext());
                break;
            case R.id.convert_action:
                if (mLyrics.isLRC()) {
                    if (lrcView != null && lrcView.dictionnary != null)
                        update(lrcView.getStaticLyrics(), true);
                } else
                    update(DatabaseHelper.getInstance(getContext())
                            .get(new String[]{mLyrics.getArtist(), mLyrics.getTitle(),
                                    mLyrics.getOriginalArtist(), mLyrics.getOriginalTrack()}), true);
                break;
            case R.id.romanize_action:
                if (RomanizeUtil.detectIdeographic(mLyrics.getText())) {
                    if (RomanizeUtil.isRomanizerInstalled(getContext())) {
                        Lyrics lyrics = mLyrics;
                        new RomanizeAsyncTask(getContext(), this).execute(lyrics);
                    } else {
                        AlertDialog dialog = new AlertDialog.Builder(getContext())
                                .setTitle(R.string.romanizer_prompt_title)
                                .setMessage(R.string.romanizer_prompt_msg).setIcon(R.drawable.splash_icon)
                                .setCancelable(true)
                                .setPositiveButton("Google Play", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.quicklyric.romanizer")));
                                    }
                                }).create();
                        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                        dialog.show();
                    }
                } else
                    update(DatabaseHelper.getInstance(getContext())
                            .get(new String[]{mLyrics.getArtist(), mLyrics.getTitle(),
                                    mLyrics.getOriginalArtist(), mLyrics.getOriginalTrack()}), true);
                break;
        }
        return false;
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
            long position = preferences.getLong("position", 0);
            if (lrcView == null)
                OverlayContentLayout.this.findViewById(R.id.lrc_view);

            if (lrcView != null)
                if (OverlayContentLayout.this != null && (position == -1 || !PreferenceManager.getDefaultSharedPreferences(lrcView.getContext()).getBoolean("pref_lrc", true))) {
                    final Lyrics staticLyrics = lrcView.getStaticLyrics();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update(staticLyrics, true);
                        }
                    });
                    return;
                } else {
                    final long finalPosition = position;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (lrcView != null)
                                lrcView.changeCurrent(finalPosition);
                        }
                    });
                }

            MusicBroadcastReceiver.forceAutoUpdate(true);
            while (preferences.getString("track", "").equalsIgnoreCase(mLyrics.getOriginalTrack()) &&
                    preferences.getString("artist", "").equalsIgnoreCase(mLyrics.getOriginalArtist()) &&
                    preferences.getBoolean("playing", true)) {
                if (getWindowToken() == null)
                    return;
                position = preferences.getLong("position", 0);
                long startTime = preferences.getLong("startTime", System.currentTimeMillis());
                long distance = System.currentTimeMillis() - startTime;
                if (preferences.getBoolean("playing", true))
                    position += distance;
                final long finalPosition = position;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (lrcView == null)
                            lrcView = findViewById(R.id.lrc_view);
                        if (lrcView != null)
                            lrcView.changeCurrent(finalPosition);
                    }
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

    @Override
    public void onLyricsRomanized(Lyrics result) {
        update(result, true);
    }

    @Override
    public void onLyricsDownloaded(Lyrics lyrics) {
        update(lyrics, true);
    }

    @Override
    public void onMetadataParsed(String[] metadata, boolean showMsg, boolean noDoubleBroadcast) {
        if (getLyrics() != null && metadata[0].equalsIgnoreCase(getLyrics().getOriginalArtist())
                && metadata[1].equalsIgnoreCase(getLyrics().getOriginalTrack())
                && (!"Storage".equals(getLyrics().getSource()) || ("Storage".equals(getLyrics().getSource()) && noDoubleBroadcast))
                && getLyrics().getFlag() == Lyrics.POSITIVE_RESULT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                    NotificationListenerService.restartNotificationListenerServiceIfNeeded(getContext()))
                new ParseTask(this, getContext(), showMsg, noDoubleBroadcast).execute();
            stopRefreshAnimation();
            if (mLyrics.isLRC())
                updateLRC();
        } else {
            fetchLyrics(metadata);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                    NotificationListenerService.restartNotificationListenerServiceIfNeeded(getContext()))
                new ParseTask(this, getContext(), showMsg, noDoubleBroadcast).execute();
        }
    }

    public void onOpened() {
    }
}
