package be.geecko.QuickLyric.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ActionViewTarget;
import com.nineoldandroids.view.ViewHelper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import be.geecko.QuickLyric.MainActivity;
import be.geecko.QuickLyric.R;
import be.geecko.QuickLyric.adapter.DrawerAdapter;
import be.geecko.QuickLyric.lyrics.Lyrics;
import be.geecko.QuickLyric.tasks.CoverArtLoader;
import be.geecko.QuickLyric.tasks.DownloadTask;
import be.geecko.QuickLyric.tasks.ParseTask;
import be.geecko.QuickLyric.tasks.PresenceChecker;
import be.geecko.QuickLyric.tasks.WriteToDatabaseTask;
import be.geecko.QuickLyric.utils.CoverCache;
import be.geecko.QuickLyric.utils.LyricsTextFactory;
import be.geecko.QuickLyric.utils.OnlineAccessVerifier;
import be.geecko.QuickLyric.utils.ShowCaseCaller;
import be.geecko.QuickLyric.view.FadeInNetworkImageView;
import be.geecko.QuickLyric.view.ObservableScrollView;
import be.geecko.QuickLyric.view.RefreshIcon;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;

public class LyricsViewFragment extends Fragment implements ObservableScrollView.Callbacks {

    private static final int STATE_ONSCREEN = 0;
    private int mState = STATE_ONSCREEN;
    private static final int STATE_OFFSCREEN = 1;
    private static final int STATE_RETURNING = 2;
    private static BroadcastReceiver broadcastReceiver;
    public boolean lyricsPresentInDB;
    public DownloadTask currentDownload;
    public boolean isActiveFragment = false;
    public boolean showTransitionAnim = true;
    private Lyrics mLyrics;
    private boolean refreshAnimationFlag = false;
    private int mMinRawY = 0;
    private RelativeLayout mQuickReturnView;
    private ObservableScrollView mObservableScrollView;
    private ImageLoader.ImageCache imageCache = new CoverCache(1024);

    public LyricsViewFragment() {
    }

    public static void sendIntent(Context context, Intent intent) {
        if (broadcastReceiver != null)
            broadcastReceiver.onReceive(context, intent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        setHasOptionsMenu(true);
        if (savedInstanceState != null)
            try {
                this.mLyrics = Lyrics.fromBytes(savedInstanceState.getByteArray("lyrics"));
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
                        fetchLyrics(artist, track);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
        }
        View layout = inflater.inflate(R.layout.lyrics_view, container, false);
        if (layout != null) {
            TextSwitcher textSwitcher = (TextSwitcher) layout.findViewById(R.id.switcher);
            textSwitcher.setFactory(new LyricsTextFactory(layout.getContext()));

            FadeInNetworkImageView cover = (FadeInNetworkImageView) layout.findViewById(R.id.cover);
            cover.setDefaultImageResId(R.drawable.default_cover);

            mQuickReturnView = (RelativeLayout) layout.findViewById(R.id.frame);

            mObservableScrollView = ((ObservableScrollView) layout.findViewById(R.id.scrollview));
            mObservableScrollView.setCallbacks(this);

            if (mLyrics == null) {
                fetchCurrentLyrics();
            } else if (mLyrics.getFlag() == Lyrics.SEARCH_ITEM) {
                startRefreshAnimation(false);
                fetchLyrics(mLyrics.getArtist(), mLyrics.getTrack());
                ((TextView) (layout.findViewById(R.id.artist))).setText(mLyrics.getArtist());
                ((TextView) (layout.findViewById(R.id.song))).setText(mLyrics.getTrack());
            } else { //Rotation, resume
                update(mLyrics, layout);
            }
        }
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String artist = intent.getStringExtra("artist");
                String track = intent.getStringExtra("track");
                if (artist != null && track != null) {
                    startRefreshAnimation(false);
                    LyricsViewFragment.this.fetchLyrics(artist, track);
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

        //fixme? ShowcaseView gets called every time
        ShowcaseView scs1 = new ShowcaseView.Builder(getActivity())
                .singleShot(1l)
                .setTarget(new ActionViewTarget(getActivity(), ActionViewTarget.Type.TITLE))
                .setContentTitle(R.string.welcome)
                .setContentText(R.string.welcome2_sub)
                .hideOnTouchOutside().build();
        scs1.setOnShowcaseEventListener(new ShowCaseCaller(getActivity()));
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

    public void startRefreshAnimation(boolean wait) {
        this.refreshAnimationFlag = true;
        if (!wait && getActivity() != null)
            this.getActivity().supportInvalidateOptionsMenu();
    }

    void stopRefreshAnimation() {
        this.refreshAnimationFlag = false;
        getActivity().supportInvalidateOptionsMenu();
    }

    public void fetchLyrics(String... params) {
        String artist = params[0];
        String song = params[1];
        URL url = null;
        if (params.length > 2)
            try {
                url = new URL(params[2]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        this.startRefreshAnimation(true);
        if (currentDownload != null && currentDownload.getStatus() != AsyncTask.Status.FINISHED)
            currentDownload.cancel(true);
        this.currentDownload = new DownloadTask();
        currentDownload.execute(this.getActivity(), artist, song, url);
    }

    void fetchCurrentLyrics() {
        if (mLyrics != null && mLyrics.getArtist() != null && mLyrics.getTrack() != null)
            new ParseTask().execute(this, mLyrics);
        else
            new ParseTask().execute(this, null);
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
                                    NdefRecord.createApplicationRecord("be.geecko.QuickLyric"), // the "application record"
                            });
                        } catch (IOException e) {
                            return null;
                        }
                    }
                }, activity);
            }
        }
    }

    public void update(Lyrics lyrics, View layout) {
        TextSwitcher textSwitcher = ((TextSwitcher) layout.findViewById(R.id.switcher));
        TextView artistTV = ((TextView) layout.findViewById(R.id.artist));
        TextView songTV = (TextView) layout.findViewById(R.id.song);
        RelativeLayout bugLayout = (RelativeLayout) layout.findViewById(R.id.error_msg);
        if (mObservableScrollView.getScrollY() != 0) {
            mObservableScrollView.smoothScrollTo(0, 0);
        }
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
        new CoverArtLoader().execute(lyrics, this);

        if (lyrics.getFlag() == Lyrics.POSITIVE_RESULT) {
            textSwitcher.setText(Html.fromHtml(lyrics.getText()));
            bugLayout.setVisibility(View.INVISIBLE);
            EditText searchbox = (EditText) (getActivity()).findViewById(R.id.searchBox);
            searchbox.setText("");
            if (getActivity().findViewById(R.id.drawer_layout) instanceof DrawerLayout)
                ((DrawerLayout) getActivity().findViewById(R.id.drawer_layout)).closeDrawers();
        } else {
            textSwitcher.setText("");
            bugLayout.setVisibility(View.VISIBLE);
            int message;
            if (!OnlineAccessVerifier.check(getActivity())) {
                message = R.string.connection_error;
            } else {
                message = R.string.no_results;
                if (((MainActivity) getActivity()).getDisplayedFragment(((MainActivity) getActivity()).getActiveFragments()) == this) {
                    View drawer = (getActivity()).findViewById(R.id.drawer_layout);
                    if (drawer instanceof DrawerLayout)
                        ((DrawerLayout) drawer).openDrawer((getActivity()).findViewById(R.id.left_drawer));
                    EditText searchbox = (EditText) (getActivity()).findViewById(R.id.searchBox);
                    searchbox.setText(lyrics.getTrack());
                    searchbox.setSelection(songTV.length() - 1);
                }
            }
            ((TextView) bugLayout.findViewById(R.id.bugtext)).setText(message);
        }
        stopRefreshAnimation();
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
                    new WriteToDatabaseTask().execute(this, this.mLyrics);
                break;
        }

        return false;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {

        Animation anim = null;
        if (nextAnim != 0)
            anim = AnimationUtils.loadAnimation(getActivity(), nextAnim);

        if (anim != null) {
            anim.setAnimationListener(new Animation.AnimationListener() {

                public void onAnimationEnd(Animation animation) {
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationStart(Animation animation) {
                    if (refreshAnimationFlag)
                        LyricsViewFragment.this.getActivity().supportInvalidateOptionsMenu();
                    MainActivity mainActivity = (MainActivity) getActivity();
                    if (mainActivity.drawer instanceof DrawerLayout && ((DrawerLayout) mainActivity.drawer).isDrawerOpen(mainActivity.drawerView))
                        ((DrawerLayout) mainActivity.drawer).closeDrawer(mainActivity.drawerView);
                }
            });

            if (!showTransitionAnim)
                anim.setDuration(0);
            else
                showTransitionAnim = false;
        }
        return anim;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MainActivity mainActivity = (MainActivity) this.getActivity();
        ActionBar actionBar = mainActivity.getSupportActionBar();
        if (mainActivity.focusOnFragment && actionBar != null) // focus is on Fragment
        {
            inflater.inflate(R.menu.lyrics, menu);
            if (actionBar.getTitle() == null || !actionBar.getTitle().equals(this.getString(R.string.app_name)))
                actionBar.setTitle(R.string.app_name);
            MenuItem refreshMenuItem = menu.findItem(R.id.refresh_action);

            if (refreshMenuItem != null) {
                RefreshIcon refreshActionView = (RefreshIcon) ((ViewGroup) MenuItemCompat.getActionView(refreshMenuItem)).getChildAt(0);
                if (refreshActionView != null) {
                    if (refreshAnimationFlag)
                        refreshActionView.startAnimation();
                    else
                        refreshActionView.stopAnimation();
                    refreshActionView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            fetchCurrentLyrics();
                        }
                    });
                }
            }
            MenuItem saveMenuItem = menu.findItem(R.id.save_action);
            if (saveMenuItem != null) {
                saveMenuItem.setIcon(lyricsPresentInDB ? R.drawable.ic_trash : R.drawable.ic_save);
                saveMenuItem.setTitle(lyricsPresentInDB ? R.string.remove_action : R.string.save_action);

            }

        } else // focus is on Drawer
            menu.clear();
    }

    public void setCoverArt(String url, FadeInNetworkImageView cover) {
        MainActivity mainActivity = (MainActivity) LyricsViewFragment.this.getActivity();
        if (cover == null)
            cover = (FadeInNetworkImageView) mainActivity.findViewById(R.id.cover);
        cover.setImageBitmap(null);
        if (url != null) {
            mLyrics.setCoverURL(url);
            cover.setImageUrl(url, new ImageLoader(Volley.newRequestQueue(mainActivity), imageCache));
        }
        cover.setDefaultImageResId(R.drawable.default_cover);
        cover.setErrorImageResId(android.R.drawable.ic_menu_close_clear_cancel);
    }

    @Override
    public void onScrollChanged() {
        int cachedVerticalScrollRange = mObservableScrollView.computeVerticalScrollRange();
        int quickReturnHeight = mQuickReturnView.getMeasuredHeight();
        int top = mQuickReturnView.getTop();
        int rawY = top - Math.min(
                cachedVerticalScrollRange - mObservableScrollView.getHeight(),
                mObservableScrollView.getScrollY());
        int translationY = 0;

        switch (mState) {
            case STATE_OFFSCREEN:
                if (rawY <= mMinRawY) {
                    mMinRawY = rawY;
                } else {
                    mState = STATE_RETURNING;
                }
                translationY = rawY;
                break;

            case STATE_ONSCREEN:
                if (rawY < -quickReturnHeight) {
                    mState = STATE_OFFSCREEN;
                    mMinRawY = rawY;
                }
                translationY = rawY;
                break;

            case STATE_RETURNING:
                translationY = (rawY - mMinRawY) - quickReturnHeight;
                if (translationY > 0) {
                    translationY = 0;
                    mMinRawY = rawY - quickReturnHeight;
                }

                if (rawY > 0) {
                    mState = STATE_ONSCREEN;
                    translationY = rawY;
                }

                if (translationY < -quickReturnHeight) {
                    mState = STATE_OFFSCREEN;
                    mMinRawY = rawY;
                }
                break;
        }

        if (mObservableScrollView.getScrollY() != 0)
            translationY = mObservableScrollView.getScrollY() + translationY;
        else
            translationY = 0;
        ViewHelper.setTranslationY(mQuickReturnView, translationY);
    }
}
