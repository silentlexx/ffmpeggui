package com.silentlexx.ffmpeggui_os.model;

import android.content.Context;
import android.util.Log;

import com.silentlexx.ffmpeggui_os.R;
import com.silentlexx.ffmpeggui_os.config.Config;
import com.silentlexx.ffmpeggui_os.utils.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FFmpegChooser implements CpuArch {



    private List<FFmpeg> list = new ArrayList<>();
    private CurrentBin current;
    private Context context;

    private static final String FULL_BIN = "libffmpeg.so";
    private static final String LITE_BIN = "libffmpeglite.so";

    //private boolean withHelp = false;

    public FFmpegChooser(Context context, final int curCode, final String curName){
        this.context = context;

        current = new CurrentBin(curCode, curName);

       // if(!Config.isLegacy()) {
            FFmpeg ffmpeg = new FFmpeg(context).addBin("ffmpeg-" + Config.INT_FFMPEG_VER, FULL_BIN);
            FFmpeg ffmpegLite = new FFmpeg(context).addBin("ffmpeg_lite-" + Config.INT_FFMPEG_VER, LITE_BIN);

            if(ffmpeg.isOk()) {
                add(ffmpeg);
            } else {
                add(new FFmpeg(context).addBinFromApk("ffmpeg-" + Config.INT_FFMPEG_VER, FULL_BIN));
            }

            if(ffmpegLite.isOk()){
                add(ffmpegLite);
            } else {
                add(new FFmpeg(context).addBinFromApk("ffmpeg_lite-" + Config.INT_FFMPEG_VER, LITE_BIN));
            }

       // }
        if(!Config.isNonLegacy()){
           // add(new FFmpeg(context).addBinFromApk("ffmpeg-" + Config.INT_FFMPEG_VER, FULL_BIN));
           // add(new FFmpeg(context).addBinFromApk("ffmpeg_lite-" + Config.INT_FFMPEG_VER, LITE_BIN));

            final int startSize = size();
            scanExtBinDir(context);
            if (size() == startSize) {

                add(new FFmpeg(context).addMessage(context.getString(R.string.addbuild_title),
                        context.getString(R.string.addbuild_message)));
            }
        }

        setCurrent(current);
    }

    private void scanExtBinDir(Context c){
        try {
            final File dir = new File(Config.getExtBinPath(c));
            if (dir.exists() && dir.isDirectory()) {
                final File[] files = dir.listFiles();
                if(files != null && files.length > 0) {
                    for (File file : files) {
                        if (!file.isDirectory() && file.canRead()) {
                            add(new FFmpeg(context).addBinFromSdCard(file.getName()));
                        }
                    }
                }


            }
        } catch (Exception e){
            Log.e("BIN", e.toString());
        }

    }

    private int size(){
        return list.size();
    }

    private void add(FFmpeg fFmpeg){
       // if(fFmpeg.isOk()){
            list.add(fFmpeg);
       // }
    }

    public boolean setCurrent(CurrentBin cur){
        int i = cur.code;
        if(i >= 0 && i < list.size()){
            Log.d("BIN", list.get(current.code).getName() + " -- " + cur.name);

            if(list.get(current.code).getName().equals(cur.name)){
               return setCurrent(cur.code);
            } else {
                removeBin(context);
               return setCurrent(0);
            }
        }
        return false;
    }

    public boolean setCurrent(int i){
        if(i < 0 || i >= list.size()){
            i = 0;
        }

            if(list.get(i).activate()) {

                current.code = i;
                current.name = list.get(current.code).getName();

                return true;

            } else {
                return false;
            }


    }


    public int getCurrentCode(){
        if(current.code >= 0 && current.code < list.size()) {
            return current.code;
        } else {
            removeBin(context);
            setCurrent(0);
            return 0;
        }
    }

    public String getCurrentName(){
        return  current.name;
    }

    private FFmpeg get(){
            return list.get(getCurrentCode());
    }

    public String replaceLibs(String str){
       return get().replaceLibs(str);
    }

    public String getBin(){
        return get().getBin();
    }

    public String[] getDirBin(){
        return get().getDistBinML();
    }

    public String getDistBin(){
        return get().getDistBin();
    }

    public String getName(){
        return get().getName();
    }


    public String[] getNames(){
        String arr[] = new String[list.size()];
        for (int i = 0; i < list.size(); i++){
            arr[i] = list.get(i).getName();
        }
        return arr;
    }

    public boolean isFail(){
        return list.size() < 1;
    }

    public static void removeBin(Context context){
        FileUtil.clearFilesDir(context);
        /*
        String p = context.getFilesDir().getAbsolutePath() + "/" +FFmpeg.BIN;
        File bin = new File(p);
        if(bin.exists()){
            if(bin.delete()){
                Log.i("BIN", " deleted: " + p);
            } else {
                Log.e("BIN", "Error!");
            }
        }
        */
    }



    public FFmpeg get(int i) {
        if(i >= 0 && i < list.size()){
            return list.get(i);
        }
        return null;
    }
}
