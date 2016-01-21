package com.leavjenn.hews.ui;


import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leavjenn.hews.R;
import com.leavjenn.hews.listener.OnRecyclerViewCreateListener;
import com.leavjenn.hews.misc.SharedPrefsManager;
import com.leavjenn.hews.network.DataManager;
import com.leavjenn.hews.ui.adapter.PostAdapter;

import rx.subscriptions.CompositeSubscription;

public class BasePostListFragment extends Fragment implements PostAdapter.OnReachBottomListener,
        SharedPreferences.OnSharedPreferenceChangeListener{
    SwipeRefreshLayout swipeRefreshLayout;
    RecyclerView rvPostList;
    
    LinearLayoutManager mLinearLayoutManager;
    PostAdapter mPostAdapter;
    PostAdapter.OnItemClickListener mOnItemClickListener;
    OnRecyclerViewCreateListener mOnRecyclerViewCreateListener;
    DataManager mDataManager;
    CompositeSubscription mCompositeSubscription;

    public BasePostListFragment() {
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnItemClickListener = (PostAdapter.OnItemClickListener) activity;
            mOnRecyclerViewCreateListener = (OnRecyclerViewCreateListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement (PostAdapter.OnItemClickListener" +
                    " && MainActivity.OnRecyclerViewCreateListener)");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataManager = new DataManager();
        mCompositeSubscription = new CompositeSubscription();
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupList();
        mOnRecyclerViewCreateListener.onRecyclerViewCreate(rvPostList);
        setupSwipeRefreshLayout();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCompositeSubscription.hasSubscriptions()) {
            mCompositeSubscription.unsubscribe();
        }
//        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void setupList() {
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        rvPostList.setLayoutManager(mLinearLayoutManager);
        mPostAdapter = new PostAdapter(getActivity(), this, mOnItemClickListener);
        rvPostList.setAdapter(mPostAdapter);
    }

    private void setupSwipeRefreshLayout() {
        swipeRefreshLayout.setColorSchemeResources(R.color.orange_600,
                R.color.orange_900, R.color.orange_900, R.color.orange_600);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
    }

    public void refresh() {
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
}
