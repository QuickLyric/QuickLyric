package be.geecko.QuickLyric.utils;

import android.app.Activity;

import com.espian.showcaseview.OnShowcaseEventListener;
import com.espian.showcaseview.ShowcaseView;
import com.espian.showcaseview.targets.Target;

public class ShowcaseCaller implements OnShowcaseEventListener {
    Object[][] nextShowcases;

    public void display(Object[]... args) {
        if (args.length != 0) {
            nextShowcases = new Object[args.length - 1][];
            System.arraycopy(args, 1, nextShowcases, 0, args.length - 1);
            ShowcaseView showcaseView = ShowcaseView.insertShowcaseView((Target) args[0][0], (Activity) args[0][1], (String) args[0][2], (String) args[0][3], (ShowcaseView.ConfigOptions) args[0][4]);
            showcaseView.setOnShowcaseEventListener(this);
            showcaseView.setScaleMultiplier(0.5f);
            showcaseView.setId((int) args[0][5]);
            if (args[0].length >= 7) {
                int[] offsets = (int[]) args[0][6];
                showcaseView.animateGesture(offsets[0],offsets[1],offsets[2],offsets[3],true);
            }
        }
    }

    @Override
    public void onShowcaseViewHide(ShowcaseView showcaseView) {
    }

    @Override
    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
        display(nextShowcases);
    }

    @Override
    public void onShowcaseViewShow(ShowcaseView showcaseView) {

    }
}
