package com.silentlexx.ffmpeggui_os.utils;


import android.text.TextUtils;

import com.silentlexx.ffmpeggui_os.model.TextFragment;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class StrUtil {


	public static String strings(String... str){
		StringBuilder sb = new StringBuilder();
		for (String aStr : str) {
			sb.append(aStr);
		}
		return sb.toString();		
	}
	

	
	private static String intToStr(int n, int zero){
		String s = Integer.toString(n);
		if(s.length()<zero){
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < zero-1; i++) sb.append("0");
			sb.append(s);
			s = sb.toString();
		}
		return s;		
	}
	
	public static String msToTime(long milliseconds){
		int seconds = (int) (milliseconds / 1000) % 60 ;
		int minutes = (int) ((milliseconds / (1000*60)) % 60);
		int hours   = (int) ((milliseconds / (1000*60*60)) % 24);
		return StrUtil.strings(StrUtil.intToStr(hours,1),":",StrUtil.intToStr(minutes,2),":",StrUtil.intToStr(seconds,2));	
	}
	
	
	public static long strToTime(String s){
		String t[] = s.split(":");
		long time = -1;
		try{
		time = (Integer.parseInt(t[0]) * 60 * 60)
				+ (Integer.parseInt(t[1]) * 60) + (Integer.parseInt(t[2]));
		} catch(Exception e){
			e.printStackTrace();
		}
		return time;
	}

	public static String getMd5(String str) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		md.update(str.getBytes());
		byte[] digest = md.digest();
		StringBuilder sb = new StringBuilder();
		for (byte b : digest) {
			sb.append(String.format("%02x", b & 0xff));
		}
		return sb.toString();
	}


	public static String arrToString(String[] arr){
		return TextUtils.join(" ", arr);
	}

	public static String trimFileName(String str){
		return "\""+str.trim()+"\"";
	}

	public static String cropTo(String str, char ch){
		final int n = str.lastIndexOf(ch);
		if(n > 0){
			str = str.substring(0, n);
		}
		return str;
	}

	public static String cropFrom(String str, char ch){
		final int n = str.lastIndexOf(ch);
		if(n > 0){
			str = str.substring(n+1);
		}
		return str;
	}

	public static String  getFileName(String path, boolean withExt){
		String name = StrUtil.cropFrom(path, '/');
		if(!withExt) {
			name = StrUtil.cropTo(name, '.');
		}
		return name;
	}

	public static String getBatchMask(String path, boolean withExt){
		String mask = StrUtil.cropTo(path, '/') + "/*";
		if(withExt){
			mask = mask + "." + StrUtil.cropFrom(path, '.');
		}

		return mask;
	}


	public static String getNewName(String name, List<String> list){
		if (name.matches("^.* \\d$")) {
			int i = Integer.parseInt(StrUtil.cropFrom(name, ' '));
			i++;
			name = StrUtil.cropTo(name, ' ') + " " + Integer.toString(i);
		} else {
			name = name + " 1";
		}

		for (String str : list){
			if (str.equals(name)){
				return getNewName(name, list);
			}
		}

		return name;
	}

	public static String normalizeText(String str){
		return str.replaceAll(" +", " ").trim();
	}

	public static String getNextWord(String text, final String find){
		String arr[] = text.split(" +");
		if(arr.length > 0){
			for (int i = 0; i < arr.length; i++){
				if(arr[i].equals(find)){
					if(arr.length > i + 1){
						return arr[i + 1];
					}
				}
			}
		}
		return "";
	}


	public static TextFragment getWord(final String text, final int point){
		final int len = text.length();

		TextFragment fragment = new TextFragment();
		fragment.fullText = text;
		fragment.startPosition = 0;
		fragment.endPosition = len;

		if(point > 0){
			for(int i = point; i > 0; i--){
				String ch = text.substring(i - 1, i);
				if(ch.equals(" ")){
					fragment.startPosition = i;
					break;
				}
			}
		}

		if(point < len){
			for(int i = point; i < len; i++){
				String ch = text.substring(i, i+1);
				if(ch.equals(" ")){
					fragment.endPosition = i;
					break;
				}
			}
		}


		return fragment.getFragment();
	}

    public static String getFileNamePretty(String str) {
		//TODO
		String fileName = StrUtil.getFileName(str, true);
		return fileName;
    }

    public static String[] normalizeCmd(String[] cmd) {
		List<String> list = new ArrayList<>();
		//list.add("sh"); list.add("-c");
		for (String c : cmd) {
			if (c != null && !c.isEmpty()) {
				list.add(c);
			}
		}
		String[] arr = new String[list.size()];
		return list.toArray(arr);
	}



}

