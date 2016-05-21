package com.leavjenn.hews.ui.post;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

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

public class PostPresenter extends BasePresenter {
    public static final String TAG = "PostPresenter";
    static final String KEY_POST_ID_LIST = "key_post_id_list";
    static final String KEY_LOADED_POSTS = "key_loaded_posts";
    static final String KEY_LOADED_TIME = "key_loaded_time";
    static final String KEY_LOADING_STATE = "key_loading_state";
    static final String KEY_SEARCH_RESULT_TOTAL_PAGE = "key_search_result_total_page";
    static final String KEY_STORY_TYPE = "story_type";
    static final String KEY_STORY_TYPE_SPEC = "story_type_spec";
    static final String KEY_LIST_STATE = "list_position";

    private PostView mPostView;
    private DataManager mDataManager;
    private SharedPrefsContract mPrefsManager;
    private UtilsContract mUtils;

    private CompositeSubscription mCompositeSubscription;
    private List<Long> mPostIdList;
    private ArrayList<Post> mCachedPostList;
    private int mLoadingState;
    private Observable<Post> mPostObservable;
    private String mStoryType;
    private String mStoryTypeSpec;
    private int mLoadedTime;
    private int mSearchResultTotalPages;
    private boolean mShowPostSummary;

    public PostPresenter() {
        mCompositeSubscription = new CompositeSubscription();
        mPostIdList = new ArrayList<>();
        mCachedPostList = new ArrayList<>();
    }

    public PostPresenter(@NonNull PostView postView) {
        mPostView = postView;
        mCompositeSubscription = new CompositeSubscription();
        mPostIdList = new ArrayList<>();
        mCachedPostList = new ArrayList<>();
    }

    public PostPresenter(@NonNull PostView postView, @NonNull DataManager dataManager,
                         @NonNull SharedPrefsContract prefsManager, @NonNull UtilsContract utils) {
        this(postView);
        mDataManager = dataManager;
        mPrefsManager = prefsManager;
        mCompositeSubscription = new CompositeSubscription();
        mUtils = utils;
    }

    public void setView(PostView postView) {
        mPostView = postView;
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

    public void setStoryType(String storyType) {
        mStoryType = storyType;
    }

    public void setStoryTypeSpec(String storyTypeSpec) {
        mStoryTypeSpec = storyTypeSpec;
    }

    @Override
    public void setup() {
        if (mStoryType == null || mStoryTypeSpec == null) {
            // rare condition:
            // not new instance, no saved instance state and not popped back stack
            Log.i("post setup", "null param");
            mStoryType = Constants.TYPE_STORY;
            mStoryTypeSpec = Constants.STORY_TYPE_TOP_PATH;
            refresh(mStoryType, mStoryTypeSpec);
            return;
        }
        if (mStoryTypeSpec.equals(Constants.TYPE_SEARCH)) {
            mPostView.showSpinnerPopularDateRange(Integer.valueOf(mStoryTypeSpec.substring(0, 1)));
        }

        if (mPostIdList.isEmpty()) {
            Log.i("post setup", "empty post id list");
            refresh(mStoryType, mStoryTypeSpec);
        } else {
            Log.i("post setup", "restore posts: " + getCachedPosts().size());
            mPostView.restoreCachedPosts(getCachedPosts());
            if (mCachedPostList.isEmpty() // post ID list is fetched, but post list is not yet
                || mLoadingState == Constants.LOADING_IN_PROGRESS) {
                Log.i("post setup", "reload");
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
        mStoryType = savedInstanceState.getString(KEY_STORY_TYPE, Constants.TYPE_STORY);
        mStoryTypeSpec = savedInstanceState.getString(KEY_STORY_TYPE_SPEC, Constants.STORY_TYPE_TOP_PATH);
        mPostIdList = Parcels.unwrap(savedInstanceState.getParcelable(KEY_POST_ID_LIST));
        mCachedPostList = Parcels.unwrap(savedInstanceState.getParcelable(KEY_LOADED_POSTS));
        mLoadedTime = savedInstanceState.getInt(KEY_LOADED_TIME);
        mSearchResultTotalPages = savedInstanceState.getInt(KEY_SEARCH_RESULT_TOTAL_PAGE);
        mLoadingState = savedInstanceState.getInt(KEY_LOADING_STATE);
    }

    @Override
    public void saveState(Bundle outState) {
        outState.putParcelable(KEY_POST_ID_LIST, Parcels.wrap(mPostIdList));
        outState.putParcelable(KEY_LOADED_POSTS, Parcels.wrap(mCachedPostList));
        outState.putInt(KEY_LOADED_TIME, mLoadedTime);
        outState.putInt(KEY_LOADING_STATE, mLoadingState);
        outState.putInt(KEY_SEARCH_RESULT_TOTAL_PAGE, mSearchResultTotalPages);
        outState.putString(KEY_STORY_TYPE, mStoryType);
        outState.putString(KEY_STORY_TYPE_SPEC, mStoryTypeSpec);
    }

    @Override
    public void destroy() {
//        if (mCompositeSubscription.hasSubscriptions()) {
        mCompositeSubscription.clear();
//        }
        mPostView.showInfoLog("destroy", mStoryType + " / " + mStoryTypeSpec);
        mPostView = null;
        mDataManager = null;
        mPrefsManager = null;
        mUtils = null;
    }

    @Override
    public void unsubscribe() {
        mCompositeSubscription.clear();
    }

    public void refresh() {
        refresh(mStoryType, mStoryTypeSpec);
    }

    public void refresh(@NonNull String type, @NonNull String spec) {
        if (!mUtils.isOnline()) {
            mPostView.hideSwipeRefresh();
            mPostView.showOfflineSnackBar();
            return;
        }
        mStoryType = type;
        mStoryTypeSpec = spec;
        mLoadedTime = 1;
        mPostIdList.clear();
        mCachedPostList.clear();
        mCompositeSubscription.clear();

        mPostView.showSwipeRefresh();
        mPostView.hideOfflineSnackBar();
        mPostView.resetAdapter();
        updateLoadingState(Constants.LOADING_IDLE);
        switch (mStoryType) {
            case Constants.TYPE_STORY:
                loadPostIdList(spec);
                break;
            case Constants.TYPE_SEARCH:
                loadPostIdListBySearch(spec, 0);
                break;
            default:
                mPostView.showErrorLog("refresh", "type");
                break;
        }
    }

    public void loadPostIdList(String storyTypeUrl) {
        mCompositeSubscription.add(mDataManager.getPostList(storyTypeUrl)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<Long>>() {
                @Override
                public void call(List<Long> longs) {
                    mPostIdList.addAll(longs);
                    loadPosts(mPostIdList.subList(0, Constants.NUM_LOADING_ITEMS), true);
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    mPostView.hideSwipeRefresh();
                    mPostView.showErrorLog("loadPostIdList", throwable.toString());
                }
            }));
    }

    public void loadPostIdListBySearch(String timeRangeCombine, int page) {
        mCompositeSubscription.add(
            mDataManager.getPopularPosts("created_at_i>" + timeRangeCombine.substring(1, 11)
                + "," + "created_at_i<" + timeRangeCombine.substring(11), page)
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
                        mPostIdList.clear();
                        mPostIdList.addAll(list);
                        loadPosts(list, true);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mPostView.hideSwipeRefresh();
                        mPostView.showErrorLog("loadPostIdListBySearch", throwable.toString());
                    }
                }));
    }

    public void loadPosts(List<Long> list, boolean updateObservable) {
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
                    mPostView.showInfoLog("loadPosts - completed", "cached: " + mCachedPostList.size());
                }

                @Override
                public void onError(Throwable throwable) {
                    mPostView.hideSwipeRefresh();
                    mPostView.showErrorLog("loadPosts", throwable.toString());
                }

                @Override
                public void onNext(Post post) {
                    mPostView.hideSwipeRefresh();
                    if (post != null) {
                        post.setIndex(mPostView.getLastPostIndex());
                        Utils.setupPostUrl(post);
                        post.setRead(mPrefsManager.isPostRead(post.getId()));
                        mPostView.showPost(post);
                        if (mLoadingState != Constants.LOADING_IN_PROGRESS) {
                            updateLoadingState(Constants.LOADING_IN_PROGRESS);
                        }
                        if (mShowPostSummary
                            && !mStoryTypeSpec.equals(Constants.STORY_TYPE_ASK_HN_PATH)
                            && !mStoryTypeSpec.equals(Constants.STORY_TYPE_SHOW_HN_PATH)
                            && post.getKids() != null) {
                            loadSummary(post);
                        }
                    }
                }
            }));
    }

    public void updateCachedPostList() {
        mCachedPostList.clear();
        mCachedPostList.addAll(mPostView.getAllPostList());
    }

    public List<Post> getCachedPosts() {
        return mCachedPostList;
    }

    public void updateLoadingState(int loadingState) {
        mLoadingState = loadingState;
        mPostView.updateListFooter(mLoadingState);
    }

    public void loadMore() {
        if (mLoadingState != Constants.LOADING_IDLE) {
            return;
        }
        if (mStoryType.equals(Constants.TYPE_STORY)
            && Constants.NUM_LOADING_ITEMS * (mLoadedTime + 1) < mPostIdList.size()) {
            int start = Constants.NUM_LOADING_ITEMS * mLoadedTime,
                end = Constants.NUM_LOADING_ITEMS * (++mLoadedTime);
            updateLoadingState(Constants.LOADING_IN_PROGRESS);
            loadPosts(mPostIdList.subList(start, end), true);
            mPostView.showInfoLog("loading story", String.valueOf(start) + " - " + end);
        } else if (mStoryType.equals(Constants.TYPE_SEARCH)
            && mLoadedTime < mSearchResultTotalPages) {
            mPostView.showInfoLog("loading pop",
                String.valueOf(mLoadedTime) + "/" + String.valueOf(mSearchResultTotalPages));
            updateLoadingState(Constants.LOADING_IN_PROGRESS);
            loadPostIdListBySearch(mStoryTypeSpec, mLoadedTime++);
        } else {
            mPostView.showLongToast(R.string.no_more_posts_prompt);
            updateLoadingState(Constants.LOADING_FINISH);
        }
    }

    public void reload() {
        if (mStoryType.equals(Constants.TYPE_STORY)
            && Constants.NUM_LOADING_ITEMS * mLoadedTime < mPostIdList.size()) {
            int start = Constants.NUM_LOADING_ITEMS * (mLoadedTime - 1),
                end = Constants.NUM_LOADING_ITEMS * mLoadedTime;
            loadPosts(mPostIdList.subList(start, end), false);
            mPostView.showInfoLog("reload story", String.valueOf(start) + "-" + end);
        } else if (mStoryType.equals(Constants.TYPE_SEARCH) && mLoadedTime < mSearchResultTotalPages) {
            mPostView.showInfoLog("reload pop",
                String.valueOf(mLoadedTime) + "/" + String.valueOf(mSearchResultTotalPages));
            loadPosts(mPostIdList, false);
        } else {
            mPostView.showLongToast(R.string.no_more_posts_prompt);
            updateLoadingState(Constants.LOADING_FINISH);
        }
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
                        mPostView.showSummary(post.getIndex());
                    } else {
                        post.setSummary(null);
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    mPostView.showErrorLog("loadSummary: " + String.valueOf(post.getId()),
                        throwable.toString());
                }
            }));
    }

    public void setShowPostSummaryPref() {
        if (mPrefsManager == null) {
            Log.e("postpresenter", "null mPrefsManager");
            mShowPostSummary = false;
        }
        mShowPostSummary = mPrefsManager.isShowPostSummary();
    }

    public void refreshPostSummaryPref() {
        if (mShowPostSummary != mPrefsManager.isShowPostSummary()) {
            refresh(mStoryType, mStoryTypeSpec);
        }
    }
}
