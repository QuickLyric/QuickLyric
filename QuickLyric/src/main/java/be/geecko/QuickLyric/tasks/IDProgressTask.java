package be.geecko.QuickLyric.tasks;

import android.os.AsyncTask;
import android.os.SystemClock;

import com.gracenote.mmid.MobileSDK.GNConfig;
import com.gracenote.mmid.MobileSDK.GNOperations;

import be.geecko.QuickLyric.Keys;
import be.geecko.QuickLyric.R;
import be.geecko.QuickLyric.fragment.IDFragment;

public class IDProgressTask extends AsyncTask<Object, Integer, Boolean> {

    private IDFragment idFragment;

    @Override
    protected Boolean doInBackground(Object[] params) {
        idFragment = (IDFragment) params[0];
        publishProgress(0);
        try {
            GNOperations.recognizeMIDStreamFromMic(idFragment, GNConfig.init(Keys.gracenote, idFragment.getActivity().getApplicationContext()));
            for (int progress = 0; progress <= 360; progress++) {
                android.os.SystemClock.sleep(19);
                publishProgress(progress);
                if (isCancelled()) {
                    GNOperations.cancel(idFragment);
                    break;
                }
            }
            SystemClock.sleep(120);
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean supported) {
        if (supported) {
            idFragment.progressWheel.setProgress(30);
            idFragment.progressWheel.spin();
            idFragment.isLoading = false;
            idFragment.isWaiting = true;
            idFragment.progressWheel.setOnClickListener(null);
        }
        else {
            idFragment.displayNotification(R.string.id_arch_err, false, idFragment.getView());
        }
    }

    protected void onProgressUpdate(Integer... progress) {
        if (progress[0] <= 360 && !isCancelled())
            idFragment.progressWheel.setProgress(progress[0]);
        else
            idFragment.progressWheel.resetCount();
    }
}
