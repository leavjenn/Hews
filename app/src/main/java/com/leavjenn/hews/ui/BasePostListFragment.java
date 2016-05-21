package com.leavjenn.hews.ui;


import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leavjenn.hews.R;
import com.leavjenn.hews.misc.Utils;
import com.leavjenn.hews.listener.OnRecyclerViewCreatedListener;
import com.leavjenn.hews.misc.SharedPrefsManager;
import com.leavjenn.hews.data.remote.DataManager;
import com.leavjenn.hews.ui.adapter.PostAdapter;

public class BasePostListFragment extends Fragment implements PostAdapter.OnReachBottomListener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = "BaseListFragment";



    public BasePostListFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnItemClickListener = (PostAdapter.OnItemClickListener) activity;
            mOnRecyclerViewCreatedListener = (OnRecyclerViewCreatedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement (PostAdapter.OnItemClickListener" +
                " && MainActivity.OnRecyclerViewCreatedListener)");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        mDataManager = new DataManager();
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
        mPrefsManager = new SharedPrefsManager(getActivity(), prefs);
        mUtils = new Utils(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_base_post_list, container, false);
        swipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_layout);
        rvPostList = (RecyclerView) v.findViewById(R.id.list_post);
        return v;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        prefs = null;
        mPrefsManager = null;
        mUtils = null;
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        mPostAdapter = null;
        mLinearLayoutManager = null;
        mOnItemClickListener = null;
        mOnRecyclerViewCreatedListener = null;
        super.onDetach();
    }

    public void refresh() {
    }

    public void setSwipeRefreshLayoutState(boolean isEnabled) {
        swipeRefreshLayout.setEnabled(isEnabled);
    }

    public void scrollUp(int appBarShowingHeight) {
        int j = mLinearLayoutManager.findFirstVisibleItemPosition();
        if (j == RecyclerView.NO_POSITION) {
            return;
        }
        if (j != 0 && mLinearLayoutManager.findViewByPosition(j) != null) {
//            for (int visibleOffsetBottom = mLinearLayoutManager.findViewByPosition(j).getBottom();
//                 visibleOffsetBottom <= appBarShowingHeight; j++) {
//                Log.i("--", "overlay: " + j);
//            }
            int visibleOffsetBottom = mLinearLayoutManager.findViewByPosition(j).getBottom();
            if (visibleOffsetBottom + 168 <= appBarShowingHeight) { // first visible item is overlaid by app bar
                Log.i("appBarShowingHeight: " + appBarShowingHeight,
                    "bottom: " + (mLinearLayoutManager.findViewByPosition(j).getBottom()));
                j++;
            }
            // offset j + 1 item the recycler view height to hide the entire item
            mLinearLayoutManager.scrollToPositionWithOffset(j + 1, rvPostList.getHeight() - appBarShowingHeight);
        }
    }

    public void scrollDown(int appBarShowingHeight) {
        int j = mLinearLayoutManager.findLastVisibleItemPosition();
        if (j == RecyclerView.NO_POSITION) {
            return;
        }
        // sometimes, findLastVisibleItemPosition() won't get the real last one visible item,
        // add more checks.
        Log.i("top: " + (mLinearLayoutManager.findViewByPosition(j).getTop()),
            "bottom: " + (mLinearLayoutManager.findViewByPosition(j).getBottom()));
        if (rvPostList.getHeight()
            - appBarShowingHeight - mLinearLayoutManager.findViewByPosition(j).getBottom() > 0) {
            Log.i("+++", "add");
            j++;
        } else if (mLinearLayoutManager.findViewByPosition(j).getTop() + appBarShowingHeight -
            rvPostList.getHeight() > 0) {
            Log.i("---", "minus");
            j--;
        }
        mLinearLayoutManager.scrollToPositionWithOffset(j, 0);
    }

    @Override
    public void OnReachBottom() {

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SharedPrefsManager.KEY_POST_FONT)
            || key.equals(SharedPrefsManager.KEY_POST_FONT_SIZE)
            || key.equals(SharedPrefsManager.KEY_POST_LINE_HEIGHT)) {
            mPostAdapter.updatePostPrefs();
            reformatListStyle();
        }
    }

    private void reformatListStyle() {
        int position = mLinearLayoutManager.findFirstVisibleItemPosition();
        int offset = 0;
        View firstChild = mLinearLayoutManager.getChildAt(0);
        if (firstChild != null) {
            offset = firstChild.getTop();
        }
        PostAdapter newAdapter = (PostAdapter) rvPostList.getAdapter();
        rvPostList.setAdapter(newAdapter);
        mLinearLayoutManager.scrollToPositionWithOffset(position, offset);
    }
}
