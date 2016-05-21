package com.leavjenn.hews;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import io.fabric.sdk.android.Fabric;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
//        Fabric.with(this, new Crashlytics());
        // disable Crashlytics while debugging
        CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build();
        Fabric.with(this, new Crashlytics.Builder().core(core).build());
    }
}
