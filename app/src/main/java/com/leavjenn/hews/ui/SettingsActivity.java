package com.leavjenn.hews.ui;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.WindowManager;

import com.leavjenn.hews.misc.ChromeCustomTabsHelper;
import com.leavjenn.hews.R;
import com.leavjenn.hews.misc.SharedPrefsManager;

public class SettingsActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(getResources().getColor(android.R.color.black));
            }
        }
        setContentView(R.layout.activity_settings);
        //Setup Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Settings");
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            SettingsFragment fragment = new SettingsFragment();
            getFragmentManager().beginTransaction().add(R.id.container, fragment).commit();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            FragmentManager fm = getFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } else {
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("themekey")) {
            Intent intent = new Intent(this, SettingsActivity.class);
            // Finish this activity and start it again
            startActivity(intent);
            finish();
            // set animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }


    public static class SettingsFragment extends PreferenceFragment {

        public SettingsFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            if (ChromeCustomTabsHelper.getPackageNameToUse(getActivity()) == null) {
                disableOpenLinkOptionPreference();
            }
        }

        private void disableOpenLinkOptionPreference() {
            CheckBoxPreference openLinkOptionPref = (CheckBoxPreference) findPreference(
                    getString(R.string.pref_key_open_link_option));
            openLinkOptionPref.setEnabled(false);
            openLinkOptionPref.setChecked(false);
            openLinkOptionPref.setSummary(R.string.summary_open_link_option_unavailable);
        }
    }
}