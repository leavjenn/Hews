package com.leavjenn.hews.ui;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.firebase.client.Firebase;
import com.leavjenn.hews.Constants;
import com.leavjenn.hews.R;
import com.leavjenn.hews.SharedPrefsManager;
import com.leavjenn.hews.listener.OnRecyclerViewCreateListener;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.ui.widget.FloatingScrollDownButton;
import com.leavjenn.hews.ui.widget.PopupFloatingWindow;


public class CommentsActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,OnRecyclerViewCreateListener {
    private PopupFloatingWindow mWindow;
    private FloatingScrollDownButton mFab;
    private Post mPost;
    private SharedPreferences prefs;

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
            mPost = intent.getParcelableExtra(Constants.KEY_POST);
            commentsFragment = CommentsFragment.newInstance(mPost);
        }

        final Uri data = intent.getData();
        if (data != null) {
            long storyId = Long.parseLong(data.getQueryParameter("id"));
            commentsFragment = CommentsFragment.newInstance(storyId);
        }

        if (savedInstanceState == null) {
            if (commentsFragment != null) {
                getFragmentManager().beginTransaction()
                        .add(R.id.container, commentsFragment, "CommentFragTag")
                        .commit();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mWindow.isWindowShowing()) {
            mWindow.dismiss();
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

            case R.id.action_open_post:
                mPost.getUrl();
                Intent urlIntent = new Intent(Intent.ACTION_VIEW);
                String url = mPost.getUrl();
                urlIntent.setData(Uri.parse(url));
                startActivity(urlIntent);
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
                        + mPost.getId();
                sendIntent.putExtra(Intent.EXTRA_TEXT, commentUrl);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_link_to)));
                break;

            case R.id.action_display:
                if(!mWindow.isWindowShowing()){
                    mWindow.show();
                }else{
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
