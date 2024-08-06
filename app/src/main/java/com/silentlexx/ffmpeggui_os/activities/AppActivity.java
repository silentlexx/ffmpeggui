package com.silentlexx.ffmpeggui_os.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.silentlexx.ffmpeggui_os.R;
import com.silentlexx.ffmpeggui_os.config.Config;
import com.silentlexx.ffmpeggui_os.utils.FileUtil;
import com.silentlexx.ffmpeggui_os.utils.MyUtil;
import com.silentlexx.ffmpeggui_os.utils.StrUtil;

import java.io.File;



public abstract class AppActivity extends AppCompatActivity {

    public Config config;
    public ProgressDialog progressDialog;

    // public key из админки Google Play
    static final int MANAGE_STORAGE_REQUEST_CODE = 211;

    private static final String PAYLOAD = StrUtil.getMd5("GuiForFFPMEG");







    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //TODO
    }

    public void onBuySuccess() {
        Toast.makeText(getApplicationContext(), getString(R.string.thx), Toast.LENGTH_SHORT).show();
    }



    public void setVisible(int id, boolean visible) {
        findViewById(id).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setEditText(int id, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        int n = text.length();
        EditText ed = findViewById(id);


        if (Config.EXPEREMENTAL && id == GuiWidgetInterface.AGRS) {
            text = text.replaceAll(" +", " ");
            ed.setText(getSpannable(text), TextView.BufferType.SPANNABLE);
        } else {
            ed.setText(text);
        }


        try {
            ed.setSelection(n);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    public String getEditText(int id) {
        EditText editText = findViewById(id);
        return editText.getText().toString();
    }

    public void setTextOnView(int v, String text) {
        if (text == null) return;
        TextView textView = findViewById(v);
        textView.setText(text);
        textView.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
    }

    public void setIconOnFab(int v, int res) {
        ((FloatingActionButton) findViewById(v)).setImageResource(res);
    }


    public void firstRun() {
        try {
            /*
            File doc = new File(Config.getSdCardDir(this));
            if (!doc.exists()) {
                doc.mkdir();
            }
             */
            File dir = new File(Config.getSdCardPath(this));
            if (!dir.exists()) {
                dir.mkdir();
            }
            File sample = new File(Config.getSamplePath(this, false));
            if (!sample.exists()) {
                MyUtil.getFileFromAssets(this, Config.DEF_IN_FILE, sample.getAbsolutePath(), false);
            }

            if (!Config.isNewSDK()) {
                File extBin = new File(Config.getExtBinPath(this));
                if (!extBin.exists()) {
                    extBin.mkdir();
                }
            }
            if (!config.getBool(Config.FIRST_RUN)) {

                if (Config.NEW_FS_SYSTEM && Config.isNewSDK()) {
                    if (Environment.isExternalStorageManager()) {
                        config.set(Config.FIRST_RUN, true);
                    }
                } else {
                    config.set(Config.FIRST_RUN, true);
                }
                //config.reInitFfmpeg();
            }
            //if (Config.isNewSDK()) {
            //    getFSAccess();
            //}

        } catch (Exception e) {
            Log.e("FIRSTRUN", e.toString());
            //  e.printStackTrace();
        }

    }



    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        FileUtil.deleteCache(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        if (Config.isNewSDK()) {
            getFSAccess();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public void openFsAccessDialog(){
        String packageName = getApplicationContext().getPackageName();
        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + packageName));
        //startActivity(intent);
        //Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + packageName));
        startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
    }


    @RequiresApi(api = Build.VERSION_CODES.R)
    private void getFSAccess() {
        if (Config.NEW_FS_SYSTEM && !Environment.isExternalStorageManager()) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setCancelable(false);
            dialog.setTitle(R.string.important);
            dialog.setMessage(R.string.fs_alert);
            dialog.setPositiveButton(R.string.fs_access_btn, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    openFsAccessDialog();
                }
            });
            dialog.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    //finish();
                }
            });
            dialog.show();
        }

    }


    public Spannable getSpannable(String text) {
        Log.d("spannable", text);

        Spannable spannable = new SpannableString(text);

        String arr[] = text.split(" +");
        for (int i = 0; i < arr.length; i++) {
            String s = arr[i];
            if (s.startsWith("-")) {
                int ofs = text.indexOf(s + " ");
                int ffs = ofs + s.length();
                spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.param_name)), ofs, ffs, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

/*
                final String ss = s;
                ClickableSpan cs = new ClickableSpan() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(AppActivity.this, ss, Toast.LENGTH_SHORT).show();
                    } };

                spannable.setSpan(cs, ofs, ffs, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
*/
                if (i + 1 < arr.length && !arr[i + 1].isEmpty() && !arr[i + 1].startsWith("-") && !arr[i + 1].startsWith("\"")) {
                    s = arr[i + 1];
                    ofs = ffs;
                    ffs = ofs + s.length() + 1;
                    spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.param_data)), ofs, ffs, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }


        }
        return spannable;
    }


    public void done(String done, int res) {


        config.setEnd(false, false, "");

        String m;
        int pic;
        if (res == 0) {
            pic = R.drawable.accept;
            m = getString(R.string.job_good);
        } else if (res == 2) {
            pic = R.drawable.cancel;
            m = getString(R.string.job_abort);
        } else {
            pic = R.drawable.cancel;
            m = getString(R.string.job_bad);
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(done)
                .setIcon(pic)
                .setCancelable(true)
                .setTitle(m)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    public abstract int getRootView();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View rootView = getLayoutInflater().inflate(getRootView(),
                null);
        setContentView(rootView);

        config = new Config(this);

    }


    public static final String ACTIVITY_MODE = "activityMode";
    public static final int START_ENCODING = 123;
    public static final int VIEW_ENCODING = 124;

    public int getMode() {
        return getMode(getIntent());
    }

    public int getMode(Intent intent) {
        return intent.getExtras() != null ? getIntent().getExtras().getInt(Gui.ACTIVITY_MODE, 0) : 0;
    }


    //---------------------------------------------------------

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


}
