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
import android.os.Process;
<<<<<<< HEAD
import android.widget.Toast;

import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.fragment.LyricsViewFragment;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.services.NotificationListenerService;
=======
>>>>>>> ec836836... Further changes to the floating lyrics

public class ParseTask extends AsyncTask<Object, Object, String[]> {

    private final boolean showMsg;
    private final boolean noDoubleBroadcast;
    private ParseCallback callback;
    private Context mContext;

    public ParseTask(ParseCallback callback, Context context, boolean showMsg, boolean noDoubleBroadcast) {
        this.callback = callback;
        this.mContext = context;
        this.showMsg = showMsg;
        this.noDoubleBroadcast = noDoubleBroadcast;
    }

    @Override
    protected String[] doInBackground(Object... arg0) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        SharedPreferences preferences = mContext.getSharedPreferences("current_music", Context.MODE_PRIVATE);
        String[] music = new String[2];
        music[0] = preferences.getString("artist", null);
        music[1] = preferences.getString("track", null);
        return music;
    }

    @Override
    protected void onPostExecute(String[] metaData) {
        callback.onMetadataParsed(metaData, showMsg, noDoubleBroadcast);
    }

    public interface ParseCallback {
        void onMetadataParsed(String[] metadata, boolean showMsg, boolean noDoubleBroadcast);
    }
}
