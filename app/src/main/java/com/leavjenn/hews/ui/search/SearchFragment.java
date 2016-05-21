package com.leavjenn.hews.ui.search;

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
import com.leavjenn.hews.ui.adapter.PostAdapter;

import java.util.List;

public class SearchFragment extends BasePostListFragment implements SearchView {
    public static final String TAG = "SearchFragment";
    private static final String KEY_LIST_STATE = "key_list_state";

    private RelativeLayout layoutRoot;
    private Snackbar snackbarNoConnection;
    // swipeRefreshLayout, rvPostList, mLinearLayoutManager and mPostAdapter are initiated in parent.

    // mPrefsManager, mDataManager and mUtils are initiated in parent.
    private SearchPresenter mSearchPresenter;

    public SearchFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setRetainInstance(true);
         mSearchPresenter = new SearchPresenter(this, mDataManager, mPrefsManager, mUtils);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search, container, false);
        layoutRoot = (RelativeLayout) rootView.findViewById(R.id.layout_search_root);
        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_layout);
        rvPostList = (RecyclerView) rootView.findViewById(R.id.list_search);

        mPostAdapter = new PostAdapter(getActivity(), this, mOnItemClickListener);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        rvPostList.setLayoutManager(mLinearLayoutManager);
        rvPostList.setAdapter(mPostAdapter);
        mOnRecyclerViewCreatedListener.onRecyclerViewCreated(rvPostList);
        swipeRefreshLayout.setColorSchemeResources(R.color.orange_600,
            R.color.orange_900, R.color.orange_900, R.color.orange_600);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSearchPresenter.refresh();
            }
        });
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i(TAG, "onActivityCreated");
        mSearchPresenter.restoreState(savedInstanceState);
        mSearchPresenter.setView(this);
        mSearchPresenter.setDataManager(mDataManager);
        mSearchPresenter.setPrefsManager(mPrefsManager);
        mSearchPresenter.setUtils(mUtils);
        mSearchPresenter.setShowPostSummaryPref();
        mSearchPresenter.setup();
        if (savedInstanceState != null) {
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
        mSearchPresenter.saveState(outState);
        if (rvPostList != null) { // if PostFragment is in back stack, rvPostList will not be created
            outState.putParcelable(KEY_LIST_STATE, rvPostList.getLayoutManager().onSaveInstanceState());
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        mSearchPresenter.destroy();
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        Log.i(TAG, "onDetach");
        mSearchPresenter.unsubscribe();
        super.onDetach();
    }

    @Override
    public void OnReachBottom() {
        mSearchPresenter.loadMore();
    }

    public SearchPresenter getPresenter() {
        return mSearchPresenter;
    }

    /* Override SearchView */
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
                mSearchPresenter.refresh();
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
    public void updatePrompt(@StringRes int resPrompt) {
        mPostAdapter.updatePrompt(resPrompt);
//        mPostAdapter.notifyDataSetChanged();
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
