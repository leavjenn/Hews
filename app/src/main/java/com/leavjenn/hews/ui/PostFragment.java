package com.leavjenn.hews.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.leavjenn.hews.Constants;
import com.leavjenn.hews.R;
import com.leavjenn.hews.SharedPrefsManager;
import com.leavjenn.hews.Utils;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.network.DataManager;
import com.leavjenn.hews.ui.adapter.PostAdapter;
import com.leavjenn.hews.ui.widget.FloatingActionButton;

import java.util.List;

import rx.Subscriber;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class PostFragment extends Fragment implements PostAdapter.OnReachBottomListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";
//    private String mParam1;
//    private String mParam2;

    public static final String KEY_LAST_TIME_POSITION = "key_last_time_position";
    public static final String KEY_STORY_TYPE_URL = "story_type";

    final static int ITEM_LOADING_NUM = 30;
    static int LOADING_TIME = 1;
    static Boolean IS_LOADING = false;
    static Boolean SHOW_POST_SUMMARY = false;


    private int mLastTimeListPosition;
    private String mStoryTypeUrl;
    private PostAdapter.OnItemClickListener mOnItemClickListener;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private FloatingActionButton fab;
    private LinearLayoutManager mLinearLayoutManager;
    private PostAdapter mPostAdapter;
    private SharedPreferences prefs;
    private List<Long> mPostIdList;
    private DataManager mDataManager;
    private CompositeSubscription mCompositeSubscription;


//    public static PostFragment newInstance(String param1, String param2) {
//        PostFragment fragment = new PostFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
//        return fragment;
//    }

    public PostFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnItemClickListener = (PostAdapter.OnItemClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement (PostAdapter.OnItemClickListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
        mStoryTypeUrl = Constants.KEY_TOP_STORIES_URL;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_post, container, false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_layout);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.item_list);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mPostAdapter = new PostAdapter(this.getActivity(), this, mOnItemClickListener);
        mRecyclerView.setAdapter(mPostAdapter);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.orange_600,
                R.color.orange_900, R.color.orange_900, R.color.orange_600);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        setupFAB();
        SHOW_POST_SUMMARY = SharedPrefsManager.getShowPostSummary(prefs, getActivity());
        mDataManager = new DataManager(Schedulers.io());
        mCompositeSubscription = new CompositeSubscription();
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mLastTimeListPosition = savedInstanceState.getInt(KEY_LAST_TIME_POSITION, 0);
            mStoryTypeUrl = savedInstanceState.getString(KEY_STORY_TYPE_URL, Constants.KEY_TOP_STORIES_URL);
            Log.d("mLastTimeListPosition", String.valueOf(mLastTimeListPosition));
        }

        if (Utils.isOnline(getActivity())) {
            // Bug: SwipeRefreshLayout.setRefreshing(true); won't show at beginning
            // https://code.google.com/p/android/issues/detail?id=77712
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
            loadPostListFromFirebase();
        } else {
            Toast.makeText(getActivity(), "No connection :(", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!SharedPrefsManager.getShowPostSummary(prefs, getActivity()).equals(SHOW_POST_SUMMARY)) {
            SHOW_POST_SUMMARY = SharedPrefsManager.getShowPostSummary(prefs, getActivity());
            refresh();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCompositeSubscription.unsubscribe();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int lastTimePosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).
                findFirstVisibleItemPosition();
        outState.putInt(KEY_LAST_TIME_POSITION, lastTimePosition);
        outState.putString(KEY_STORY_TYPE_URL, mStoryTypeUrl);
        Log.d("saveState", String.valueOf(lastTimePosition));
    }

    private void setupFAB() {
        String mode = SharedPrefsManager.getFabMode(prefs);
        if (!mode.equals(SharedPrefsManager.FAB_DISABLE)) {
            fab.setVisibility(View.VISIBLE);
            fab.setRecyclerView(mRecyclerView);
            fab.setScrollDownMode(SharedPrefsManager.getFabMode(prefs));
            //set fab position to default
            fab.setTranslationX(0f);
            fab.setTranslationY(0f);
        } else {
            fab.setVisibility(View.GONE);
        }
    }

    void loadPostListFromFirebase() {
        mCompositeSubscription.add(AppObservable.bindActivity(getActivity(),
                mDataManager.getPostListFromFirebase(mStoryTypeUrl))
                .subscribeOn(mDataManager.getScheduler())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Long>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("loadPostList", e.toString());
                    }

                    @Override
                    public void onNext(List<Long> longs) {
                        mPostIdList = longs;
                        Toast.makeText(getActivity(), "Feed list loaded", Toast.LENGTH_SHORT).show();
                        loadPostFromList(mPostIdList.subList(0, ITEM_LOADING_NUM));
                        IS_LOADING = true;
                    }
                }));
    }

    void loadPostFromList(List<Long> list) {
        mCompositeSubscription.add(AppObservable.bindActivity(getActivity(),
                mDataManager.getPostFromListFirebase(list))
                .subscribeOn(mDataManager.getScheduler())
                .subscribe(new Subscriber<Post>() {
                    @Override
                    public void onCompleted() {
                        IS_LOADING = false;
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("loadPostFromList", e.toString());
                    }

                    @Override
                    public void onNext(Post post) {
                        mSwipeRefreshLayout.setRefreshing(false);
                        mRecyclerView.setVisibility(View.VISIBLE);
                        if (post != null) {
                            mPostAdapter.add(post);
                            post.setIndex(mPostAdapter.getItemCount() - 1);
                            if (mStoryTypeUrl != Constants.KEY_ASK_HN_URL
                                    && mStoryTypeUrl != Constants.KEY_SHOW_HN_URL
                                    && SHOW_POST_SUMMARY && post.getKids() != null) {
                                loadSummary(post);
                            }
                        }
                    }
                }));
    }


    void refresh() {
        if (Utils.isOnline(getActivity())) {
            LOADING_TIME = 1;
            mCompositeSubscription.clear();
            mSwipeRefreshLayout.setRefreshing(true);
            mPostAdapter.clear();
            mPostAdapter.notifyDataSetChanged();
            loadPostListFromFirebase();
        } else {
            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
            Toast.makeText(getActivity(), "No connection :(", Toast.LENGTH_LONG).show();
        }
    }

    private void reformatListStyle() {
        int position = mLinearLayoutManager.findFirstVisibleItemPosition();
        int offset = 0;
        View firstChild = mLinearLayoutManager.getChildAt(0);
        if (firstChild != null) {
            offset = firstChild.getTop();
        }
        PostAdapter newAdapter = (PostAdapter) mRecyclerView.getAdapter();
        mRecyclerView.setAdapter(newAdapter);
        mLinearLayoutManager.scrollToPositionWithOffset(position, offset);
    }

    void loadSummary(final Post post) {
        mCompositeSubscription.add(AppObservable.bindActivity(getActivity(),
                        mDataManager.getSummary(post.getKids()))
                        .subscribeOn(mDataManager.getScheduler())
                        .subscribe(new Subscriber<Comment>() {
                            @Override
                            public void onCompleted() {
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e("err " + String.valueOf(post.getId()), e.toString());
                            }

                            @Override
                            public void onNext(Comment comment) {
                                if (comment != null) {
                                    post.setSummary(Html.fromHtml(comment.getText()).toString());
                                    mPostAdapter.notifyItemChanged(post.getIndex());
                                } else {
                                    post.setSummary(null);
                                }
                            }
                        })
        );
    }

    public void updateStoryFeed(String storyType) {
        mStoryTypeUrl = storyType;
        refresh();
    }

    @Override
    public void OnReachBottom() {
        if (!IS_LOADING) {
            int start = ITEM_LOADING_NUM * LOADING_TIME, end = ITEM_LOADING_NUM * (++LOADING_TIME);
            if (ITEM_LOADING_NUM * LOADING_TIME < mPostIdList.size()) {
                loadPostFromList(mPostIdList.subList(start, end));
                IS_LOADING = true;
                //Toast.makeText(getActivity(), "Loading more", Toast.LENGTH_SHORT).show();
                Log.d("loading", String.valueOf(start) + " - " + end);
            } else {
                Toast.makeText(getActivity(), "No more posts", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SharedPrefsManager.KEY_POST_FONT)
                || key.equals(SharedPrefsManager.KEY_POST_FONT_SIZE)
                || key.equals(SharedPrefsManager.KEY_POST_LINE_HEIGHT)) {
            mPostAdapter.updatePostPrefs();
            reformatListStyle();
        }
        if (key.equals(SharedPrefsManager.KEY_FAB_MODE)) {
            setupFAB();
        }
    }

}
