package com.silentlexx.ffmpeggui_os.parts;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.silentlexx.ffmpeggui_os.activities.Gui;
import com.silentlexx.ffmpeggui_os.config.Config;
import com.silentlexx.ffmpeggui_os.model.FileInfo;
import com.silentlexx.ffmpeggui_os.utils.StrUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bin {

    public static final String FFMPEG = "ffmpeg";

    public static final String IN = "file_in";
    public static final String OUT = "file_out";
    public static final String AGRS = "arguments";
    public static final int ABORTED_CODE = 255;
    public static final String PREFIX = "-hwaccel auto -y" ;


    private static final int DELTA = 1;

    public static String[] getBin(Context context) {
        Config config = new Config(context);
        return config.getCurrent().getDirBin();
    }

    public static String ls(String path) {
        String ps = exec(new String[]{"ls", path});
        Log.d("LS", ps);
        return ps;
    }

    public static void killAllKill(Context context) {
        killAll(context, "-KILL");
    }

    public static void killAll(Context context) {
        //killAll("-SIGINT");
        killAll(context, "-TERM");
    }

    public static void killAll(Context context, String param) {
        String procs = getProcess(context);
        if(procs != null) exec(new String[]{"kill", param, procs});
    }

    public static String getProcess(Context context){
        Config config = new Config(context);
        String ps = exec(new String[]{"ps"});
        String[] arr = ps.split("\n");
        for (String anArr : arr) {
            if (anArr.contains(config.getCurrent().getBin())) {
                final String[] procs = anArr.split(" +");
                return procs[1];
            }
        }
        return null;
    }

    public static boolean isProcessRunning(Context context){
        return getProcess(context) != null;
    }

    public static String exec(final String[] c) throws NullPointerException {

        final String[] cmd = StrUtil.normalizeCmd(c);

        Log.d("EXEC", TextUtils.join(" ", cmd));


        Process p;
        StringBuilder sb = new StringBuilder();
        try {

            p = Runtime.getRuntime().exec(cmd);

            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            in.close();
            in = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append("\n");

            }


            in.close();

            p.waitFor();

            p.getOutputStream().close();
            p.getInputStream().close();
            p.getErrorStream().close();

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        return sb.toString();
    }

    public static String getBinVersion(Context context) {
        return execBin(context, "-version");
    }

    public static String getBinCodecs(Context context) {
        return execBin(context, "-codecs");
    }

    public static String getBinDecoders(Context context) {
        return execBin(context, "-decoders");
    }


    public static String getBinEncoders(Context context) {
        return execBin(context, "-encoders");
    }


    public static String getBinFormats(Context context) {
        return execBin(context, "-formats");
    }


    public static List<String> getExtList(Context context) {
        List<String> list = new ArrayList<>();
        String str = getBinFormats(context);
        String[] lines = str.split("\n");
        for (String line : lines) {
            String l = line.trim();
            if (l.startsWith("E") || l.startsWith("DE")) {
                String arr[] = l.split(" +");
                if (arr.length > 1 && arr[1].length() < 5) {
                    list.add(arr[1].toLowerCase());
                }
            }
        }
        return list;
    }

	/*
	public static String getBinBits() {
		return execBin("-bsfs");
	}
*/

    public static String getBinFilters(Context context) {
        return execBin(context, "-filters");
    }

    public static String getBinPixfmts(Context context) {
        return execBin(context, "-pix_fmts");
    }

    public static String getBinHelp(Context context) {
        return execBin(context, "-help");
    }

    public static String getBinFileInfo(Context context, String path) {
        //FIXME
        Log.d("FILE", path);
        String[] cmd = new String[]{getBin(context)[0], getBin(context)[1], "-i", path};
        return exec(cmd);
    }

    public static String getBinVideoInfo(Context context, String path) {
        String s = exec(new String[]{getBin(context)[0], getBin(context)[1], "-i", path});
        //Log.i("RES", s);
        String r;
        //Matcher m = Pattern.compile("Video:.*\\ ([0-9]+x[0-9]+).*").matcher(s);
        Matcher m = Pattern.compile("Video:.* ([0-9]+x[0-9]+).*").matcher(s);
        if (m.find()) {
            r = m.toMatchResult().group(1);
        } else {
            return null;
        }

        Log.v(Gui.TAG, " Res: " + r);

        return r;
    }

    private static String execBin(Context context, String agrs) {
        String cmd[] = new String[]{getBin(context)[0], getBin(context)[1], agrs};
        return exec(cmd);
    }
	
	/*

	public static boolean execEnc(String in, String out,String agrs) {
	
		//String cmd = Shell.BIN + DEF_AGRS + agrs + FILE_OUTPUT + LOG + " && echo "+ DONE;
		String cmd[] = new String[]{ BIN,"-y","-i",in,agrs,out" + FILE_OUTPUT + LOG + " && echo "+ DONE };
		MyUtil.rmFile(LOG);
		MyUtil.createFile(LOG);
		String o = exec(cmd);
		Log.i(Gui.TAG, "ffmpeg exit: " + o);
		if (o != "" && o != null) {
			Shell.isSuccess = true;
		} else {
			Shell.isSuccess = false;
		}
		return Shell.isSuccess;
	}
	*/

    public static FileInfo getFileInfo(Context context, String in_file) {
        String cmd[] = new String[]{getBin(context)[0], getBin(context)[1], "-i", in_file};
        String s = Bin.exec(cmd);
        String dur;
        String fps;


        Matcher m = Pattern
                .compile("Duration:.* ([0-9]+:[0-9]+:[0-9]+)\\..*")
                .matcher(s);
        if (m.find()) {
            dur = m.toMatchResult().group(1);
        } else {
            dur = "00:00:00";
        }
        m = Pattern.compile("Video:.* (.*?) tbr.*").matcher(s);
        if (m.find()) {
            fps = m.toMatchResult().group(1);
        } else {
            fps = null;
        }

        long time = StrUtil.strToTime(dur) + DELTA;

        Log.v(Gui.TAG, "dur: " + dur + " fps:" + fps);
        int ffps = 0;
        try {
            if (fps != null) {
                ffps = (int) Float.parseFloat(fps);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new FileInfo(time, ffps * time);

    }

    public static int[] getRes(String s) {
        if (s == null) {
            return new int[]{0, 0};
        }
        String arr[] = s.split("x");
        if (arr.length != 2)
            return null;
        try {
            int r[] = new int[2];
            r[0] = Integer.parseInt(arr[0].replace(",", ""));
            r[1] = Integer.parseInt(arr[1].replace(",", ""));
            return r;
        } catch (Exception e) {
            return null;
        }

    }
/*
	public static String getStopLine() {
		String o = MyUtil.readString(LOG);
		String line = "";
		String s[] = o.split("\n");
		line = s[s.length - 1];
		return line;
	}
*/

}
