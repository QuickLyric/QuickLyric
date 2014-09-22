package be.geecko.QuickLyric.utils;

import android.app.Activity;
import android.content.Context;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ActionItemTarget;

import be.geecko.QuickLyric.R;

/**
 * This file is part of QuickLyric
 * Created by geecko on 17/09/14.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class ShowCaseCaller implements OnShowcaseEventListener {
    Activity mActivity;

    public ShowCaseCaller(Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public void onShowcaseViewHide(ShowcaseView showcaseView) {
        //pass
    }

    @Override
    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
        new ShowcaseView.Builder(mActivity)
                .singleShot(2l)
                .setTarget(new ActionItemTarget(mActivity, R.id.refresh_action))
                .setContentTitle(R.string.refresh_desc)
                .setContentText(R.string.refresh_desc_sub)
                .hideOnTouchOutside().build();
    }

    @Override
    public void onShowcaseViewShow(ShowcaseView showcaseView) {
        //pass
    }
}
