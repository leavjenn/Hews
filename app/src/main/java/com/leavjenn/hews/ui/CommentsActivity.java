package com.leavjenn.hews.ui;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsIntent;
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
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.leavjenn.hews.ChromeCustomTabsHelper;
import com.leavjenn.hews.Constants;
import com.leavjenn.hews.R;
import com.leavjenn.hews.SharedPrefsManager;
import com.leavjenn.hews.Utils;
import com.leavjenn.hews.listener.OnRecyclerViewCreateListener;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.network.DataManager;
import com.leavjenn.hews.ui.widget.FloatingScrollDownButton;
import com.leavjenn.hews.ui.widget.LoginDialogFragment;
import com.leavjenn.hews.ui.widget.PopupFloatingWindow;

import rx.Subscriber;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;


public class CommentsActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener, OnRecyclerViewCreateListener {
    private PopupFloatingWindow mWindow;
    private FloatingScrollDownButton mFab;
    private String mUrl;
    private long mPostId;
    private SharedPreferences prefs;
    private ChromeCustomTabsHelper mChromeCustomTabsHelper;
    DataManager mDataManager;
    CompositeSubscription mCompositeSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set theme
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        String theme = SharedPrefsManager.getTheme(prefs);
        if (theme.equals(SharedPrefsManager.THEME_SEPIA)) {
            setTheme(R.style.AppTheme_Sepia);
        }
        if (theme.equals(SharedPrefsManager.THEME_DARK)) {
            setTheme(R.style.AppTheme_Dark);
        }
        setContentView(R.layout.activity_comments);
        Firebase.setAndroidContext(this);
        //Setup Toolbar
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

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        CommentsFragment commentsFragment = null;
        if (bundle != null) {
            Post post = intent.getParcelableExtra(Constants.KEY_POST);
            commentsFragment = CommentsFragment.newInstance(post);
            mUrl = post.getUrl();
            mPostId = post.getId();
        }

        final Uri data = intent.getData();
        if (data != null) {
            long storyId = Long.parseLong(data.getQueryParameter("id"));
            commentsFragment = CommentsFragment.newInstance(storyId);
            mPostId = storyId;
        }

        if (savedInstanceState == null) {
            if (commentsFragment != null) {
                getFragmentManager().beginTransaction()
                        .add(R.id.container, commentsFragment, "CommentFragTag")
                        .commit();
            }
        }

        mDataManager = new DataManager(Schedulers.io());
        mCompositeSubscription = new CompositeSubscription();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (!SharedPrefsManager.getIsOpenLinkInBrowser(prefs, this)
                && ChromeCustomTabsHelper.getPackageNameToUse(this) != null) {
            mChromeCustomTabsHelper = new ChromeCustomTabsHelper();
            mChromeCustomTabsHelper.bindCustomTabsService(this);
            if (mUrl != null) {
                mChromeCustomTabsHelper.mayLaunchUrl(Uri.parse(mUrl), null, null);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mWindow.isWindowShowing()) {
            mWindow.dismiss();
        }
        if (mChromeCustomTabsHelper != null) {
            mChromeCustomTabsHelper.unbindCustomTabsService(this);
        }
        if (mCompositeSubscription.hasSubscriptions()) {
            mCompositeSubscription.unsubscribe();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_comments, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                FragmentManager fm = getFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                } else {
                    finish();
                }
                break;
            case R.id.action_upvote:
                vote();
                break;

            case R.id.action_open_post:
                if (mUrl != null) {
                    if (mChromeCustomTabsHelper != null) {
                        // build CustomTabs UI
                        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
                        if (SharedPrefsManager.getTheme(prefs).equals(SharedPrefsManager.THEME_DARK)) {
                            intentBuilder.setToolbarColor(getResources().getColor(R.color.grey_900));
                        } else {
                            intentBuilder.setToolbarColor(getResources().getColor(R.color.orange_600));
                        }

                        intentBuilder.setShowTitle(true);
                        intentBuilder.setCloseButtonIcon(
                                BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_back));

                        ChromeCustomTabsHelper.openCustomTab(this, intentBuilder.build(),
                                Uri.parse(mUrl), null);
                    } else {
                        Intent urlIntent = new Intent(Intent.ACTION_VIEW);
                        urlIntent.setData(Uri.parse(mUrl));
                        startActivity(urlIntent);
                    }
                }
                break;

            case R.id.action_refresh:
                CommentsFragment commentFragment =
                        (CommentsFragment) getFragmentManager().findFragmentByTag("CommentFragTag");
                commentFragment.refresh();
                break;

            case R.id.action_share:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                String commentUrl = "https://news.ycombinator.com/item?id="
                        + mPostId;
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

    void vote() {
        if (!Utils.isOnline(this)) {
            Utils.showOfflineToast(this);
        } else {
            mCompositeSubscription.add(AppObservable.bindActivity(this, mDataManager.vote(mPostId, prefs))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<Integer>() {
                        @Override
                        public void onCompleted() {
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e("post vote err", e.toString());
                        }

                        @Override
                        public void onNext(Integer integer) {
                            switch (integer) {
                                case Constants.OPERATE_ERROR_COOKIE_EXPIRED:
                                case Constants.OPERATE_ERROR_NO_COOKIE:
                                    Snackbar.make(null, "Not login.", Snackbar.LENGTH_INDEFINITE)
                                            .setAction("Login", new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    login();
                                                }
                                            }).show();
                                    break;
                                case Constants.OPERATE_ERROR_HAVE_VOTED:
                                    Toast.makeText(CommentsActivity.this, "already voted", Toast.LENGTH_LONG).show();
                                    break;
                                case Constants.OPERATE_SUCCESS:
                                    Toast.makeText(CommentsActivity.this, "vote secceed", Toast.LENGTH_LONG).show();
                                    break;
                                case Constants.OPERATE_ERROR_UNKNOWN:
                                    Snackbar.make(null, "vote failed.", Snackbar.LENGTH_INDEFINITE)
                                            .setAction("Vote again", new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    vote();
                                                }
                                            }).show();
                                    break;
                            }
                        }
                    }));
        }
    }

    void login() {
        final LoginDialogFragment loginDialogFragment = new LoginDialogFragment();
        LoginDialogFragment.OnLoginListener onLoginListener =
                new LoginDialogFragment.OnLoginListener() {
                    @Override
                    public void onLogin(final String username, String password) {
                        mCompositeSubscription.add(AppObservable.bindActivity(CommentsActivity.this,
                                mDataManager.login(username, password))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Subscriber<String>() {
                                    @Override
                                    public void onCompleted() {
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        Log.e("login err", e.toString());
                                    }

                                    @Override
                                    public void onNext(String s) {
                                        if (s.isEmpty()) {// login failed
                                            loginDialogFragment.resetLogin();
                                        } else {
                                            loginDialogFragment.getDialog().dismiss();
                                            Toast.makeText(CommentsActivity.this, "login secceed",
                                                    Toast.LENGTH_LONG).show();
                                            SharedPrefsManager.setUsername(prefs, username);
                                            SharedPrefsManager.setLoginCookie(prefs, s);
                                        }
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
}
