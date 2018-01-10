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
import android.os.AsyncTask;

import com.geecko.QuickLyric.utils.DatabaseHelper;

import java.lang.ref.WeakReference;

public class PresenceChecker extends AsyncTask<Object, Void, Boolean> {
    private final WeakReference<PresenceCheckerCallback> callback;

    public PresenceChecker(PresenceCheckerCallback callback) {
        this.callback = new WeakReference<>(callback);
    }

    @Override
    protected Boolean doInBackground(Object... params) {
        Context context = (Context) params[0];
        String[] metaData = (String[]) params[1];
        return context != null &&
                !DatabaseHelper.getInstance(context).isClosed() &&
                DatabaseHelper.getInstance(context).presenceCheck(metaData);
    }

    @Override
    protected void onPostExecute(Boolean present) {
        if (callback.get() != null)
            callback.get().onPresenceChecked(present);
    }

    public interface PresenceCheckerCallback {
        void onPresenceChecked(boolean present);
    }
}
