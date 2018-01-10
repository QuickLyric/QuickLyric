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

package com.geecko.QuickLyric.utils;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.widget.Toast;

import com.drivemode.spotify.ClientConfig;
import com.drivemode.spotify.Response;
import com.drivemode.spotify.SpotifyApi;
import com.drivemode.spotify.SpotifyLoader;
import com.drivemode.spotify.SpotifyService;
import com.drivemode.spotify.models.Pager;
import com.drivemode.spotify.models.Playlist;
import com.drivemode.spotify.models.PlaylistTrack;
import com.drivemode.spotify.models.SavedTrack;
import com.drivemode.spotify.models.User;
import com.geecko.QuickLyric.BuildConfig;
import com.geecko.QuickLyric.Keys;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.services.BatchDownloaderService;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Callback;

public class Spotify {

    private static boolean mPlaylists;

    public static void getPlaylistTracks(Activity activity) {
        if (Keys.SPOTIFY_SECRET.isEmpty())
            startAuthWithRemoteKey(activity, true);
        else
            new SpotifyKeyCallback(activity, true).startAuth();
    }

    public static void getUserTracks(Activity activity) {
        if (Keys.SPOTIFY_SECRET.isEmpty())
            startAuthWithRemoteKey(activity, false);
        else
            new SpotifyKeyCallback(activity, false).startAuth();
    }

    public static void startAuthWithRemoteKey(final Activity activity, boolean playlists) {
        AssetManager assetManager = activity.getAssets();
        String message;
        try {
            InputStream keyStoreInputStream = assetManager.open("quicklyric.store");
            KeyStore trustStore = KeyStore.getInstance("BKS");
            trustStore.load(keyStoreInputStream, null);
            PublicKey publicKey = trustStore.getCertificate("myAlias").getPublicKey();

            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT));
            String plain = String.format(Locale.ENGLISH, "%s_%s_%d",
                    BuildConfig.APPLICATION_ID, "spotify", System.currentTimeMillis()).trim();
            byte[] encryptedBytes = Base64.encode(cipher.doFinal(plain.getBytes("UTF-8")), Base64.DEFAULT);
            message = new String(encryptedBytes, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        TrustManager tm = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        // Install the all-trusting trust manager
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] {tm}, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
            return;
        }
        // Create an ssl socket factory with our all-trusting manager
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, (X509TrustManager) tm)
                .readTimeout(20, TimeUnit.SECONDS)
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        RequestBody formBody = new FormBody.Builder()
                .add("p", message)
                .build();

        final Request spotifyRequest = new Request.Builder()
                .url("https://api.quicklyric.be:4433/keys/spotify")
                .post(formBody)
                .build();
        client.newCall(spotifyRequest).enqueue(new SpotifyKeyCallback(activity, playlists));
    }

    public static void onCallback(Intent intent, Activity activity) {
        try {
            SpotifyApi.getInstance().onCallback(intent.getData(), new AuthListener(activity, 0));
        } catch (IllegalStateException e) {
            Toast.makeText(activity, R.string.connection_error, Toast.LENGTH_LONG).show();
        }
    }

    private static class SpotifyKeyCallback implements okhttp3.Callback {

        private final Activity mActivity;

        public SpotifyKeyCallback(Activity activity, boolean playlists) {
            this.mActivity = activity;
            mPlaylists = playlists;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            e.printStackTrace();
            mActivity.runOnUiThread(() -> Toast.makeText(mActivity, R.string.connection_error, Toast.LENGTH_LONG).show());
        }

        @Override
        public void onResponse(Call call, okhttp3.Response response) throws IOException {
            if (response.code() != 404) {
                Keys.SPOTIFY_SECRET = response.body().string();
                startAuth();
            } else
                onFailure(null, new IOException("Wrong POST parameter"));
        }

        public void startAuth() {
            SpotifyApi.initialize(mActivity.getApplication(), new ClientConfig.Builder()
                    .setClientId(Keys.SPOTIFY_PUBLIC)
                    .setClientSecret(Keys.SPOTIFY_SECRET)
                    .setRedirectUri("quicklyric://spotify/callback")
                    .build());
            SpotifyApi.getInstance().authorize(mActivity, mPlaylists ?
                            new String[]{"user-library-read", "playlist-read-private"} :
                            new String[]{"user-library-read"},
                    false);
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
            if (mPlaylists)
                new SpotifyTracks().getAllPlaylistTracks();
            else
                getUserSavedTrack();
        }

        private void getUserSavedTrack() {
            final ArrayList<SavedTrack> savedTracks = new ArrayList<>();
            SpotifyApi.getInstance().getApiService().getMySavedTracks(mOffset, 50).enqueue(new Callback<Pager<SavedTrack>>() {
                        @Override
                        public void onResponse(retrofit2.Call<Pager<SavedTrack>> call, retrofit2.Response<Pager<SavedTrack>> response) {
                            savedTracks.addAll(response.body().items);
                            if (mActivity == null || mActivity.isFinishing())
                                return;
                            if (response.body().next != null) {
                                SpotifyApi.getInstance().getApiService()
                                        .getMySavedTracks(response.body().offset + response.body().limit, 50).enqueue(this);
                            } else if (savedTracks.size() > 0) {
                                progressDialog.dismiss();
                                final int time = (int) Math.ceil(savedTracks.size() / 500f);
                                String prompt = mActivity.getResources()
                                        .getQuantityString(R.plurals.scan_dialog, savedTracks.size());
                                AlertDialog.Builder confirmDialog = new AlertDialog.Builder(mActivity);
                                confirmDialog
                                        .setTitle(R.string.warning)
                                        .setMessage(String.format(prompt, savedTracks.size(), time))
                                        .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                                            Intent scanInfo = new Intent(mActivity,
                                                    BatchDownloaderService.class);
                                            scanInfo.putExtra("spotifyTracks", cleanResults(savedTracks));
                                            mActivity.startService(scanInfo);
                                        })
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .create().show();
                            } else {
                                progressDialog.dismiss();
                                Toast.makeText(mActivity, R.string.scan_error_no_content, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<Pager<SavedTrack>> call, Throwable t) {
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
                return service.getMe().execute().body();
            }
        }

        private class SpotifyTracks {
            private ArrayList<String[]> tracks = new ArrayList<>();
            private final int playListsToFetch = 1;

            @NonNull
            private String[] cleanTrack(PlaylistTrack playlistTrack) {
                if (playlistTrack == null || playlistTrack.track == null)
                    return null;
                return new String[]{playlistTrack.track.artists.get(0).name,
                        playlistTrack.track.name};
            }

            protected void getAllPlaylistTracks() {
                SpotifyApi.getInstance().getApiService().getMe().enqueue(new Callback<User>() {

                    @Override
                    public void onResponse(retrofit2.Call<User> call, retrofit2.Response<User> response) {
                        PlayListCallback playListCallback = new PlayListCallback(response.body());
                        SpotifyApi.getInstance().getApiService().getPlaylists(response.body().id, 0, playListsToFetch)
                        .enqueue(playListCallback);
                    }

                    @Override
                    public void onFailure(retrofit2.Call<User> call, Throwable t) {
                        progressDialog.dismiss();
                        Toast.makeText(mActivity, R.string.connection_error, Toast.LENGTH_LONG).show();
                    }
                });
            }

            private class PlayListCallback implements Callback<Pager<Playlist>> {
                private User user;
                public PlayListCallback(User user) {
                    this.user = user;
                }

                @Override
                public void onResponse(retrofit2.Call<Pager<Playlist>> call, retrofit2.Response<Pager<Playlist>> response) {
                    try {
                        SpotifyApi.getInstance().getApiService()
                                .getPlaylistTracks(response.body().items.get(0).owner.id, response.body().items.get(0).id)
                                .enqueue(new PlaylistTrackCallback(response.body(), this, user));
                    } catch (Exception e) {
                        onFailure(null, null);
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<Pager<Playlist>> call, Throwable t) {
                    progressDialog.dismiss();
                    Toast.makeText(mActivity, R.string.connection_error, Toast.LENGTH_LONG).show();
                }
            }

            private class PlaylistTrackCallback implements Callback<Pager<PlaylistTrack>> {

                private final PlayListCallback playListCallback;
                private Pager<Playlist> playlistPager;
                private User user;


                public PlaylistTrackCallback(Pager<Playlist> playlistPager, PlayListCallback playListCallback, User user) {
                    this.playlistPager = playlistPager;
                    this.playListCallback = playListCallback;
                    this.user = user;
                }

                @Override
                public void onResponse(retrofit2.Call<Pager<PlaylistTrack>> call, retrofit2.Response<Pager<PlaylistTrack>> response) {
                    for (PlaylistTrack playlistTrack : response.body().items) {
                        tracks.add(cleanTrack(playlistTrack));
                    }
                    if (playlistPager.next != null) {
                        SpotifyApi.getInstance().getApiService()
                                .getPlaylists(user.id, playlistPager.offset + playlistPager.limit, playListsToFetch)
                                .enqueue(playListCallback);
                    } else {
                        progressDialog.dismiss();
                        if (tracks.isEmpty()) {
                            Toast.makeText(mActivity, R.string.scan_error_no_content, Toast.LENGTH_LONG).show();
                            return;
                        }
                        final int time = (int) Math.ceil(tracks.size() / 500f);
                        String prompt = mActivity.getResources()
                                .getQuantityString(R.plurals.scan_dialog, tracks.size());
                        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(mActivity);
                        confirmDialog
                                .setTitle(R.string.warning)
                                .setMessage(String.format(prompt, tracks.size(), time))
                                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                                    Intent scanInfo = new Intent(mActivity,
                                            BatchDownloaderService.class);
                                    scanInfo.putExtra("spotifyTracks", tracks);
                                    mActivity.startService(scanInfo);
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .create().show();

                    }
                }

                @Override
                public void onFailure(retrofit2.Call<Pager<PlaylistTrack>> call, Throwable t) {
                    progressDialog.dismiss();
                    Toast.makeText(mActivity, R.string.connection_error, Toast.LENGTH_LONG).show();
                }
            }

        }
    }
}
