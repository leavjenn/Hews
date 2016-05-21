package com.leavjenn.hews.ui.comment;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
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
import android.widget.TextView;
import android.widget.Toast;

import com.leavjenn.hews.R;
import com.leavjenn.hews.misc.Utils;
import com.leavjenn.hews.data.local.LocalDataManager;
import com.leavjenn.hews.listener.OnRecyclerViewCreatedListener;
import com.leavjenn.hews.misc.SharedPrefsContract;
import com.leavjenn.hews.misc.SharedPrefsManager;
import com.leavjenn.hews.misc.UtilsContract;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.data.remote.DataManager;
import com.leavjenn.hews.ui.adapter.CommentAdapter;

import org.parceler.Parcels;

import java.util.List;

public class CommentsFragment extends Fragment
    implements CommentView, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String KEY_LIST_STATE = "key_list_state";

    private RelativeLayout layoutRoot;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvCommentList;
    private Snackbar snackbarNoConnection;
    private LinearLayoutManager mLinearLayoutManager;
    private CommentAdapter mCommentAdapter;

    private float mFontSize, mLineHeight;

    private CommentPresenter mCommentPresenter;
    private SharedPreferences prefs;
    private DataManager mDataManager;
    private LocalDataManager mLocalDataManager;
    private SharedPrefsContract mSharedPrefs;
    private UtilsContract mUtils;

    private OnRecyclerViewCreatedListener mOnRecyclerViewCreatedListener;

    public CommentsFragment() {
    }

    public static CommentsFragment newInstance(Parcelable postParcel, boolean isBookmarked) {
        CommentsFragment fragment = new CommentsFragment();
        Bundle args = new Bundle();
        args.putParcelable(CommentPresenter.KEY_POST_PARCEL, postParcel);
        args.putBoolean(CommentPresenter.KEY_IS_BOOKMARKED, isBookmarked);
        fragment.setArguments(args);
        return fragment;
    }

    public static CommentsFragment newInstance(Long postId) {
        CommentsFragment fragment = new CommentsFragment();
        Bundle args = new Bundle();
        args.putLong(CommentPresenter.KEY_POST_ID, postId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnRecyclerViewCreatedListener = (OnRecyclerViewCreatedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement (MainActivity.OnRecyclerViewCreatedListener)");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mDataManager = new DataManager();
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
        mLocalDataManager = new LocalDataManager(getActivity());
        mSharedPrefs = new SharedPrefsManager(getActivity(), prefs);
        mUtils = new Utils(getActivity());
        mCommentPresenter = new CommentPresenter(this, mDataManager, mLocalDataManager,
            mSharedPrefs, mUtils);
        if (getArguments() != null) {
            if (getArguments().containsKey(CommentPresenter.KEY_POST_PARCEL)) {
                Post post = Parcels.unwrap(getArguments().getParcelable(CommentPresenter.KEY_POST_PARCEL));
                mCommentPresenter.setPost(post);
                mCommentPresenter.setPostId(post.getId());
                mCommentPresenter.setBookmarkState(getArguments().getBoolean(CommentPresenter.KEY_IS_BOOKMARKED));
            } else if (getArguments().containsKey(CommentPresenter.KEY_POST_ID)) {
                mCommentPresenter.setPostId(getArguments().getLong(CommentPresenter.KEY_POST_ID));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_comments, container, false);
        layoutRoot = (RelativeLayout) rootView.findViewById(R.id.layout_fragment_comment_root);
        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_layout);
        rvCommentList = (RecyclerView) rootView.findViewById(R.id.comment_list);
        mCommentAdapter = new CommentAdapter(getActivity(), rvCommentList);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        rvCommentList.setLayoutManager(mLinearLayoutManager);
        rvCommentList.setAdapter(mCommentAdapter);
        swipeRefreshLayout.setColorSchemeResources(R.color.orange_600,
            R.color.orange_900, R.color.orange_900, R.color.orange_600);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mCommentPresenter.refresh();
            }
        });
        mOnRecyclerViewCreatedListener.onRecyclerViewCreated(rvCommentList);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCommentPresenter.restoreState(savedInstanceState);
        mCommentPresenter.setView(this);
        mCommentPresenter.setDataManager(mDataManager);
        mCommentPresenter.setLocalDataManager(mLocalDataManager);
        mCommentPresenter.setPrefsManager(mSharedPrefs);
        mCommentPresenter.setUtils(mUtils);
        if (SharedPrefsManager.getScrollMode(prefs).equals(SharedPrefsManager.SCROLL_MODE_BUTTON)) {
            mFontSize = Utils.convertSpToPixels(SharedPrefsManager.getCommentFontSize(prefs), getActivity());
            mLineHeight = Utils.convertSpToPixels(SharedPrefsManager.getCommentLineHeight(prefs), getActivity());
        }
        mCommentPresenter.setup();
        if (savedInstanceState != null) {
            // restore list position
            final Parcelable listState = savedInstanceState.getParcelable(KEY_LIST_STATE);
            if (listState != null) {
                rvCommentList.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        rvCommentList.getLayoutManager().onRestoreInstanceState(listState);
                    }
                }, 300);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mCommentPresenter.saveState(outState);
        outState.putParcelable(KEY_LIST_STATE, rvCommentList.getLayoutManager().onSaveInstanceState());
    }

    @Override
    public void onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        prefs = null;
        mSharedPrefs = null;
        mLocalDataManager = null;
        mCommentAdapter = null;
        mLinearLayoutManager = null;
        mOnRecyclerViewCreatedListener = null;
        mCommentPresenter.destroy();
        super.onDestroy();
    }

    public void scrollUp(int appBarCurrentHeight) {
        rvCommentList.smoothScrollBy(0,
            (int) -(rvCommentList.getHeight() - appBarCurrentHeight - mFontSize - mLineHeight));
    }

    public void scrollDown(int appBarCurrentHeight) {
        rvCommentList.smoothScrollBy(0,
            (int) (rvCommentList.getHeight() - appBarCurrentHeight - mFontSize - mLineHeight));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SharedPrefsManager.KEY_COMMENT_FONT_SIZE)
            || key.equals(SharedPrefsManager.KEY_COMMENT_LINE_HEIGHT)
            || key.equals(SharedPrefsManager.KEY_COMMENT_FONT)) {
            mCommentAdapter.updateCommentPrefs();
            reformatListStyle();
            if (SharedPrefsManager.getScrollMode(prefs).equals(SharedPrefsManager.SCROLL_MODE_BUTTON)) {
                mFontSize = Utils.convertSpToPixels(SharedPrefsManager.getCommentFontSize(prefs), getActivity());
                mLineHeight = Utils.convertSpToPixels(SharedPrefsManager.getCommentLineHeight(prefs), getActivity());
            }
        }
    }

    private void reformatListStyle() {
        if (mLinearLayoutManager != null) {
            int position = mLinearLayoutManager.findFirstVisibleItemPosition();
            int offset = 0;
            View firstChild = mLinearLayoutManager.getChildAt(0);
            if (firstChild != null) {
                offset = firstChild.getTop();
            }
            CommentAdapter newAdapter = (CommentAdapter) rvCommentList.getAdapter();
            rvCommentList.setAdapter(newAdapter);
            mLinearLayoutManager.scrollToPositionWithOffset(position, offset);
        }
    }

    public void setSwipeRefreshLayoutState(boolean isEnabled) {
        swipeRefreshLayout.setEnabled(isEnabled);
    }

    public CommentPresenter getPresenter() {
        return mCommentPresenter;
    }

    /* Override CommentView */
    @Override
    public void hideSwipeRefresh() {
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
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
    public void showOfflineSnackBar() {
        snackbarNoConnection = Snackbar.make(layoutRoot, R.string.no_connection_prompt,
            Snackbar.LENGTH_INDEFINITE);
        Utils.setSnackBarTextColor(snackbarNoConnection, getActivity(), android.R.color.white);
        snackbarNoConnection.setAction(R.string.snackebar_action_retry, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCommentPresenter.refresh();
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
    public void showOfflineSnackBarForShowComments(final Post post, final boolean updateObservable) {
        snackbarNoConnection = Snackbar.make(layoutRoot, R.string.no_connection_prompt,
            Snackbar.LENGTH_INDEFINITE);
        Utils.setSnackBarTextColor(snackbarNoConnection, getActivity(), android.R.color.white);
        snackbarNoConnection.setAction(R.string.snackebar_action_retry, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCommentPresenter.getComments(post, updateObservable);
            }
        });
        snackbarNoConnection.setActionTextColor(getResources().getColor(R.color.orange_800));
        snackbarNoConnection.show();
    }

    @Override
    public void showBookmarkSuccessSnackBar() {
        Snackbar snackbarSucceed = Snackbar.make(layoutRoot, "Post saved!",
            Snackbar.LENGTH_LONG);
        TextView tvSnackbarText = (TextView) snackbarSucceed.getView()
            .findViewById(android.support.design.R.id.snackbar_text);
        tvSnackbarText.setTextColor(getResources().getColor(R.color.orange_600));
        snackbarSucceed.show();
    }

    @Override
    public void showUnbookmarkSuccessSnackBar() {
        Snackbar snackbarSucceed = Snackbar.make(layoutRoot, "Unbookmark succeed!",
            Snackbar.LENGTH_LONG);
        TextView tvSnackbarText = (TextView) snackbarSucceed.getView()
            .findViewById(android.support.design.R.id.snackbar_text);
        tvSnackbarText.setTextColor(getResources().getColor(R.color.orange_600));
        snackbarSucceed.show();
    }

    @Override
    public void showHeader(Post post) {
        mCommentAdapter.addHeader(post);
    }

    @Override
    public void showComments(List<Comment> commentList) {
        mCommentAdapter.addAllComments(commentList);
    }

    @Override
    public void clearAdapter() {
        mCommentAdapter.clear();
        mCommentAdapter.notifyDataSetChanged();
    }

    @Override
    public void showFooter() {
        mCommentAdapter.addFooter(new HNItem.Footer());
    }

    @Override
    public void updateListFooter(int loadingState) {
        mCommentAdapter.updateFooter(loadingState);
    }

    @Override
    public List<Comment> getAllComments() {
        return mCommentAdapter.getCommentList();
    }

    @Override
    public int getCommentsCount() {
        return mCommentAdapter.getCommentList().size();
    }

    @Override
    public Comment getComment(int index) {
        return mCommentAdapter.getCommentList().get(index);
    }

    @Override
    public void restoreCachedComments(List<Comment> commentList) {
        mCommentAdapter.addAllComments(commentList);
    }

    @Override
    public void setToolbarUrl(String url) {
        ((CommentsActivity) getActivity()).setUrl(url);
    }

    @Override
    public void showLongToast(@StringRes int stringId) {
        Toast.makeText(getActivity(), stringId, Toast.LENGTH_LONG).show();
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
