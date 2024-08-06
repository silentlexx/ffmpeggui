package com.silentlexx.ffmpeggui_os.config;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.silentlexx.ffmpeggui_os.BuildConfig;
import com.silentlexx.ffmpeggui_os.R;
import com.silentlexx.ffmpeggui_os.activities.Gui;
import com.silentlexx.ffmpeggui_os.model.FFmpeg;
import com.silentlexx.ffmpeggui_os.model.FFmpegChooser;
import com.silentlexx.ffmpeggui_os.model.Preset;
import com.silentlexx.ffmpeggui_os.parts.Bin;
import com.silentlexx.ffmpeggui_os.utils.FileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Config implements ConfigInterface {

	public static final String INT_FFMPEG_VER = "6.0.0";

	public static final boolean NEW_FS_SYSTEM = true;

	public static final boolean USE_CHECK_BY_PROCESS_ONLY = false;

	public static final boolean TESTING_ADS = false; //FIXME

	public static final boolean MULTIWIN = true;

	public static final boolean FINISH_ON_CLOSE = true;
	private static final int AD_OPEN_COUNT = -1; //if -1 == off


	public static final boolean EXPEREMENTAL = false;

	public static final boolean DEBUG = BuildConfig.DEBUG;

	public static final String BACKUP_EXT = ".json";

	public static final String ARCH = System.getProperty("os.arch");

	public static boolean isLegacy() {
		return Build.VERSION.SDK_INT < 28;
	}

	public static boolean isNonLegacy() {
		return Build.VERSION.SDK_INT >= 29;
	}

	public static boolean isNewSDK(){
		return Build.VERSION.SDK_INT >= 30;
	}

	public static String  getAbi(){
		if(Build.VERSION.SDK_INT >= 21) {
			return Build.SUPPORTED_ABIS[0];
		} else {
			if(ARCH.contains("arm")){
				return "armeabi-v7a";
			} else {
				return "x86";
			}

		}

	}


	//private static final String SD = Context.getExternalFilesDir();
	//Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
	private static final String SD_OLD = Environment.getExternalStorageDirectory().getAbsolutePath();

	private static List<String> extList = new ArrayList<>();


	private static final String[] FILE_EXT = {
			"mp4", "ogg", "avi", "mp3",  "wav",
			"flv", "mpg", "3gp", "flac", "mkv",
			"mpeg", "wmv", "dv", "wma",  "acc",
			"amr", "asf", "ogv", "amv",   "rm",
			"m4v", "m4a", "m4b", "aac",  "jpg",
			"png", "bmp", "gif", "webm", "webp",
			"mov" ,"opus","pcm", "raw",  "jpeg",
			"awb", "adts", "ass" , "ast" , "ts" };

	public static final long DOUBLE_CL_TIME = 1000;

	//private static final String SD_CARD_DIR = Build.VERSION.SDK_INT >= 30 ? SD + "/" + Config.APP_DIR :  SD_OLD + "/" + Config.APP_DIR;
	//public static final String SAMPLE_PATH = SD_CARD_DIR + "/" + Config.DEF_IN_FILE;
	//public static final String TEMP_PATH = SD_CARD_DIR + "/temp";
	//public static final String EXT_BIN_PATH = SD_CARD_DIR + "/bin";
	//public static final String BACKUP_PATH = SD_CARD_DIR + "/backup";
	//public static final String DOCDIR = SD;

	public static String getSdCardPath(Context c){
		String sd = c.getExternalFilesDir("").getAbsolutePath();
		//return SD_OLD + "/" + Config.APP_DIR;
		return Build.VERSION.SDK_INT >= 30 ? sd  :  SD_OLD + "/" + Config.APP_DIR;
	}

	public static String getSamplePath(Context c, boolean isOut){
		if(isOut){
			return FileUtil.getOutputPath(c) + "/" + Config.DEF_IN_FILE;
		}	else  {
			return getSdCardPath(c) + "/" + Config.DEF_IN_FILE;
		}
	}

	public static String getTempPath(Context c){
		return c.getExternalCacheDir().getAbsolutePath()  ;
	}

	public static String getExtBinPath(Context c){
		return getSdCardPath(c) + "/bin";
	}

	public static String getDownloadPath(){
		return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
	}

	private Prefs prefs;
	private Context context;
	private FFmpegChooser ffmpeg;

	public Config(Context c){
		context = c;

		prefs = new Prefs(context, Gui.SHARED_PREFS_NAME);
		if(BuildConfig.VERSION_CODE != getInt(VERSION_CODE)){
			FFmpegChooser.removeBin(context);
			set(VERSION_CODE, BuildConfig.VERSION_CODE);
		}
		initFfmpeg();

		//FIXME
		//set(CURRENT_BIN, ffmpeg.getCurrentCode());
		//set(CURRENT_BIN_NAME, ffmpeg.getCurrentName());
	}


	public void initFfmpeg() {
		if(ffmpeg ==  null)
			ffmpeg = new FFmpegChooser(context, getInt(CURRENT_BIN_INT), get(CURRENT_BIN_NAME, ""));
	}

	public void clearFfmpeg() {
		ffmpeg = null;
	}

	public boolean isMute(){
		return getBool(MUTE);
	}

	public List<Preset> getPresets(){
		return getPresets(false);
	}

	public List<Preset> getPresets(boolean extOnly){
		List<Preset> pres = new ArrayList<>();
		List<String> name = new ArrayList<>();

		if(!extOnly) {
			String[] presets = context.getResources().getStringArray(R.array.presets);
			for (String preset : presets) {
				Preset p = new Preset(preset);
				if (p.isOk()) {
					pres.add(p);
					name.add(p.getName());
				}
			}
			pres.add(new Preset(BLANK, "", "mp4"));
			Preset.setSysPresetsCount(pres.size());
		}

		String[] name2 = getArrayOfArray(PRESET_NAME);
		String[] agrs2 = getArrayOfArray(PRESET_PATERN);
		String[] type2 = getArrayOfArray(PRESET_EXT);
		for(int i=0; i<name2.length;i++){
			if(checkName(name, name2[i]))pres.add(new Preset(name2[i], agrs2[i], type2[i]));
		}
		return pres;
		
	}

	public void setPresets(List<Preset> pres){
		setPresets(pres, Preset.getSysPresetsCount());
	}

	public void setPresets(List<Preset> pres, final int offset){
		final int n = pres.size() - offset;
		String[] name = new String[n];
		String[] agrs = new String[n];
		String[] type = new String[n];
		for(int i=0; i < n ; i++ ){
			name[i]=pres.get(i + offset).getName();
			agrs[i]=pres.get(i + offset).getArgs();
			type[i]=pres.get(i + offset).getType();
		}
		
		 setArrayOfArray(PRESET_NAME, name);
		 setArrayOfArray(PRESET_PATERN, agrs);
		 setArrayOfArray(PRESET_EXT, type);
	}


	public boolean savePresetsToFile(Context c, final Uri u){
		List<Preset> presets = getPresets(true);
		if(presets.size() < 1) {
			return false;
		}
		JSONArray array = new JSONArray();
		for(Preset preset : presets){
			try {
				JSONObject object = new JSONObject();
				object.put(PRESET_NAME, preset.getName());
				object.put(PRESET_PATERN, preset.getArgs());
				object.put(PRESET_EXT, preset.getType());
				array.put(object);
			} catch (JSONException e){
				e.printStackTrace();
			}
		}

		String text = array.toString();

		return FileUtil.saveStringToFile(c, text, u);
	}

	public boolean restorePresetsFromFile(Context c, final Uri u){
		try {
			String text = FileUtil.getStringFromFile(c, u);
			JSONArray array = new JSONArray(text);
			List<Preset> presets = new ArrayList<>();
			for(int i = 0 ; i < array.length(); i++){
				JSONObject object = array.getJSONObject(i);
				Preset preset = new Preset(
						object.getString(PRESET_NAME),
						object.getString(PRESET_PATERN),
						object.getString(PRESET_EXT)
				);
				//Log.d("DB", preset.toString());
				presets.add(preset);
			}
			setPresets(presets, 0);
		} catch (Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}


	public String[] getNamesArray(List<Preset> pres){
		String[] arr = new String[pres.size()];
		for(int i=0;i<pres.size();i++){
			arr[i] = pres.get(i).getName();
		}
		return arr;
	}
	
	private boolean checkName(List<String> list, String name){
		for (String str : list) {
			if (str.equals(name) || name.equals(BLANK)) return false;
		}
		return true;
	}
	
	public void setEnd(boolean is, boolean end, String log){
		prefs.set(END_MSG, is);
		prefs.set(END_STAT, end);
		prefs.set(END_LOG, log);
	}
	
	public boolean[] getEnd(){
		boolean b[] = new boolean[3];
		b[0]= prefs.get(END_MSG, false);
		b[1]= prefs.get(END_STAT, false);
		return b;
	}

	public void set(String key, String value){
		prefs.set(key, value);
	}

	public void set(String key, boolean value){
		prefs.set(key, value);
	}

	public int getInt(String key){
		return  prefs.get(key, Prefs.INT);
	}

	public boolean getBool(String key){
		return  prefs.get(key, Prefs.BOOL);
	}
	
	
	public void set(String key, int value){
		prefs.set(key, value);
	}

	public boolean get(String key, boolean defValue){
		return  prefs.get(key, defValue);
	}

	public String get(String key, String defValue){
		return  prefs.get(key, defValue);
	}
	
	public String get(String key){
		return  prefs.get(key, "");
	}

	private String[] getArrayOfArray(String key){
		List<String> list = readArray(key); 
		int size = list.size();
		String array[] = new String[size];
		for(int i = 0; i < size; i++){
			array[i] = list.get(i);
		}
		return array;
	}
	


	private List<String> readArray(String key){
		int size = getInt(key+SIZE);
		List<String> list = new ArrayList<>();
		for(int i = 0; i < size; i++){
			list.add(get(key+Integer.toString(i)));
		}
		return list;
	}
	
	private void  setArrayOfArray(String key, String[] list){
		int size = list.length;
		for(int i = 0; i < size; i++){
		   prefs.set(key+Integer.toString(i), list[i]);
		}		
		prefs.set(key+SIZE, size);
	}




	public FFmpegChooser getCurrent(){
		return ffmpeg;
	}



	public FFmpeg getFFmpeg(int i){
		return ffmpeg.get(i);
	}

	public boolean setCurrentBin(int i){
		if(ffmpeg.setCurrent(i)) {
			set(CURRENT_BIN_INT, ffmpeg.getCurrentCode());
			set(CURRENT_BIN_NAME, ffmpeg.getCurrentName());
			set(CURRENT_BIN_PATH, ffmpeg.getDistBin());
			set(CURRENT_BIN_FILE, ffmpeg.getBin());
			return true;
		}
		return false;
	}

	public static String[] getExtensions(Context context){
		//TODO
		if(extList.size()<1){
			extList = Bin.getExtList(context);

			for (String hard : FILE_EXT){
				boolean find = false;
				for(String e : extList){
					if(hard.equals(e)) {
						find = true;
						break;
					}
				}
				if(!find){
					extList.add(hard);
				}
			}
			Collections.sort(extList);
		}

		return extList.toArray(new String[0]);
	}

	public static int getExtPosition(String ext){
		int i = 0;
		for(String e : extList){
			if(ext.toLowerCase().equals(e)){
				return i;
			}
			i++;
		}
		return 0;
	}

	/*
	public static String getBackupFilePath(String name){
		return BACKUP_PATH + "/" + name;
	}
	*/

	public static int getOLdExtPosition(int old){
		try {
			return getExtPosition(FILE_EXT[old]);
		} catch (Exception e){
			return 0;
		}

	}



}
