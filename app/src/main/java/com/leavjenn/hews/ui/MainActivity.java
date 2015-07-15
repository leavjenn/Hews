package com.leavjenn.hews.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.firebase.client.Firebase;
import com.leavjenn.hews.Constants;
import com.leavjenn.hews.R;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.ui.adapter.PostAdapter;
import com.leavjenn.hews.ui.widget.PopupFloatingWindow;


public class MainActivity extends AppCompatActivity implements PostAdapter.OnItemClickListener {

    private DrawerLayout mDrawerLayout;
    private static final long DRAWER_CLOSE_DELAY_MS = 250;
    private final Handler mDrawerActionHandler = new Handler();
    private ActionBarDrawerToggle mDrawerToggle;
    private PopupFloatingWindow mWindow;
    //    private SearchView mSearchView;
    String mStoryType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Firebase.setAndroidContext(this);
        // setup Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        //setup drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            setupDrawerContent(navigationView);
        }
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.open_drawer,
                R.string.close_drawer);
        mStoryType = Constants.KEY_TOP_STORIES_URL;
        mWindow = new PopupFloatingWindow(this,toolbar);
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
//        MenuItem searchItem = menu.findItem(R.id.action_search);
//        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_search:
                Intent urlIntent = new Intent(Intent.ACTION_VIEW);
                String url = "https://hn.algolia.com/";
                urlIntent.setData(Uri.parse(url));
                startActivity(urlIntent);
//                mSearchView.setIconified(false);
//                mDrawerToggle.setDrawerIndicatorEnabled(false);
                break;

            case R.id.action_refresh:
                PostFragment currentFrag = (PostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.frag_post_list);
                currentFrag.refresh();
                break;

            case R.id.action_display:
                if(!mWindow.isWindowShowing()){
                    mWindow.show();
                }else{
                    mWindow.dismiss();
                }
                break;

            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(final MenuItem menuItem) {
                        final int type = menuItem.getItemId();
                        switch (type) {
                            case R.id.nav_top_story:
                                mStoryType = Constants.KEY_TOP_STORIES_URL;
                                break;

                            case R.id.nav_new_story:
                                mStoryType = Constants.KEY_NEW_STORIES_URL;
                                break;

                            case R.id.nav_ask_hn:
                                mStoryType = Constants.KEY_ASK_HN_URL;
                                break;

                            case R.id.nav_show_hn:
                                mStoryType = Constants.KEY_SHOW_HN_URL;
                                break;

                            case R.id.nav_settings:
                                break;
                        }
                        // allow some time after closing the drawer before performing real navigation
                        // so the user can see what is happening
                        mDrawerLayout.closeDrawer(GravityCompat.START);
                        mDrawerActionHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (type != R.id.nav_settings) {
                                    menuItem.setChecked(true);
                                    PostFragment currentFrag = (PostFragment) getSupportFragmentManager()
                                            .findFragmentById(R.id.frag_post_list);
                                    currentFrag.updateStoryFeed(mStoryType);
                                } else {
                                    Intent i = new Intent(getBaseContext(), SettingsActivity.class);
                                    startActivity(i);
                                }
                            }
                        }, DRAWER_CLOSE_DELAY_MS);
                        return true;
                    }
                });
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
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
}
