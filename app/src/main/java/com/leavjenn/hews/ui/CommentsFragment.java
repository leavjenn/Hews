package com.leavjenn.hews.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.leavjenn.hews.Constants;
import com.leavjenn.hews.R;
import com.leavjenn.hews.listener.OnRecyclerViewCreateListener;
import com.leavjenn.hews.misc.SharedPrefsManager;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.network.DataManager;
import com.leavjenn.hews.ui.adapter.CommentAdapter;

import org.parceler.Parcels;

import java.util.List;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class CommentsFragment extends Fragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Post mPost;
    private long mPostId;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;

    private CommentAdapter mCommentAdapter;
    private SharedPreferences prefs;
    private DataManager mDataManager;
    private Subscription mPostSubscription, mCommentsSubscription;
    private CompositeSubscription mCompositeSubscription;
    OnRecyclerViewCreateListener mOnRecyclerViewCreateListener;

    private static final String ARG_POST_PARCEL = "post_parcel";
    private static final String ARG_POST_ID = "post_id";

    public CommentsFragment() {
    }

    public static CommentsFragment newInstance(Parcelable postParcel) {
        CommentsFragment fragment = new CommentsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_POST_PARCEL, postParcel);
        fragment.setArguments(args);
        return fragment;
    }

    public static CommentsFragment newInstance(Long postId) {
        CommentsFragment fragment = new CommentsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_POST_ID, postId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (getArguments().containsKey(ARG_POST_PARCEL)) {
                mPost = Parcels.unwrap(getArguments().getParcelable(ARG_POST_PARCEL));
            } else if (getArguments().containsKey(ARG_POST_ID)) {
                mPostId = getArguments().getLong(ARG_POST_ID);
            }
        }

        mCompositeSubscription = new CompositeSubscription();
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_comments, container, false);
        initRecyclerView(rootView);
        //setupFab(rootView);

        mDataManager = new DataManager();
        if (mPost != null) {
            mCommentAdapter.addFooter(new HNItem.Footer());
            mCommentAdapter.addHeader(mPost);
            getComments(mPost);
        } else {
            if (savedInstanceState != null) {
                mPostId = savedInstanceState.getLong(ARG_POST_ID);
            }
            getPost(mPostId);
        }
        return rootView;
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPostId = mPost.getId();
        outState.putLong(ARG_POST_ID, mPostId);
    }

    @Override
    public void onDetach() {
        if (mCompositeSubscription.hasSubscriptions()) {
            mCompositeSubscription.unsubscribe();
        }
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDetach();
    }

    private void initRecyclerView(View rootView) {
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.comment_list);
        mCommentAdapter = new CommentAdapter(getActivity(), mRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setAdapter(mCommentAdapter);
        mOnRecyclerViewCreateListener.onRecyclerViewCreate(mRecyclerView);
    }

    public void refresh() {
        mCommentAdapter.clear();
        mCommentAdapter.notifyDataSetChanged();
        getPost(mPost != null ? mPost.getId() : mPostId);
    }

    public void getPost(long postId) {
        mCommentAdapter.addFooter(new HNItem.Footer());
        mCommentAdapter.updateFooter(Constants.LOADING_IN_PROGRESS);
        if (mPostSubscription != null) {
            mPostSubscription.unsubscribe();
        }
        mPostSubscription = mDataManager.getPost(postId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Post>() {
                    @Override
                    public void call(Post post) {
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

    public void getComments(Post post) {
        if (post.getKids() != null && !post.getKids().isEmpty()) {
            if (mCommentsSubscription != null) {
                mCommentsSubscription.unsubscribe();
            }
            mCommentsSubscription =
//                    mDataManager.getCommentsUseFirebase(commentIds, 0))
                    mDataManager.getComments(post, 0)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Subscriber<List<Comment>>() {
                                @Override
                                public void onCompleted() {
                                    mCommentAdapter.updateFooter(Constants.LOADING_FINISH);
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
                                    for (Comment comment : commentList) {
                                        mCommentAdapter.addComment(comment);
                                    }
                                }
                            });
            mCompositeSubscription.add(mCommentsSubscription);
        } else {
            mCommentAdapter.updateFooter(Constants.LOADING_PROMPT_NO_CONTENT);
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SharedPrefsManager.KEY_COMMENT_FONT_SIZE)
                || key.equals(SharedPrefsManager.KEY_COMMENT_LINE_HEIGHT)
                || key.equals(SharedPrefsManager.KEY_COMMENT_FONT)) {
            mCommentAdapter.updateCommentPrefs();
            reformatListStyle();
        }
    }
}
