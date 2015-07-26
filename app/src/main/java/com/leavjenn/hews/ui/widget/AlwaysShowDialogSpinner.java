package com.leavjenn.hews.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;

public class AlwaysShowDialogSpinner extends Spinner {
    boolean isSameSelectionEnable = false;
        OnItemSelectedListener listener;
    private int lastSelected = 0;

    public AlwaysShowDialogSpinner(Context context) {
        super(context);
    }

    public AlwaysShowDialogSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AlwaysShowDialogSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

//    @Override
//    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//        super.onLayout(changed, l, t, r, b);
//        if (this.lastSelected == this.getSelectedItemPosition() && getOnItemSelectedListener() != null)
//            getOnItemSelectedListener().onItemSelected(this, getSelectedView(),
//                    this.getSelectedItemPosition(), getSelectedItemId());
//        if (!changed)
//            lastSelected = this.getSelectedItemPosition();
//
//    }

    @Override
    public void setSelection(int position) {
        super.setSelection(position);

        if (position == getSelectedItemPosition()) {
            listener.onItemSelected(null, getSelectedView(), position, 0);
        }
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.listener = listener;
    }

    public void enableSameSelection() {
        isSameSelectionEnable = true;
    }

    public void disableSameSelection() {
        isSameSelectionEnable = false;
    }
}
