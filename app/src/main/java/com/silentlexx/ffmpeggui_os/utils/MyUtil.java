package com.silentlexx.ffmpeggui_os.utils;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.silentlexx.ffmpeggui_os.config.Config;
import com.silentlexx.ffmpeggui_os.model.CpuArch;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class MyUtil {


    public static boolean dividesByTwo(int a) {
        return (a % 2 == 0);
    }


    public static String getAppVer(Context c) {
        PackageInfo pi;
        try {
            pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            return "unknown";
        }
        return pi.versionName;
    }


    public static boolean checkArch(String arch) {
        final String sysArch = Config.ARCH;
        if (sysArch == null) {
            return true;
        }

        switch (arch) {
            case CpuArch.ARM64:
                return sysArch.contains("aarch64") || sysArch.contains("arm64");
            case CpuArch.ARM:
                return sysArch.contains("arm"); // || sysArch.contains("aarch64") || sysArch.contains("arm64");
            case CpuArch.X86_64:
                return sysArch.contains("x86_64");
            case CpuArch.X86:
                return sysArch.contains("i686") || sysArch.contains("x86"); // || sysArch.contains("x86_64");
        }


        return false;
    }

    public static String getNewName(String name, final String ext) {
        File file = getFile(name, ext);
        int i = 0;
        while (file.exists()) {
            i++;
            if (name.matches("^.*_\\d$")) {
                i = Integer.parseInt(StrUtil.cropFrom(name, '_'));
                i++;
                name = StrUtil.cropTo(name, '_') + "_" + Integer.toString(i);

            } else {
                name = name + "_" + Integer.toString(i);
            }
            file = getFile(name, ext);
        }
        //Log.d("newname", name);
        return name;
    }

    public static File getFile(String name, String ext) {
        return new File(name + "." + ext);
    }

    public static boolean getFileFromAssets(Context context, String fileName, String outPath, boolean exe) {
        File file = new File(outPath);
        if (!file.exists()) {
            try {
                InputStream is = context.getAssets().open(fileName);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(buffer);
                fos.close();
                Log.v("BIN", "Added " + file.getAbsolutePath());
                if (exe) {
                    return file.setExecutable(true);
                } else {
                    return true;
                }
            } catch (Exception e) {
                Log.e("BIN", "Can't add " + file.getAbsolutePath());
                return false;
            }
        } else {
            if (exe && !file.canExecute()) {
                return file.setExecutable(true);
            } else {
                return true;
            }
        }
    }


    public static boolean getFileFromApk(Context context, String fileName, String outPath, boolean exe) {
        File file = new File(outPath +  "/" +fileName);
        File apkPath = new File(getApkName(context));


        if (!apkPath.canRead()) {
            Log.e("APK", "Can't read " + apkPath.getAbsolutePath());
            return false;
        } else {
            Log.v("APK", "Reading " + apkPath.getAbsolutePath());
        }

        if (!file.exists()) {
            try {

                return unzip4j(apkPath.getAbsolutePath(), fileName, outPath);

            } catch (Exception e) {
                Log.e("APK", "Can't add " + file.getAbsolutePath());
                return false;
            }
        } else {
            if (exe && !file.canExecute()) {
                return file.setExecutable(true);
            } else {
                return true;
            }
        }
    }


    public static boolean unzip4j(String zipFilePath, final String fileName, final String destPath) {
        try {
            ZipFile zipFile = new ZipFile(zipFilePath);
            Log.d("APK",  "fileName: " + fileName + ", dest: "+destPath );
            File file = new File(destPath + "/" + fileName);

            String inPath = "lib/" + Config.getAbi() + "/" + fileName;
            zipFile.extractFile(inPath, destPath, fileName);
            return file.exists() && file.setExecutable(true);


        } catch (ZipException e) {
            Log.e("APK", e.toString(), e);
        }
        return false;
    }


    public static String getApkName(Context context) {
        String packageName = context.getPackageName();
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            String apk = ai.publicSourceDir;
            return apk;
        } catch (Throwable x) {
        }
        return null;
    }


    public static String getDateTime(){
        Date c = Calendar.getInstance().getTime();

        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy_HH:mm:ss", Locale.getDefault());
        return df.format(c);
    }
}
