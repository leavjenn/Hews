package com.leavjenn.hews.ui;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leavjenn.hews.R;
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
        getPostBookmarks();
        return v;
    }

    private void getPostBookmarks() {
        mCompositeSubscription.add(mDataManager.getAllPostBookmarks(getActivity())
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
                        mSwipeRefreshLayout.setRefreshing(false);
                        for (Post post : posts) {
                            mPostAdapter.add(post);
                        }
                    }
                }));
    }

}
