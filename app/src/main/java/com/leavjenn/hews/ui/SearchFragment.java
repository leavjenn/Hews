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

import com.leavjenn.hews.R;
import com.leavjenn.hews.SharedPrefsManager;
import com.leavjenn.hews.Utils;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.network.DataManager;
import com.leavjenn.hews.ui.adapter.PostAdapter;
import com.leavjenn.hews.ui.widget.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;
import rx.android.app.AppObservable;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class SearchFragment extends Fragment implements PostAdapter.OnReachBottomListener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String KEY_KEYWORD = "keyword";
    private static final String KEY_TIME_RANGE = "time_range";
    private static final String KEY_SORT = "sort";

    private String mKeyword;
    private String mTimeRange;
    private boolean mIsSortByDate;

    static int LOADING_TIME = 1;
    static Boolean IS_LOADING = false;
    static Boolean SHOW_POST_SUMMARY = false;

    private int mLastTimeListPosition;
    private PostAdapter.OnItemClickListener mOnItemClickListener;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private FloatingActionButton fab;
    private LinearLayoutManager mLinearLayoutManager;
    private PostAdapter mPostAdapter;
    private SharedPreferences prefs;
    int searchResultTotalPages = 0;
    private DataManager mDataManager;
    private CompositeSubscription mCompositeSubscription;

    /**
     * @param keyword   Parameter 1.
     * @param timeRange Parameter 2.
     * @return A new instance of fragment SearchFragment.
     */
    public static SearchFragment newInstance(String keyword, String timeRange, boolean isSortByDate) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString(KEY_KEYWORD, keyword);
        args.putString(KEY_TIME_RANGE, timeRange);
        args.putBoolean(KEY_SORT, isSortByDate);
        fragment.setArguments(args);
        return fragment;
    }

    public SearchFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mKeyword = getArguments().getString(KEY_KEYWORD);
            mTimeRange = getArguments().getString(KEY_TIME_RANGE);
            mIsSortByDate = getArguments().getBoolean(KEY_SORT);
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search, container, false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_layout);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list_search);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mPostAdapter = new PostAdapter(this.getActivity(), this, mOnItemClickListener);
        mRecyclerView.setAdapter(mPostAdapter);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.orange_600,
                R.color.orange_900, R.color.orange_900, R.color.orange_600);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh(mKeyword, mTimeRange, mIsSortByDate);
            }
        });
        SHOW_POST_SUMMARY = SharedPrefsManager.getShowPostSummary(prefs, getActivity());
        mDataManager = new DataManager(Schedulers.io());
        mCompositeSubscription = new CompositeSubscription();
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (Utils.isOnline(getActivity())) {
            // Bug: SwipeRefreshLayout.setRefreshing(true); won't show at beginning
            // https://code.google.com/p/android/issues/detail?id=77712
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
            //loadPostListFromSearch(mKeyword, mTimeRange, 0, mIsSortByDate);
        } else {
            Toast.makeText(getActivity(), "No connection :(", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnItemClickListener = (PostAdapter.OnItemClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement (PostAdapter.OnItemClickListener)");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCompositeSubscription.unsubscribe();
    }

    void loadPostListFromSearch(String keyword, String timeRange, int page, boolean isSortByDate) {
        mCompositeSubscription.add(AppObservable.bindActivity(getActivity(),
                mDataManager.getSearchResult(keyword, "created_at_i>" + timeRange.substring(0, 10)
                        + "," + "created_at_i<" + timeRange.substring(10), page, isSortByDate))
                .subscribeOn(mDataManager.getScheduler())
                .subscribe(new Subscriber<HNItem.SearchResult>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i("search", e.toString());
                    }

                    @Override
                    public void onNext(HNItem.SearchResult searchResult) {
                        List<Long> list = new ArrayList<>();
                        searchResultTotalPages = searchResult.getNbPages();
                        for (int i = 0; i < searchResult.getHits().length; i++) {
                            list.add(searchResult.getHits()[i].getObjectID());
                        }
                        loadPostFromList(list);
                        IS_LOADING = true;
                    }
                }));
    }

    void loadPostFromList(List<Long> list) {
        mCompositeSubscription.add(AppObservable.bindActivity(getActivity(),
                mDataManager.getPostFromList(list))
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
                            if (SHOW_POST_SUMMARY && post.getKids() != null) {
                                loadSummary(post);
                            }
                        }
                    }
                }));
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

    public void refresh(String keyword, String timeRange, boolean isSortByDate) {
        mKeyword = keyword;
        mTimeRange = timeRange;
        mIsSortByDate = isSortByDate;

        if (Utils.isOnline(getActivity())) {
            LOADING_TIME = 1;
            mCompositeSubscription.clear();
            mSwipeRefreshLayout.setRefreshing(true);
            mPostAdapter.clear();
            mPostAdapter.notifyDataSetChanged();
            loadPostListFromSearch(keyword, timeRange, 0, isSortByDate);
        } else {
            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
            Toast.makeText(getActivity(), "No connection :(", Toast.LENGTH_LONG).show();
        }
    }

    public void refresh(String timeRange) {
        refresh(mKeyword, timeRange, mIsSortByDate);
    }

    public void refresh(boolean isSortByDate) {
        refresh(mKeyword, mTimeRange, isSortByDate);
    }

    public String getKeyword() {
        return mKeyword;
    }

    public String getTimeRange() {
        return mTimeRange;
    }

    public boolean getIsSortByDate() {
        return mIsSortByDate;
    }

    @Override
    public void OnReachBottom() {
        if (!IS_LOADING) {
            if (LOADING_TIME < searchResultTotalPages) {
                Log.i(String.valueOf(searchResultTotalPages), String.valueOf(LOADING_TIME));
                loadPostListFromSearch(mKeyword, mTimeRange, LOADING_TIME++, mIsSortByDate);
                IS_LOADING = true;
            } else {
                Toast.makeText(getActivity(), "No more posts", Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }
}
