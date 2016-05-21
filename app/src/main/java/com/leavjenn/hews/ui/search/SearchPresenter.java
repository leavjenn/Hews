package com.leavjenn.hews.ui.search;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.leavjenn.hews.Constants;
import com.leavjenn.hews.R;
import com.leavjenn.hews.misc.Utils;
import com.leavjenn.hews.ui.BasePresenter;
import com.leavjenn.hews.misc.SharedPrefsContract;
import com.leavjenn.hews.misc.UtilsContract;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.data.remote.DataManager;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class SearchPresenter extends BasePresenter {
    public static final String TAG = "SearchPresenter";
    private static final String KEY_KEYWORD = "key_keyword";
    private static final String KEY_TIME_RANGE = "key_time_range";
    private static final String KEY_SORT_METHOD = "key_sort_method";
    private static final String KEY_POST_ID_LIST = "key_post_id_list";
    private static final String KEY_LOADED_POSTS = "key_loaded_posts";
    private static final String KEY_LOADED_TIME = "key_loaded_time";
    private static final String KEY_LOADING_STATE = "key_loading_state";
    private static final String KEY_SEARCH_RESULT_TOTAL_PAGE = "key_search_result_total_page";

    private SearchView mSearchView;
    private DataManager mDataManager;
    private SharedPrefsContract mPrefsManager;
    private UtilsContract mUtils;

    private String mKeyword;
    private String mDateRange;
    private boolean mIsSortByDate;
    private List<Long> mPostIdList;
    private ArrayList<Post> mCachedPostList;
    private int mLoadingState;
    private Observable<Post> mPostObservable;
    private CompositeSubscription mCompositeSubscription;
    private int mLoadedTime;
    private int mSearchResultTotalPages;
    private boolean mShowPostSummary;

    public SearchPresenter(@NonNull SearchView searchView) {
        mSearchView = searchView;
        mCompositeSubscription = new CompositeSubscription();
        mPostIdList = new ArrayList<>();
        mCachedPostList = new ArrayList<>();
    }

    public SearchPresenter(@NonNull SearchView searchView, @NonNull DataManager dataManager,
                           @NonNull SharedPrefsContract prefsManager, @NonNull UtilsContract utils) {
        this(searchView);
        mDataManager = dataManager;
        mPrefsManager = prefsManager;
        mCompositeSubscription = new CompositeSubscription();
        mUtils = utils;
    }

    public void setView(SearchView searchView) {
        mSearchView = searchView;
    }

    public void setDataManager(DataManager dataManager) {
        mDataManager = dataManager;
    }

    public void setPrefsManager(SharedPrefsContract sharedPrefsContract) {
        mPrefsManager = sharedPrefsContract;
    }

    public void setUtils(UtilsContract utils) {
        mUtils = utils;
    }

    @Override
    public void setup() {
        if (mPostIdList.isEmpty()) {
            mSearchView.showInfoLog("search setup", "empty post id list");
            refresh();
        } else {
            mSearchView.showInfoLog("search setup", "restore posts: " + getCachedPosts().size());
            mSearchView.restoreCachedPosts(getCachedPosts());

            if (mCachedPostList.isEmpty() // post ID list is fetched, but post list is not yet
                || mLoadingState == Constants.LOADING_IN_PROGRESS) {
                mSearchView.showInfoLog("search setup", "reload");
                reload();
                updateLoadingState(Constants.LOADING_IN_PROGRESS);
            }
        }
    }

    @Override
    public void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        mKeyword = savedInstanceState.getString(KEY_KEYWORD);
        mDateRange = savedInstanceState.getString(KEY_TIME_RANGE);
        mIsSortByDate = savedInstanceState.getBoolean(KEY_SORT_METHOD);
        mPostIdList = Parcels.unwrap(savedInstanceState.getParcelable(KEY_POST_ID_LIST));
        mCachedPostList = Parcels.unwrap(savedInstanceState.getParcelable(KEY_LOADED_POSTS));
        mSearchResultTotalPages = savedInstanceState.getInt(KEY_SEARCH_RESULT_TOTAL_PAGE);
        mLoadedTime = savedInstanceState.getInt(KEY_LOADED_TIME);
        mLoadingState = savedInstanceState.getInt(KEY_LOADING_STATE);
    }

    @Override
    public void saveState(Bundle outState) {
        outState.putString(KEY_KEYWORD, mKeyword);
        outState.putString(KEY_TIME_RANGE, mDateRange);
        outState.putBoolean(KEY_SORT_METHOD, mIsSortByDate);
        outState.putParcelable(KEY_LOADED_POSTS, Parcels.wrap(mCachedPostList));
        outState.putParcelable(KEY_POST_ID_LIST, Parcels.wrap(mPostIdList));
        outState.putInt(KEY_SEARCH_RESULT_TOTAL_PAGE, mSearchResultTotalPages);
        outState.putInt(KEY_LOADED_TIME, mLoadedTime);
        outState.putInt(KEY_LOADING_STATE, mLoadingState);
    }

    @Override
    public void destroy() {
//        if (mCompositeSubscription.hasSubscriptions()) {
        mCompositeSubscription.clear();
//        }
        mSearchView.showInfoLog(TAG, "destroy");
        mSearchView = null;
        mDataManager = null;
        mPrefsManager = null;
        mUtils = null;
    }

    @Override
    public void unsubscribe() {
        mCompositeSubscription.clear();
    }

    public void refresh() {
        if (mKeyword != null && mDateRange != null) {
            refresh(mKeyword, mDateRange, mIsSortByDate);
        } else {
            mSearchView.hideSwipeRefresh();
            //TODO show prompt
        }
    }

    public void refresh(String dateRange) {
        refresh(mKeyword, dateRange, mIsSortByDate);
    }

    public void refresh(boolean isSortByDate) {
        refresh(mKeyword, mDateRange, isSortByDate);
    }

    public void refresh(String keyword, String dateRange, boolean isSortByDate) {
        if (!mUtils.isOnline()) {
            mSearchView.hideSwipeRefresh();
            mSearchView.showOfflineSnackBar();
            return;
        }
        mKeyword = keyword;
        mDateRange = dateRange;
        mIsSortByDate = isSortByDate;
        mLoadedTime = 1;
        mPostIdList.clear();
        mCachedPostList.clear();
        mCompositeSubscription.clear();

        mSearchView.showSwipeRefresh();
        mSearchView.hideOfflineSnackBar();
        mSearchView.resetAdapter();
        updateLoadingState(Constants.LOADING_IDLE);
        loadPostIdListBySearch(keyword, dateRange, 0, isSortByDate);
    }

    public void loadPostIdListBySearch(String keyword, String dateRange, int page, boolean isSortByDate) {
        mCompositeSubscription.add(
            mDataManager.getSearchResult(keyword, "created_at_i>" + dateRange.substring(0, 10)
                + "," + "created_at_i<" + dateRange.substring(10), page, isSortByDate)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<HNItem.SearchResult>() {
                    @Override
                    public void call(HNItem.SearchResult searchResult) {
                        if (searchResult.getHits().length == 0) {
                            mSearchView.hideSwipeRefresh();
                            mSearchView.updatePrompt(R.string.no_search_result_prompt);
                            updateLoadingState(Constants.LOADING_PROMPT_NO_CONTENT);
                            return;
                        }
                        List<Long> list = new ArrayList<>();
                        mSearchResultTotalPages = searchResult.getNbPages();
                        for (int i = 0; i < searchResult.getHits().length; i++) {
                            list.add(searchResult.getHits()[i].getObjectID());
                        }
                        mPostIdList.clear();
                        mPostIdList.addAll(list);
                        loadPosts(list, true);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mSearchView.hideSwipeRefresh();
                        mSearchView.showErrorLog("loadPostIdListBySearch", throwable.toString());
                    }
                }));
    }

    void loadPosts(List<Long> list, boolean updateObservable) {
        if (updateObservable || mPostObservable == null) {
            mPostObservable = mDataManager.getPosts(list).cache();
        }
        mCompositeSubscription.add(mPostObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Post>() {
                @Override
                public void onCompleted() {
                    updateLoadingState(Constants.LOADING_IDLE);
                    updateCachedPostList();
                    mSearchView.showInfoLog("loadPosts - completed", "cached: " + mCachedPostList.size());
                }

                @Override
                public void onError(Throwable throwable) {
                    mSearchView.hideSwipeRefresh();
                    updateLoadingState(Constants.LOADING_ERROR);
                    mSearchView.showErrorLog("loadPosts", throwable.toString());
                }

                @Override
                public void onNext(Post post) {
                    mSearchView.hideSwipeRefresh();
                    if (post != null) {
                        post.setIndex(mSearchView.getLastPostIndex());
                        Utils.setupPostUrl(post);
                        post.setRead(mPrefsManager.isPostRead(post.getId()));
                        mSearchView.showPost(post);
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
                        post.setSummary(mUtils.convertHtmlToString(
                            comment.getText().replace("<p>", "<br /><br />").replace("\n", "<br />")));
                        mSearchView.showSummary(post.getIndex());
                    } else {
                        post.setSummary(null);
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    mSearchView.showErrorLog("loadSummary: " + String.valueOf(post.getId()),
                        throwable.toString());
                }
            })
        );
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

    public boolean isSortByDate() {
        return mIsSortByDate;
    }

    public void setSortByDate(boolean isSortByDate) {
        mIsSortByDate = isSortByDate;
    }

    private void updateLoadingState(int loadingState) {
        mLoadingState = loadingState;
        mSearchView.updateListFooter(mLoadingState);
    }

    public void updateCachedPostList() {
        mCachedPostList.clear();
        mCachedPostList.addAll(mSearchView.getAllPostList());
    }

    public List<Post> getCachedPosts() {
        return mCachedPostList;
    }

    public void loadMore() {
        if (mLoadingState != Constants.LOADING_IDLE) {
            return;
        }
        if (mLoadedTime < mSearchResultTotalPages) {
            mSearchView.showInfoLog("loadMore",
                String.valueOf(mSearchResultTotalPages) + "/" + String.valueOf(mLoadedTime));
            updateLoadingState(Constants.LOADING_IN_PROGRESS);
            loadPostIdListBySearch(mKeyword, mDateRange, mLoadedTime++, mIsSortByDate);
        } else {
            mSearchView.showLongToast(R.string.no_more_posts_prompt);
            updateLoadingState(Constants.LOADING_FINISH);
        }
    }

    private void reload() {
        if (mLoadedTime < mSearchResultTotalPages) {
            mSearchView.showInfoLog("reload search",
                String.valueOf(mLoadedTime) + "/" + String.valueOf(mSearchResultTotalPages));
            loadPosts(mPostIdList, false);
        } else {
            mSearchView.showLongToast(R.string.no_more_posts_prompt);
            updateLoadingState(Constants.LOADING_FINISH);
        }
    }

    public void setShowPostSummaryPref() {
        mShowPostSummary = mPrefsManager.isShowPostSummary();
    }
}
