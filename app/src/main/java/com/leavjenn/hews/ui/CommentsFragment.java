package com.leavjenn.hews.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.leavjenn.hews.Constants;
import com.leavjenn.hews.R;
import com.leavjenn.hews.Utils;
import com.leavjenn.hews.listener.OnRecyclerViewCreateListener;
import com.leavjenn.hews.misc.SharedPrefsManager;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.network.DataManager;
import com.leavjenn.hews.ui.adapter.CommentAdapter;
import com.pushtorefresh.storio.sqlite.operations.delete.DeleteResult;
import com.pushtorefresh.storio.sqlite.operations.put.PutResult;
import com.pushtorefresh.storio.sqlite.operations.put.PutResults;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class CommentsFragment extends Fragment
    implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String KEY_POST_PARCEL = "arg_post_parcel";
    private static final String KEY_IS_BOOKMARKED = "arg_is_bookmarked";
    private static final String KEY_POST_ID = "arg_post_id";
    private static final String KEY_IS_FETCH_COMPLETED = "arg_is_fetch_completed";
    private static final String KEY_COMMENTS_PARCEL = "arg_comments_parcel";
    private static final String KEY_LAST_TIME_POSITION = "key_last_time_position";

    private RelativeLayout layoutRoot;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private Snackbar snackbarNoConnection;

    private boolean mIsBookmarked, isFetchingCompleted;
    private Post mPost;
    private long mPostId;
    private int mLastTimeListPosition;
    private LinearLayoutManager mLinearLayoutManager;
    private CommentAdapter mCommentAdapter;
    private SharedPreferences prefs;
    private DataManager mDataManager;
    private Subscription mPostSubscription, mCommentsSubscription;
    private CompositeSubscription mCompositeSubscription;
    private OnRecyclerViewCreateListener mOnRecyclerViewCreateListener;

    public CommentsFragment() {
    }

    public static CommentsFragment newInstance(Parcelable postParcel, boolean isBookmarked) {
        CommentsFragment fragment = new CommentsFragment();
        Bundle args = new Bundle();
        args.putParcelable(KEY_POST_PARCEL, postParcel);
        args.putBoolean(KEY_IS_BOOKMARKED, isBookmarked);
        fragment.setArguments(args);
        return fragment;
    }

    public static CommentsFragment newInstance(Long postId) {
        CommentsFragment fragment = new CommentsFragment();
        Bundle args = new Bundle();
        args.putLong(KEY_POST_ID, postId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnRecyclerViewCreateListener = (OnRecyclerViewCreateListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement (MainActivity.OnRecyclerViewCreateListener)");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (getArguments().containsKey(KEY_POST_PARCEL)) {
                mPost = Parcels.unwrap(getArguments().getParcelable(KEY_POST_PARCEL));
                mIsBookmarked = getArguments().getBoolean(KEY_IS_BOOKMARKED);
            } else if (getArguments().containsKey(KEY_POST_ID)) {
                mPostId = getArguments().getLong(KEY_POST_ID);
            }
        }

        mDataManager = new DataManager();
        mCompositeSubscription = new CompositeSubscription();
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_comments, container, false);
        layoutRoot = (RelativeLayout) rootView.findViewById(R.id.layout_fragment_comment_root);
        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_layout);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.comment_list);
        setupRecyclerView();
        swipeRefreshLayout.setColorSchemeResources(R.color.orange_600,
            R.color.orange_900, R.color.orange_900, R.color.orange_600);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mPostId = savedInstanceState.getLong(KEY_POST_ID);
            mPost = Parcels.unwrap(savedInstanceState.getParcelable(KEY_POST_PARCEL));
            mIsBookmarked = savedInstanceState.getBoolean(KEY_IS_BOOKMARKED);
            isFetchingCompleted = savedInstanceState.getBoolean(KEY_IS_FETCH_COMPLETED);
            if (isFetchingCompleted) { // restore loaded comments
                if (mCommentAdapter.getItemCount() == 0) {
                    mCommentAdapter.addFooter(new HNItem.Footer());
                    mCommentAdapter.addHeader(mPost);
                    mCommentAdapter.addAllComments((ArrayList<Comment>) Parcels.unwrap(
                        savedInstanceState.getParcelable(KEY_COMMENTS_PARCEL)));
                    mCommentAdapter.updateFooter(Constants.LOADING_FINISH);
                }
                mLastTimeListPosition = savedInstanceState.getInt(KEY_LAST_TIME_POSITION, 0);
                mRecyclerView.getLayoutManager().scrollToPosition(mLastTimeListPosition);
            } else {
                mCommentAdapter.addFooter(new HNItem.Footer());
                mCommentAdapter.addHeader(mPost);
                if (mIsBookmarked) {
                    getCommentsFromDb(mPost);
                } else {
                    getComments(mPost);
                }
            }
        } else if (mPost != null) { // new instance, no saved instance state
            if (mIsBookmarked && !SharedPrefsManager.areCommentsBookmarked(prefs, mPost.getId())) {
                // post is bookmarked but comments are not
                getPost(mPost.getId());
            } else {
                mCommentAdapter.addFooter(new HNItem.Footer());
                mCommentAdapter.addHeader(mPost);
                if (mIsBookmarked) {
                    getCommentsFromDb(mPost);
                } else {
                    getComments(mPost);
                }
            }
        } else { // start from other app
            getPost(mPostId);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPostId = mPost.getId();
        outState.putLong(KEY_POST_ID, mPostId);
        outState.putParcelable(KEY_POST_PARCEL, Parcels.wrap(mPost));
        outState.putBoolean(KEY_IS_BOOKMARKED, mIsBookmarked);
        outState.putBoolean(KEY_IS_FETCH_COMPLETED, isFetchingCompleted);
        if (isFetchingCompleted) {
            outState.putParcelable(KEY_COMMENTS_PARCEL, Parcels.wrap(mCommentAdapter.getCommentList()));
            mLastTimeListPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).
                findFirstVisibleItemPosition();
            outState.putInt(KEY_LAST_TIME_POSITION, mLastTimeListPosition);
        }
    }

    @Override
    public void onDetach() {
        if (mCompositeSubscription.hasSubscriptions()) {
            mCompositeSubscription.unsubscribe();
        }
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        mOnRecyclerViewCreateListener = null;
        super.onDetach();
    }

    private void setupRecyclerView() {
        mCommentAdapter = new CommentAdapter(getActivity(), mRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setAdapter(mCommentAdapter);
        mOnRecyclerViewCreateListener.onRecyclerViewCreate(mRecyclerView);
    }

    public void refresh() {
        // Bug: SwipeRefreshLayout.setRefreshing(true); won't show at beginning
        // https://code.google.com/p/android/issues/detail?id=77712
        if (Utils.isOnline(getActivity())) {
            swipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(true);
                }
            });
            if (snackbarNoConnection != null && snackbarNoConnection.isShown()) {
                snackbarNoConnection.dismiss();
            }
            mCommentAdapter.clear();
            mCommentAdapter.notifyDataSetChanged();
            isFetchingCompleted = false;
            getPost(mPost != null ? mPost.getId() : mPostId);
        } else {
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            snackbarNoConnection = Snackbar.make(layoutRoot, R.string.no_connection_prompt,
                Snackbar.LENGTH_INDEFINITE);
            Utils.setSnackBarTextColor(snackbarNoConnection, getActivity(), R.color.orange_600);
            snackbarNoConnection.setAction(R.string.snackebar_action_retry, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    refresh();
                }
            });
            snackbarNoConnection.setActionTextColor(getResources().getColor(R.color.orange_800));
            snackbarNoConnection.show();
        }
    }

    public void getPost(long postId) {
        if (mPostSubscription != null && mPostSubscription.isUnsubscribed()) {
            mPostSubscription.unsubscribe();
        }
        if (mCommentsSubscription != null && mCommentsSubscription.isUnsubscribed()) {
            mCommentsSubscription.unsubscribe();
        }
        mCompositeSubscription.clear();
        mPostSubscription = mDataManager.getPost(postId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<Post>() {
                @Override
                public void call(Post post) {
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    mCommentAdapter.addFooter(new HNItem.Footer());
                    mCommentAdapter.updateFooter(Constants.LOADING_IN_PROGRESS);
                    mPost = post;
                    mCommentAdapter.addHeader(mPost);
                    getComments(mPost);
                    ((CommentsActivity) getActivity()).setUrl(mPost.getUrl());
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.e("getPost", throwable.toString());
                }
            });
        mCompositeSubscription.add(mPostSubscription);
    }

    public void getComments(final Post post) {
        if (post.getKids() != null && !post.getKids().isEmpty()) {
            if (mCommentsSubscription != null) {
                mCommentsSubscription.unsubscribe();
            }
            mCommentsSubscription = mDataManager.getComments(post, 0)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Comment>>() {
                    @Override
                    public void onCompleted() {
                        mCommentAdapter.updateFooter(Constants.LOADING_FINISH);
                        for (int i = 0; i < mCommentAdapter.getCommentList().size(); i++) {
                            Comment comment = mCommentAdapter.getCommentList().get(i);
                            comment.setParent(post.getId());
                            comment.setIndex(i);
                        }
                        isFetchingCompleted = true;
                        if (SharedPrefsManager.isPostBookmarked(prefs, post.getId())) {
                            putCommentsToDb(mCommentAdapter.getCommentList());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        mCommentAdapter.updateFooter(Constants.LOADING_ERROR);
                        Toast.makeText(getActivity(), "Comments loading error",
                            Toast.LENGTH_LONG).show();
                        Log.e("error", "There was an error retrieving the comments " + e);
                    }

                    @Override
                    public void onNext(List<Comment> commentList) {
                        mCommentAdapter.addAllComments(commentList);
                    }
                });
            mCompositeSubscription.add(mCommentsSubscription);
        } else {
            mCommentAdapter.updateFooter(Constants.LOADING_PROMPT_NO_CONTENT);
        }
    }

    private void getCommentsFromDb(Post post) {
        mCompositeSubscription.add(mDataManager.getStoryCommentsFromDb(getActivity(), post.getId())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<Comment>>() {
                @Override
                public void call(List<Comment> comments) {
                    mCommentAdapter.addAllComments(comments);
                    mCommentAdapter.updateFooter(Constants.LOADING_FINISH);
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.e("getCommentFromDb", throwable.toString());
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
        mCompositeSubscription.add(mDataManager.putPostToDb(getActivity(), post)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<PutResult>() {
                @Override
                public void call(PutResult putResult) {
                    Snackbar snackbarSucceed = Snackbar.make(layoutRoot, "Post saved!",
                        Snackbar.LENGTH_LONG);
                    TextView tvSnackbarText = (TextView) snackbarSucceed.getView()
                        .findViewById(android.support.design.R.id.snackbar_text);
                    tvSnackbarText.setTextColor(getResources().getColor(R.color.orange_600));
                    snackbarSucceed.show();
                    SharedPrefsManager.setPostBookmarked(prefs, mPost.getId());
                    if (isFetchingCompleted) {
                        putCommentsToDb(mCommentAdapter.getCommentList());
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.e("---putPostToDb", throwable.toString());
                }
            }));
    }

    private void putCommentsToDb(List<Comment> comments) {
        mCompositeSubscription.add(mDataManager.putCommentsToDb(getActivity(), comments)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<PutResults<Comment>>() {
                @Override
                public void call(PutResults<Comment> commentPutResults) {
                    SharedPrefsManager.setCommentsBookmarked(prefs, mPost.getId());
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.e("---putCommentsToDb", throwable.toString());
                }
            }));
    }

    private void deletePostAndCommentFromDb() {
        mCompositeSubscription.add(Observable.merge(mDataManager.deletePostFromDb(getActivity(), mPost),
            mDataManager.deleteStoryCommentsFromDb(getActivity(), mPost.getId()))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<DeleteResult>() {
                @Override
                public void onCompleted() {
                    Snackbar snackbarSucceed = Snackbar.make(layoutRoot, "Unbookmark succeed!",
                        Snackbar.LENGTH_LONG);
                    TextView tvSnackbarText = (TextView) snackbarSucceed.getView()
                        .findViewById(android.support.design.R.id.snackbar_text);
                    tvSnackbarText.setTextColor(getResources().getColor(R.color.orange_600));
                    snackbarSucceed.show();
                    SharedPrefsManager.setPostUnbookmarked(prefs, mPost.getId());
                    SharedPrefsManager.setCommentsUnbookmarked(prefs, mPost.getId());
                }

                @Override
                public void onError(Throwable e) {
                    Log.e("-delPost&CommentFromDb", e.toString());
                }

                @Override
                public void onNext(DeleteResult deleteResult) {
                    Log.i(deleteResult.affectedTables().toString(),
                        String.valueOf(deleteResult.numberOfRowsDeleted()));
                }
            }));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SharedPrefsManager.KEY_COMMENT_FONT_SIZE)
            || key.equals(SharedPrefsManager.KEY_COMMENT_LINE_HEIGHT)
            || key.equals(SharedPrefsManager.KEY_COMMENT_FONT)) {
            mCommentAdapter.updateCommentPrefs();
            reformatListStyle();
        }
    }

    private void reformatListStyle() {
        if (mLinearLayoutManager != null) {
            int position = mLinearLayoutManager.findFirstVisibleItemPosition();
            int offset = 0;
            View firstChild = mLinearLayoutManager.getChildAt(0);
            if (firstChild != null) {
                offset = firstChild.getTop();
            }
            CommentAdapter newAdapter = (CommentAdapter) mRecyclerView.getAdapter();
            mRecyclerView.setAdapter(newAdapter);
            mLinearLayoutManager.scrollToPositionWithOffset(position, offset);
        }
    }

    public void setSwipeRefreshLayoutState(boolean isEnabled) {
        swipeRefreshLayout.setEnabled(isEnabled);
    }
}
