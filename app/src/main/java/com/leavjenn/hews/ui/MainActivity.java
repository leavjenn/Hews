package com.leavjenn.hews.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.firebase.client.Firebase;
import com.leavjenn.hews.Constants;
import com.leavjenn.hews.R;
import com.leavjenn.hews.SharedPrefsManager;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.ui.adapter.PostAdapter;
import com.leavjenn.hews.ui.widget.AlwaysShowDialogSpinner;
import com.leavjenn.hews.ui.widget.DateRangeDialogFragment;
import com.leavjenn.hews.ui.widget.PopupFloatingWindow;

import java.util.Calendar;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity implements PostAdapter.OnItemClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private DrawerLayout mDrawerLayout;
    private static final long DRAWER_CLOSE_DELAY_MS = 250;
    private final Handler mDrawerActionHandler = new Handler();
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar toolbar;
    private AlwaysShowDialogSpinner mSpinnerTimeRange;
    private Spinner mSpinnerSortOrder;
    private PopupFloatingWindow mWindow;
    private SearchView mSearchView;
    private MenuItem mSearchItem;
    private String mStoryType, mStoryTypeSpec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set theme
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = SharedPrefsManager.getTheme(prefs);
        if (theme.equals(SharedPrefsManager.THEME_SEPIA)) {
            setTheme(R.style.AppTheme_Sepia);
        }
        if (theme.equals(SharedPrefsManager.THEME_DARK)) {
            setTheme(R.style.AppTheme_Dark);
        }
        setContentView(R.layout.activity_main);
        Firebase.setAndroidContext(this);
        // setup Toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // set up time range spinner
        mSpinnerTimeRange = (AlwaysShowDialogSpinner) findViewById(R.id.spinner_time_range);
        mSpinnerSortOrder = (Spinner) findViewById(R.id.spinner_sort_order);
        setUpSpinnerPopularTimeRange();
        setUpSpinnerSortOrder();
        //setup drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            setupDrawerContent(navigationView);
        }
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.open_drawer,
                R.string.close_drawer);

        mStoryType = Constants.STORY_TYPE_TOP_URL;
        mStoryTypeSpec = Constants.STORY_TYPE_TOP_URL;
        mWindow = new PopupFloatingWindow(this, toolbar);

        PostFragment postFragment = PostFragment.newInstance(mStoryType, mStoryTypeSpec);
        getSupportFragmentManager().beginTransaction().add(R.id.container, postFragment).commit();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
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
                //mSpinnerTimeRange.setVisibility(View.VISIBLE);
                //mSpinnerSortOrder.setVisibility(View.VISIBLE);
                //Intent urlIntent = new Intent(Intent.ACTION_VIEW);
                //String url = "https://hn.algolia.com/";
                //urlIntent.setData(Uri.parse(url));
                //startActivity(urlIntent);
//                mSearchView.setIconified(false);
//                mDrawerToggle.setDrawerIndicatorEnabled(false);
                break;

            case R.id.action_refresh:
                PostFragment currentFrag = (PostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.container);
                currentFrag.refresh(currentFrag.getStoryType(), currentFrag.getStoryTypeSpec());
                break;

            case R.id.action_display:
                if (!mWindow.isWindowShowing()) {
                    mWindow.show();
                } else {
                    mWindow.dismiss();
                }
                break;

            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
        }
        return super.onOptionsItemSelected(item);
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
                mSpinnerSortOrder.setVisibility(View.VISIBLE);
                mSpinnerTimeRange.setVisibility(View.VISIBLE);
                setUpSpinnerSearchTimeRange();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mSpinnerSortOrder.setVisibility(View.GONE);
                mSpinnerTimeRange.setVisibility(View.GONE);
                return true;
            }
        });
        SearchView.OnQueryTextListener onQueryTextListener = new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
                String secEnd = String.valueOf(c.getTimeInMillis() / 1000);
                c.add(Calendar.YEAR, -1);
                String secStart = String.valueOf(c.getTimeInMillis() / 1000);
                SearchFragment searchFragment = SearchFragment.newInstance(query, secStart + secEnd, false);
                FragmentTransaction transaction = MainActivity.this.getSupportFragmentManager()
                        .beginTransaction();
                transaction.replace(R.id.container, searchFragment);
                transaction.addToBackStack(null);
                transaction.commit();
                getSupportFragmentManager().executePendingTransactions();
                searchFragment.refresh(query, secStart + secEnd, false);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
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
                                mStoryTypeSpec = Constants.STORY_TYPE_TOP_URL;
                                break;
                            case R.id.nav_new_story:
                                mStoryTypeSpec = Constants.STORY_TYPE_NEW_URL;
                                break;
                            case R.id.nav_ask_hn:
                                mStoryTypeSpec = Constants.STORY_TYPE_ASK_HN_URL;
                                break;
                            case R.id.nav_show_hn:
                                mStoryTypeSpec = Constants.STORY_TYPE_SHOW_HN_URL;
                                break;
                            case R.id.nav_popular:
                                mStoryTypeSpec = Constants.TYPE_SEARCH;
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
                                    PostFragment currentFrag = (PostFragment) getSupportFragmentManager()
                                            .findFragmentById(R.id.container);
                                    Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
                                    String secEnd = String.valueOf(c.getTimeInMillis() / 1000);
                                    c.add(Calendar.DAY_OF_YEAR, -1);
                                    String secStart = String.valueOf(c.getTimeInMillis() / 1000);
                                    currentFrag.refresh(Constants.TYPE_SEARCH, secStart + secEnd);

                                    setUpSpinnerPopularTimeRange();
                                    mSpinnerTimeRange.setVisibility(View.VISIBLE);
                                    mSpinnerTimeRange.setSelection(0);
                                } else {
                                    menuItem.setChecked(true);
                                    PostFragment currentFrag = (PostFragment) getSupportFragmentManager()
                                            .findFragmentById(R.id.container);
                                    currentFrag.refresh(Constants.TYPE_STORY, mStoryTypeSpec);
                                    mSpinnerTimeRange.setVisibility(View.GONE);
                                }

                            }
                        };

                        mDrawerActionHandler.postDelayed(r, DRAWER_CLOSE_DELAY_MS);
                        return true;
                    }
                }
        );
    }

    void setUpSpinnerPopularTimeRange() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getApplicationContext(),
                R.array.time_range_popular, android.R.layout.simple_spinner_item);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.spinner_drop_down_item_custom);
        mSpinnerTimeRange.setAdapter(adapter);
        mSpinnerTimeRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, final View view, int position, long id) {
                final PostFragment currentFrag = (PostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.container);
                final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
                String secStart, secEnd;
                switch (position) {
                    case 0: // Past 24 hours
                        secEnd = String.valueOf(c.getTimeInMillis() / 1000);
                        c.add(Calendar.DAY_OF_YEAR, -1);
                        secStart = String.valueOf(c.getTimeInMillis() / 1000);
                        currentFrag.refresh(Constants.TYPE_SEARCH, secStart + secEnd);
                        break;
                    case 1: // Past 3 days
                        secEnd = String.valueOf(c.getTimeInMillis() / 1000);
                        c.add(Calendar.DAY_OF_YEAR, -3);
                        secStart = String.valueOf(c.getTimeInMillis() / 1000);
                        currentFrag.refresh(Constants.TYPE_SEARCH, secStart + secEnd);
                        break;
                    case 2: // Past 7 days
                        secEnd = String.valueOf(c.getTimeInMillis() / 1000);
                        c.add(Calendar.DAY_OF_YEAR, -7);
                        secStart = String.valueOf(c.getTimeInMillis() / 1000);
                        currentFrag.refresh(Constants.TYPE_SEARCH, secStart + secEnd);
                        break;
                    case 3: // Custom range
                        DateRangeDialogFragment newFragment = new DateRangeDialogFragment();
                        newFragment.show(getSupportFragmentManager(), "datePicker");
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
                                        // month starts from 0
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
                                            String.valueOf(startDate) + String.valueOf(endDate + 86400));
                                } else {
                                    Log.i(String.valueOf(endDate), String.valueOf(startDate + 86400));
                                    ((TextView) view).setText(String.valueOf(endMonth + 1)
                                            + "/" + String.valueOf(endDay)
                                            + "/" + String.valueOf(endYear).substring(2)
                                            + " - " + String.valueOf(startMonth + 1)
                                            + "/" + String.valueOf(startDay)
                                            + "/" + String.valueOf(startYear).substring(2));
                                    currentFrag.refresh(Constants.TYPE_SEARCH,
                                            String.valueOf(endDate) + String.valueOf(startDate + 86400));
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
    }

    String setupDatePicker(final View dropDownView) {
        final StringBuffer sb = new StringBuffer();
        final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
        DateRangeDialogFragment dateRangeDialogFragment = new DateRangeDialogFragment();
        dateRangeDialogFragment.show(getSupportFragmentManager(), "datePicker");

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
                    sb.append(startDate);
                    sb.append(endDate + 86400);
                } else {
                    Log.i(String.valueOf(endDate), String.valueOf(startDate + 86400));
                    ((TextView) dropDownView).setText(String.valueOf(endMonth + 1)
                            + "/" + String.valueOf(endDay)
                            + "/" + String.valueOf(endYear).substring(2)
                            + " - " + String.valueOf(startMonth + 1)
                            + "/" + String.valueOf(startDay)
                            + "/" + String.valueOf(startYear).substring(2));
                    sb.append(endDate);
                    sb.append(startDate + 86400);
                }
            }
        });
        Log.i("sb", sb.toString());
        return sb.toString();
    }

    void setUpSpinnerSearchTimeRange() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getApplicationContext(),
                R.array.time_range_search, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_drop_down_item_custom);
        mSpinnerTimeRange.setAdapter(adapter);
        mSpinnerTimeRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Fragment currentFrag = getSupportFragmentManager().findFragmentById(R.id.container);
                if (currentFrag instanceof SearchFragment) {
                    SearchFragment currentSearchFrag = (SearchFragment) currentFrag;
                    final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
                    String secStart, secEnd;
                    switch (position) {
                        case 0: // All time
                            break;
                        case 1: // Last year
                            secEnd = String.valueOf(c.getTimeInMillis() / 1000);
                            c.add(Calendar.YEAR, -1);
                            secStart = String.valueOf(c.getTimeInMillis() / 1000);
                            currentSearchFrag.refresh(secStart + secEnd);
                            break;
                        case 2: // Last month
                            secEnd = String.valueOf(c.getTimeInMillis() / 1000);
                            c.add(Calendar.YEAR, -1);
                            secStart = String.valueOf(c.getTimeInMillis() / 1000);
                            currentSearchFrag.refresh(secStart + secEnd);
                            break;
                        case 3: // Custom range
                            setupDatePicker(view);
                            break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

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
                Fragment curFrag = getSupportFragmentManager()
                        .findFragmentById(R.id.container);
                switch (position) {
                    case 0: // popularity
                        if (curFrag instanceof SearchFragment) {
                            ((SearchFragment) curFrag).refresh(false);
                        }
                        break;
                    case 1: // date
                        if (curFrag instanceof SearchFragment) {
                            ((SearchFragment) curFrag).refresh(true);
                        }
                        break;
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
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                Log.i("back pressed", "popback");
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onItemClick(Post post) {
        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra(Constants.KEY_POST, post);
        startActivity(intent);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }
}
