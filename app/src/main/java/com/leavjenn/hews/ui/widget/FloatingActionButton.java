package com.leavjenn.hews.ui.widget;


import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.leavjenn.hews.SharedPrefsManager;

public class FloatingActionButton extends android.support.design.widget.FloatingActionButton {

    public static final int FAB_DRAG_MODE = 1;
    public static final int FAB_PRESS_MODE = 2;

    private int mScrollDownMode = 0;
    private RecyclerView recyclerView;
    float mOriginX = 0, mOriginY = 0;
    Handler handler = new Handler();


    public FloatingActionButton(Context context) {
        this(context, null);
    }

    public FloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private Runnable scrollDownThread = new Runnable() {
        @Override
        public void run() {
            if (recyclerView != null) {
                recyclerView.scrollBy(0, 6);
            }
            handler.postDelayed(this, 10);
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //int iOffsetX, iOffsetY, iNewX, iNewY, iRelX = 0, iRelY = 0;
        int[] aLocation = new int[2];
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // preserve offset of this touch within the View
//                iRelX = (int) event.getX();
//                iRelY = (int) event.getY();
                mOriginX = event.getRawX();
                mOriginY = event.getRawY();
                if (mScrollDownMode == FAB_PRESS_MODE) {
                    handler.post(scrollDownThread);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mScrollDownMode == FAB_DRAG_MODE) {
                    setFABPosition(aLocation, event);
                    scrollDownWhenDragging(event);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mScrollDownMode == FAB_PRESS_MODE) {
                    handler.removeCallbacks(scrollDownThread);
                }

//                else if (mScrollDownMode == SharedPrefsManager.FAB_CLICK_SCROLL_DOWN) {
//                    LinearLayoutManager linearLayoutManager =
//                            ((LinearLayoutManager) recyclerView.getLayoutManager());
//                    int currentPosition = linearLayoutManager.findFirstVisibleItemPosition();
//                    if (currentPosition != linearLayoutManager.findLastVisibleItemPosition()) {
//                        linearLayoutManager.scrollToPositionWithOffset(currentPosition + 1, 0);
//                    } else {
//                        recyclerView.scrollBy(0,mScreenHeight/2);
//                        linearLayoutManager.scrollToPositionWithOffset(currentPosition, -mScreenHeight / 2);
//                    }
//                }
                break;
        }
        return super.onTouchEvent(event);
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        //recyclerView.setOnScrollListener(scrollListener);
    }

    public void setScrollDownMode(int mode) {
        mScrollDownMode = mode;
    }

    public void setScrollDownMode(String mode) {
        if (mode.equals(SharedPrefsManager.FAB_PRESS_SCROLL_DOWN)) {
            mScrollDownMode = FAB_PRESS_MODE;
        } else if (mode.equals(SharedPrefsManager.FAB_DRAG_SCROLL_DOWN)) {
            mScrollDownMode = FAB_DRAG_MODE;
        }
    }

    private void setFABPosition(int[] location, MotionEvent event) {
        // subtract out this View's relative location within its parent View
        getLocationOnScreen(location);
        // ...yielding the offsets that convert getRawX/Y's coords to setX/Y's coords
        int offsetX = (location[0] - (int) getX());
        int offsetY = (location[1] - (int) getY());
        // get absolute physical coords of this touch
        int newX = (int) event.getRawX();
        int newY = (int) event.getRawY();
        // remove parent View's screen offset (calc'd above)
        newX -= offsetX;
        newY -= offsetY;
        // remove stored touch offset
//                iNewX -= iRelX;
//                iNewY -= iRelY;
        newX -= getWidth() / 2;
        newY -= getHeight() / 2;

        setX(newX);
        setY(newY);
//                setX(event.getRawX() - 56);
//                setY(event.getRawY() - 214);
    }

    private void scrollDownWhenDragging(MotionEvent event) {
        float disX = event.getRawX() - mOriginX;
        float disY = event.getRawY() - mOriginY;
        float dis = (float) Math.sqrt(disX * disX + disY * disY);
        if (recyclerView != null) {
            recyclerView.scrollBy(0, (int) (dis * 2));
        }
        mOriginX = event.getRawX();
        mOriginY = event.getRawY();
    }

//    public void setScreenHeight(int screenHeight) {
//        mScreenHeight = screenHeight;
//    }
}
