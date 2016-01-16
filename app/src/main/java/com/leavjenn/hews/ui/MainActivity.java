package com.leavjenn.hews.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.leavjenn.hews.ui.adapter.PostAdapter;
import com.leavjenn.hews.ui.widget.AlwaysShowDialogSpinner;
import com.leavjenn.hews.ui.widget.DateRangeDialogFragment;
import com.leavjenn.hews.ui.widget.FeedbackDialogFragment;
import com.leavjenn.hews.ui.widget.FloatingScrollDownButton;
import com.leavjenn.hews.ui.widget.LoginDialogFragment;
import com.leavjenn.hews.ui.widget.PopupFloatingWindow;

import org.parceler.Parcels;

import java.util.Calendar;
import java.util.TimeZone;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;


public class MainActivity extends AppCompatActivity implements PostAdapter.OnItemClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener, OnRecyclerViewCreateListener,
        AppBarLayout.OnOffsetChangedListener {

    private static final int DRAWER_CLOSE_DELAY_MS = 250;
    private static final String STATE_DRAWER_SELECTED_ITEM = "state_drawer_selected_item";
    private static final String STATE_POPULAR_DATE_RANGE = "state_popular_date_range";
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private View drawerHeader;
    private LinearLayout layoutLogin;
    private TextView tvLoginName;
    private ImageView ivExpander;
    private boolean isLoginMenuExpanded;
    private int mDrawerSelectedItem;
    private final Handler mDrawerActionHandler = new Handler();
    private ActionBarDrawerToggle mDrawerToggle;
    private AppBarLayout mAppbar;
    private Toolbar toolbar;
    private AlwaysShowDialogSpinner mSpinnerDateRange;
    private Spinner mSpinnerSortOrder;
    private PopupFloatingWindow mWindow;
    private SearchView mSearchView;
    private MenuItem mSearchItem;
    private boolean isSearchKeywordSubmitted;
    private FloatingScrollDownButton mFab;
    private String mStoryType, mStoryTypeSpec;
    private SharedPreferences prefs;
    private DataManager mDataManager;
    private CompositeSubscription mCompositeSubscription;
    private ChromeCustomTabsHelper mChromeCustomTabsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set theme
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        String theme = SharedPrefsManager.getTheme(prefs);
        if (theme.equals(SharedPrefsManager.THEME_SEPIA)) {
            setTheme(R.style.AppTheme_Sepia);
        } else if (theme.equals(SharedPrefsManager.THEME_DARK)) {
            setTheme(R.style.AppTheme_Dark);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(R.color.grey_900));
            }
        } else if (theme.equals(SharedPrefsManager.THEME_AMOLED_BLACK)) {
            setTheme(R.style.AppTheme_AMOLEDBlack);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(android.R.color.black));
            }
        }
        setContentView(R.layout.activity_main);
        Firebase.setAndroidContext(this);
        // setup Toolbar
        mAppbar = (AppBarLayout) findViewById(R.id.appbar);
        mAppbar.addOnOffsetChangedListener(this);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // init spinner
        mSpinnerDateRange = (AlwaysShowDialogSpinner) findViewById(R.id.spinner_time_range);
        mSpinnerSortOrder = (Spinner) findViewById(R.id.spinner_sort_order);
        //setup drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        if (mNavigationView != null) {
            setupDrawerContent(mNavigationView);
        }
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.open_drawer,
                R.string.close_drawer);
        // TODO inflate drawerHeader due to a bug:
        // https://code.google.com/p/android/issues/detail?id=190226
        drawerHeader = getLayoutInflater().inflate(R.layout.drawer_header, null);
        mNavigationView.addHeaderView(drawerHeader);
        layoutLogin = (LinearLayout) drawerHeader.findViewById(R.id.layout_login);
        tvLoginName = (TextView) drawerHeader.findViewById(R.id.tv_account);
        updateLoginName();
        layoutLogin = (LinearLayout) findViewById(R.id.layout_login);
        layoutLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickLogin();
            }
        });
        ivExpander = (ImageView) findViewById(R.id.iv_expander);

        mFab = (FloatingScrollDownButton) findViewById(R.id.fab);

        mWindow = new PopupFloatingWindow(this, mAppbar);

        if (getFragmentManager().findFragmentById(R.id.container) == null) {
            Log.i("act oncreate", "null frag");
            mStoryType = Constants.TYPE_STORY;
            mStoryTypeSpec = Constants.STORY_TYPE_TOP_PATH;
            PostFragment postFragment = PostFragment.newInstance(mStoryType, mStoryTypeSpec);
            getFragmentManager().beginTransaction().add(R.id.container, postFragment).commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //init mCompositeSubscription here due to onCreate() will not be called
//        when theme changed (call recreate())
        mCompositeSubscription = new CompositeSubscription();
        mDataManager = new DataManager();
        if (SharedPrefsManager.getIsOpenLinkInApp(prefs, this)
                && ChromeCustomTabsHelper.getPackageNameToUse(this) != null) {
            mChromeCustomTabsHelper = new ChromeCustomTabsHelper();
            mChromeCustomTabsHelper.bindCustomTabsService(this);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mDrawerSelectedItem = savedInstanceState.getInt(STATE_DRAWER_SELECTED_ITEM, 0);
        Menu menu = mNavigationView.getMenu();
        //mDrawerSelectedItem + 2 to skip login and logout
        menu.getItem(mDrawerSelectedItem + 2).setChecked(true);
        //TODO bug: item including login part
        Log.i("mDrawerSelectedItem", String.valueOf(mDrawerSelectedItem));
        if (mDrawerSelectedItem == 4) { // popular
            Log.i("mDrawerSelectedItem", "DateRange");
            setUpSpinnerPopularDateRange();
            int selectedDateRange = savedInstanceState.getInt(STATE_POPULAR_DATE_RANGE, 0);
            mSpinnerDateRange.setSelection(selectedDateRange);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_DRAWER_SELECTED_ITEM, mDrawerSelectedItem);
        if (mSpinnerDateRange.getVisibility() == View.VISIBLE) {
            outState.putInt(STATE_POPULAR_DATE_RANGE, mSpinnerDateRange.getSelectedItemPosition());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCompositeSubscription.hasSubscriptions()) {
            mCompositeSubscription.unsubscribe();
        }
        if (mChromeCustomTabsHelper != null) {
            mChromeCustomTabsHelper.unbindCustomTabsService(this);
            // if ChromeCustomTabsHelper was not null and called unbind, error would occur
            mChromeCustomTabsHelper = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAppbar.removeOnOffsetChangedListener(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        setUpSearchBar(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_search:
                //mSpinnerDateRange.setVisibility(View.VISIBLE);
                //mSpinnerSortOrder.setVisibility(View.VISIBLE);
                //Intent urlIntent = new Intent(Intent.ACTION_VIEW);
                //String url = "https://hn.algolia.com/";
                //urlIntent.setData(Uri.parse(url));
                //startActivity(urlIntent);
//                mSearchView.setIconified(false);
//                mDrawerToggle.setDrawerIndicatorEnabled(false);
                break;

            case R.id.action_refresh:
                Fragment currentFrag = getFragmentManager()
                        .findFragmentById(R.id.container);
                if (currentFrag instanceof PostFragment) {
                    ((PostFragment) currentFrag).refresh();
                } else if (currentFrag instanceof SearchFragment) {
                    ((SearchFragment) currentFrag).refresh();
                }
                break;

            case R.id.action_display:
                if (!mWindow.isWindowShowing()) {
                    mWindow.show();
                } else {
                    mWindow.dismiss();
                }
                break;

            case android.R.id.home:
                if (mWindow.isWindowShowing()) {
                    mWindow.dismiss();
                }
                mDrawerLayout.openDrawer(GravityCompat.START);
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

    void setUpSearchBar(Menu menu) {
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
//        SearchView mSearchView =
//                (SearchView) menu.findItem(R.id.action_search).getActionView();
        mSearchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
        if (mSearchView != null) {
            mSearchView.setSearchableInfo(
                    searchManager.getSearchableInfo(getComponentName()));
        }
        MenuItemCompat.setOnActionExpandListener(mSearchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                SearchFragment searchFragment = new SearchFragment();
                FragmentTransaction transaction = MainActivity.this.getFragmentManager()
                        .beginTransaction();
                transaction.replace(R.id.container, searchFragment);
                transaction.addToBackStack(null);
                transaction.commit();
                getFragmentManager().executePendingTransactions();
                mSpinnerSortOrder.setVisibility(View.VISIBLE);
                mSpinnerDateRange.setVisibility(View.VISIBLE);
                setUpSpinnerSearchDateRange();
                setUpSpinnerSortOrder();
                mSearchView.requestFocus();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mSpinnerSortOrder.setVisibility(View.GONE);
                mSpinnerDateRange.setVisibility(View.GONE);
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    Log.i("fragment", "popback");
                    getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
                return true;
            }
        });
        SearchView.OnQueryTextListener onQueryTextListener = new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                Fragment currentFrag = getFragmentManager().findFragmentById(R.id.container);
                if (currentFrag instanceof SearchFragment) {
                    String dateRange = ((SearchFragment) currentFrag).getDateRange();
                    if (dateRange == null) {
                        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
                        String secEnd = String.valueOf(c.getTimeInMillis() / 1000);
                        c.add(Calendar.YEAR, -1);
                        String secStart = String.valueOf(c.getTimeInMillis() / 1000);
                        dateRange = secStart + secEnd;
                    }
                    isSearchKeywordSubmitted = true;
                    ((SearchFragment) currentFrag).setKeyword(query);
                    ((SearchFragment) currentFrag).refresh(query, dateRange,
                            ((SearchFragment) currentFrag).getIsSortByDate());
                }
                mSearchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                isSearchKeywordSubmitted = false;
                return true;
            }
        };
        mSearchView.setOnQueryTextListener(onQueryTextListener);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(final MenuItem menuItem) {
                        final int type = menuItem.getItemId();
                        switch (type) {
                            case R.id.nav_top_story:
                                mStoryTypeSpec = Constants.STORY_TYPE_TOP_PATH;
                                mDrawerSelectedItem = 0;
                                break;
                            case R.id.nav_new_story:
                                mStoryTypeSpec = Constants.STORY_TYPE_NEW_PATH;
                                mDrawerSelectedItem = 1;
                                break;
                            case R.id.nav_ask_hn:
                                mStoryTypeSpec = Constants.STORY_TYPE_ASK_HN_PATH;
                                mDrawerSelectedItem = 2;
                                break;
                            case R.id.nav_show_hn:
                                mStoryTypeSpec = Constants.STORY_TYPE_SHOW_HN_PATH;
                                mDrawerSelectedItem = 3;
                                break;
                            case R.id.nav_popular:
                                mStoryTypeSpec = Constants.TYPE_SEARCH;
                                mDrawerSelectedItem = 4;
                            case R.id.nav_bookmark:
                                mStoryTypeSpec = Constants.TYPE_BOOKMARK;
                                mDrawerSelectedItem = 5;
                                break;
                            case R.id.nav_settings:
                                break;
                        }
                        // allow some time after closing the drawer before performing real navigation
                        // so the user can see what is happening
                        mDrawerLayout.closeDrawer(GravityCompat.START);

                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                if (type == R.id.nav_settings) {
                                    Intent i = new Intent(getBaseContext(), SettingsActivity.class);
                                    startActivity(i);
                                } else if (type == R.id.nav_popular) {
                                    menuItem.setChecked(true);
                                    setUpSpinnerPopularDateRange();
                                    Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
                                    String secEnd = String.valueOf(c.getTimeInMillis() / 1000);
                                    c.add(Calendar.DAY_OF_YEAR, -1);
                                    String secStart = String.valueOf(c.getTimeInMillis() / 1000);
                                    if (getFragmentManager().findFragmentById(R.id.container)
                                            instanceof PostFragment) {
                                        PostFragment currentFrag = (PostFragment) getFragmentManager()
                                                .findFragmentById(R.id.container);
                                        currentFrag.refresh(Constants.TYPE_SEARCH, "0" + secStart + secEnd);
                                    } else {
                                        PostFragment postFragment = PostFragment
                                                .newInstance(Constants.TYPE_SEARCH, "0" + secStart + secEnd);
                                        FragmentTransaction transaction = getFragmentManager()
                                                .beginTransaction();
                                        transaction.replace(R.id.container, postFragment);
                                        transaction.addToBackStack(null);
                                        transaction.commit();
                                    }
                                } else if (type == R.id.nav_bookmark) {
                                    //TODO
                                    menuItem.setChecked(true);
                                    BookmarkFragment bookmarkFragment = new BookmarkFragment();
                                    FragmentTransaction transaction = getFragmentManager()
                                            .beginTransaction();
                                    transaction.replace(R.id.container, bookmarkFragment);
                                    transaction.addToBackStack(null);
                                    transaction.commit();
                                    mSpinnerDateRange.setVisibility(View.GONE);
                                } else if (type == R.id.nav_login) {
                                    login();
                                } else if (type == R.id.nav_logout) {
                                    Utils.showLongToast(MainActivity.this, "Logout succeed");
                                    SharedPrefsManager.setUsername(prefs,
                                            MainActivity.this.getResources().getString(R.string.nav_logout));
                                    SharedPrefsManager.setLoginCookie(prefs, "");
                                    updateLoginName();
                                } else if (type == R.id.nav_feedback) {
                                    feedback();
                                } else {
                                    menuItem.setChecked(true);
                                    mSpinnerDateRange.setVisibility(View.GONE);
                                    if (getFragmentManager().findFragmentById(R.id.container)
                                            instanceof PostFragment) {
                                        PostFragment currentFrag = (PostFragment) getFragmentManager()
                                                .findFragmentById(R.id.container);
                                        currentFrag.refresh(Constants.TYPE_STORY, mStoryTypeSpec);
                                    } else {
                                        PostFragment postFragment =
                                                PostFragment.newInstance(Constants.TYPE_STORY, mStoryTypeSpec);
                                        FragmentTransaction transaction = getFragmentManager()
                                                .beginTransaction();
                                        transaction.replace(R.id.container, postFragment);
                                        transaction.addToBackStack(null);
                                        transaction.commit();
                                    }
                                }
                                mNavigationView.getMenu().setGroupVisible(R.id.group_login, false);
                                ivExpander.setImageResource(R.drawable.expander_open);
                            }
                        };

                        mDrawerActionHandler.postDelayed(r, DRAWER_CLOSE_DELAY_MS);
                        return true;
                    }
                }
        );
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
                                            Utils.showLongToast(MainActivity.this, "Login succeed");
                                            SharedPrefsManager.setUsername(prefs, username);
                                            SharedPrefsManager.setLoginCookie(prefs, s);
                                            updateLoginName();
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

    private void feedback() {
        FeedbackDialogFragment feedbackDialog = new FeedbackDialogFragment();
        feedbackDialog.setOnFeedbackListClickListener(
                new FeedbackDialogFragment.OnFeedbackListClickListener() {
                    @Override
                    public void onTwitter() {
                        Intent urlIntent = new Intent(Intent.ACTION_VIEW);
                        String url = "https://twitter.com/leavjenn";
                        urlIntent.setData(Uri.parse(url));
                        startActivity(urlIntent);
                    }

                    @Override
                    public void onGooglePlus() {
                        Intent urlIntent = new Intent(Intent.ACTION_VIEW);
                        String url = "https://plus.google.com/u/0/101572751825365377306";
                        urlIntent.setData(Uri.parse(url));
                        startActivity(urlIntent);
                    }

                    @Override
                    public void onEmail() {
                        Intent intent = new Intent(Intent.ACTION_SENDTO,
                                Uri.fromParts("mailto", "", null));
                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"leavjenn@gmail.com"});
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback on Hews");
                        startActivity(Intent.createChooser(intent, "Send Email"));
                    }

                    @Override
                    public void onGooglePlayReview() {
                        Intent urlIntent = new Intent(Intent.ACTION_VIEW);
                        String url =
                                "https://play.google.com/store/apps/details?id=com.leavjenn.hews";
                        urlIntent.setData(Uri.parse(url));
                        startActivity(urlIntent);
                    }
                });
        feedbackDialog.show(getFragmentManager(), "feedbackFrag");
    }


    void clickLogin() {
        isLoginMenuExpanded = !isLoginMenuExpanded;
        Menu menu = mNavigationView.getMenu();
        if (isLoginMenuExpanded) {
            menu.setGroupVisible(R.id.group_login, true);
            MenuItem menuLogin = menu.findItem(R.id.nav_login);
            MenuItem menuLogout = menu.findItem(R.id.nav_logout);
            if (SharedPrefsManager.getLoginCookie(prefs).isEmpty()) {
                menuLogin.setVisible(true);
                menuLogout.setVisible(false);
            } else {
                menuLogin.setVisible(false);
                menuLogout.setVisible(true);
            }
            ivExpander.setImageResource(R.drawable.expander_close);
        } else {
            menu.setGroupVisible(R.id.group_login, false);
            ivExpander.setImageResource(R.drawable.expander_open);
        }
    }

    void updateLoginName() {
        tvLoginName.setText(SharedPrefsManager.getUsername(prefs, this));
        Log.i("usename", tvLoginName.getText().toString());
    }

    public void setUpSpinnerPopularDateRange() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getApplicationContext(),
                R.array.time_range_popular, android.R.layout.simple_spinner_item);
        //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.spinner_drop_down_item_custom);
        mSpinnerDateRange.setAdapter(adapter);
        mSpinnerDateRange.setSelection(0);
        mSpinnerDateRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, final View view, int position, long id) {
                final PostFragment currentFrag = (PostFragment) getFragmentManager()
                        .findFragmentById(R.id.container);
                final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
                String secStart, secEnd;
                switch (position) {
                    case 0: // Past 24 hours
                        secEnd = String.valueOf(c.getTimeInMillis() / 1000);
                        c.add(Calendar.DAY_OF_YEAR, -1);
                        secStart = String.valueOf(c.getTimeInMillis() / 1000);
                        currentFrag.refresh(Constants.TYPE_SEARCH, "0" + secStart + secEnd);
                        break;
                    case 1: // Past 3 days
                        secEnd = String.valueOf(c.getTimeInMillis() / 1000);
                        c.add(Calendar.DAY_OF_YEAR, -3);
                        secStart = String.valueOf(c.getTimeInMillis() / 1000);
                        currentFrag.refresh(Constants.TYPE_SEARCH, "1" + secStart + secEnd);
                        break;
                    case 2: // Past 7 days
                        secEnd = String.valueOf(c.getTimeInMillis() / 1000);
                        c.add(Calendar.DAY_OF_YEAR, -7);
                        secStart = String.valueOf(c.getTimeInMillis() / 1000);
                        currentFrag.refresh(Constants.TYPE_SEARCH, "2" + secStart + secEnd);
                        break;
                    case 3: // Custom range
                        DateRangeDialogFragment newFragment = new DateRangeDialogFragment();
                        newFragment.show(getFragmentManager(), "datePicker");
                        newFragment.setOnDateSetListner(new DateRangeDialogFragment.onDateSetListener() {
                            @Override
                            public void onDateSet(final int startYear, final int startMonth,
                                                  final int startDay, final int endYear,
                                                  final int endMonth, final int endDay) {
                                c.set(startYear, startMonth, startDay, 0, 0, 0);
                                long startDate = c.getTimeInMillis() / 1000;
                                c.set(endYear, endMonth, endDay, 0, 0, 0);
                                long endDate = c.getTimeInMillis() / 1000;
                                if (endDate >= startDate) {
                                    Log.i(String.valueOf(startDate), String.valueOf(endDate + 86400));
                                    if (endDate == startDate) {
                                        ((TextView) view).setText(String.valueOf(startMonth + 1)
                                                + "/" + String.valueOf(startDay)
                                                + "/" + String.valueOf(startYear).substring(2));
                                    } else {
                                        ((TextView) view).setText(String.valueOf(startMonth + 1)
                                                + "/" + String.valueOf(startDay)
                                                + "/" + String.valueOf(startYear).substring(2)
                                                + " - " + String.valueOf(endMonth + 1)
                                                + "/" + String.valueOf(endDay)
                                                + "/" + String.valueOf(endYear).substring(2));
                                    }
                                    currentFrag.refresh(Constants.TYPE_SEARCH,
                                            "3" + String.valueOf(startDate) + String.valueOf(endDate + 86400));
                                } else {
                                    Log.i(String.valueOf(endDate), String.valueOf(startDate + 86400));

                                    ((TextView) view).setText(String.valueOf(endMonth + 1)
                                            + "/" + String.valueOf(endDay)
                                            + "/" + String.valueOf(endYear).substring(2)
                                            + " - " + String.valueOf(startMonth + 1)
                                            + "/" + String.valueOf(startDay)
                                            + "/" + String.valueOf(startYear).substring(2));

                                    currentFrag.refresh(Constants.TYPE_SEARCH,
                                            "3" + String.valueOf(endDate) + String.valueOf(startDate + 86400));
                                }
                            }
                        });
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        mSpinnerDateRange.setVisibility(View.VISIBLE);
    }

    public void setUpSpinnerPopularDateRange(int selection) {
        setUpSpinnerPopularDateRange();
        mSpinnerDateRange.setSelection(selection);
    }

    void setUpSpinnerSearchDateRange() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getApplicationContext(),
                R.array.time_range_search, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_drop_down_item_custom);
        mSpinnerDateRange.setAdapter(adapter);
        mSpinnerDateRange.setSelection(1);
        mSpinnerDateRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Fragment currentFrag = getFragmentManager().findFragmentById(R.id.container);
                if (currentFrag instanceof SearchFragment) {
                    final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
                    String secStart, secEnd;
                    secEnd = String.valueOf(c.getTimeInMillis() / 1000);
                    // the day before a year
                    secStart = String.valueOf(c.getTimeInMillis() / 1000 - 31536000);
                    switch (position) {
                        case 0: // All time
                            secEnd = String.valueOf(c.getTimeInMillis() / 1000);
                            // the time of first post
                            secStart = "1160418110";
                            if (((SearchFragment) currentFrag).getKeyword() != null
                                    && isSearchKeywordSubmitted) {
                                ((SearchFragment) currentFrag).refresh(secStart + secEnd);
                                mSearchView.clearFocus();
                            }
                            break;
                        case 1: // Last year
                            secEnd = String.valueOf(c.getTimeInMillis() / 1000);
                            c.add(Calendar.YEAR, -1);
                            secStart = String.valueOf(c.getTimeInMillis() / 1000);
                            if (((SearchFragment) currentFrag).getKeyword() != null
                                    && isSearchKeywordSubmitted) {
                                ((SearchFragment) currentFrag).refresh(secStart + secEnd);
                                mSearchView.clearFocus();
                            }
                            break;
                        case 2: // Last month
                            secEnd = String.valueOf(c.getTimeInMillis() / 1000);
                            c.add(Calendar.MONTH, -1);
                            secStart = String.valueOf(c.getTimeInMillis() / 1000);
                            if (((SearchFragment) currentFrag).getKeyword() != null
                                    && isSearchKeywordSubmitted) {
                                ((SearchFragment) currentFrag).refresh(secStart + secEnd);
                                mSearchView.clearFocus();
                            }
                            break;
                        case 3: // Custom range
                            setupDateRangePicker(view, currentFrag);
                            break;
                    }
                    ((SearchFragment) currentFrag).setDateRange(secStart + secEnd);
                    Log.i(secStart, secEnd);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    void setupDateRangePicker(final View dropDownView, final Fragment currentFrag) {
        final String[] dateRange = new String[2];
        final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
        DateRangeDialogFragment dateRangeDialogFragment = new DateRangeDialogFragment();
        dateRangeDialogFragment.show(getFragmentManager(), "datePicker");

        dateRangeDialogFragment.setOnDateSetListner(new DateRangeDialogFragment.onDateSetListener() {
            @Override
            public void onDateSet(final int startYear, final int startMonth,
                                  final int startDay, final int endYear,
                                  final int endMonth, final int endDay) {
                c.set(startYear, startMonth, startDay, 0, 0, 0);
                long startDate = c.getTimeInMillis() / 1000;
                c.set(endYear, endMonth, endDay, 0, 0, 0);
                long endDate = c.getTimeInMillis() / 1000;
                if (endDate >= startDate) {
                    // add one day to endDate, make it as next day 00:00
                    Log.i(String.valueOf(startDate), String.valueOf(endDate + 86400));
                    if (endDate == startDate) {
                        // month starts from 0
                        ((TextView) dropDownView).setText(String.valueOf(startMonth + 1)
                                + "/" + String.valueOf(startDay)
                                + "/" + String.valueOf(startYear).substring(2));
                    } else {
                        ((TextView) dropDownView).setText(String.valueOf(startMonth + 1)
                                + "/" + String.valueOf(startDay)
                                + "/" + String.valueOf(startYear).substring(2)
                                + " - " + String.valueOf(endMonth + 1)
                                + "/" + String.valueOf(endDay)
                                + "/" + String.valueOf(endYear).substring(2));
                    }
                    dateRange[0] = String.valueOf(startDate);
                    dateRange[1] = String.valueOf(endDate + 86400);
                } else { // endDate < startDate, use endDate as start
                    Log.i(String.valueOf(endDate), String.valueOf(startDate + 86400));
                    ((TextView) dropDownView).setText(String.valueOf(endMonth + 1)
                            + "/" + String.valueOf(endDay)
                            + "/" + String.valueOf(endYear).substring(2)
                            + " - " + String.valueOf(startMonth + 1)
                            + "/" + String.valueOf(startDay)
                            + "/" + String.valueOf(startYear).substring(2));
                    dateRange[0] = String.valueOf(endDate);
                    dateRange[1] = String.valueOf(startDate + 86400);
                }
                if (currentFrag instanceof PostFragment) {
                    ((PostFragment) currentFrag).refresh(Constants.TYPE_SEARCH,
                            dateRange[0] + dateRange[1]);
                } else if (currentFrag instanceof SearchFragment) {
                    ((SearchFragment) currentFrag).setDateRange(dateRange[0] + dateRange[1]);
                    if (((SearchFragment) currentFrag).getKeyword() != null
                            && isSearchKeywordSubmitted) {
                        ((SearchFragment) currentFrag).refresh(dateRange[0] + dateRange[1]);
                        mSearchView.clearFocus();
                    }
                }
            }
        });
    }

    void setUpSpinnerSortOrder() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getApplicationContext(),
                R.array.sort_order, android.R.layout.simple_spinner_item);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.spinner_drop_down_item_custom);
        mSpinnerSortOrder.setAdapter(adapter);
        mSpinnerSortOrder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Fragment currentFrag = getFragmentManager()
                        .findFragmentById(R.id.container);
                if (currentFrag instanceof SearchFragment) {
                    boolean isSortByDate = false;
                    switch (position) {
                        case 0: // popularity
                            isSortByDate = false;
                            break;
                        case 1: // date
                            isSortByDate = true;
                            break;
                    }
                    ((SearchFragment) currentFrag).setIsSortByDate(isSortByDate);
                    if (((SearchFragment) currentFrag).getKeyword() != null
                            && isSearchKeywordSubmitted) {
                        ((SearchFragment) currentFrag).refresh(isSortByDate);
                        mSearchView.clearFocus();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
        } else if (mSearchItem.isActionViewExpanded()) {
            mSearchItem.collapseActionView();
//            if (getFragmentManager().getBackStackEntryCount() > 0) {
//                Log.i("back pressed", "popback");
//                getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
//            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onOpenComment(Post post) {
        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra(Constants.KEY_POST_PARCEL, Parcels.wrap(post));
        startActivity(intent);
    }

    @Override
    public void onOpenLink(Post post) {
        if (mChromeCustomTabsHelper != null) {
            // build CustomTabs UI
            CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
            Utils.setupIntentBuilder(intentBuilder, this, prefs);
            // open comments section option
            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_comment);
            Intent goToCommentIntent = new Intent(this, CommentsActivity.class);
            goToCommentIntent.putExtra(Constants.KEY_POST_PARCEL, Parcels.wrap(post));
            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                    goToCommentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            intentBuilder.setActionButton(icon, getString(R.string.go_to_comment), pi);

            ChromeCustomTabsHelper.openCustomTab(this, intentBuilder.build(),
                    Utils.validateAndParseUri(post.getUrl(), post.getId()), null);
        } else {
            Intent urlIntent = new Intent(Intent.ACTION_VIEW);
            urlIntent.setData(Utils.validateAndParseUri(post.getUrl(), post.getId()));
            startActivity(urlIntent);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SharedPrefsManager.KEY_FAB_MODE)) {
            setupFAB();
        }
        if (key.equals(SharedPrefsManager.KEY_THEME)) {
            recreate();
        }
    }

    @Override
    public void onRecyclerViewCreate(RecyclerView recyclerView) {
        mFab.setRecyclerView(recyclerView);
        setupFAB();
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        if (getFragmentManager().findFragmentById(R.id.container) instanceof PostFragment) {
            ((PostFragment) getFragmentManager().findFragmentById(R.id.container))
                    .setSwipeRefreshLayoutState(i == 0);
        } else if (getFragmentManager().findFragmentById(R.id.container) instanceof SearchFragment) {
            ((SearchFragment) getFragmentManager().findFragmentById(R.id.container))
                    .setSwipeRefreshLayoutState(i == 0);
        }
    }
}
