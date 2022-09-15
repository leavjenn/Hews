package com.leavjenn.hews.ui;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.WebView;

import com.leavjenn.hews.R;
import com.leavjenn.hews.misc.SharedPrefsManager;

public class NewVersionAnnounceActivity extends AppCompatActivity {
    private SharedPreferences prefs;

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
        setContentView(R.layout.activity_new_version_announce);
        //Setup Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("New version");
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        WebView webView = (WebView) findViewById(R.id.webview);
        webView.loadUrl("https://leavjenn.github.io/hews/the_next_page_of_hews");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
