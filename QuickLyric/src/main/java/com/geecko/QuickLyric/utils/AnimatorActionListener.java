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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;


public class AnimatorActionListener extends AnimatorListenerAdapter {

    public enum ActionType {
        START, END
    }

    final private ActionType type;
    final private Runnable action;
    private boolean mFinished = false;

    public AnimatorActionListener(Runnable action, ActionType type) {
        this.action = action;
        this.type = type;
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if (type == ActionType.END && action != null && !mFinished) {
            action.run();
            mFinished = true;
        }
    }

    @Override
    public void onAnimationStart(Animator animation) {
        if (type == ActionType.START) {
            action.run();
        }
    }
}
