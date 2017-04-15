package com.geecko.QuickLyric.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.preference.PreferenceManager;

import com.geecko.QuickLyric.event.RecentsRetrievedEvent;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.tasks.DownloadThread;
import com.geecko.QuickLyric.utils.DatabaseHelper;

import org.greenrobot.eventbus.EventBus;

import java.util.Set;
import java.util.TreeSet;

public class SingleLyricRetrievalService extends IntentService implements Lyrics.Callback {
    private static final String ACTION_RETRIEVE = "com.geecko.QuickLyric.services.action.FOO";

    private static final String EXTRA_ARTIST = "com.geecko.QuickLyric.services.extra.PARAM1";
    private static final String EXTRA_TITLE = "com.geecko.QuickLyric.services.extra.PARAM2";

    public SingleLyricRetrievalService() {
        super("SingleLyricRetrievalService");
    }

    public static void startActionRetrieve(Context context, String param1, String param2) {
        Intent intent = new Intent(context, SingleLyricRetrievalService.class);
        intent.setAction(ACTION_RETRIEVE);
        intent.putExtra(EXTRA_ARTIST, param1);
        intent.putExtra(EXTRA_TITLE, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_RETRIEVE.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_ARTIST);
                final String param2 = intent.getStringExtra(EXTRA_TITLE);
                handleActionRetrieve(param1, param2);
            }
        }
    }

    private void handleActionRetrieve(String artist, String title) {
        String[] metadata = new String[2];
        metadata[0] = artist;
        metadata[1] = title;

        Lyrics lyrics = DatabaseHelper.getInstance(this).get(metadata);
        if (lyrics == null) {
            downloadLyrics(artist, title);
        }
        else
        {
            EventBus.getDefault().post(new RecentsRetrievedEvent(lyrics));
        }
    }


    private void downloadLyrics(String artist, String title) {
        Set<String> providersSet = PreferenceManager.getDefaultSharedPreferences(this)
                .getStringSet("pref_providers", new TreeSet<String>());
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_lrc", false))
            providersSet.add("ViewLyrics");
        DownloadThread.setProviders(providersSet);

        DownloadThread.getRunnable(this, true, artist, title).run();
    }

    @Override
    public void onLyricsDownloaded(Lyrics lyrics) {
        EventBus.getDefault().post(new RecentsRetrievedEvent(lyrics));
    }
}
