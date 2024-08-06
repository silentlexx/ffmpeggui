package com.silentlexx.ffmpeggui_os.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.silentlexx.ffmpeggui_os.R;

public class Settings extends PreferenceActivity
implements SharedPreferences.OnSharedPreferenceChangeListener {

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getPreferenceManager().setSharedPreferencesName(
           Gui.SHARED_PREFS_NAME);
    addPreferencesFromResource(R.xml.sets);
    getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(
            this);
    /*
    CheckBoxPreference pref = (CheckBoxPreference)findPreference("forcelegacy");
    if(Config.isNewSDK()){
        pref.setChecked(false);
        pref.setEnabled(false);
    } else if(Config.isLegacy()){
        pref.setChecked(true);
        pref.setEnabled(false);
    }
    */

  // 	pref.setEnabled(new Root().isDeviceRooted());
}

@Override
protected void onResume() {
    super.onResume();
}

@Override
protected void onDestroy() {
    getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
            this);
    super.onDestroy();
}

public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
        String key) {
   // new Config(this);
}
}