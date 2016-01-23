package com.leavjenn.hews.ui;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leavjenn.hews.Constants;
import com.leavjenn.hews.R;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

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
        getBookmarkedPosts();
    }

    private void getBookmarkedPosts() {
        mPostAdapter.clear();
        mPostAdapter.addFooter(new HNItem.Footer());
        mCompositeSubscription.add(mDataManager.getAllPostsFromDb(getActivity())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<Post>>() {
                @Override
                public void call(List<Post> posts) {
                    swipeRefreshLayout.setRefreshing(false);
                    mPostAdapter.addAllPosts(posts);
                    if (posts.isEmpty()) { // no bookmarked post
                        mPostAdapter.updatePrompt(R.string.no_bookmark_prompt);
                        mPostAdapter.updateFooter(Constants.LOADING_PROMPT_NO_CONTENT);
                        mPostAdapter.notifyDataSetChanged();
                    } else {
                        mPostAdapter.updateFooter(Constants.LOADING_FINISH);
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.e("getBookmarkedPosts", throwable.toString());
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
