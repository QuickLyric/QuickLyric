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

import android.app.Activity;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.drivemode.spotify.ClientConfig;
import com.drivemode.spotify.Response;
import com.drivemode.spotify.SpotifyApi;
import com.drivemode.spotify.SpotifyLoader;
import com.drivemode.spotify.SpotifyService;
import com.drivemode.spotify.models.Pager;
import com.drivemode.spotify.models.SavedTrack;
import com.drivemode.spotify.models.User;
import com.geecko.QuickLyric.Keys;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.services.BatchDownloaderService;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import retrofit.Callback;
import retrofit.RetrofitError;

public class Spotify {

    private static ArrayList<SavedTrack> savedTracks;

    public static void getTracks(Activity activity) {
        savedTracks = new ArrayList<>();
        if (Keys.SPOTIFY_SECRET.isEmpty())
            startAuthWithRemoteKey(activity);
        else
            new SpotifyKeyCallback(activity).startAuth();
    }

    public static void startAuthWithRemoteKey(final Activity activity) {
        final OkHttpClient client = new OkHttpClient();
        String param;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update("spotify_key_request_KBxrcRvcxjo3Gr".getBytes());
            param = String.format("%032x", new BigInteger(1, md5.digest()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }
        RequestBody formBody = new FormEncodingBuilder()
                .add("p", param)
                .build();

        final Request spotifyRequest = new Request.Builder()
                .url("https://www.quicklyric.be/keys/spotify.php?")
                .post(formBody)
                .build();
        final SpotifyKeyCallback callback = new SpotifyKeyCallback(activity);
        Thread networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                AssetManager assetManager = activity.getAssets();
                SSLContext sslContext;
                try {
                    InputStream keyStoreInputStream = assetManager.open("quicklyric.store");
                    KeyStore trustStore = KeyStore.getInstance("BKS");

                    trustStore.load(keyStoreInputStream, null);

                    TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
                    tmf.init(trustStore);

                    sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, tmf.getTrustManagers(), null);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                client.setSslSocketFactory(sslContext.getSocketFactory());
                client.newCall(spotifyRequest).enqueue(callback);
            }
        });
        networkThread.run();
    }

    public static void onCallback(Intent intent, Activity activity) {
        try {
            SpotifyApi.getInstance().onCallback(intent.getData(), new AuthListener(activity, 0));
        } catch (IllegalStateException e) {
            Toast.makeText(activity, R.string.connection_error, Toast.LENGTH_LONG).show();
        }
    }

    private static class SpotifyKeyCallback implements com.squareup.okhttp.Callback {

        private final Activity mActivity;

        public SpotifyKeyCallback(Activity activity) {
            this.mActivity = activity;
        }

        @Override
        public void onResponse(com.squareup.okhttp.Response response) throws IOException {
            if (response.code() != 404) {
                Keys.SPOTIFY_SECRET = response.body().string();
                startAuth();
            } else
                onFailure(null, new IOException("Wrong POST parameter"));
        }

        @Override
        public void onFailure(Request request, IOException e) {
            e.printStackTrace();
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mActivity, R.string.connection_error, Toast.LENGTH_LONG).show();
                }
            });
        }

        public void startAuth() {
            SpotifyApi.initialize(mActivity.getApplication(), new ClientConfig.Builder()
                    .setClientId(Keys.SPOTIFY_PUBLIC)
                    .setClientSecret(Keys.SPOTIFY_SECRET)
                    .setRedirectUri("quicklyric://spotify/callback")
                    .build());
            SpotifyApi.getInstance().authorize(mActivity, new String[]{"user-library-read"}, false);
        }
    }

    private static class AuthListener implements SpotifyApi.AuthenticationListener,
            LoaderManager.LoaderCallbacks<Response<User>> {

        private final int mOffset;
        private final Activity mActivity;
        private ProgressDialog progressDialog;

        public AuthListener(Activity activity, int offset) {
            this.mActivity = activity;
            this.mOffset = offset;
        }

        @Override
        public void onReady() {
            progressDialog = new ProgressDialog(mActivity);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(mActivity.getString(R.string.spotify_connection));
            progressDialog.show();
            mActivity.getLoaderManager().initLoader(0, null, this);
        }

        @Override
        public void onError() {
            Toast.makeText(mActivity, R.string.connection_error, Toast.LENGTH_LONG).show();
        }

        @Override
        public Loader<Response<User>> onCreateLoader(int id, Bundle args) {
            return new SelfLoader(mActivity, SpotifyApi.getInstance());
        }

        @Override
        public void onLoadFinished(Loader<Response<User>> loader, Response<User> data) {
            SpotifyApi.getInstance().getApiService().getMySavedTracks(mOffset, 50, new Callback<Pager<SavedTrack>>() {
                        @Override
                        public void success(Pager<SavedTrack> savedTracksPager, retrofit.client.Response response) {
                            savedTracks.addAll(savedTracksPager.items);
                            if (savedTracksPager.next != null) {
                                SpotifyApi.getInstance().getApiService()
                                        .getMySavedTracks(savedTracksPager.offset + savedTracksPager.limit, 50, this);
                            } else if (savedTracks.size() > 0) {
                                progressDialog.dismiss();
                                final int time = (int) Math.ceil(savedTracks.size() / 500f);
                                String prompt = mActivity.getResources()
                                        .getQuantityString(R.plurals.scan_dialog, savedTracks.size() > 1 ? 2 : 1);
                                AlertDialog.Builder confirmDialog = new AlertDialog.Builder(mActivity);
                                confirmDialog
                                        .setTitle(R.string.warning)
                                        .setMessage(String.format(prompt, savedTracks.size(), time))
                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                Intent scanInfo = new Intent(mActivity,
                                                        BatchDownloaderService.class);
                                                scanInfo.putExtra("spotifyTracks", cleanResults(savedTracks));
                                                mActivity.startService(scanInfo);
                                            }
                                        })
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .create().show();
                            } else {
                                progressDialog.dismiss();
                                Toast.makeText(mActivity, R.string.scan_error_no_content, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            Toast.makeText(mActivity, R.string.connection_error, Toast.LENGTH_LONG).show();
                            if (progressDialog != null)
                                progressDialog.dismiss();
                        }
                    }

            );
        }

        private ArrayList<String[]> cleanResults(ArrayList<SavedTrack> savedTracks) {
            ArrayList<String[]> results = new ArrayList<>(savedTracks.size());
            for (SavedTrack savedTrack : savedTracks)
                results.add(new String[]{savedTrack.track.artists.get(0).name, savedTrack.track.name});
            return results;
        }

        @Override
        public void onLoaderReset(Loader<Response<User>> loader) {
        }

        static class SelfLoader extends SpotifyLoader<User> {
            public SelfLoader(Context context, SpotifyApi api) {
                super(context, api);
            }

            @Override
            public User call(SpotifyService service) throws Exception {
                return service.getMe();
            }
        }
    }
}
