package com.silentlexx.ffmpeggui_os.config;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
	 static final int INT = 0;
	 static final boolean BOOL = false;
	private SharedPreferences preferences;

	
	Prefs(Context p, final String PREFS_STRING){
	    preferences = p.getSharedPreferences(PREFS_STRING, Context.MODE_MULTI_PROCESS);
	}
	


	 void set(String key, int value){
		preferences.edit().putInt(key, value).apply();
	}

	 void set(String key, String value){
		preferences.edit().putString(key, value).apply();
	}
	
	 void set(String key, boolean value){
		preferences.edit().putBoolean(key, value).apply();
	}
	
	public String get(String key, String defValue){
		return preferences.getString(key, defValue);
	}

	public int get(String key, int defValue){
		return preferences.getInt(key, defValue);
	}
	
	public boolean get(String key, boolean defValue){
		return preferences.getBoolean(key, defValue);
	}
	

}
