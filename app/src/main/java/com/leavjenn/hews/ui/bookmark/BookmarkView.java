package com.leavjenn.hews.ui.bookmark;

import android.support.annotation.StringRes;

import com.leavjenn.hews.model.Post;

import java.util.List;

public interface BookmarkView {
    void showSwipeRefresh();

    void hideSwipeRefresh();

    void showPosts(List<Post> postList);

    void resetAdapter();

    void updateListFooter(int loadingState);

    void updatePrompt(@StringRes int resPrompt);

    void showInfoLog(String tag, String msg);

    void showErrorLog(String tag, String msg);
}
