package com.silentlexx.ffmpeggui_os.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;

import com.silentlexx.ffmpeggui_os.config.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public final class FileUtil {
    private static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;


    public static boolean isPathIsCorrect(String path) {

        return path.startsWith("/") || path.contains("://");
                /*
      List<String> vols = getDisks();
      vols.add("/sdcard");
      vols.add("/mnt");
      vols.add("/home")
      for (String vol : vols ){
          if(path.startsWith(vol)){
              return true;
          }
      }
      return false;
      */
    }


    public static File from(Context context, Uri uri) {
        File tempFile;
        try {
            if (uri.getScheme() != null && uri.getScheme().equals("content")) {
                File dir = new File(Config.getTempPath(context));
                if (dir.exists() && !dir.isDirectory()) {
                    dir.delete();
                }
                if (!dir.exists()) {
                    dir.mkdirs();
                }


                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                String fileName = getFileName(context, uri);
                if (fileName == null) {
                    return null;
                }
                //String[] splitName = splitFileName(fileName);
                tempFile = new File(dir.getAbsolutePath() + "/" + fileName);
                tempFile = rename(tempFile, fileName);
                //tempFile.deleteOnExit();
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(tempFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (inputStream != null) {
                    copy(inputStream, out);
                    inputStream.close();
                }

                if (out != null) {
                    out.close();
                }
            } else {
                tempFile = new File(uri.getPath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return tempFile;
    }


    @SuppressLint("Range")
    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst() ) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = 0;
            if (result != null) {
                cut = result.lastIndexOf(File.separator);
            }
            if (cut != -1) {
                if (result != null) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }

    public static String getOutputPath(Context c){
        String out;
        File dwn = new File(Config.getDownloadPath());
        if(dwn.exists() &&  dwn.isDirectory() && dwn.canWrite()){
            out = Config.getDownloadPath();
        } else {
            out = Config.getSdCardPath(c);
        }
        return out;
    }

    public static void copy(File src, File dst) throws IOException {

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){

            try (InputStream in = new FileInputStream(src)) {
                try (OutputStream out = new FileOutputStream(dst)) {
                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
            }

        } else {

            InputStream in = new FileInputStream(src);
            try {
                OutputStream out = new FileOutputStream(dst);
                try {
                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
        }


    }

    private static File rename(File file, String newName) {
        File newFile = new File(file.getParent(), newName);
        if (!newFile.equals(file)) {
            if (newFile.exists() && newFile.delete()) {
                Log.d("FileUtil", "Delete old " + newName + " file");
            }
            if (file.renameTo(newFile)) {
                Log.d("FileUtil", "Rename file to " + newName);
            }
        }
        return newFile;
    }

    private static void copy(InputStream input, OutputStream output) throws Exception {

        int n;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);

        }
    }

    // NEW MY

/*
    public static void clearDir(String dir) {
        clearDir(dir, null, null);
    }

    public static void clearDir(String dir, final String matches, final String exclude) {
        final File folder = new File(dir);
        final File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir,
                                  final String name) {
                if (matches == null) {
                    return true;
                }
                return name.matches(matches);
            }
        });
        if (files == null || files.length < 1) {
            return;
        }
        for (final File file : files) {
            //Log.d("FFMPEG_file", file.getAbsolutePath() + " == " + currentDirBin );
            if (exclude == null || exclude.isEmpty() || !exclude.equals(file.getAbsolutePath())) {
                if (!file.delete()) {
                    Log.e("FFMPEG_file", "Can't remove " + file.getAbsolutePath());
                } else {
                    Log.v("FFMPEG_file", "Removed " + file.getAbsolutePath());
                }
            }
        }
    }

 */

    public static List<String> getDisks(Context c, boolean writable) {
        List<String> list = new ArrayList<>();
        /*
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            StorageManager storageManager=
                    (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            //get the list of all storage Volume
            List<StorageVolume> storageVolumeList=storageManager.getStorageVolumes();
            for (StorageVolume sv : storageVolumeList){
                Log.d("DISK","List"+ sv.toString());
                storageManager.ge
            }
        }
        else
        {*/

        /*
        final String[] pl = new String[]{
            Environment.DIRECTORY_DOCUMENTS,
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_PICTURES,
            Environment.DIRECTORY_NOTIFICATIONS,
            Environment.DIRECTORY_SCREENSHOTS,
            Environment.DIRECTORY_AUDIOBOOKS
        };

         */

        //File storage = new File(Config.SD_CARD_DIR);

       // if (dOk(storage)) {
       //     list.add(storage.getAbsolutePath());
       // }

       // for(String en : pl){
       //     storage = new File(getEnvDir(en));
        //    if (dOk(storage)) {
           //     list.add(storage.getAbsolutePath());
       //     }
      //  }

        File storage = new File(Config.getSdCardPath(c));

        if (dOk(storage, writable)) {
           list.add(storage.getAbsolutePath());
        }

        String dwn = Config.getDownloadPath();
        storage =  new File(dwn);
        if (dOk(storage, writable)) {
            list.add(storage.getAbsolutePath());
        } else {
            dwn = "...";
        }

        storage = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

        if (dOk(storage, false)) {
            for (File f : storage.listFiles()) {
                if (dOk(f, writable) && !f.getAbsolutePath().equals(dwn)) {
                    list.add(f.getAbsolutePath());
                }
            }
        }

        storage = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

        if (dOk(storage, writable)) {
            list.add(storage.getAbsolutePath());
        }


        storage = new File("/storage");

        if (dOk(storage, false)) {
            for (File f : storage.listFiles()) {
                if (dOk(f, false)) {
                    list.add(f.getAbsolutePath());
                }
            }
        }

        String sd = System.getenv("EXTERNAL_STORAGE");
        if(sd==null){
            sd = "/sdcard";
        }

        if(!sd.equals(Environment.getExternalStorageDirectory().getAbsoluteFile().toString())) {
            storage = new File(sd);
            if (dOk(storage, writable)) {
                list.add(storage.getAbsolutePath());
            }
        }

        return list;
    }

    private static boolean dOk(File f, boolean writable){
        if(!f.isDirectory()) {
            return false;
        }
        if(writable){
            return f.canWrite();
        } else {
            return f.canRead();
        }
    }

    private static String getEnvDir(String s){
        return Environment.getExternalStoragePublicDirectory(s).getAbsolutePath();
    }

    public static File[] getDirFileList(String str) {
        final String dir = StrUtil.cropTo(str, '/');
        final String matches = StrUtil.cropFrom(str, '/')
                .replace("*", ".*");
        //final boolean force = matches.equals(".*") || matches.equals(".*..*"); //FIXME critical

        Log.d("MATCHES", matches);
        final File folder = new File(dir);
       // FileFilter fileFilter = new WildcardFileFilter("sample*.java");
        final File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir,
                                  final String name) {
                return name.matches(matches);
            }
        });
        return files;
    }


    public static void clearFilesDir(Context context){
       // if(true) return;
        final File dir = new File(context.getFilesDir().getAbsolutePath());
        if(dir.isDirectory()){
            final File[] files = dir.listFiles();
            if(files.length > 0){
                for(File file :  files){
                    if(file.getName().endsWith(".bin") && file.delete()){
                        Log.i("DEL", " deleted: " + file.getName());
                    }
                    if(file.getName().endsWith(".so") && file.delete()){
                        Log.i("DEL", " deleted: " + file.getName());
                    }
                }
            }
        }

    }



    public static boolean saveStringToFile(Context c, String text, Uri u){

        try (PrintStream out = new PrintStream(c.getContentResolver().openOutputStream(u))) {
            out.print(text);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile (Context c, Uri u) {
        String ret = "";
        try {
            ret = convertStreamToString(c.getContentResolver().openInputStream(u));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }


    public static void deleteCache(Context context) {
        try {
            File dir = context.getExternalCacheDir();
            deleteDir(dir);
        } catch (Exception e) { e.printStackTrace();}
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
}