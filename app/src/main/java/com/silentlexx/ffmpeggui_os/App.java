package com.silentlexx.ffmpeggui_os;

import android.util.Log;

import androidx.multidex.MultiDexApplication;

public class App extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        if (getResources() == null) {
            Log.e("APP", "app is replacing...kill");
            System.exit(1);
        }
    }
}