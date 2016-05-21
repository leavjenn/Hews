package com.leavjenn.hews.misc;

public interface SharedPrefsContract {
    boolean isPostRead(long postId);

    boolean isShowPostSummary();

    boolean isPostBookmarked(long postId);

    boolean areCommentsBookmarked(long postId);

    void setPostBookmarked(long postId);

    void setCommentsBookmarked(long postId);

    void setPostUnbookmarked(long postId);

    void setCommentsUnbookmarked(long postId);
}
