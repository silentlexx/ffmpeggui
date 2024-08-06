package com.silentlexx.ffmpeggui_os.model;

import android.content.Context;

import com.silentlexx.ffmpeggui_os.R;
import com.silentlexx.ffmpeggui_os.config.Config;
import com.silentlexx.ffmpeggui_os.parts.Bin;
import com.silentlexx.ffmpeggui_os.utils.FileUtil;
import com.silentlexx.ffmpeggui_os.utils.MyUtil;

import java.io.File;
import java.io.IOException;

public class FFmpeg {

    private static final String PREFIX_NAME="ffmpeg-";
    private static final String BIN = "ffmpeg.bin";
    private static final String PREFIX_ARCH="_";
    //private static final String DIR = "/data/data/" + Gui.APP+"/lib";
    private static final String ML = "magiclen.org";

    private String name;
    private String bin;
    private String distBin;
    private String sdBin;
    private String dist;
    private String message = "";

    private LibReplace libReplace = null;// new LibReplace().add("libfaac", "aac");
    private boolean isOk = false;
    private Context context;

    public Type getType() {
        return type;
    }

    public enum Type {
        INTERNAL, EXTERNAL, BIN, MESSAGE, APK
    }

    private Type type;

    public FFmpeg(Context context){
        this.context = context;
    }

    public FFmpeg addMessage(String name, String message){
        type = Type.MESSAGE;
        this.name = name;
        this.message = message;
        return this;
    }

    public FFmpeg addBin(String code, String bin){
        type = Type.BIN;
        this.name = code + "_" + Config.getAbi()  + " (" + context.getString(R.string.builtin) + ")" ;
        this.bin = bin;
        this.distBin = context.getApplicationInfo().nativeLibraryDir + "/" + bin;
        this.dist = context.getApplicationInfo().nativeLibraryDir;
        return this;
    }

    public FFmpeg addBinFromApk(String code, String bin){
        type = Type.APK;
        this.name = code + "_" + Config.getAbi() + " (" + context.getString(R.string.fromapk) + ")";
        this.bin = bin;
        this.distBin = context.getFilesDir().toString()+"/"+bin;
        this.dist = context.getFilesDir().toString();
        return this;
    }

    @Deprecated
    public FFmpeg addBinFromAssets(String code, String bin){
        type = Type.INTERNAL;
        this.name = code + "_" + Config.getAbi() + " (" + context.getString(R.string.fromassest) + ")";
        this.bin = bin;
        this.distBin = context.getFilesDir().toString()+"/"+bin;
        this.dist = context.getFilesDir().toString();
        return this;
    }

    public FFmpeg addBinFromSdCard(String binFile) {
        type = Type.EXTERNAL;
        this.name = binFile + " (" + context.getString(R.string.fromsdcard) + ")";
        this.bin = BIN;
        this.sdBin = binFile;
        this.distBin = context.getFilesDir().toString()+"/"+BIN;
        this.dist = context.getFilesDir().toString();
        return this;
    }

    public boolean activate(){
        switch (type){
            case INTERNAL: return setAssetsBin();
            case EXTERNAL: return setExternalBin(context);
            case APK: return setApkBin();
            case BIN: return setSoBin();
            case MESSAGE:
               // showMessage();
                return false;
        }


        return true;
    }



    public FFmpeg setReplacer(LibReplace replace){
        this.libReplace = replace;
        return this;
    }

    boolean isOk(){
        File exe = new File(this.distBin);
        //Log.d("CHKBIN", distBin);
        return exe.exists() && exe.canExecute();
    }

    String replaceLibs(String str){
        if(libReplace == null) {
            return str;
        } else {
            return libReplace.replace(str);
        }
    }

    public String getMessage(){
        return message;
    }

    public String getDistBin(){
        return distBin;
    }

    public String[] getDistBinML(){
        String[] out;
        if(isMLBuild()){
            out = new String[]{distBin, "-" + ML};
        } else {
            out = new String[]{distBin, ""};
        }

        return out;
    }

    public String getName(){
        return name;
    }

    String getBin(){
        return bin;
    }


    boolean setExternalBin(Context c){
        File dist = new File(distBin);
        if(!dist.exists()){

            File ext = new File(Config.getExtBinPath(c)+"/"+sdBin);
            if(ext.canRead() && !ext.isDirectory()){
                try {
                    FileUtil.copy(ext, dist);
                    if(dist.exists()){
                        isOk = dist.setExecutable(true);
                        return true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        return false;
    }

    boolean setAssetsBin(){
       isOk = MyUtil.getFileFromAssets(context, bin, distBin, true);
       if(!isOk){
           isOk = MyUtil.getFileFromAssets(context, Config.getAbi() + "/" + bin, distBin, true);
       }
       return isOk;
    }




    boolean setApkBin(){
        isOk = MyUtil.getFileFromApk(context, bin, dist, true);
        return isOk;
    }


    boolean setSoBin(){
        final File f = new File(distBin);
        if( f.exists() ){
            if(f.canExecute()){
                isOk = true;
            } else {
                isOk = f.setExecutable(true);
            }
        } else {
            isOk = false;
        }
        return isOk;
    }

    boolean isMLBuild(){
        if (type == Type.EXTERNAL){
            String out = Bin.exec(new String[]{distBin});
            return out.contains(ML);
        }
        return false;
    }
}
