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

package com.geecko.QuickLyric.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

//TODO This doesn't need to be an AsyncTask... Come on.
public class ParseTask extends AsyncTask<Object, Object, Object[]> {

    private final boolean showMsg;
    private final boolean noDoubleBroadcast;
    private final boolean requestPermission;
    private WeakReference<ParseCallback> callback;
    private WeakReference<Context> mContext;

    public ParseTask(ParseCallback callback, Context context, boolean showMsg, boolean requestPermission, boolean noDoubleBroadcast) {
        this.callback = new WeakReference<>(callback);
        this.mContext = new WeakReference<>(context);
        this.showMsg = showMsg;
        this.noDoubleBroadcast = noDoubleBroadcast;
        this.requestPermission = requestPermission;
    }

    @Override
    protected Object[] doInBackground(Object... arg0) {
        SharedPreferences preferences = mContext.get().getSharedPreferences("current_music", Context.MODE_PRIVATE);
        Object[] music = new Object[4];
        music[0] = preferences.getString("artist", null);
        music[1] = preferences.getString("track", null);
        music[2] = preferences.getString("player", null);
        music[3] = preferences.getLong("duration", 0L);
        return music;
    }

    @Override
    protected void onPostExecute(Object[] results) {
        if (callback.get() != null) {
            String[] metadata = new String[3];
            for (int i = 0; i < 3; i++) {
                metadata[i] = (String) results[i];
            }
            callback.get().onMetadataParsed(metadata, (Long) results[3], showMsg, requestPermission, noDoubleBroadcast);
        }
    }

    public interface ParseCallback {
        void onMetadataParsed(String[] metadata, long duration, boolean showMsg, boolean requestPermission, boolean noDoubleBroadcast);
    }
}
