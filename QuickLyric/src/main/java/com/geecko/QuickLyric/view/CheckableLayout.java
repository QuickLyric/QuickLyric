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

package com.geecko.QuickLyric.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class CheckableLayout extends RelativeLayout {
    private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
    private boolean isChecked = false;

    public CheckableLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void setChecked(boolean checked) {
        isChecked = checked;
    }

    void check(boolean checked) {
        if (checked != isChecked) {
            setChecked(checked);
            refreshDrawableState();
        }
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void toggle() {
        check(!isChecked);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] drawableState = super.onCreateDrawableState(extraSpace + CHECKED_STATE_SET.length);
        if (isChecked)
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        return drawableState;
    }
}
