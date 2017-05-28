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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.model.Lyrics;
import com.geecko.QuickLyric.utils.RomanizeUtil;

public class RomanizeAsyncTask extends AsyncTask<Lyrics, Void, Lyrics> {

    private final Context mContext;
    private ProgressDialog mProgressDialog;

    public RomanizeAsyncTask(Context context) {
        mContext = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setTitle(R.string.loading);
        mProgressDialog.show();
    }

    @Override
    protected Lyrics doInBackground(Lyrics... params) {
        Lyrics lyrics = params[0];
        lyrics.setText(RomanizeUtil.romanize(params[0].getText()));
        lyrics.setArtist(RomanizeUtil.romanize(params[0].getArtist()));
        lyrics.setTitle(RomanizeUtil.romanize(params[0].getTitle()));
        lyrics.setReported(true);
        return lyrics;
    }

    @Override
    protected void onPostExecute(Lyrics result) {
        super.onPostExecute(result);
        mProgressDialog.dismiss();
        ((MainActivity)mContext).updateLyricsFragment(0, 0, false, result);
    }
}