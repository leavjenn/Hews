package com.leavjenn.hews.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.leavjenn.hews.data.local.LocalDataManager;
import com.leavjenn.hews.misc.ChromeCustomTabsHelper;
import com.leavjenn.hews.R;
import com.leavjenn.hews.misc.SharedPrefsManager;
import com.leavjenn.hews.model.Post;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class SettingsActivity extends AppCompatActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int RC_WRITE_STORAGE = 1234;

    private SharedPreferences prefs;

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

            Preference exportBookmark = findPreference(getString(R.string.pref_key_export_bookmark));
            exportBookmark.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
                        && ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            new AlertDialog.Builder(getActivity())
                                .setMessage("Write storage permission is required for export bookmark.")
                                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RC_WRITE_STORAGE);
                                })
                                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                                    Toast.makeText(getActivity(),
                                        "App cannot export bookmark without write storage permission", Toast.LENGTH_LONG).show();
                                })
                                .show();
                        } else {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RC_WRITE_STORAGE);
                        }
                    } else {
                        exportBookmark();
                    }
                    return false;
                }
            });
        }

        private void disableOpenLinkOptionPreference() {
            CheckBoxPreference openLinkOptionPref = (CheckBoxPreference) findPreference(
                getString(R.string.pref_key_open_link_option));
            openLinkOptionPref.setEnabled(false);
            openLinkOptionPref.setChecked(false);
            openLinkOptionPref.setSummary(R.string.summary_open_link_option_unavailable);
        }

        private void exportBookmark() {
            CompositeSubscription exportSubscription = new CompositeSubscription();
            exportSubscription.add(new LocalDataManager(getActivity()).getAllPostsFromDb()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Post>>() {
                    @Override
                    public void call(List<Post> posts) {
                        try {
                            String fileName = "Hews_bookmark_" + Calendar.getInstance().getTimeInMillis() + ".csv";

                            String path;
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + fileName;
                            } else {
                                path = getActivity().getFilesDir().getAbsolutePath() + "/" + fileName;
                            }

                            File csv = new File(path);

                            CSVWriter writer;
                            writer = new CSVWriter(new FileWriter(csv));
                            List<String[]> data = new ArrayList<>();
                            data.add(new String[]{"ID", "Title", "Url"});
                            for (Post post : posts) {
                                data.add(new String[]{"" + post.getId(), post.getTitle(), post.getUrl()});
                            }
                            writer.writeAll(data);
                            writer.close();

                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                                ContentResolver contentResolver = getActivity().getContentResolver();
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/comma-separated-values");
                                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                                Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                                Uri fileUri = contentResolver.insert(collection, contentValues);

                                final OutputStream outStream = contentResolver.openOutputStream(fileUri);
                                FileInputStream inStream = new FileInputStream(path);
                                byte[] buffer = new byte[1024];
                                int lengthRead;
                                while ((lengthRead = inStream.read(buffer)) > 0) {
                                    outStream.write(buffer, 0, lengthRead);
                                    outStream.flush();
                                }
                            }
                            Toast.makeText(getActivity(), "Export successfully. Path: Downloads/" + fileName, Toast.LENGTH_LONG).show();
                            exportSubscription.clear();
                        } catch (Exception e) {
                            Toast.makeText(getActivity(), "Export failed: " + e, Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                            exportSubscription.clear();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Toast.makeText(getActivity(), "Export failed: " + throwable, Toast.LENGTH_LONG).show();
                        exportSubscription.clear();
                    }
                }));
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            if (requestCode == RC_WRITE_STORAGE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    exportBookmark();
                } else {
                    Toast.makeText(getActivity(),
                        "App cannot export bookmark without write storage permission", Toast.LENGTH_LONG).show();
                }
            } else {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }
}