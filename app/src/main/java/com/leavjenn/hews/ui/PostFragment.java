package com.leavjenn.hews.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
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
import com.leavjenn.hews.listener.OnRecyclerViewCreateListener;
import com.leavjenn.hews.misc.SharedPrefsManager;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.network.DataManager;
import com.leavjenn.hews.ui.adapter.PostAdapter;
import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class PostFragment extends Fragment implements PostAdapter.OnReachBottomListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    static final String KEY_POST_ID_LIST = "key_post_id_list";
    static final String KEY_LOADED_POSTS = "key_loaded_posts";
    static final String KEY_LOADED_TIME = "key_loaded_time";
    static final String KEY_LOADING_STATE = "key_loading_state";
    static final String KEY_SEARCH_RESULT_TOTAL_PAGE = "key_search_result_total_page";
    static final String KEY_STORY_TYPE = "story_type";
    static final String KEY_STORY_TYPE_SPEC = "story_type_spec";

    private RelativeLayout layoutRoot;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private Snackbar snackbarNoConnection;
    private LinearLayoutManager mLinearLayoutManager;
    private ToolTipRelativeLayout tooltipLayout;
    private ToolTip toolTip;

    private PostAdapter.OnItemClickListener mOnItemClickListener;
    private OnRecyclerViewCreateListener mOnRecyclerViewCreateListener;

    private SharedPreferences prefs;
    private boolean mShowPostSummary;
    private String mStoryType, mStoryTypeSpec;
    private int mLoadedTime;
    private int mLoadingState = Constants.LOADING_IDLE;
    private int mSearchResultTotalPages;
    private PostAdapter mPostAdapter;
    private List<Long> mPostIdList;
    private DataManager mDataManager;
    private CompositeSubscription mCompositeSubscription;

    public static PostFragment newInstance(String storyType, String storyTypeSpec) {
        PostFragment fragment = new PostFragment();
        Bundle args = new Bundle();
        args.putString(KEY_STORY_TYPE, storyType);
        args.putString(KEY_STORY_TYPE_SPEC, storyTypeSpec);
        fragment.setArguments(args);
        return fragment;
    }

    public PostFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnItemClickListener = (PostAdapter.OnItemClickListener) activity;
            mOnRecyclerViewCreateListener = (OnRecyclerViewCreateListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement (PostAdapter.OnItemClickListener" +
                " && MainActivity.OnRecyclerViewCreateListener)");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mStoryType = getArguments().getString(KEY_STORY_TYPE);
            mStoryTypeSpec = getArguments().getString(KEY_STORY_TYPE_SPEC);
        }
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
        mDataManager = new DataManager();
        mCompositeSubscription = new CompositeSubscription();

        /**
         * when Fragment goes to back stack,
         * if Activity gets onSaveInstanceState, so does the back stack Fragment;
         * but when Activity gets restored, the back stack Fragment will not invoke
         * onCreateView() nor onActivityCreated(), so if Activity gets onSaveInstanceState again
         * and instance state of back stack Fragment not restored before,
         * some NPE might happened.
         * To avoid this, the instance state gets restore during onCreate(),
         * which will get called when Activity restoring.
         */

        mPostAdapter = new PostAdapter(this.getActivity(), this, mOnItemClickListener);
        if (savedInstanceState != null) {
            mStoryType = savedInstanceState.getString(KEY_STORY_TYPE, Constants.TYPE_STORY);
            mStoryTypeSpec = savedInstanceState.getString(KEY_STORY_TYPE_SPEC, Constants.STORY_TYPE_TOP_PATH);
            mPostIdList = Parcels.unwrap(savedInstanceState.getParcelable(KEY_POST_ID_LIST));
            if (mPostAdapter.getItemCount() == 0) {
                mPostAdapter.addFooter(new HNItem.Footer());
                mPostAdapter.addAllPosts((ArrayList<Post>) Parcels.unwrap(savedInstanceState.getParcelable(KEY_LOADED_POSTS)));
            }
            mLoadedTime = savedInstanceState.getInt(KEY_LOADED_TIME);
            mSearchResultTotalPages = savedInstanceState.getInt(KEY_SEARCH_RESULT_TOTAL_PAGE);
            mLoadingState = savedInstanceState.getInt(KEY_LOADING_STATE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_post, container, false);
        layoutRoot = (RelativeLayout) rootView.findViewById(R.id.layout_post_root);
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_layout);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list_post);

        if (SharedPrefsManager.getIsShowTooltip(prefs)) {
            tooltipLayout = (ToolTipRelativeLayout) rootView.findViewById(R.id.tooltip_layout_post);
            toolTip = new ToolTip()
                .withText(getString(R.string.tooltip_post_action))
                .withTextColor(getResources().getColor(android.R.color.white))
                .withColor(getResources().getColor(R.color.orange_600))
                .withAnimationType(ToolTip.AnimationType.FROM_MASTER_VIEW);
        }
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mOnRecyclerViewCreateListener.onRecyclerViewCreate(mRecyclerView);
        mRecyclerView.setAdapter(mPostAdapter);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.orange_600,
            R.color.orange_900, R.color.orange_900, R.color.orange_600);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh(mStoryType, mStoryTypeSpec);
            }
        });

        mShowPostSummary = SharedPrefsManager.getShowPostSummary(prefs, getActivity());
        if (savedInstanceState != null) {
            if (mStoryType.equals(Constants.TYPE_SEARCH)) {
                ((MainActivity) getActivity()).
                    setUpSpinnerPopularDateRange(Integer.valueOf(mStoryTypeSpec.substring(0, 1)));
            }
            if (mPostIdList == null) {
                refresh(mStoryType, mStoryTypeSpec);
            } else if (mLoadingState == Constants.LOADING_IN_PROGRESS) {
                loadMore();
            }
        } else {
            if (mStoryType == null || mStoryTypeSpec == null) {
                // rare condition:
                // not new instance, no saved instance state and not popped back stack
                mStoryType = Constants.TYPE_STORY;
                mStoryTypeSpec = Constants.STORY_TYPE_TOP_PATH;
            }
            refresh(mStoryType, mStoryTypeSpec);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // invoked when mShowPostSummary setting changed
        if (!SharedPrefsManager.getShowPostSummary(prefs, getActivity()).equals(mShowPostSummary)) {
            mShowPostSummary = SharedPrefsManager.getShowPostSummary(prefs, getActivity());
            refresh(mStoryType, mStoryTypeSpec);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_POST_ID_LIST, Parcels.wrap(mPostIdList));
        if (mPostAdapter != null) {
            outState.putParcelable(KEY_LOADED_POSTS, Parcels.wrap(mPostAdapter.getPostList()));
        }
        outState.putInt(KEY_LOADED_TIME, mLoadedTime);
        outState.putInt(KEY_LOADING_STATE, mLoadingState);
        outState.putInt(KEY_SEARCH_RESULT_TOTAL_PAGE, mSearchResultTotalPages);
        outState.putString(KEY_STORY_TYPE, mStoryType);
        outState.putString(KEY_STORY_TYPE_SPEC, mStoryTypeSpec);
        Log.i("postfrag saveState", mStoryType);
        Log.i("postfrag saveState", mStoryTypeSpec);
    }

    @Override
    public void onDestroy() {
        if (mCompositeSubscription.hasSubscriptions()) {
            mCompositeSubscription.unsubscribe();
        }
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        mOnItemClickListener = null;
        mOnRecyclerViewCreateListener = null;
        super.onDetach();
    }

    void loadPostList(String storyTypeUrl) {
        mCompositeSubscription.add(mDataManager.getPostList(storyTypeUrl)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<Long>>() {
                @Override
                public void call(List<Long> longs) {
                    mPostIdList = longs;
                    loadPostFromList(mPostIdList.subList(0, Constants.NUM_LOADING_ITEM));
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.e("loadPostList", throwable.toString());
                }
            }));
    }

    void loadPostListFromSearch(String timeRangeCombine, int page) {
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
                        loadPostFromList(list);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mSwipeRefreshLayout.setRefreshing(false);
                        Log.e("loadPostListFromSearch", throwable.toString());

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
                    updateLoadingState(Constants.LOADING_IDLE);
                }

                @Override
                public void onError(Throwable e) {
                    mSwipeRefreshLayout.setRefreshing(false);
                    Log.e("loadPostFromList", e.toString());
                }

                @Override
                public void onNext(Post post) {
                    if (mSwipeRefreshLayout.isRefreshing()) {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                    mRecyclerView.setVisibility(View.VISIBLE);
                    if (post != null) {
                        post.setIndex(mPostAdapter.getItemCount() - 1);
                        Utils.setupPostUrl(post);
                        mPostAdapter.addPost(post);
                        if (mLoadingState != Constants.LOADING_IN_PROGRESS) {
                            updateLoadingState(Constants.LOADING_IN_PROGRESS);
                        }
                        if (mShowPostSummary
                            && !mStoryTypeSpec.equals(Constants.STORY_TYPE_ASK_HN_PATH)
                            && !mStoryTypeSpec.equals(Constants.STORY_TYPE_SHOW_HN_PATH)
                            && post.getKids() != null) {
                            loadSummary(post);
                        }
                        if (SharedPrefsManager.getIsShowTooltip(prefs)
                            && mRecyclerView.getLayoutManager().getChildCount() > 4) {
                            final ToolTipView myToolTipView = tooltipLayout
                                .showToolTipForView(toolTip,
                                    mRecyclerView.getLayoutManager().getChildAt(3));
                            myToolTipView.setOnToolTipViewClickedListener(
                                new ToolTipView.OnToolTipViewClickedListener() {
                                    @Override
                                    public void onToolTipViewClicked(ToolTipView toolTipView) {
                                        myToolTipView.remove();
                                    }
                                });
                            SharedPrefsManager.setIsShowTooltip(prefs, false);
                        }
                    }
                }
            }));
    }

    public void refresh(String type, String spec) {
        mStoryType = type;
        mStoryTypeSpec = spec;
        if (Utils.isOnline(getActivity())) {
            mLoadedTime = 1;
            mCompositeSubscription.clear();
            // Bug: SwipeRefreshLayout.setRefreshing(true); won't show at beginning
            // https://code.google.com/p/android/issues/detail?id=77712
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
            if (snackbarNoConnection != null && snackbarNoConnection.isShown()) {
                snackbarNoConnection.dismiss();
            }
            mPostAdapter.clear();
            mPostAdapter.addFooter(new HNItem.Footer());
            updateLoadingState(Constants.LOADING_IDLE);
            switch (type) {
                case Constants.TYPE_SEARCH:
                    loadPostListFromSearch(spec, 0);
                    break;
                case Constants.TYPE_STORY:
                    loadPostList(spec);
                    break;
                default:
                    Log.e("refresh", "type");
                    break;
            }
        } else {
            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
            snackbarNoConnection = Snackbar.make(layoutRoot, R.string.no_connection_prompt,
                Snackbar.LENGTH_INDEFINITE);
            Utils.setSnackBarTextColor(snackbarNoConnection, getActivity(), android.R.color.white);
            snackbarNoConnection.setAction(R.string.snackebar_action_retry, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    refresh(mStoryType, mStoryTypeSpec);
                }
            });
            snackbarNoConnection.setActionTextColor(getResources().getColor(R.color.orange_600));
            snackbarNoConnection.show();
        }
    }

    public void refresh() {
        refresh(mStoryType, mStoryTypeSpec);
    }

    private void updateLoadingState(int loadingState) {
        mLoadingState = loadingState;
        mPostAdapter.updateFooter(mLoadingState);
    }

    private void loadMore() {
        if (mStoryType.equals(Constants.TYPE_STORY)
            && Constants.NUM_LOADING_ITEM * (mLoadedTime + 1) < mPostIdList.size()) {
            int start = Constants.NUM_LOADING_ITEM * mLoadedTime,
                end = Constants.NUM_LOADING_ITEM * (++mLoadedTime);
            updateLoadingState(Constants.LOADING_IN_PROGRESS);
            loadPostFromList(mPostIdList.subList(start, end));
            Log.i("loading", String.valueOf(start) + " - " + end);
        } else if (mStoryType.equals(Constants.TYPE_SEARCH)
            && mLoadedTime < mSearchResultTotalPages) {
            Log.i(String.valueOf(mSearchResultTotalPages), String.valueOf(mLoadedTime));
            updateLoadingState(Constants.LOADING_IN_PROGRESS);
            loadPostListFromSearch(mStoryTypeSpec, mLoadedTime++);
        } else {
            Utils.showLongToast(getActivity(), R.string.no_more_posts_prompt);
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
                        post.setSummary(Html.fromHtml(comment.getText()
                            .replace("<p>", "<br /><br />").replace("\n", "<br />")).toString());
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
            }));
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

    public void setSwipeRefreshLayoutState(boolean isEnabled) {
        mSwipeRefreshLayout.setEnabled(isEnabled);
    }

    @Override
    public void OnReachBottom() {
        if (mLoadingState == Constants.LOADING_IDLE) {
            loadMore();
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
    }
}
