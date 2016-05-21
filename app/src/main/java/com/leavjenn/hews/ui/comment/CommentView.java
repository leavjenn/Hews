package com.leavjenn.hews.ui.comment;

import android.support.annotation.StringRes;

import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.Post;

import java.util.List;

public interface CommentView {
    void hideSwipeRefresh();

    void showSwipeRefresh();

    void showOfflineSnackBar();

    void hideOfflineSnackBar();

    void showOfflineSnackBarForShowComments(Post post, boolean update);

    void showBookmarkSuccessSnackBar();

    void showUnbookmarkSuccessSnackBar();

    void showHeader(Post post);

    void showComments(List<Comment> commentList);

    void clearAdapter();

    void showFooter();

    void updateListFooter(int loadingState);

    List<Comment> getAllComments();

    int getCommentsCount();

    Comment getComment(int index);

    void restoreCachedComments(List<Comment> commentList);

    void setToolbarUrl(String url);

    void showLongToast(@StringRes int stringId);

    void showInfoLog(String tag, String msg);

    void showErrorLog(String tag, String msg);
}
