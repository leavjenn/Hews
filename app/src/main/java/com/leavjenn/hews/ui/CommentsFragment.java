package com.leavjenn.hews.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.leavjenn.hews.SharedPrefsManager;
import com.leavjenn.hews.listener.OnRecyclerViewCreateListener;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.network.DataManager;
import com.leavjenn.hews.network.HackerNewsService;
import com.leavjenn.hews.network.RetrofitHelper;
import com.leavjenn.hews.ui.adapter.CommentAdapter;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class CommentsFragment extends Fragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Post mPost;
    private long mPostId;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;

    private CommentAdapter mCommentAdapter;
    private SharedPreferences prefs;
    private DataManager mDataManager;
    private Subscription mSubscription;
    private HackerNewsService mService;
    OnRecyclerViewCreateListener mOnRecyclerViewCreateListener;

    private static final String ARG_POST = "post";
    private static final String ARG_POST_ID = "post_id";


    public static CommentsFragment newInstance(Post post) {
        CommentsFragment fragment = new CommentsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_POST, post);
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


    public CommentsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (getArguments().containsKey(ARG_POST)) {
                mPost = getArguments().getParcelable(ARG_POST);
            } else if (getArguments().containsKey(ARG_POST_ID)) {
                mPostId = getArguments().getLong(ARG_POST_ID);
            }
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_comments, container, false);
        initRecyclerView(rootView);
        //setupFab(rootView);

        mDataManager = new DataManager(Schedulers.io());
        if (mPost != null) {
            mCommentAdapter.addFooter(new HNItem.Footer());
            mCommentAdapter.addHeader(mPost);
            getComments(mPost.getKids());
        } else {
            if (savedInstanceState != null) {
                mPostId = savedInstanceState.getLong(ARG_POST_ID);
            }
            mService = new RetrofitHelper().getHackerNewsService();
//            getPost(mPostId);
            getPostInfo(mPostId);
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
        super.onDetach();
        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initRecyclerView(View rootView) {
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.comment_list);
        mCommentAdapter = new CommentAdapter(getActivity(), mRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setAdapter(mCommentAdapter);
        mOnRecyclerViewCreateListener.onRecyclerViewCreate(mRecyclerView);
    }

    void getPost(long postId) {
        mSubscription = (mService.getStory(String.valueOf(postId))
                .subscribeOn(mDataManager.getScheduler())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Post>() {
                    @Override
                    public void onCompleted() {
                        getComments(mPost.getKids());
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Post post) {
                        mPost = post;
                    }
                }));
    }

    public void refresh() {
        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
        mCommentAdapter.clear();
        mCommentAdapter.notifyDataSetChanged();
        if (mPost != null) {
            getPostInfo(mPost.getId());
        } else {
            getPostInfo(mPostId);
        }
    }

    public void getPostInfo(long postId) {
        mCommentAdapter.addFooter(new HNItem.Footer());
        mCommentAdapter.updateFooter(Constants.LOADING_IN_PROGRESS);
        mService = new RetrofitHelper().getHackerNewsService();
        mService.getItem(String.valueOf(postId), new Callback<Post>() {
            @Override
            public void success(Post post, Response response) {
                mPost = post;
                mCommentAdapter.addHeader(mPost);
                getComments(mPost.getKids());
                ((CommentsActivity) getActivity()).setUrl(mPost.getUrl());
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("getPostInfo", error.toString());
            }
        });
    }

    public void getComments(List<Long> commentIds) {
        if (commentIds != null && !commentIds.isEmpty()) {
            if (mSubscription != null) {
                mSubscription.unsubscribe();
            }
            mSubscription =
//                    mDataManager.getComments(commentIds, 0))
                    mDataManager.getCommentsFromFirebase(commentIds, 0)
                            .subscribeOn(mDataManager.getScheduler())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Subscriber<Comment>() {
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
                                public void onNext(Comment comment) {
                                    if (comment.getText() != null) {
                                        mCommentAdapter.addComment(comment);
                                    }
                                }
                            });
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
