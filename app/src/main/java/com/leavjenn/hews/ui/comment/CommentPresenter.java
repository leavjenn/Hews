package com.leavjenn.hews.ui.comment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.leavjenn.hews.Constants;
import com.leavjenn.hews.R;
import com.leavjenn.hews.data.local.LocalContract;
import com.leavjenn.hews.data.local.LocalDataManager;
import com.leavjenn.hews.ui.BasePresenter;
import com.leavjenn.hews.misc.SharedPrefsContract;
import com.leavjenn.hews.misc.UtilsContract;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.data.remote.DataManager;
import com.pushtorefresh.storio.sqlite.operations.delete.DeleteResult;
import com.pushtorefresh.storio.sqlite.operations.put.PutResult;
import com.pushtorefresh.storio.sqlite.operations.put.PutResults;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class CommentPresenter extends BasePresenter {
    public static final String TAG = "CommentPresenter";
    public static final String KEY_POST_PARCEL = "arg_post_parcel";
    public static final String KEY_IS_BOOKMARKED = "arg_is_bookmarked";
    public static final String KEY_POST_ID = "arg_post_id";
    public static final String KEY_IS_COMMENTS_LOADING_COMPLETED = "arg_is_fetch_completed";
    public static final String KEY_COMMENTS_PARCEL = "arg_comments_parcel";
    private CommentView mCommentView;
    private DataManager mDataManager;
    private LocalContract mLocalDataManager;
    private SharedPrefsContract mPrefsManager;
    private UtilsContract mUtils;

    private boolean mIsBookmarked, isCommentsLoadingCompleted;
    private Post mPost;
    private long mPostId;
    private List<Comment> mCachedCommentList;
    private Observable<List<Comment>> mCommentListObservable;
    private CompositeSubscription mCompositeSubscription;

    public CommentPresenter(@NonNull CommentView commentView) {
        mCommentView = commentView;
        mCompositeSubscription = new CompositeSubscription();
        mCachedCommentList = new ArrayList<>();
    }

    public CommentPresenter(@NonNull CommentView commentView, @NonNull DataManager dataManager,
                            @NonNull LocalContract localDataManager, @NonNull SharedPrefsContract prefsManager,
                            @NonNull UtilsContract utils) {
        this(commentView);
        mDataManager = dataManager;
        mLocalDataManager = localDataManager;
        mPrefsManager = prefsManager;
        mCompositeSubscription = new CompositeSubscription();
        mUtils = utils;
    }

    public void setView(CommentView commentView) {
        mCommentView = commentView;
    }

    public void setDataManager(DataManager dataManager) {
        mDataManager = dataManager;
    }

    public void setLocalDataManager(LocalDataManager localDataManager) {
        mLocalDataManager = localDataManager;
    }

    public void setPrefsManager(SharedPrefsContract sharedPrefsContract) {
        mPrefsManager = sharedPrefsContract;
    }

    public void setUtils(UtilsContract utils) {
        mUtils = utils;
    }

    public void setPost(Post post) {
        mPost = post;
    }

    public void setPostId(long postId) {
        mPostId = postId;
    }

    public void setBookmarkState(boolean isBookmarked) {
        mIsBookmarked = isBookmarked;
    }

    @Override
    public void setup() {
        if (mPost == null) {
            mCommentView.showInfoLog(TAG + ":setup", "null post");
            refresh();
        } else {
            mCommentView.showFooter();
            mCommentView.showHeader(mPost);
            if (mIsBookmarked) {
                if (mPrefsManager.areCommentsBookmarked(mPost.getId())) {
                    mCommentView.showInfoLog(TAG + ":setup", "get db comment");
                    getCommentsFromDb(mPost);
                } else {
                    // post kid list is not stored, get from getPost()
                    refresh();
                }
            }
            else if (isCommentsLoadingCompleted) {
                mCommentView.showInfoLog(TAG + ":setup", "get cached comment");
                mCommentView.restoreCachedComments(mCachedCommentList);
                mCommentView.updateListFooter(Constants.LOADING_FINISH);
            } else {
                mCommentView.showInfoLog(TAG + ":setup", "get comment");
                getComments(mPost, mCommentListObservable == null);
            }
        }
    }

    @Override
    public void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        mPostId = savedInstanceState.getLong(KEY_POST_ID);
        mPost = Parcels.unwrap(savedInstanceState.getParcelable(KEY_POST_PARCEL));
        mIsBookmarked = savedInstanceState.getBoolean(KEY_IS_BOOKMARKED);
        isCommentsLoadingCompleted = savedInstanceState.getBoolean(KEY_IS_COMMENTS_LOADING_COMPLETED);
        if (isCommentsLoadingCompleted) {
            mCachedCommentList = ((ArrayList<Comment>) Parcels.unwrap(
                savedInstanceState.getParcelable(KEY_COMMENTS_PARCEL)));
        }
    }

    @Override
    public void saveState(Bundle outState) {
        outState.putLong(KEY_POST_ID, mPostId);
        outState.putParcelable(KEY_POST_PARCEL, Parcels.wrap(mPost));
        outState.putBoolean(KEY_IS_BOOKMARKED, mIsBookmarked);
        outState.putBoolean(KEY_IS_COMMENTS_LOADING_COMPLETED, isCommentsLoadingCompleted);
        if (isCommentsLoadingCompleted) {
            outState.putParcelable(KEY_COMMENTS_PARCEL, Parcels.wrap(mCachedCommentList));
        }
    }

    @Override
    public void destroy() {
//        if (mCompositeSubscription.hasSubscriptions()) {
        mCompositeSubscription.clear();
//        }
        mCommentView.showInfoLog(TAG, "destroy");
        mCommentView = null;
        mDataManager = null;
        mLocalDataManager = null;
        mPrefsManager = null;
        mUtils = null;
    }

    @Override
    public void unsubscribe() {
        mCompositeSubscription.clear();
    }

    public void refresh() {
        if (!mUtils.isOnline()) {
            mCommentView.hideSwipeRefresh();
            mCommentView.showOfflineSnackBar();
            return;
        }
        isCommentsLoadingCompleted = false;
        mCompositeSubscription.clear();
        mCachedCommentList.clear();

        mCommentView.showSwipeRefresh();
        mCommentView.hideOfflineSnackBar();
        mCommentView.clearAdapter();
        getPost(mPost != null ? mPost.getId() : mPostId);
    }


    public void getPost(long postId) {
        mCompositeSubscription.add(mDataManager.getPost(postId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<Post>() {
                @Override
                public void call(Post post) {
                    mCommentView.hideSwipeRefresh();
                    mCommentView.showFooter();
                    mCommentView.updateListFooter(Constants.LOADING_IN_PROGRESS);
                    mPost = post;
                    mCommentView.showHeader(mPost);
                    getComments(mPost, true);
                    mCommentView.setToolbarUrl(mPost.getUrl());
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    mCommentView.showErrorLog("getPost", throwable.toString());
                }
            }));
    }

    public void getComments(final Post post, final boolean updateObservable) {
        if (!mUtils.isOnline()) {
            mCommentView.showOfflineSnackBarForShowComments(post, updateObservable);
            mCommentView.updateListFooter(Constants.LOADING_ERROR);
            return;
        }
        Log.i("getComments", "kids:" + String.valueOf(post.getKids()));
        if (post.getKids() != null && !post.getKids().isEmpty()) {
            if (updateObservable || mCommentListObservable == null) {
                mCommentListObservable = mDataManager.getComments(post, 0).cache();
                Log.i("getComments", "updateObservable");
            }
            mCompositeSubscription.add(mCommentListObservable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<List<Comment>>() {
                        @Override
                        public void onCompleted() {
                            mCommentView.updateListFooter(Constants.LOADING_FINISH);
                            for (int i = 0; i < mCommentView.getCommentsCount(); i++) {
                                Comment comment = mCommentView.getComment(i);
                                comment.setParent(post.getId());
                                comment.setIndex(i);
                            }
                            isCommentsLoadingCompleted = true;
                            mCachedCommentList = mCommentView.getAllComments();
//                        if (SharedPrefsManager.isPostBookmarked(prefs, post.getId())) {
                            if (mPrefsManager.isPostBookmarked(post.getId())) {
//                            putCommentsToDb(mCommentAdapter.getCommentList());
                                putCommentsToDb(mCommentView.getAllComments());
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            mCommentView.updateListFooter(Constants.LOADING_ERROR);
                            mCommentView.showLongToast(R.string.prompt_comments_loading_error);
                            mCommentView.showErrorLog("getComments", e.toString());
                        }

                        @Override
                        public void onNext(List<Comment> commentList) {
                            mCommentView.showComments(commentList);
                        }
                    })
            );
        } else {
            mCommentView.updateListFooter(Constants.LOADING_PROMPT_NO_CONTENT);
        }
    }

    private void getCommentsFromDb(Post post) {
        mCompositeSubscription.add(
            mLocalDataManager.getStoryCommentsFromDb(post.getId())
//            mDataManager.getStoryCommentsFromDb(getActivity(), post.getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Comment>>() {
                    @Override
                    public void call(List<Comment> comments) {
                        mCommentView.showComments(comments);
                        mCommentView.updateListFooter(Constants.LOADING_FINISH);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mCommentView.updateListFooter(Constants.LOADING_ERROR);
                        mCommentView.showErrorLog("getCommentFromDb", throwable.toString());
                    }
                }));
    }

    public void addBookmark() {
        putPostToDb(mPost);
    }

    public void removeBookmark() {
        deletePostAndCommentFromDb();
    }

    private void putPostToDb(Post post) {
        mCompositeSubscription.add(
            mLocalDataManager.putPostToDb(post)
//            mDataManager.putPostToDb(getActivity(), post)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<PutResult>() {
                    @Override
                    public void call(PutResult putResult) {
                        mCommentView.showBookmarkSuccessSnackBar();
//                        SharedPrefsManager.setPostBookmarked(prefs, mPost.getId());
                        mPrefsManager.setPostBookmarked(mPost.getId());
                        if (isCommentsLoadingCompleted) {
                            putCommentsToDb(mCommentView.getAllComments());
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mCommentView.showErrorLog("putPostToDb", throwable.toString());
                    }
                }));
    }

    private void putCommentsToDb(List<Comment> comments) {
        mCompositeSubscription.add(
            mLocalDataManager.putCommentsToDb(comments)
//            mDataManager.putCommentsToDb(getActivity(), comments)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<PutResults<Comment>>() {
                    @Override
                    public void call(PutResults<Comment> commentPutResults) {
//                        SharedPrefsManager.setCommentsBookmarked(prefs, mPost.getId());
                        mPrefsManager.setCommentsBookmarked(mPost.getId());
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mCommentView.showErrorLog("putCommentsToDb", throwable.toString());
                    }
                }));
    }

    private void deletePostAndCommentFromDb() {
        mCompositeSubscription.add(Observable.merge(
            mLocalDataManager.deletePostFromDb(mPost),
            mLocalDataManager.deleteStoryCommentsFromDb(mPost.getId())
//            mDataManager.deletePostFromDb(getActivity(), mPost),
//            mDataManager.deleteStoryCommentsFromDb(getActivity(), mPost.getId())
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<DeleteResult>() {
                @Override
                public void onCompleted() {
                    mCommentView.showUnbookmarkSuccessSnackBar();
//                    SharedPrefsManager.setPostUnbookmarked(prefs, mPost.getId());
                    mPrefsManager.setPostUnbookmarked(mPost.getId());
//                    SharedPrefsManager.setCommentsUnbookmarked(prefs, mPost.getId());
                    mPrefsManager.setCommentsUnbookmarked(mPost.getId());
                }

                @Override
                public void onError(Throwable e) {
                    mCommentView.showErrorLog("delPost&CommentFromDb", e.toString());
                }

                @Override
                public void onNext(DeleteResult deleteResult) {
                    mCommentView.showInfoLog(deleteResult.affectedTables().toString(),
                        String.valueOf(deleteResult.numberOfRowsDeleted()));
                }
            }));
    }
}
