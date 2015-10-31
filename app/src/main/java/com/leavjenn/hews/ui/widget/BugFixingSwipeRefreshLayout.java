package com.leavjenn.hews.ui.widget;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.View;

public class BugFixingSwipeRefreshLayout extends SwipeRefreshLayout {
    public BugFixingSwipeRefreshLayout(Context context) {
        super(context);
    }

    public BugFixingSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return !isRefreshing() && super.onStartNestedScroll(child, target, nestedScrollAxes);
    }
}
