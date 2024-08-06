package com.silentlexx.ffmpeggui_os.model;

import com.silentlexx.ffmpeggui_os.config.Config;

public class Preset {

	private static int sysPresets = 0;

	private String name;
	private String args;
	private String type;
	private int pos = 0;

	public Preset(String n, String a, String t){
		name=n;
		args=a;
		type=t;
		init();
	}

	public Preset(String str){
		String arr[] = str.split("\\|");
		if(arr.length >= 3) {
			name = arr[0];
			args = arr[1];
			type = arr[2];

		}
		init();
	}

	private void init(){
		if(args == null || args.isEmpty()){
			args = " ";
		}
		if(type.matches("\\d+")){
			pos = Config.getOLdExtPosition(Integer.parseInt(type));
		} else {
			pos = Config.getExtPosition(type);
		}


	}
	
	public void setType(String t){
		type = t;
		init();
	}

	public void setArgs(String a){
		args = a;
		init();
	}

	public String getName(){
		return name;
	}

	public String getArgs(){
		return args;
	}

	public int getPos(){
		return pos;
	}

	public String getType(){
		return type;
	}

	public boolean isOk(){
		return name !=null && !name.isEmpty()  && type!=null && !type.isEmpty();
	}

	public static void setSysPresetsCount(int count){
		sysPresets = count;
	}

	public static int getSysPresetsCount(){
		return sysPresets;
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("name: ");
		sb.append(name);
		sb.append(", args: ");
		sb.append(args);
		sb.append(", type: ");
		sb.append(type);
		return sb.toString();
	}
}
