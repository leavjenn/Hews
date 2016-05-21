package com.leavjenn.hews.ui.post;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.leavjenn.hews.R;
import com.leavjenn.hews.misc.Utils;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.ui.BasePostListFragment;
import com.leavjenn.hews.ui.MainActivity;
import com.leavjenn.hews.ui.adapter.PostAdapter;

import java.util.List;

public class PostFragment extends BasePostListFragment implements PostView {
    public static final String TAG = "PostFragment";
    static final String KEY_LIST_STATE = "list_position";

    private RelativeLayout layoutRoot;
    private Snackbar snackbarNoConnection;
    // swipeRefreshLayout, rvPostList, mLinearLayoutManager and mPostAdapter are initiated in parent.

    // mPrefsManager, mDataManager and mUtils are initiated in parent.
    private PostPresenter mPostPresenter;

    public static PostFragment newInstance(String storyType, String storyTypeSpec) {
        PostFragment fragment = new PostFragment();
        Bundle args = new Bundle();
        args.putString(PostPresenter.KEY_STORY_TYPE, storyType);
        args.putString(PostPresenter.KEY_STORY_TYPE_SPEC, storyTypeSpec);
        fragment.setArguments(args);
        return fragment;
    }

    public PostFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setRetainInstance(true);
        mPostPresenter = new PostPresenter(this, mDataManager, mPrefsManager, mUtils);

        if (getArguments() != null) {
            mPostPresenter.setStoryType(getArguments().getString(PostPresenter.KEY_STORY_TYPE));
            mPostPresenter.setStoryTypeSpec(getArguments().getString(PostPresenter.KEY_STORY_TYPE_SPEC));
        }
        /**
         * after Fragment is added to back stack,
         * if Activity invokes onSaveInstanceState(), so does the back stack Fragment;
         * But when Activity restored, the back stack Fragment will not invoke
         * onCreateView() nor onActivityCreated(), so if Activity invokes onSaveInstanceState() again
         * and instance state of back stack Fragment not restored before,
         * the NPE might happened.
         * To avoid this, the instance state should be restored during onCreate(),
         * which will get called when Activity is restoring.
         */
//        if (savedInstanceState != null) {
//            mStoryType = savedInstanceState.getString(KEY_STORY_TYPE, Constants.TYPE_STORY);
//            mStoryTypeSpec = savedInstanceState.getString(KEY_STORY_TYPE_SPEC, Constants.STORY_TYPE_TOP_PATH);
//            mPostIdList = Parcels.unwrap(savedInstanceState.getParcelable(KEY_POST_ID_LIST));
//            mCachedPostList = Parcels.unwrap(savedInstanceState.getParcelable(KEY_LOADED_POSTS));
//            mLoadedTime = savedInstanceState.getInt(KEY_LOADED_TIME);
//            mSearchResultTotalPages = savedInstanceState.getInt(KEY_SEARCH_RESULT_TOTAL_PAGE);
//            mLoadingState = savedInstanceState.getInt(KEY_LOADING_STATE);
//            mListState = savedInstanceState.getParcelable(KEY_LIST_STATE);
//        }
        mPostPresenter.restoreState(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_post, container, false);
        layoutRoot = (RelativeLayout) rootView.findViewById(R.id.layout_post_root);
        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_layout);
        rvPostList = (RecyclerView) rootView.findViewById(R.id.list_post);

        mPostAdapter = new PostAdapter(getActivity(), this, mOnItemClickListener);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        rvPostList.setLayoutManager(mLinearLayoutManager);
        rvPostList.setAdapter(mPostAdapter);
        mOnRecyclerViewCreatedListener.onRecyclerViewCreated(rvPostList);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPostPresenter.refresh();
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.orange_600,
            R.color.orange_900, R.color.orange_900, R.color.orange_600);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i(TAG, "onActivityCreated");
        mPostPresenter.restoreState(savedInstanceState);
        mPostPresenter.setView(this);
        mPostPresenter.setDataManager(mDataManager);
        mPostPresenter.setPrefsManager(mPrefsManager);
        mPostPresenter.setUtils(mUtils);
        mPostPresenter.setShowPostSummaryPref();
        mPostPresenter.setup();
        if (savedInstanceState != null) {
            Log.i("post frag act created", "has state");
            // view state
            final Parcelable listState = savedInstanceState.getParcelable(KEY_LIST_STATE);
            if (listState != null) {
                rvPostList.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        rvPostList.getLayoutManager().onRestoreInstanceState(listState);
                    }
                }, 300);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // invoked when mShowPostSummary setting changed
        mPostPresenter.refreshPostSummaryPref();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
        mPostPresenter.saveState(outState);
        if (rvPostList != null) { // if PostFragment is in back stack, rvPostList will not be created
            outState.putParcelable(KEY_LIST_STATE, rvPostList.getLayoutManager().onSaveInstanceState());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        forceHideSwipeRefresh();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        mPostPresenter.destroy();
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        Log.i(TAG, "onDetach");
        mPostPresenter.unsubscribe();
        super.onDetach();
    }

    @Override
    public void refresh() {
        mPostPresenter.refresh();
    }

    @Override
    public void OnReachBottom() {
        mPostPresenter.loadMore();
    }

    public PostPresenter getPresenter() {
        return mPostPresenter;
    }


    /* Override PostView */
    @Override
    public void showPost(Post post) {
        mPostAdapter.addPost(post);
    }

    @Override
    public void restoreCachedPosts(List<Post> postList) {
        resetAdapter();
        mPostAdapter.addAllPosts(postList);
    }

    @Override
    public void showSummary(int postPosition) {
        mPostAdapter.notifyItemChanged(postPosition);
    }

    @Override
    public void showSwipeRefresh() {
        // Bug: SwipeRefreshLayout.setRefreshing(true); won't show at beginning
        // https://code.google.com/p/android/issues/detail?id=77712
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(true);
            }
        });
    }

    @Override
    public void hideSwipeRefresh() {
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void forceHideSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
            swipeRefreshLayout.destroyDrawingCache();
            swipeRefreshLayout.clearAnimation();
        }
    }

    @Override
    public void showOfflineSnackBar() {
        snackbarNoConnection = Snackbar.make(layoutRoot, R.string.no_connection_prompt,
            Snackbar.LENGTH_INDEFINITE);
        Utils.setSnackBarTextColor(snackbarNoConnection, getActivity(), android.R.color.white);
        snackbarNoConnection.setAction(R.string.snackebar_action_retry, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPostPresenter.refresh();
            }
        });
        snackbarNoConnection.setActionTextColor(getResources().getColor(R.color.orange_600));
        snackbarNoConnection.show();
    }

    @Override
    public void hideOfflineSnackBar() {
        if (snackbarNoConnection != null && snackbarNoConnection.isShown()) {
            snackbarNoConnection.dismiss();
        }
    }

    @Override
    public void showSpinnerPopularDateRange(int selection) {
        ((MainActivity) getActivity()).setUpSpinnerPopularDateRange(selection);
    }

    @Override
    public void showLongToast(@StringRes int stringId) {
        Utils.showLongToast(getActivity(), stringId);
    }

    @Override
    public void resetAdapter() {
        mPostAdapter.clearAndAddFooter();
    }

    @Override
    public void updateListFooter(int loadingState) {
        mPostAdapter.updateFooter(loadingState);
    }

    @Override
    public List<Post> getAllPostList() {
        return mPostAdapter.getPostList();
    }

    @Override
    public int getLastPostIndex() {
        return mPostAdapter.getItemCount() - 1;
    }

    @Override
    public void showInfoLog(String tag, String msg) {
        Log.i(tag, msg);
    }

    @Override
    public void showErrorLog(String tag, String msg) {
        Log.e(tag, msg);
    }
}
