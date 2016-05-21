package com.leavjenn.hews.ui.bookmark;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.leavjenn.hews.Constants;
import com.leavjenn.hews.R;
import com.leavjenn.hews.data.local.LocalContract;
import com.leavjenn.hews.ui.BasePresenter;
import com.leavjenn.hews.model.Post;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class BookmarkPresenter extends BasePresenter{
    public static final String TAG = "BookmarkPresenter";
    private BookmarkView mBookmarkView;
    private LocalContract mLocalDataManager;

    private CompositeSubscription mCompositeSubscription;


    public BookmarkPresenter(@NonNull BookmarkView bookmarkView) {
        mBookmarkView = bookmarkView;
        mCompositeSubscription = new CompositeSubscription();
    }

    public BookmarkPresenter(@NonNull BookmarkView bookmarkView, @NonNull LocalContract localDataManager) {
        this(bookmarkView);
        mLocalDataManager = localDataManager;
        mCompositeSubscription = new CompositeSubscription();
    }

    public void setView(BookmarkView bookmarkView) {
        mBookmarkView = bookmarkView;
    }

    public void setLocalDataManager(LocalContract localDataManager) {
        mLocalDataManager = localDataManager;
    }

    @Override
    public void setup() {
        mBookmarkView.showSwipeRefresh();
        mBookmarkView.resetAdapter();
        getBookmarkedPosts();
    }

    @Override
    public void restoreState(Bundle savedInstanceState) {

    }

    @Override
    public void saveState(Bundle outState) {

    }

    @Override
    public void destroy() {
        mCompositeSubscription.clear();
        mBookmarkView = null;
        mLocalDataManager = null;
    }

    @Override
    public void unsubscribe() {
        mCompositeSubscription.clear();
    }

    public void getBookmarkedPosts() {
        Log.i("presenter", "getBookmarkedPosts");
        mCompositeSubscription.add(mLocalDataManager.getAllPostsFromDb()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<Post>>() {
                @Override
                public void call(List<Post> posts) {
                    mBookmarkView.hideSwipeRefresh();
                    mBookmarkView.showPosts(posts);
                    if (posts.isEmpty()) { // no bookmarked post
                        mBookmarkView.updatePrompt(R.string.no_bookmark_prompt);
                        mBookmarkView.updateListFooter(Constants.LOADING_PROMPT_NO_CONTENT);
                    } else {
                        mBookmarkView.updateListFooter(Constants.LOADING_FINISH);
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    mBookmarkView.showErrorLog("getBookmarkedPosts", throwable.toString());
                }
            }));
    }
}
