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
import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class PostFragment extends Fragment implements PostAdapter.OnReachBottomListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    static final String KEY_POST_ID_LIST = "key_post_id_list";
    static final String KEY_LOADED_POSTS = "key_loaded_posts";
    static final String KEY_LOADED_TIME = "key_loaded_time";
    static final String KEY_LOADING_STATE = "key_loading_state";
    static final String KEY_LAST_TIME_POSITION = "key_last_time_position";
    static final String KEY_SEARCH_RESULT_TOTAL_PAGE = "key_search_result_total_page";
    static final String KEY_STORY_TYPE = "story_type";
    static final String KEY_STORY_TYPE_SPEC = "story_type_spec";

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ToolTipRelativeLayout tooltipLayout;
    private ToolTip toolTip;

    private PostAdapter.OnItemClickListener mOnItemClickListener;
    private OnRecyclerViewCreateListener mOnRecyclerViewCreateListener;

    private SharedPreferences prefs;
    private boolean showPostSummary;
    private String mStoryType, mStoryTypeSpec;
    private int loadedTime;
    private int mLoadingState = Constants.LOADING_IDLE;
    private int mLastTimeListPosition;
    private int mSearchResultTotalPages;
    private PostAdapter mPostAdapter;
    private List<Long> mPostIdList;
    private DataManager mDataManager;
    private CompositeSubscription mCompositeSubscription;

    // no meaningful effect
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

//        if (getArguments() != null) {
//            mStoryType = getArguments().getString(KEY_STORY_TYPE);
//            mStoryTypeSpec = getArguments().getString(KEY_STORY_TYPE_SPEC);
//        }
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
        mDataManager = new DataManager();
        mCompositeSubscription = new CompositeSubscription();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_post, container, false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_layout);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list_post);
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
                refresh(mStoryType, mStoryTypeSpec);
            }
        });

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
        showPostSummary = SharedPrefsManager.getShowPostSummary(prefs, getActivity());
        if (savedInstanceState != null) {
            mStoryType = savedInstanceState.getString(KEY_STORY_TYPE, Constants.TYPE_STORY);
            mStoryTypeSpec = savedInstanceState.getString(KEY_STORY_TYPE_SPEC, Constants.STORY_TYPE_TOP_PATH);
            if (mStoryType.equals(Constants.TYPE_SEARCH)) {
                ((MainActivity) getActivity()).
                        setUpSpinnerPopularDateRange(Integer.valueOf(mStoryTypeSpec.substring(0, 1)));
            }

            mPostIdList = Parcels.unwrap(savedInstanceState.getParcelable(KEY_POST_ID_LIST));
            if (mPostAdapter.getItemCount() == 0) {
                mPostAdapter.addAll((ArrayList<Post>) Parcels.unwrap(savedInstanceState.getParcelable(KEY_LOADED_POSTS)));
            }
            mLastTimeListPosition = savedInstanceState.getInt(KEY_LAST_TIME_POSITION, 0);
            mRecyclerView.getLayoutManager().scrollToPosition(mLastTimeListPosition);

            loadedTime = savedInstanceState.getInt(KEY_LOADED_TIME);
            mSearchResultTotalPages = savedInstanceState.getInt(KEY_SEARCH_RESULT_TOTAL_PAGE);
            mLoadingState = savedInstanceState.getInt(KEY_LOADING_STATE);
            if (mLoadingState == Constants.LOADING_IN_PROGRESS) {
                loadingMore();
            }
            //Log.d("postfragActivityCreated", mStoryType);
            //Log.d("postfragActivityCreated", mStoryTypeSpec);
        } else {
            mStoryType = Constants.TYPE_STORY;
            mStoryTypeSpec = Constants.STORY_TYPE_TOP_PATH;
            refresh(mStoryType, mStoryTypeSpec);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // invoked when showPostSummary setting changed
        if (!SharedPrefsManager.getShowPostSummary(prefs, getActivity()).equals(showPostSummary)) {
            showPostSummary = SharedPrefsManager.getShowPostSummary(prefs, getActivity());
            refresh(mStoryType, mStoryTypeSpec);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_POST_ID_LIST, Parcels.wrap(mPostIdList));
        outState.putParcelable(KEY_LOADED_POSTS, Parcels.wrap(mPostAdapter.getPostList()));
        outState.putInt(KEY_LOADED_TIME, loadedTime);
        outState.putInt(KEY_LOADING_STATE, mLoadingState);
        outState.putInt(KEY_SEARCH_RESULT_TOTAL_PAGE, mSearchResultTotalPages);
        outState.putString(KEY_STORY_TYPE, mStoryType);
        outState.putString(KEY_STORY_TYPE_SPEC, mStoryTypeSpec);
        mLastTimeListPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).
                findFirstVisibleItemPosition();
        outState.putInt(KEY_LAST_TIME_POSITION, mLastTimeListPosition);
        Log.d("postfrag saveState", mStoryType);
        Log.d("postfrag saveState", mStoryTypeSpec);
        Log.d("postfrag saveState", String.valueOf(mLastTimeListPosition));
    }

    @Override
    public void onDetach() {
        mOnItemClickListener = null;
        mOnRecyclerViewCreateListener = null;
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        if (mCompositeSubscription.hasSubscriptions()) {
            mCompositeSubscription.unsubscribe();
        }
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    void loadPostList(String storyTypeUrl) {
        mCompositeSubscription.add(mDataManager.getPostList(storyTypeUrl)
                .subscribeOn(Schedulers.io())
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
                        loadPostFromList(mPostIdList.subList(0, Constants.NUM_LOADING_ITEM));
                        mLoadingState = Constants.LOADING_IN_PROGRESS;
                    }
                }));
    }

    void loadPostListFromSearch(String timeRangeCombine, int page) {
        mCompositeSubscription.add(
                mDataManager.getPopularPosts("created_at_i>" + timeRangeCombine.substring(1, 11)
                        + "," + "created_at_i<" + timeRangeCombine.substring(11), page)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
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
                                mSearchResultTotalPages = searchResult.getNbPages();
                                for (int i = 0; i < searchResult.getHits().length; i++) {
                                    list.add(searchResult.getHits()[i].getObjectID());
                                }
                                loadPostFromList(list);
                                mLoadingState = Constants.LOADING_IN_PROGRESS;
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
                            post.setIndex(mPostAdapter.getItemCount() - 1);
                            // setup url and pretty url
                            String url = post.getUrl();
                            if (url == null || url.isEmpty()) {
                                url = Constants.YCOMBINATOR_ITEM_URL + post.getId();
                                post.setUrl(url);
                            }
                            String[] splitUrl = url.split("/");
                            if (splitUrl.length > 2) {
                                url = splitUrl[2];
                                post.setPrettyUrl(url);
                            }
                            mPostAdapter.add(post);
                            if (showPostSummary
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
            loadedTime = 1;
            mCompositeSubscription.clear();
            // Bug: SwipeRefreshLayout.setRefreshing(true); won't show at beginning
            // https://code.google.com/p/android/issues/detail?id=77712
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
            mPostAdapter.clear();
            mPostAdapter.notifyDataSetChanged();
            if (type.equals(Constants.TYPE_SEARCH)) {
                loadPostListFromSearch(spec, 0);
            } else if (type.equals(Constants.TYPE_STORY)) {
                loadPostList(spec);
            } else {
                Log.e("refresh", "type");
            }
        } else {
            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
            Toast.makeText(getActivity(), "No connection :(", Toast.LENGTH_LONG).show();
        }
    }

    public void refresh() {
        refresh(mStoryType, mStoryTypeSpec);
    }

    private void loadingMore() {
        if (mStoryType.equals(Constants.TYPE_STORY)
                && Constants.NUM_LOADING_ITEM * (loadedTime + 1) < mPostIdList.size()) {
            int start = Constants.NUM_LOADING_ITEM * loadedTime,
                    end = Constants.NUM_LOADING_ITEM * (++loadedTime);
            loadPostFromList(mPostIdList.subList(start, end));
            mLoadingState = Constants.LOADING_IN_PROGRESS;
            //Toast.makeText(getActivity(), "Loading more", Toast.LENGTH_SHORT).show();
            Log.i("loading", String.valueOf(start) + " - " + end);
        } else if (mStoryType.equals(Constants.TYPE_SEARCH)
                && loadedTime < mSearchResultTotalPages) {
            Log.i(String.valueOf(mSearchResultTotalPages), String.valueOf(loadedTime));
            loadPostListFromSearch(mStoryTypeSpec, loadedTime++);
            mLoadingState = Constants.LOADING_IN_PROGRESS;
        } else {
            Toast.makeText(getActivity(), "No more posts", Toast.LENGTH_SHORT).show();
            mLoadingState = Constants.LOADING_FINISH;
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
        mCompositeSubscription.add(mDataManager.getSummary(post.getKids())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
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

    public void setSwipeRefreshLayoutState(boolean isEnable) {
        mSwipeRefreshLayout.setEnabled(isEnable);
    }

    @Override
    public void OnReachBottom() {
        if (mLoadingState == Constants.LOADING_IDLE) {
            loadingMore();
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
