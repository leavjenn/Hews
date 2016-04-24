package com.leavjenn.hews.ui;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.leavjenn.hews.Constants;
import com.leavjenn.hews.R;
import com.leavjenn.hews.Utils;
import com.leavjenn.hews.misc.SharedPrefsManager;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

//public class SearchFragment extends Fragment implements PostAdapter.OnReachBottomListener,
//    SharedPreferences.OnSharedPreferenceChangeListener {
public class SearchFragment extends BasePostListFragment {
    private static final String KEY_KEYWORD = "key_keyword";
    private static final String KEY_TIME_RANGE = "key_time_range";
    private static final String KEY_SORT_METHOD = "key_sort_method";
    private static final String KEY_POST_ID_LIST = "key_post_id_list";
    private static final String KEY_LOADED_POSTS = "key_loaded_posts";
    private static final String KEY_LOADED_TIME = "key_loaded_time";
    private static final String KEY_LOADING_STATE = "key_loading_state";
    private static final String KEY_LAST_TIME_POSITION = "key_last_time_position";
    private static final String KEY_SEARCH_RESULT_TOTAL_PAGE = "key_search_result_total_page";

    private RelativeLayout layoutRoot;
    //    private SwipeRefreshLayout swipeRefreshLayout;
//    private RecyclerView rvPostList;
    private Snackbar snackbarNoConnection;

    private String mKeyword;
    private String mDateRange;
    private boolean mIsSortByDate;
    private int mLoadedTime;
    private int mLoadingState = Constants.LOADING_IDLE;
    //    private int mLastTimeListPosition;
    private int mSearchResultTotalPages;
    private List<Long> mPostIdList;
    private Boolean mShowPostSummary;
//    private PostAdapter.OnItemClickListener mOnItemClickListener;
//    private OnRecyclerViewCreateListener mOnRecyclerViewCreateListener;
//    private LinearLayoutManager mLinearLayoutManager;
//    private PostAdapter mPostAdapter;
//    private SharedPreferences prefs;
//    private DataManager mDataManager;
//    private CompositeSubscription mCompositeSubscription;

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

//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        try {
//            mOnItemClickListener = (PostAdapter.OnItemClickListener) activity;
//            mOnRecyclerViewCreateListener = (OnRecyclerViewCreateListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                + " must implement (PostAdapter.OnItemClickListener)");
//        }
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            mKeyword = getArguments().getString(KEY_KEYWORD);
//            mDateRange = getArguments().getString(KEY_TIME_RANGE);
//            mIsSortByDate = getArguments().getBoolean(KEY_SORT_METHOD);
//        }

//        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
//        prefs.registerOnSharedPreferenceChangeListener(this);
//        mDataManager = new DataManager();
//        mCompositeSubscription = new CompositeSubscription();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search, container, false);
        layoutRoot = (RelativeLayout) rootView.findViewById(R.id.layout_search_root);
        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_layout);
        rvPostList = (RecyclerView) rootView.findViewById(R.id.list_search);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        mLinearLayoutManager = new LinearLayoutManager(getActivity());
//        rvPostList.setLayoutManager(mLinearLayoutManager);
//        mOnRecyclerViewCreateListener.onRecyclerViewCreate(rvPostList);
//        mPostAdapter = new PostAdapter(this.getActivity(), this, mOnItemClickListener);
//        rvPostList.setAdapter(mPostAdapter);
//        swipeRefreshLayout.setColorSchemeResources(R.color.orange_600,
//            R.color.orange_900, R.color.orange_900, R.color.orange_600);
//        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//                refresh();
//            }
//        });
        mShowPostSummary = SharedPrefsManager.getShowPostSummary(prefs, getActivity());
        if (savedInstanceState != null) {
            mKeyword = savedInstanceState.getString(KEY_KEYWORD);
            mDateRange = savedInstanceState.getString(KEY_TIME_RANGE);
            mIsSortByDate = savedInstanceState.getBoolean(KEY_SORT_METHOD);
            mPostIdList = Parcels.unwrap(savedInstanceState.getParcelable(KEY_POST_ID_LIST));
            if (mPostAdapter.getItemCount() == 0) {
                mPostAdapter.addFooter(new HNItem.Footer());
                mPostAdapter.addAllPosts((ArrayList<Post>) Parcels.unwrap(savedInstanceState.getParcelable(KEY_LOADED_POSTS)));
            }
            mSearchResultTotalPages = savedInstanceState.getInt(KEY_SEARCH_RESULT_TOTAL_PAGE);
//            mLastTimeListPosition = savedInstanceState.getInt(KEY_LAST_TIME_POSITION, 0);
//            rvPostList.getLayoutManager().scrollToPosition(mLastTimeListPosition);
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
//        mLastTimeListPosition = ((LinearLayoutManager) rvPostList.getLayoutManager()).
//            findFirstVisibleItemPosition();
//        outState.putInt(KEY_LAST_TIME_POSITION, mLastTimeListPosition);
        outState.putInt(KEY_LOADED_TIME, mLoadedTime);
        outState.putInt(KEY_LOADING_STATE, mLoadingState);
    }

//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        if (mCompositeSubscription.hasSubscriptions()) {
//            mCompositeSubscription.unsubscribe();
//        }
//        prefs.unregisterOnSharedPreferenceChangeListener(this);
//    }

    void loadPostListFromSearch(String keyword, String dateRange, int page, boolean isSortByDate) {
        mCompositeSubscription.add(
            mDataManager.getSearchResult(keyword, "created_at_i>" + dateRange.substring(0, 10)
                + "," + "created_at_i<" + dateRange.substring(10), page, isSortByDate)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<HNItem.SearchResult>() {
                    @Override
                    public void call(HNItem.SearchResult searchResult) {
                        if (searchResult.getHits().length == 0) {
                            swipeRefreshLayout.setRefreshing(false);
                            mPostAdapter.updatePrompt(R.string.no_search_result_prompt);
                            updateLoadingState(Constants.LOADING_PROMPT_NO_CONTENT);
                            mPostAdapter.notifyDataSetChanged();
                            return;
                        }
                        List<Long> list = new ArrayList<>();
                        mSearchResultTotalPages = searchResult.getNbPages();
                        for (int i = 0; i < searchResult.getHits().length; i++) {
                            list.add(searchResult.getHits()[i].getObjectID());
                        }
                        loadPostFromList(list, true);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        swipeRefreshLayout.setRefreshing(false);
                        Log.i("loadPostListFromSearch", throwable.toString());
                    }
                }));
    }

    void loadPostFromList(List<Long> list, boolean isByOrder) {
        mCompositeSubscription.add(mDataManager.getPosts(list, isByOrder)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Post>() {
                @Override
                public void onCompleted() {
                    updateLoadingState(Constants.LOADING_IDLE);
                }

                @Override
                public void onError(Throwable e) {
                    swipeRefreshLayout.setRefreshing(false);
                    updateLoadingState(Constants.LOADING_ERROR);
                    Log.e("loadPostFromList", e.toString());
                }

                @Override
                public void onNext(Post post) {
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    rvPostList.setVisibility(View.VISIBLE);
                    if (post != null) {
                        post.setIndex(mPostAdapter.getItemCount() - 1);
                        Utils.setupPostUrl(post);
                        mPostAdapter.addPost(post);
                        if (mLoadingState != Constants.LOADING_IN_PROGRESS) {
                            updateLoadingState(Constants.LOADING_IN_PROGRESS);
                        }
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

    @Override
    public void refresh() {
        if (mKeyword != null && mDateRange != null) {
            refresh(mKeyword, mDateRange, mIsSortByDate);
        } else {
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    public void refresh(String dateRange) {
        refresh(mKeyword, dateRange, mIsSortByDate);
    }

    public void refresh(boolean isSortByDate) {
        refresh(mKeyword, mDateRange, isSortByDate);
    }

    public void refresh(String keyword, String dateRange, boolean isSortByDate) {
        mKeyword = keyword;
        mDateRange = dateRange;
        mIsSortByDate = isSortByDate;

        if (Utils.isOnline(getActivity())) {
            mLoadedTime = 1;
            mCompositeSubscription.clear();
            swipeRefreshLayout.setRefreshing(true);
            if (snackbarNoConnection != null && snackbarNoConnection.isShown()) {
                snackbarNoConnection.dismiss();
            }
            mPostAdapter.clear();
            mPostAdapter.addFooter(new HNItem.Footer());
            updateLoadingState(Constants.LOADING_IDLE);
            loadPostListFromSearch(keyword, dateRange, 0, isSortByDate);
        } else {
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            snackbarNoConnection = Snackbar.make(layoutRoot, R.string.no_connection_prompt,
                Snackbar.LENGTH_INDEFINITE);
            Utils.setSnackBarTextColor(snackbarNoConnection, getActivity(), android.R.color.white);
            snackbarNoConnection.setAction(R.string.snackebar_action_retry, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    refresh();
                }
            });
            snackbarNoConnection.setActionTextColor(getResources().getColor(R.color.orange_600));
            snackbarNoConnection.show();
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
        swipeRefreshLayout.setEnabled(isEnable);
    }

    private void updateLoadingState(int loadingState) {
        mLoadingState = loadingState;
        mPostAdapter.updateFooter(mLoadingState);
    }

    private void loadMore() {
        if (mLoadedTime < mSearchResultTotalPages) {
            Log.i(String.valueOf(mSearchResultTotalPages), String.valueOf(mLoadedTime));
            updateLoadingState(Constants.LOADING_IN_PROGRESS);
            loadPostListFromSearch(mKeyword, mDateRange, mLoadedTime++, mIsSortByDate);
        } else {
            Utils.showLongToast(getActivity(), R.string.no_more_posts_prompt);
            updateLoadingState(Constants.LOADING_FINISH);
        }
    }

//    private void reformatListStyle() {
//        int position = mLinearLayoutManager.findFirstVisibleItemPosition();
//        int offset = 0;
//        View firstChild = mLinearLayoutManager.getChildAt(0);
//        if (firstChild != null) {
//            offset = firstChild.getTop();
//        }
//        PostAdapter newAdapter = (PostAdapter) rvPostList.getAdapter();
//        rvPostList.setAdapter(newAdapter);
//        mLinearLayoutManager.scrollToPositionWithOffset(position, offset);
//    }

    @Override
    public void OnReachBottom() {
        if (mLoadingState == Constants.LOADING_IDLE) {
            loadMore();
        }
    }

//    @Override
//    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//        if (key.equals(SharedPrefsManager.KEY_POST_FONT)
//            || key.equals(SharedPrefsManager.KEY_POST_FONT_SIZE)
//            || key.equals(SharedPrefsManager.KEY_POST_LINE_HEIGHT)) {
//            mPostAdapter.updatePostPrefs();
//            reformatListStyle();
//        }
//    }
}
