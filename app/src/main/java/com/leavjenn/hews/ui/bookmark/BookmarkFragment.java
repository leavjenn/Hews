package com.leavjenn.hews.ui.bookmark;

import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leavjenn.hews.R;
import com.leavjenn.hews.data.local.LocalDataManager;
import com.leavjenn.hews.ui.BasePresenter;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.ui.BasePostListFragment;
import com.leavjenn.hews.ui.adapter.PostAdapter;

import java.util.List;

public class BookmarkFragment extends BasePostListFragment implements BookmarkView {
    private BookmarkPresenter mBookmarkPresenter;
    private LocalDataManager mLocalDataManager;

    public BookmarkFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mLocalDataManager = new LocalDataManager(getActivity());
        mBookmarkPresenter = new BookmarkPresenter(this, mLocalDataManager);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        mPostAdapter = new PostAdapter(getActivity(), this, mOnItemClickListener);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        rvPostList.setLayoutManager(mLinearLayoutManager);
        rvPostList.setAdapter(mPostAdapter);
        mOnRecyclerViewCreatedListener.onRecyclerViewCreated(rvPostList);
        swipeRefreshLayout.setColorSchemeResources(R.color.orange_600,
            R.color.orange_900, R.color.orange_900, R.color.orange_600);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mBookmarkPresenter.setView(this);
        mBookmarkPresenter.setLocalDataManager(mLocalDataManager);
    }

    @Override
    public void onResume() {
        super.onResume();
        // reset and retrieve bookmark posts here since if unbookmark from CommentActivity
        // and back to BookmarkFragment,
        // onActivityCreated() won't be invoked.
        mBookmarkPresenter.setup();
    }

    @Override
    public void onDestroy() {
        mLocalDataManager = null;
        mBookmarkPresenter.destroy();
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        mBookmarkPresenter.unsubscribe();
        super.onDetach();
    }

    @Override
    public void refresh() {
        hideSwipeRefresh();
    }

    public BasePresenter getPresenter() {
        return mBookmarkPresenter;
    }

    /* Override BookmarkView */
    @Override
    public void showSwipeRefresh() {
        swipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void hideSwipeRefresh() {
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void showPosts(List<Post> postList) {
        mPostAdapter.addAllPosts(postList);
    }

    @Override
    public void resetAdapter() {
        mPostAdapter.clearAndAddFooter();
    }

    @Override
    public void updateListFooter(int loadingState) {
        mPostAdapter.updateFooter(loadingState);
    }

    @Override
    public void updatePrompt(@StringRes int resPrompt) {
        mPostAdapter.updatePrompt(resPrompt);
    }

    @Override
    public void showInfoLog(String tag, String msg) {
        Log.i(tag, msg);
    }

    @Override
    public void showErrorLog(String tag, String msg) {
        Log.e(tag, msg);
    }
}
