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

package com.geecko.QuickLyric.tasks;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;

import com.geecko.QuickLyric.model.Lyrics;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;

public class RomanizeAsyncTask extends AsyncTask<Lyrics, Void, Lyrics> {

    private final WeakReference<Context> mContext;
    private final WeakReference<RomanisationCallback> callback;

    public RomanizeAsyncTask(Context context, RomanisationCallback callback) {
        mContext = new WeakReference<>(context);
        this.callback = new WeakReference<>(callback);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Lyrics doInBackground(final Lyrics... params) {
        final Lyrics lyrics = params[0];

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Intent romanizeIntent = new Intent();
        romanizeIntent.setComponent(new ComponentName("com.quicklyric.romanizer", "com.quicklyric.romanizer.RomanisationBroadcastReceiver"));
        romanizeIntent.setAction("com.geecko.QuickLyric.ROMANIZE");
        romanizeIntent.putExtra("inputs", new String[] {lyrics.getText(), lyrics.getTitle()});

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String[] outputs = intent.getExtras().getStringArray("output");
                if (outputs != null && outputs.length == 2) {
                    lyrics.setText(outputs[0]);
                    lyrics.setTitle(outputs[1]);
                }
                countDownLatch.countDown();
            }
        };

        mContext.get().registerReceiver(receiver, new IntentFilter("com.geecko.QuickLyric.ROMANIZED_OUTPUT"));
        mContext.get().sendBroadcast(romanizeIntent);

        lyrics.setReported(true);
        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {
        }
        return lyrics;
    }

    @Override
    protected void onPostExecute(Lyrics result) {
        super.onPostExecute(result);
        if (callback.get() != null)
            callback.get().onLyricsRomanized(result);
    }

    public interface RomanisationCallback {
        void onLyricsRomanized(Lyrics result);
    }
}