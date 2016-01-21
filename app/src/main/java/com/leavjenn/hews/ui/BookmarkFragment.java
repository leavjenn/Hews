package com.leavjenn.hews.ui;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leavjenn.hews.model.Post;

import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class BookmarkFragment extends BasePostListFragment {

    public BookmarkFragment() {
    }

    public static BookmarkFragment newInstance() {
        BookmarkFragment fragment = new BookmarkFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        getPostBookmarks();
    }

    private void getPostBookmarks() {
        mPostAdapter.clear();
        mCompositeSubscription.add(mDataManager.getAllPostsFromDb(getActivity())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Post>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(List<Post> posts) {
                        swipeRefreshLayout.setRefreshing(false);
                        for (Post post : posts) {
                            mPostAdapter.add(post);
                        }
                    }
                }));
    }

    @Override
    public void refresh() {
        super.refresh();
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}
