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

package com.geecko.QuickLyric.utils;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;

import com.geecko.QuickLyric.fragment.LyricsViewFragment;

import java.io.IOException;

public class UpdateChecker {

    public static boolean isUpdateAvailable(Context context) {

        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        int versionCode = pInfo.versionCode;
        int newVersionCode = versionCode;

        try {
            String version = Net.getUrlAsString("http://quicklyric.azurewebsites.net/current_version.txt");
            version = version.replaceAll("\\D+", "");
            newVersionCode = Integer.valueOf(version);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return versionCode < newVersionCode;
    }

    public static void showDialog(final Context context) {
        AlertDialog.Builder updateDialog = new AlertDialog.Builder(context);
        updateDialog.setTitle("Update available")
                .setMessage("Dear Beta tester, a new update is available on the Google+ page.")
                .setPositiveButton("Download", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://goo.gl/forms/98hSOLsQyO")
                        );
                        context.startActivity(browserIntent);
                    }
                })
                .setCancelable(true)
                .show();
        // TODO: Remove
    }

    public static class UpdateCheckTask extends AsyncTask<Void, Void, Boolean> {

        private final LyricsViewFragment lyricsFragment;

        public UpdateCheckTask(LyricsViewFragment lyricsViewFragment) {
            this.lyricsFragment = lyricsViewFragment;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return lyricsFragment.getActivity() != null &&
                    UpdateChecker.isUpdateAvailable(lyricsFragment.getActivity().getApplicationContext());
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result)
                showDialog(lyricsFragment.getActivity());
            else
                lyricsFragment.updateChecked = true;
        }
    }

}
