package be.geecko.QuickLyric.fragment;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.gracenote.mmid.MobileSDK.GNSearchResponse;
import com.gracenote.mmid.MobileSDK.GNSearchResult;
import com.gracenote.mmid.MobileSDK.GNSearchResultReady;

import be.geecko.QuickLyric.MainActivity;
import be.geecko.QuickLyric.R;
import be.geecko.QuickLyric.adapter.DrawerAdapter;
import be.geecko.QuickLyric.tasks.IDProgressTask;
import be.geecko.QuickLyric.view.ProgressWheel;

public class IDFragment extends Fragment implements GNSearchResultReady, View.OnClickListener {

    public ProgressWheel progressWheel;
    public boolean isWaiting = false;
    private IDProgressTask progressTask;
    public boolean isLoading = false;
    public boolean isActiveFragment = false;
    public boolean showTransitionAnim = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        setRetainInstance(true);
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.id_view, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (this.isHidden())
            return;
        MainActivity activity = ((MainActivity) this.getActivity());

        ImageView gracenoteLogo = (ImageView) view.findViewById(R.id.gracenote);
        this.progressWheel = (ProgressWheel) view.findViewById(R.id.progressWheel);
        Context mContext = this.getActivity();
        PackageManager pm = mContext.getPackageManager();
        if (pm == null || !pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            displayNotification(R.string.id_nomic, false, view);
            progressWheel.setEnabled(false);
        }
        if (isWaiting) {
            this.progressWheel.setProgress(30);
            this.progressWheel.spin();
            gracenoteLogo.setVisibility(View.VISIBLE);
            progressWheel.setOnClickListener(null);
        } else {
            progressWheel.setOnClickListener(this);
        }

        DrawerAdapter drawerAdapter = ((DrawerAdapter) ((ListView) activity.findViewById(R.id.drawer_list)).getAdapter());
        if (drawerAdapter.getSelectedItem() != 1) {
            drawerAdapter.setSelectedItem(1);
            drawerAdapter.notifyDataSetChanged();
        }
        this.isActiveFragment = true;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden)
            this.onViewCreated(getView(), null);
        else
            this.isActiveFragment = false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MainActivity mainActivity = (MainActivity) this.getActivity();
        ActionBar actionBar = (mainActivity).getSupportActionBar();
        if (mainActivity.focusOnFragment && actionBar != null) // focus is on Fragment
        {
            if (actionBar.getTitle() == null || !actionBar.getTitle().equals(this.getString(R.string.id_title)))
                actionBar.setTitle(R.string.id_title);
        } else
            menu.clear();
    }

    public void displayNotification(int stringID, boolean icon, View layout) {
        final TextView alertText = (TextView) layout.findViewById(R.id.alertText);
        alertText.setText(stringID);
        if (icon)
            alertText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_clock, 0, 0, 0);
        else
            alertText.setCompoundDrawables(null, null, null, null);
        Animation animation = AnimationUtils.loadAnimation(this.getActivity(), android.R.anim.fade_in);
        if (animation != null) {
            animation.setRepeatCount(1);
            if (stringID != R.string.id_arch_err)
                animation.setRepeatMode(Animation.REVERSE);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    alertText.setVisibility(View.VISIBLE);
                    animation.setDuration(4000);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    alertText.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    animation.setDuration(2000);
                }
            });
            alertText.startAnimation(animation);
        }
    }


    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        Animation anim = null;
        if (nextAnim != 0)
            anim = AnimationUtils.loadAnimation(getActivity(), nextAnim);
        if (anim != null) {
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    if (mainActivity.drawer instanceof DrawerLayout && ((DrawerLayout) mainActivity.drawer).isDrawerOpen(mainActivity.drawerView))
                        ((DrawerLayout) mainActivity.drawer).closeDrawer(mainActivity.drawerView);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            if (!showTransitionAnim)
                anim.setDuration(0);
            else
                showTransitionAnim = false;
        }
        return anim;
    }

    @Override
    public void onClick(View v) {
        if (v.isEnabled())
            if (!isLoading) {
                progressTask = new IDProgressTask();
                progressTask.execute(this);
                isLoading = true;
            } else {
                if (progressTask != null)
                    progressTask.cancel(true);
                isLoading = false;
                progressWheel.resetCount();
            }
    }

    @Override
    public void GNResultReady(GNSearchResult result) {
        progressWheel.stopSpinning();
        progressWheel.resetCount();
        progressWheel.setOnClickListener(this);
        isWaiting = false;

        if (result.isFailure()) {
            int message;
            boolean icon = false;
            if (result.isFingerprintingFailure() || result.isRecordingFailure() || result.isAnyFingerprintFailure())
                message = R.string.id_error_recording;
            else if (result.isAnyWebservicesFailure() || result.isNetworkFailure())
                message = R.string.connection_error;
            else if (result.getErrCode() == 1000)
                message = R.string.id_arch_err;
            else
                message = R.string.unknown_error;
            displayNotification(message, icon, this.getView());
        } else if (!result.isFingerprintSearchNoMatchStatus()) {
            GNSearchResponse bestResponse = result.getBestResponse();
            ((MainActivity) this.getActivity()).updateLyricsFragment(R.anim.slide_out_start, bestResponse.getArtist(), bestResponse.getTrackTitle());
        } else {
            displayNotification(R.string.no_results, false, this.getView());
            //TO DO broadcast result.getErrMessage() to Google dev console
        }
    }
}
