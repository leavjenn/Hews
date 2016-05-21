package com.leavjenn.hews.ui.comment;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.client.Firebase;
import com.leavjenn.hews.Constants;
import com.leavjenn.hews.R;
import com.leavjenn.hews.misc.Utils;
import com.leavjenn.hews.listener.OnRecyclerViewCreatedListener;
import com.leavjenn.hews.misc.ChromeCustomTabsHelper;
import com.leavjenn.hews.misc.SharedPrefsManager;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.data.remote.DataManager;
import com.leavjenn.hews.ui.widget.CommentOptionDialogFragment;
import com.leavjenn.hews.ui.widget.FloatingScrollDownButton;
import com.leavjenn.hews.ui.widget.LoginDialogFragment;
import com.leavjenn.hews.ui.widget.PopupFloatingWindow;

import org.parceler.Parcels;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class CommentsActivity extends AppCompatActivity implements OnRecyclerViewCreatedListener,
    AppBarLayout.OnOffsetChangedListener {
    private static final String FRAGMENT_TAG_SELECT_COMMENT_DIALOG = "select_comment_dialog_fragment";
    private static final String FRAGMENT_TAG_LOGIN_DIALOG = "login_dialog_fragment";
    private static final String STATE_SELECTED_COMMENT = "selected_comment";
    private static final String STATE_IS_IN_REPLAY_MODE = "is_in_reply_mode";
    private static final String STATE_REPLY_ITEM_ID = "reply_item_id";
    private static final String STATE_POST_ID = "post_id";
    private static final String STATE_URL = "url";

    private PopupFloatingWindow mWindow;
    private FloatingScrollDownButton mFab;
    private CoordinatorLayout coordinatorLayout;
    private AppBarLayout appbar;
    private LinearLayout layoutReply;
    private EditText etReply;
    private FloatingActionButton btnReplySend;
    private CommentOptionDialogFragment dialogSelectComment;
    private LoginDialogFragment dialogLogin;

    private int mAppBarOffset;
    private long mPostId;
    private long mReplyItemId;
    private boolean mIsInReplyMode;
    private boolean mIsKeyScrollEnabled;
    private String mUrl;
    private Comment mSelectedComment;
    private Menu mMenu;
    private SharedPreferences prefs;
    private ChromeCustomTabsHelper mChromeCustomTabsHelper;
    private DataManager mDataManager;
    private CompositeSubscription mCompositeSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set theme
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = SharedPrefsManager.getTheme(prefs);
        switch (theme) {
            case SharedPrefsManager.THEME_SEPIA:
                setTheme(R.style.AppTheme_Sepia);
                break;
            case SharedPrefsManager.THEME_DARK:
                setTheme(R.style.AppTheme_Dark);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    getWindow().setStatusBarColor(getResources().getColor(R.color.grey_900));
                }
                break;
            case SharedPrefsManager.THEME_AMOLED_BLACK:
                setTheme(R.style.AppTheme_AMOLEDBlack);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    getWindow().setStatusBarColor(getResources().getColor(android.R.color.black));
                }
                break;
        }
        setContentView(R.layout.activity_comments);
        Firebase.setAndroidContext(this);
        appbar = (AppBarLayout) findViewById(R.id.appbar);
        appbar.addOnOffsetChangedListener(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Comments");
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mWindow = new PopupFloatingWindow(this, toolbar);
        mFab = (FloatingScrollDownButton) findViewById(R.id.fab);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        layoutReply = (LinearLayout) findViewById(R.id.layout_reply);
        btnReplySend = (FloatingActionButton) findViewById(R.id.btn_reply_send);
        etReply = (EditText) findViewById(R.id.et_reply);

        Intent intent = getIntent();
        CommentsFragment commentsFragment = null;
        Parcelable postParcel = intent.getParcelableExtra(Constants.KEY_POST_PARCEL);
        if (postParcel != null) {
            commentsFragment = CommentsFragment.newInstance(postParcel,
                intent.getBooleanExtra(Constants.KEY_IS_BOOKMARKED, false));
            Post post = Parcels.unwrap(postParcel);
            //FIXME how the url could be null?!
            mUrl = (post.getUrl() != null ? post.getUrl() : "https://news.ycombinator.com/");
            mPostId = post.getId();
        } else {
            final Uri data = intent.getData();
            if (data != null && data.getQueryParameter("id") != null) {
                long storyId = Long.parseLong(data.getQueryParameter("id"));
                commentsFragment = CommentsFragment.newInstance(storyId);
                mPostId = storyId;
            }
        }

        if (savedInstanceState == null) {
            if (commentsFragment != null) {
                getFragmentManager().beginTransaction()
                    .add(R.id.container, commentsFragment, Constants.FRAGMENT_TAG_COMMENT)
                    .commit();
            }
        }

        mDataManager = new DataManager();
        mCompositeSubscription = new CompositeSubscription();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (SharedPrefsManager.getIsOpenLinkInApp(prefs, this)
            && ChromeCustomTabsHelper.getPackageNameToUse(this) != null) {
            if (mChromeCustomTabsHelper == null) {
                mChromeCustomTabsHelper = new ChromeCustomTabsHelper();
                mChromeCustomTabsHelper.bindCustomTabsService(this);
            }
            if (mUrl != null) {
                mChromeCustomTabsHelper.mayLaunchUrl(Utils.validateAndParseUri(mUrl, mPostId), null, null);
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            // restore SelectCommentDialog if it exists
            mSelectedComment = Parcels.unwrap(savedInstanceState.getParcelable(STATE_SELECTED_COMMENT));
            dialogSelectComment = (CommentOptionDialogFragment) getFragmentManager()
                .findFragmentByTag(FRAGMENT_TAG_SELECT_COMMENT_DIALOG);
            if (dialogSelectComment != null) {
                dialogSelectComment.setOnSelectCommentListener(mOnSelectCommentListener);
                if (mSelectedComment != null) {
                    dialogSelectComment.setSelectedComment(mSelectedComment);
                }
            }
            // restore LoginDialog if it exists
            dialogLogin = (LoginDialogFragment) getFragmentManager()
                .findFragmentByTag(FRAGMENT_TAG_LOGIN_DIALOG);
            if (dialogLogin != null) {
                dialogLogin.setOnLoginListener(mOnLoginListener);
            }
            mPostId = savedInstanceState.getLong(STATE_POST_ID);
            mUrl = savedInstanceState.getString(STATE_URL);
            mIsInReplyMode = savedInstanceState.getBoolean(STATE_IS_IN_REPLAY_MODE);
            mReplyItemId = savedInstanceState.getLong(STATE_REPLY_ITEM_ID);
            if (mIsInReplyMode) {
                switchReplyMode(mIsInReplyMode, mReplyItemId);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedComment != null) {
            outState.putParcelable(STATE_SELECTED_COMMENT, Parcels.wrap(mSelectedComment));
        }
        outState.putLong(STATE_POST_ID, mPostId);
        outState.putString(STATE_URL, mUrl);
        outState.putBoolean(STATE_IS_IN_REPLAY_MODE, mIsInReplyMode);
        outState.putLong(STATE_REPLY_ITEM_ID, mReplyItemId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mWindow.isWindowShowing()) {
            mWindow.dismiss();
        }
        if (mIsInReplyMode && !etReply.getText().toString().isEmpty()) {
            SharedPrefsManager.setReplyText(prefs, etReply.getText().toString());
        }
    }

    @Override
    protected void onDestroy() {
        appbar.removeOnOffsetChangedListener(this);
        if (mChromeCustomTabsHelper != null) {
            mChromeCustomTabsHelper.unbindCustomTabsService(this);
            // if ChromeCustomTabsHelper was not null and called unbind, error would occur
            mChromeCustomTabsHelper = null;
        }
        if (mCompositeSubscription.hasSubscriptions()) {
            mCompositeSubscription.unsubscribe();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_comments, menu);
        mMenu = menu;
        if (SharedPrefsManager.isPostBookmarked(prefs, mPostId)) {
            MenuItem itemBookmark = mMenu.findItem(R.id.action_bookmark);
            itemBookmark.setTitle(getString(R.string.menu_unbookmark));
            itemBookmark.setIcon(R.drawable.ic_unbookmark);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mIsInReplyMode) {
                    showDiscardReplyDialog();
                    return true;
                }
                FragmentManager fm = getFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                } else {
                    finish();
                }
                break;

            case R.id.action_open_post:
                if (mUrl != null) {
                    if (mChromeCustomTabsHelper != null) {
                        // build CustomTabs UI
                        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
                        Utils.setupIntentBuilder(intentBuilder, this, prefs);
                        ChromeCustomTabsHelper.openCustomTab(this, intentBuilder.build(),
                            Utils.validateAndParseUri(mUrl, mPostId), null);
                    } else {
                        Intent urlIntent = new Intent(Intent.ACTION_VIEW);
                        urlIntent.setData(Utils.validateAndParseUri(mUrl, mPostId));
                        startActivity(urlIntent);
                    }
                }
                break;

            case R.id.action_bookmark:
                changeBookmarkState();
                break;

            case R.id.action_upvote:
                vote(mPostId, Constants.VOTE_UP);
                break;

            case R.id.action_reply:
                if (!Utils.isOnline(this)) {
                    Utils.showLongToast(this, R.string.no_connection_prompt);
                    return false;
                }
                switchReplyMode(true, mPostId);
                break;

            case R.id.action_refresh:
                CommentsFragment commentFragment =
                    (CommentsFragment) getFragmentManager().findFragmentByTag(Constants.FRAGMENT_TAG_COMMENT);
                commentFragment.getPresenter().refresh();
                break;

            case R.id.action_share:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                String commentUrl = "https://news.ycombinator.com/item?id=" + mPostId;
                sendIntent.putExtra(Intent.EXTRA_TEXT, commentUrl);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_link_to)));
                break;

            case R.id.action_typography:
                if (!mWindow.isWindowShowing()) {
                    mWindow.show();
                } else {
                    mWindow.dismiss();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void switchReplyMode(boolean isEnabled, long itemId) {
        if (isEnabled) {
            mIsInReplyMode = true;
            mReplyItemId = itemId;
//                view.setSelected(true);
            layoutReply.setVisibility(View.VISIBLE);
            etReply.requestFocus();
            // soft keyboard will not show without this dirty hack
            // https://stackoverflow.com/questions/5105354/how-to-show-soft-keyboard-when-edittext-is-focused#
            // https://stackoverflow.com/questions/13694995/android-softkeyboard-showsoftinput-vs-togglesoftinput
            etReply.postDelayed(new Runnable() {
                @Override
                public void run() {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(etReply, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 200);
            etReply.setText(SharedPrefsManager.getReplyText(prefs));
            mFab.setVisibility(View.GONE);

            btnReplySend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reply(mReplyItemId, etReply.getText().toString());
                }
            });
        } else {
            mIsInReplyMode = false;
            mReplyItemId = -1;
            layoutReply.setVisibility(View.GONE);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(etReply.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
            if (!SharedPrefsManager.getScrollMode(prefs).equals(SharedPrefsManager.SCROLL_MODE_DISABLE)) {
                mFab.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showDiscardReplyDialog() {
        if (etReply.getText().toString().isEmpty()) {
            switchReplyMode(false, mPostId);
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(CommentsActivity.this);
        builder.setTitle(R.string.discard_dialog_discard)
            .setMessage(R.string.discard_dialog_prompt_discard)
            .setPositiveButton(R.string.discard_dialog_discard, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPrefsManager.setReplyText(prefs, "");
                    switchReplyMode(false, mPostId);
                }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void setupScrollMode() {
        String mode = SharedPrefsManager.getScrollMode(prefs);
        switch (mode) {
            case SharedPrefsManager.SCROLL_MODE_FAB_DRAG:
            case SharedPrefsManager.SCROLL_MODE_FAB_HOLD:
                mFab.setVisibility(View.VISIBLE);
                mFab.setScrollDownMode(SharedPrefsManager.getScrollMode(prefs));
                //set fab position to default
                mFab.setTranslationX(0f);
                mFab.setTranslationY(0f);
                mIsKeyScrollEnabled = false;
                setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
                break;
            case SharedPrefsManager.SCROLL_MODE_BUTTON:
                mIsKeyScrollEnabled = true;
                setVolumeControlStream(AudioManager.STREAM_MUSIC);
                mFab.setVisibility(View.GONE);
                break;
            case SharedPrefsManager.SCROLL_MODE_DISABLE:
                mIsKeyScrollEnabled = false;
                setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
                mFab.setVisibility(View.GONE);
                break;
        }
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    private void changeBookmarkState() {
        CommentsFragment commentsFragment =
            (CommentsFragment) getFragmentManager().findFragmentByTag(Constants.FRAGMENT_TAG_COMMENT);
        MenuItem itemBookmark = mMenu.findItem(R.id.action_bookmark);
        if (SharedPrefsManager.isPostBookmarked(prefs, mPostId)) {
            commentsFragment.getPresenter().removeBookmark();
            itemBookmark.setTitle(getString(R.string.menu_bookmark));
            itemBookmark.setIcon(R.drawable.ic_bookmark);
        } else {
            commentsFragment.getPresenter().addBookmark();
            itemBookmark.setTitle(getString(R.string.menu_unbookmark));
            itemBookmark.setIcon(R.drawable.ic_unbookmark);
        }
    }

    public void showCommentOptionDialog(Comment comment) {
        CommentOptionDialogFragment dialogComment =
            CommentOptionDialogFragment.newInstance(mOnSelectCommentListener, comment);
        dialogComment.show(getFragmentManager(), FRAGMENT_TAG_SELECT_COMMENT_DIALOG);
        mSelectedComment = comment;
    }

    CommentOptionDialogFragment.OnSelectCommentListener mOnSelectCommentListener =
        new CommentOptionDialogFragment.OnSelectCommentListener() {
            @Override
            public void onUpVote(Comment comment) {
                vote(comment.getCommentId(), Constants.VOTE_UP);
            }

            @Override
            public void onDownVote(Comment comment) {
                vote(comment.getCommentId(), Constants.VOTE_DOWN);
            }

            @Override
            public void onReply(Comment comment) {
                switchReplyMode(true, comment.getCommentId());
            }

            @Override
            public void onAuthorProfile(Comment comment) {
                Intent urlIntent = new Intent(Intent.ACTION_VIEW);
                String url = "https://news.ycombinator.com/user?id=" + comment.getBy();
                urlIntent.setData(Uri.parse(url));
                startActivity(urlIntent);
            }

            @Override
            public void onShare(Comment comment) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                String url = "https://news.ycombinator.com/item?id=" + comment.getCommentId();
                sendIntent.putExtra(Intent.EXTRA_TEXT, url);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_link_to)));
            }

            @Override
            public void onShareCommentTextTo(Comment comment) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                String text = comment.getBy() + ":\n" + Html.fromHtml(comment.getText());
                sendIntent.putExtra(Intent.EXTRA_TEXT, text);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.send_to)));
            }
        };

    private void vote(final long itemId, final int voteState) {
        if (!Utils.isOnline(this)) {
            Utils.showLongToast(this, R.string.no_connection_prompt);
            return;
        }
        final Snackbar snackbarProcessing = Snackbar.make(coordinatorLayout, "Upvoting...", Snackbar.LENGTH_INDEFINITE);
        TextView tvSnackbarText = (TextView) snackbarProcessing.getView()
            .findViewById(android.support.design.R.id.snackbar_text);
        tvSnackbarText.setTextColor(Color.WHITE);
        snackbarProcessing.show();
        mCompositeSubscription.add(mDataManager.vote(itemId, voteState, prefs)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<Integer>() {
                @Override
                public void call(Integer integer) {
                    snackbarProcessing.dismiss();
                    AlertDialog.Builder builder =
                        new AlertDialog.Builder(CommentsActivity.this)
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                    switch (integer) {
                        case Constants.OPERATE_ERROR_COOKIE_EXPIRED:
                            builder.setTitle("Login cookie expired")
                                .setMessage("Would you like to login again?")
                                .setPositiveButton("Login again", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        login();
                                    }
                                }).create().show();
                            break;
                        case Constants.OPERATE_ERROR_NO_COOKIE:
                            builder.setTitle("Not login")
                                .setMessage("Would you like to login?")
                                .setPositiveButton("Login", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        login();
                                    }
                                }).create().show();
                            break;
                        case Constants.OPERATE_ERROR_HAVE_VOTED:
                            Utils.showLongToast(CommentsActivity.this, "Already voted");
                            break;
                        case Constants.OPERATE_ERROR_NOT_ENOUGH_KARMA:
                            Utils.showLongToast(CommentsActivity.this, "Karma is not enough " +
                                "or you've voted already");
                            break;
                        case Constants.OPERATE_SUCCESS:
                            Utils.showLongToast(CommentsActivity.this, "Vote succeed");
                            break;
                        case Constants.OPERATE_ERROR_UNKNOWN:
                            builder.setTitle("Vote failed")
                                .setMessage("Would you like to vote again?")
                                .setPositiveButton("Vote again", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        vote(itemId, voteState);
                                    }
                                }).create().show();
                            break;
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    snackbarProcessing.dismiss();
                    Log.e("post vote err", throwable.toString());

                }
            }));
    }

    private void login() {
        dialogLogin =  LoginDialogFragment.newInstance(mOnLoginListener);
        dialogLogin.show(getFragmentManager(), FRAGMENT_TAG_LOGIN_DIALOG);
        // guarantee getDialog() will not return null
        getFragmentManager().executePendingTransactions();
        // show keyboard when dialog shows
        dialogLogin.getDialog().getWindow()
            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    private LoginDialogFragment.OnLoginListener mOnLoginListener = new LoginDialogFragment.OnLoginListener() {
            @Override
            public void onLogin(final String username, String password) {
                mCompositeSubscription.add(mDataManager.login(username, password)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            if (s.isEmpty()) {// login failed
                                dialogLogin.resetLogin();
                            } else {
                                dialogLogin.getDialog().dismiss();
                                Utils.showLongToast(CommentsActivity.this, "Login succeed");
                                SharedPrefsManager.setUsername(prefs, username);
                                SharedPrefsManager.setLoginCookie(prefs, s);
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Log.e("login", throwable.toString());
                            dialogLogin.resetLogin();
                        }
                    }));
            }
        };

    private void reply(final long itemId, String replyText) {
        if (!Utils.isOnline(this)) {
            Utils.showLongToast(this, R.string.no_connection_prompt);
            return;
        }
        if (etReply.getText().toString().isEmpty()) {
            Utils.showLongToast(this, "Sound of silence...");
            return;
        }
        final Snackbar snackbarProcessing = Snackbar.make(coordinatorLayout, "Replying...", Snackbar.LENGTH_INDEFINITE);
        TextView tvSnackbarText = (TextView) snackbarProcessing.getView()
            .findViewById(android.support.design.R.id.snackbar_text);
        tvSnackbarText.setTextColor(Color.WHITE);
        snackbarProcessing.show();
        mCompositeSubscription.add(
            mDataManager.reply(itemId, replyText, SharedPrefsManager.getLoginCookie(prefs))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        snackbarProcessing.dismiss();
                        AlertDialog.Builder builder = new AlertDialog.Builder(CommentsActivity.this)
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                        switch (integer) {
                            case Constants.OPERATE_ERROR_COOKIE_EXPIRED:
                                builder.setTitle("Login cookie expired")
                                    .setMessage("Would you like to login again?")
                                    .setPositiveButton("Login again", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            login();
                                        }
                                    }).create().show();
                                break;
                            case Constants.OPERATE_ERROR_NO_COOKIE:
                                builder.setTitle("Not login")
                                    .setMessage("Would you like to login?")
                                    .setPositiveButton("Login", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            login();
                                        }
                                    }).create().show();
                                break;
                            case Constants.OPERATE_SUCCESS:
                                Utils.showLongToast(CommentsActivity.this, "Reply succeeded");
                                SharedPrefsManager.setReplyText(prefs, "");
                                switchReplyMode(false, mPostId);
                                break;
                            case Constants.OPERATE_ERROR_UNKNOWN:
                                builder.setTitle("Reply failed")
                                    .setMessage("Would you like to reply again?")
                                    .setPositiveButton("Reply again", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            reply(itemId, etReply.getText().toString());
                                        }
                                    }).create().show();
                                break;
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        snackbarProcessing.dismiss();
                        Log.e("reply", throwable.toString());

                    }
                }));
    }

    public ChromeCustomTabsHelper getChromeCustomTabsHelper() {
        return mChromeCustomTabsHelper;
    }

    public SharedPreferences getSharedPreferences() {
        return prefs;
    }

    @Override
    public void onBackPressed() {
        if (mIsInReplyMode) {
            showDiscardReplyDialog();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mIsKeyScrollEnabled) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_PAGE_UP) {
                if (getFragmentManager().findFragmentById(R.id.container) instanceof CommentsFragment) {
                    ((CommentsFragment) getFragmentManager().findFragmentById(R.id.container))
                        .scrollUp(appbar.getHeight() + mAppBarOffset);
                }
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_PAGE_DOWN) {
                if (getFragmentManager().findFragmentById(R.id.container) instanceof CommentsFragment) {
                    ((CommentsFragment) getFragmentManager().findFragmentById(R.id.container))
                        .scrollDown(appbar.getHeight() + mAppBarOffset);
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onRecyclerViewCreated(RecyclerView recyclerView) {
        mFab.setRecyclerView(recyclerView);
        setupScrollMode();
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        mAppBarOffset = i;
        if (getFragmentManager().findFragmentByTag(Constants.FRAGMENT_TAG_COMMENT) instanceof CommentsFragment) {
            ((CommentsFragment) getFragmentManager().findFragmentById(R.id.container))
                .setSwipeRefreshLayoutState(i == 0);
        }
    }
}
