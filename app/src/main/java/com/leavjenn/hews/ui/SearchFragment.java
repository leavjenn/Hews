package com.leavjenn.hews.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.leavjenn.hews.misc.SharedPrefsManager;
import com.leavjenn.hews.Utils;
import com.leavjenn.hews.listener.OnRecyclerViewCreateListener;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.network.DataManager;
import com.leavjenn.hews.ui.adapter.PostAdapter;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class SearchFragment extends Fragment implements PostAdapter.OnReachBottomListener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String KEY_KEYWORD = "key_keyword";
    private static final String KEY_TIME_RANGE = "key_time_range";
    private static final String KEY_SORT_METHOD = "key_sort_method";
    private static final String KEY_POST_ID_LIST = "key_post_id_list";
    private static final String KEY_LOADED_POSTS = "key_loaded_posts";
    private static final String KEY_LOADED_TIME = "key_loaded_time";
    private static final String KEY_LOADING_STATE = "key_loading_state";
    private static final String KEY_LAST_TIME_POSITION = "key_last_time_position";
    private static final String KEY_SEARCH_RESULT_TOTAL_PAGE = "key_search_result_total_page";

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private String mKeyword;
    private String mDateRange;
    private boolean mIsSortByDate;
    private int mLoadedTime;
    private int mLoadingState = Constants.LOADING_IDLE;
    private int mLastTimeListPosition;
    private int mSearchResultTotalPages;
    private List<Long> mPostIdList;
    private Boolean mShowPostSummary;
    private PostAdapter.OnItemClickListener mOnItemClickListener;
    private OnRecyclerViewCreateListener mOnRecyclerViewCreateListener;
    private LinearLayoutManager mLinearLayoutManager;
    private PostAdapter mPostAdapter;
    private SharedPreferences prefs;
    private DataManager mDataManager;
    private CompositeSubscription mCompositeSubscription;

    /**
     * @param keyword   Parameter 1.
     * @param dateRange Parameter 2.
     * @return A new instance of fragment SearchFragment.
     */
    public static SearchFragment newInstance(String keyword, String dateRange, boolean isSortByDate) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString(KEY_KEYWORD, keyword);
        args.putString(KEY_TIME_RANGE, dateRange);
        args.putBoolean(KEY_SORT_METHOD, isSortByDate);
        fragment.setArguments(args);
        return fragment;
    }

    public SearchFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnItemClickListener = (PostAdapter.OnItemClickListener) activity;
            mOnRecyclerViewCreateListener = (OnRecyclerViewCreateListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement (PostAdapter.OnItemClickListener)");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            mKeyword = getArguments().getString(KEY_KEYWORD);
//            mDateRange = getArguments().getString(KEY_TIME_RANGE);
//            mIsSortByDate = getArguments().getBoolean(KEY_SORT_METHOD);
//        }

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
        mDataManager = new DataManager();
        mCompositeSubscription = new CompositeSubscription();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search, container, false);
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_layout);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list_search);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mOnRecyclerViewCreateListener.onRecyclerViewCreate(mRecyclerView);
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
        mShowPostSummary = SharedPrefsManager.getShowPostSummary(prefs, getActivity());
        if (savedInstanceState != null) {
            mKeyword = getArguments().getString(KEY_KEYWORD);
            mDateRange = getArguments().getString(KEY_TIME_RANGE);
            mIsSortByDate = getArguments().getBoolean(KEY_SORT_METHOD);
            mPostIdList = Parcels.unwrap(savedInstanceState.getParcelable(KEY_POST_ID_LIST));
            if (mPostAdapter.getItemCount() == 0) {
                mPostAdapter.addAll((ArrayList<Post>) Parcels.unwrap(savedInstanceState.getParcelable(KEY_LOADED_POSTS)));
            }
            mSearchResultTotalPages = savedInstanceState.getInt(KEY_SEARCH_RESULT_TOTAL_PAGE);
            mLastTimeListPosition = savedInstanceState.getInt(KEY_LAST_TIME_POSITION, 0);
            mRecyclerView.getLayoutManager().scrollToPosition(mLastTimeListPosition);
            mLoadedTime = savedInstanceState.getInt(KEY_LOADED_TIME);
            mLoadingState = savedInstanceState.getInt(KEY_LOADING_STATE);
            if (mLoadingState == Constants.LOADING_IN_PROGRESS) {
                loadMore();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_KEYWORD, mKeyword);
        outState.putString(KEY_TIME_RANGE, mDateRange);
        outState.putBoolean(KEY_SORT_METHOD, mIsSortByDate);
        outState.putParcelable(KEY_LOADED_POSTS, Parcels.wrap(mPostAdapter.getPostList()));
        outState.putParcelable(KEY_POST_ID_LIST, Parcels.wrap(mPostIdList));
        outState.putInt(KEY_SEARCH_RESULT_TOTAL_PAGE, mSearchResultTotalPages);
        mLastTimeListPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).
            findFirstVisibleItemPosition();
        outState.putInt(KEY_LAST_TIME_POSITION, mLastTimeListPosition);
        outState.putInt(KEY_LOADED_TIME, mLoadedTime);
        outState.putInt(KEY_LOADING_STATE, mLoadingState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCompositeSubscription.hasSubscriptions()) {
            mCompositeSubscription.unsubscribe();
        }
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    void loadPostListFromSearch(String keyword, String dateRange, int page, boolean isSortByDate) {
        mCompositeSubscription.add(
            mDataManager.getSearchResult(keyword, "created_at_i>" + dateRange.substring(0, 10)
                + "," + "created_at_i<" + dateRange.substring(10), page, isSortByDate)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<HNItem.SearchResult>() {
                    @Override
                    public void call(HNItem.SearchResult searchResult) {
                        List<Long> list = new ArrayList<>();
                        mSearchResultTotalPages = searchResult.getNbPages();
                        for (int i = 0; i < searchResult.getHits().length; i++) {
                            list.add(searchResult.getHits()[i].getObjectID());
                        }
                        loadPostFromList(list);
                        mLoadingState = Constants.LOADING_IN_PROGRESS;
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.i("search", throwable.toString());
                    }
                }));
    }

    void loadPostFromList(List<Long> list) {
        mCompositeSubscription.add(mDataManager.getPosts(list)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Post>() {
                @Override
                public void onCompleted() {
                    mLoadingState = Constants.LOADING_IDLE;
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
                        if (mShowPostSummary && post.getKids() != null) {
                            loadSummary(post);
                        }
                    }
                }
            }));
    }

    void loadSummary(final Post post) {
        mCompositeSubscription.add(mDataManager.getSummary(post.getKids())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<Comment>() {
                @Override
                public void call(Comment comment) {
                    if (comment != null) {
                        post.setSummary(Html.fromHtml(comment.getText()).toString());
                        mPostAdapter.notifyItemChanged(post.getIndex());
                    } else {
                        post.setSummary(null);
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.e("loadSummary: " + String.valueOf(post.getId()), throwable.toString());
                }
            })
        );
    }

    public void refresh(String keyword, String dateRange, boolean isSortByDate) {
        mKeyword = keyword;
        mDateRange = dateRange;
        mIsSortByDate = isSortByDate;

        if (Utils.isOnline(getActivity())) {
            mLoadedTime = 1;
            mCompositeSubscription.clear();
            mSwipeRefreshLayout.setRefreshing(true);
            mPostAdapter.clear();
            loadPostListFromSearch(keyword, dateRange, 0, isSortByDate);
        } else {
            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
            Toast.makeText(getActivity(), "No connection :(", Toast.LENGTH_LONG).show();
        }
    }

    public void refresh(String dateRange) {
        refresh(mKeyword, dateRange, mIsSortByDate);
    }

    public void refresh(boolean isSortByDate) {
        refresh(mKeyword, mDateRange, isSortByDate);
    }

    public void refresh() {
        if (mKeyword != null && mDateRange != null) {
            refresh(mKeyword, mDateRange, mIsSortByDate);
        }
    }

    public String getKeyword() {
        return mKeyword;
    }

    public void setKeyword(String keyword) {
        mKeyword = keyword;
    }

    public String getDateRange() {
        return mDateRange;
    }

    public void setDateRange(String dateRange) {
        mDateRange = dateRange;
    }

    public boolean getIsSortByDate() {
        return mIsSortByDate;
    }

    public void setSortByDate(boolean isSortByDate) {
        mIsSortByDate = isSortByDate;
    }

    public void setSwipeRefreshLayoutState(boolean isEnable) {
        mSwipeRefreshLayout.setEnabled(isEnable);
    }

    private void loadMore() {
        if (mLoadingState == Constants.LOADING_IDLE) {
            if (mLoadedTime < mSearchResultTotalPages) {
                Log.i(String.valueOf(mSearchResultTotalPages), String.valueOf(mLoadedTime));
                loadPostListFromSearch(mKeyword, mDateRange, mLoadedTime++, mIsSortByDate);
                mLoadingState = Constants.LOADING_IN_PROGRESS;
            } else {
                Utils.showLongToast(getActivity(), R.string.no_more_posts_prompt);
                mLoadingState = Constants.LOADING_FINISH;
            }
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

    @Override
    public void OnReachBottom() {
        loadMore();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SharedPrefsManager.KEY_POST_FONT)
            || key.equals(SharedPrefsManager.KEY_POST_FONT_SIZE)
            || key.equals(SharedPrefsManager.KEY_POST_LINE_HEIGHT)) {
            mPostAdapter.updatePostPrefs();
            reformatListStyle();
        }
    }
}
