package com.leavjenn.hews.ui.post;

import android.support.annotation.StringRes;

import com.leavjenn.hews.model.Post;

import java.util.List;

public interface PostView {
    void showPost(Post post);

    void restoreCachedPosts(List<Post> postList);

    void showSummary(int postPosition);

    void showSwipeRefresh();

    void hideSwipeRefresh();

    void forceHideSwipeRefresh();

    void showOfflineSnackBar();

    void hideOfflineSnackBar();

    void showSpinnerPopularDateRange(int selection);

    void showLongToast(@StringRes int stringId);

    void resetAdapter();

    void updateListFooter(int loadingState);

    List<Post> getAllPostList();

    int getLastPostIndex();

    void showInfoLog(String tag, String msg);

    void showErrorLog(String tag, String msg);

}