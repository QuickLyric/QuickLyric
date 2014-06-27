package be.geecko.QuickLyric.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class CheckableLayout extends LinearLayout {
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
