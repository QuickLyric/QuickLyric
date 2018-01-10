package com.geecko.QuickLyric.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.geecko.QuickLyric.event.RecentsAddedEvent;
import com.geecko.QuickLyric.event.RecentsRemovedEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import org.greenrobot.eventbus.EventBus;

import java.util.LinkedList;

/**
 * Created by steve on 4/7/17.
 */

public class Recents {

    private static Recents instance;

    private static Context context;

    private static final String PREFS_NAME = "recent_tracks_pref";
    private static final int MAX_ENTRIES = 30;

    private LinkedList<Track> tracks;

    private Recents() {
        tracks = new LinkedList<>();
    }

    public Track get(int i) {
        return tracks.get(i);
    }

    public void add(Track track) {
        if (track.mTitle.equals("") && track.mArtist.equals("") ||
                tracks.size() > 0 && track.equals(tracks.get(0))) {
            return;
        }

        int i = tracks.indexOf(track);
        if (i >= 0) {
            tracks.remove(i);
            EventBus.getDefault().post(new RecentsRemovedEvent(i));
        }
        tracks.addFirst(track);
        EventBus.getDefault().post(new RecentsAddedEvent(0));

        if (tracks.size() > MAX_ENTRIES) {
            tracks.removeLast();
            EventBus.getDefault().post(new RecentsRemovedEvent(MAX_ENTRIES - 1));
        }

        saveToPrefs();
    }

    public int size() {
        return tracks.size();
    }

    private void saveToPrefs() {
        SharedPreferences.Editor edit =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        Gson gson = new GsonBuilder().create();
        edit.putString(PREFS_NAME, gson.toJson(this));
        edit.apply();
    }

    public synchronized static Recents getInstance(Context context) {
        if (instance == null) {
            Recents.context = context.getApplicationContext();
            SharedPreferences prefs =
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Gson gson = new GsonBuilder().create();
            String str = prefs.getString(PREFS_NAME, "");
            if (str.equals("")) {
                instance = new Recents();
            } else {
                try {
                    instance = gson.fromJson(str, Recents.class);
                } catch (JsonSyntaxException e) {
                    instance = new Recents();
                }
            }
        }
        return instance;
    }
}
