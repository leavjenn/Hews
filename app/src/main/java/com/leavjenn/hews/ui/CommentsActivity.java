package com.leavjenn.hews.ui;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.util.Log;
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
import com.leavjenn.hews.Utils;
import com.leavjenn.hews.listener.OnRecyclerViewCreateListener;
import com.leavjenn.hews.misc.ChromeCustomTabsHelper;
import com.leavjenn.hews.misc.SharedPrefsManager;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.network.DataManager;
import com.leavjenn.hews.ui.widget.FloatingScrollDownButton;
import com.leavjenn.hews.ui.widget.LoginDialogFragment;
import com.leavjenn.hews.ui.widget.PopupFloatingWindow;

import org.parceler.Parcels;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class CommentsActivity extends AppCompatActivity implements
    SharedPreferences.OnSharedPreferenceChangeListener, OnRecyclerViewCreateListener,
    AppBarLayout.OnOffsetChangedListener {
    private PopupFloatingWindow mWindow;
    private FloatingScrollDownButton mFab;
    private CoordinatorLayout coordinatorLayout;
    private AppBarLayout appbar;
    private LinearLayout layoutReply;
    private EditText etReply;
    private FloatingActionButton btnReplySend;

    private String mUrl;
    private long mPostId;
    private boolean isReplyEnabled;
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
        prefs.registerOnSharedPreferenceChangeListener(this);
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
            mChromeCustomTabsHelper = new ChromeCustomTabsHelper();
            mChromeCustomTabsHelper.bindCustomTabsService(this);
            if (mUrl != null) {
                mChromeCustomTabsHelper.mayLaunchUrl(Utils.validateAndParseUri(mUrl, mPostId), null, null);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mWindow.isWindowShowing()) {
            mWindow.dismiss();
        }
        appbar.removeOnOffsetChangedListener(this);
        if (mChromeCustomTabsHelper != null) {
            mChromeCustomTabsHelper.unbindCustomTabsService(this);
        }
        if (mCompositeSubscription.hasSubscriptions()) {
            mCompositeSubscription.unsubscribe();
        }
        if (isReplyEnabled && !etReply.getText().toString().isEmpty()) {
            SharedPrefsManager.setReplyText(prefs, etReply.getText().toString());
        }
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
                if (isReplyEnabled) {
                    showDiscardReplyAlert();
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
                vote(mPostId);
                break;

            case R.id.action_reply:
                if (!Utils.isOnline(this)) {
                    Utils.showLongToast(this, R.string.no_connection_prompt);
                    return false;
                }
                enableReplyMode(true, mPostId);
                break;

            case R.id.action_refresh:
                CommentsFragment commentFragment =
                    (CommentsFragment) getFragmentManager().findFragmentByTag(Constants.FRAGMENT_TAG_COMMENT);
                commentFragment.refresh();
                break;

            case R.id.action_share:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                String commentUrl = "https://news.ycombinator.com/item?id=" + mPostId;
                sendIntent.putExtra(Intent.EXTRA_TEXT, commentUrl);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_link_to)));
                break;

            case R.id.action_display:
                if (!mWindow.isWindowShowing()) {
                    mWindow.show();
                } else {
                    mWindow.dismiss();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void enableReplyMode(boolean isEnabled, final long itemId) {
        if (isEnabled) {
            isReplyEnabled = true;
//                view.setSelected(true);
            layoutReply.setVisibility(View.VISIBLE);
            etReply.requestFocus();
            //the soft keyborad will not show without this drity hack
            //https://stackoverflow.com/questions/5105354/how-to-show-soft-keyboard-when-edittext-is-focused#
            //https://stackoverflow.com/questions/13694995/android-softkeyboard-showsoftinput-vs-togglesoftinput
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
                    reply(itemId, etReply.getText().toString());
                }
            });
        } else {
            isReplyEnabled = false;
            layoutReply.setVisibility(View.GONE);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(etReply.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
            if (!SharedPrefsManager.getFabMode(prefs).equals(SharedPrefsManager.FAB_DISABLE)) {
                mFab.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showDiscardReplyAlert() {
        if (etReply.getText().toString().isEmpty()) {
            enableReplyMode(false, mPostId);
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(CommentsActivity.this);
        builder.setTitle("Discard")
            .setMessage("Discard your reply?")
            .setPositiveButton("Discard", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    enableReplyMode(false, mPostId);
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void setupFAB() {
        String mode = SharedPrefsManager.getFabMode(prefs);
        if (!mode.equals(SharedPrefsManager.FAB_DISABLE)) {
            mFab.setVisibility(View.VISIBLE);
            mFab.setScrollDownMode(SharedPrefsManager.getFabMode(prefs));
            //set fab position to default
            mFab.setTranslationX(0f);
            mFab.setTranslationY(0f);
        } else {
            mFab.setVisibility(View.GONE);
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
            commentsFragment.removeBookmark();
            itemBookmark.setTitle(getString(R.string.menu_bookmark));
            itemBookmark.setIcon(R.drawable.ic_bookmark);
        } else {
            commentsFragment.addBookmark();
            itemBookmark.setTitle(getString(R.string.menu_unbookmark));
            itemBookmark.setIcon(R.drawable.ic_unbookmark);
        }
    }

    public void vote(final long itemId) {
        if (!Utils.isOnline(this)) {
            Utils.showLongToast(this, R.string.no_connection_prompt);
            return;
        }
        final Snackbar snackbarProcessing = Snackbar.make(coordinatorLayout, "Upvoting...", Snackbar.LENGTH_INDEFINITE);
        TextView tvSnackbarText = (TextView) snackbarProcessing.getView()
            .findViewById(android.support.design.R.id.snackbar_text);
        tvSnackbarText.setTextColor(Color.WHITE);
        snackbarProcessing.show();
        mCompositeSubscription.add(mDataManager.vote(itemId, prefs)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<Integer>() {
                @Override
                public void call(Integer integer) {
                    snackbarProcessing.dismiss();
                    AlertDialog.Builder builder =
                        new AlertDialog.Builder(CommentsActivity.this)
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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
                            Utils.showLongToast(CommentsActivity.this, "Already upvoted");
                            break;
                        case Constants.OPERATE_SUCCESS:
                            Utils.showLongToast(CommentsActivity.this, "Upvote succeed");
                            break;
                        case Constants.OPERATE_ERROR_UNKNOWN:
                            builder.setTitle("Upvote failed")
                                .setMessage("Would you like to upvote again?")
                                .setPositiveButton("Upvote again", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        vote(itemId);
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

    void login() {
        final LoginDialogFragment loginDialogFragment = new LoginDialogFragment();
        LoginDialogFragment.OnLoginListener onLoginListener =
            new LoginDialogFragment.OnLoginListener() {
                @Override
                public void onLogin(final String username, String password) {
                    mCompositeSubscription.add(mDataManager.login(username, password)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<String>() {
                            @Override
                            public void call(String s) {
                                if (s.isEmpty()) {// login failed
                                    loginDialogFragment.resetLogin();
                                } else {
                                    loginDialogFragment.getDialog().dismiss();
                                    Utils.showLongToast(CommentsActivity.this, "Login succeed");
                                    SharedPrefsManager.setUsername(prefs, username);
                                    SharedPrefsManager.setLoginCookie(prefs, s);
                                }
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Log.e("login err", throwable.toString());

                            }
                        }));
                }
            };
        loginDialogFragment.setListener(onLoginListener);
        loginDialogFragment.show(getFragmentManager(), "loginDialogFragment");
        // guarantee getDialog() will not return null
        getFragmentManager().executePendingTransactions();
        // show keyboard when dialog shows
        loginDialogFragment.getDialog().getWindow()
            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    void reply(final long itemId, String replyText) {
        if (!Utils.isOnline(this)) {
            Utils.showLongToast(this, R.string.no_connection_prompt);
            return;
        }
        if (etReply.getText().toString().isEmpty()) {
            Utils.showLongToast(this, "Sound of slience...");
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
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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
                                Utils.showLongToast(CommentsActivity.this, "Reply succeed");
                                enableReplyMode(false, mPostId);
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
                        Log.e("post reply err", throwable.toString());

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
        if (isReplyEnabled) {
            showDiscardReplyAlert();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SharedPrefsManager.KEY_FAB_MODE)) {
            setupFAB();
        }
    }

    @Override
    public void onRecyclerViewCreate(RecyclerView recyclerView) {
        mFab.setRecyclerView(recyclerView);
        setupFAB();
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        if (getFragmentManager().findFragmentByTag(Constants.FRAGMENT_TAG_COMMENT) instanceof CommentsFragment) {
            ((CommentsFragment) getFragmentManager().findFragmentById(R.id.container))
                .setSwipeRefreshLayoutState(i == 0);
        }
    }
}
