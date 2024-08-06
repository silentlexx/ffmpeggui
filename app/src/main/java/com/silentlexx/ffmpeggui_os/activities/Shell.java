package com.silentlexx.ffmpeggui_os.activities;


import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.silentlexx.ffmpeggui_os.R;
import com.silentlexx.ffmpeggui_os.config.Config;
import com.silentlexx.ffmpeggui_os.config.ConfigInterface;
import com.silentlexx.ffmpeggui_os.model.BatchJob;
import com.silentlexx.ffmpeggui_os.model.Job;
import com.silentlexx.ffmpeggui_os.model.JsonInterface;
import com.silentlexx.ffmpeggui_os.parts.Bin;
import com.silentlexx.ffmpeggui_os.services.Commands;
import com.silentlexx.ffmpeggui_os.services.EncodeService;
import com.silentlexx.ffmpeggui_os.services.SendIntent;
import com.silentlexx.ffmpeggui_os.utils.MyUtil;
import com.silentlexx.ffmpeggui_os.utils.StrUtil;

import java.util.List;

public class Shell extends AppActivity implements GuiWidgetInterface, Commands {

    public static final String CANCEL = "cancel";

    private static boolean wakeLocking = false;

    private static final boolean USE_DIALOG = false;

    public static boolean isAborted = false;

    private int stopCount = 0;



    private TextView textPrgs;
    private TextView outtext;
    private ProgressBar pb;
    private ImageView sIcon;
    private FloatingActionButton fabAbort;
    private FloatingActionButton fabClose;
    private FloatingActionButton fabLog;
    private FloatingActionButton fabAddJob;
    private FloatingActionButton fabListJobs;
    private LinearLayout logLayout;
    private LinearLayout listLayout;
    private String strProg;

    private enum ShellMode {
        LOG,
        LIST
    }

    private ShellMode shellMode = ShellMode.LOG;

    private PowerManager.WakeLock wakeLock = null;

    @Override
    public int getRootView() {
        return R.layout.out;
    }


    @SuppressLint({"InvalidWakeLockTag", "RestrictedApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Config config = new Config(this);
        //Bin.USE_ROOT = mPrefs.getBoolean("root", false);

        wakeLocking = config.getBool("screenoff");


        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (wakeLocking) {
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                    Gui.TAG);
        }


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
       // Objects.requireNonNull(getActionBar()).setDisplayHomeAsUpEnabled(true);

        logLayout = findViewById(R.id.logWrp);
        listLayout = findViewById(R.id.listWrp);

        outtext = findViewById(OUTPUT);

        sIcon = findViewById(STATICON);
        sIcon.setVisibility(View.GONE);

        outtext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog();
            }
        });

        outtext.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String s = outtext.getText().toString();
                copyLog(s);
                return true;
            }
        });

        strProg = getString(R.string.progress);
        pb = findViewById(PROGRS);
        pb.setMax(100);


        textPrgs = findViewById(TEXT_PROGRESS);


        fabListJobs = findViewById(JOBLIST);
        fabListJobs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode();
            }
        });


        fabAddJob = findViewById(ADDJOB);
        fabAddJob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startShellGuiActivity();
            }
        });

        fabLog = findViewById(SHOW_LOG);
        fabLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog();
            }
        });

        fabLog.setVisibility(View.GONE);

        fabAddJob.setVisibility(View.VISIBLE);
        fabClose = findViewById(FABCLOSE);
        fabClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                close();
            }

        });

        fabClose.setVisibility(View.GONE);

        fabAbort = findViewById(ABORT);
        fabAbort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (!isDone) {
                    cancel();
                } else {
                    close();
                }

            }

        });
        fabAbort.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                killService();
                return true;
            }
        });
        fabAbort.setVisibility(View.VISIBLE);

        parseIntent(getIntent());
    }

    private void switchMode() {

        switch (shellMode){
            case LOG:
                logLayout.setVisibility(View.GONE);
                listLayout.setVisibility(View.VISIBLE);
                getList();
                shellMode = ShellMode.LIST;
                fabListJobs.setImageResource(R.drawable.action_log);
                break;
            case LIST:
                listLayout.setVisibility(View.GONE);
                logLayout.setVisibility(View.VISIBLE);
                shellMode = ShellMode.LOG;
                fabListJobs.setImageResource(R.drawable.jobslist);
                break;
        }

    }


    public void parseIntent(Intent intent){

        if(getMode(intent)!=START_ENCODING) {
            return;
        }

        Bundle b = intent.getExtras();
        if (b != null) {
            String ffmpegAgrs = b.getString(Bin.AGRS);

            if(ffmpegAgrs==null || ffmpegAgrs.isEmpty()){
                return;
            }

            String inFile, outFile = "";

            String[] arr = ffmpegAgrs.split("\"");


            if (arr.length >= 2) {
                inFile = arr[1];
            } else {
                inFile = b.getString(Bin.IN);
            }


            if (arr.length >= 4) {
                outFile = arr[3];
            } else {
                outFile = b.getString(Bin.OUT);
            }


            if (arr.length >= 6) {
                inFile += "\n" + outFile;
                outFile = arr[5];
            }

            if(!EncodeService.isRun(this)){
                runService(inFile, outFile, ffmpegAgrs);
            } else {
                sendNewJob(inFile, outFile, ffmpegAgrs);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() != null && intent.getAction().equals(CANCEL)) {
            cancel();
        } else {
            parseIntent(intent);
        }
        setIntent(intent);
    }

    @Override
    public void onBackPressed() {
        if (Config.MULTIWIN) {
            if (!isDone) {
                startShellGuiActivity();
            } else {
                close();
            }

        } else {
            if (!isDone) {
                cancel();
            } else {
                close();
            }
        }
        super.onBackPressed();
    }

    private boolean isDone = false;

    @SuppressLint("RestrictedApi")
    @Override
    public void done(String done, int res) {

        isDone = true;

        config.setEnd(false, false, "");

        fabAbort.setVisibility(View.GONE);
        fabClose.setVisibility(View.VISIBLE);
        fabAddJob.setVisibility(View.GONE);
        fabLog.setVisibility(View.VISIBLE);

        pb.setVisibility(View.GONE);
        String m;
        int pic;
        if (res == 0) {
            pic = R.drawable.accept;
            m = getString(R.string.job_good);
        } else if (res == Bin.ABORTED_CODE) {
            pic = R.drawable.abort;
            m = getString(R.string.job_abort);
        } else {
            pic = R.drawable.cancel;
            m = getString(R.string.job_bad);
        }

        sIcon.setImageResource(pic);
        sIcon.setVisibility(View.VISIBLE);

        textPrgs.setText(m);
        outtext.setText(done);


        if (USE_DIALOG) {
            done(done, res);
        }
    }

    public void setInfo(String line, int procent) {
        if (line != null) {
            outtext.setText(line);
        }

        if (pb.getVisibility() != View.VISIBLE)
            pb.setVisibility(View.VISIBLE);

        if (procent > -1 && procent <= 100) {
            pb.setIndeterminate(false);
            textPrgs.setText(StrUtil.strings(strProg, ": ", Integer.toString(procent), "%"));

            pb.setProgress(procent);
        } else {
            textPrgs.setText("");
            pb.setIndeterminate(true);
        }


    }


    private void runService(String in, String out, String agrs) {
        isAborted = false;

        Intent encIntent = new Intent(this, EncodeService.class);
        encIntent.putExtra(Bin.AGRS, agrs);
        encIntent.putExtra(Bin.IN, in);
        encIntent.putExtra(Bin.OUT, out);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            startForegroundService(encIntent);
        } else {
            startService(encIntent);
        }
    }
	

    @Override
    protected void onDestroy() {
        //Bin.killAll(this);
        hideAllNotice();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        config.set(ConfigInterface.SHELL_MODE, shellMode == ShellMode.LIST);
        if (wakeLocking)
            wakeLock.release();
        unregisterReceiver(commandReceiver);
        super.onPause();
    }

    @SuppressLint("WakelockTimeout")
    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, new IntentFilter(SHELL), Context.RECEIVER_NOT_EXPORTED);
        } else  {
            registerReceiver(commandReceiver, new IntentFilter(SHELL));
        }

        if(config.get(ConfigInterface.SHELL_MODE, false) && shellMode == ShellMode.LOG ){
            switchMode();
        }

        if (wakeLocking)
            wakeLock.acquire();
        if (EncodeService.isRun(this)) {
            isAborted = false;
            sendPing();
            if(shellMode == ShellMode.LIST){
                getList();
            }
        } else {

            boolean b[] = config.getEnd();
            if (b[0]) {

                done(config.get(ConfigInterface.END_LOG, ""), b[1] ? 0 : 1);

                hideAllNotice();
            }
        }

    }

    private ProgressDialog pdialog;

    private void showWaitDalog() {
        pdialog = ProgressDialog.show(this, null,
                getString(R.string.ended_enc), true);
    }

    private void hideWaitDalog() {
        // hideAllNotice();
        if (pdialog != null && pdialog.isShowing()) {
            pdialog.dismiss();
        }
    }

    public void hideAllNotice() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
    }

    private void killService() {
        new SendIntent(this, Commands.SERVICE, Commands.KILL).send(this);
    }

    private void stopService() {
        new SendIntent(this, Commands.SERVICE, Commands.STOP).send(this);
    }

    private void cancel() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.cancel_job)
                .setTitle(R.string.warning)
                .setIcon(R.drawable.question)
                .setCancelable(true)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                isAborted = true;
                                //showWaitDalog();
                                stopService();
                                stopCount++;
                                if(MyUtil.dividesByTwo(stopCount)){
                                    Toast.makeText(Shell.this, R.string.stop_hint, Toast.LENGTH_LONG).show();
                                }
                                //Bin.killAll();
                                //done(ABORT_KEY, false);
                            }
                        })
                .setNegativeButton(android.R.string.no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void showLog() {
            progressDialog = ProgressDialog.show(this, null, getString(R.string.init), true);
            getLog();
    }

    private void getList() {
        new SendIntent(this, Commands.SERVICE, Commands.GET_LIST).send(this);
    }

    private void sendPing() {
        new SendIntent(this, Commands.SERVICE, Commands.PING).send(this);
    }


    private void removeJob(String id) {
        new SendIntent(this, Commands.SERVICE, Commands.REMOVE_JOB)
                .add(Commands.JOB_ID, id)
                .send(this);
    }

    private void getLog() {
        new SendIntent(this, Commands.SERVICE, Commands.GET_LOG).send(this);
    }

    private void sendNewJob(String in, String out, String args){
        new SendIntent(this, Commands.SERVICE, Commands.ADD_JOB)
                .add(Commands.IN_FILE, in)
                .add(Commands.OUT_FILE, out)
                .add(Commands.AGRS, args)
                .send(this);
    }

    private void copyLog(String log) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.log), log);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show();
    }


    private void close() {
        hideWaitDalog();
        //EncodeService.clearLog();
        hideAllNotice();
        stopService(new Intent(this, EncodeService.class));
        Intent intent = getIntent();
        try {
            intent.removeFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.removeFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (NoSuchMethodError e){
            Log.e("Intent.removeFlags", e.toString(), e);
        }
        setResult(RESULT_OK, intent);
        finish();
        //Intent myIntent = new Intent(this, Gui.class);
        //startActivity(myIntent);

    }


    private void setLog(final String log) {

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.LogAlertDialogStyle);
        builder.setMessage(log)
                .setIcon(R.drawable.full_log)
                .setCancelable(true)
                .setTitle(R.string.log)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (USE_DIALOG) close();
                    }
                })
                .setNeutralButton(R.string.copy, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        copyLog(log);
                        if (USE_DIALOG) close();
                    }
                })
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                if (USE_DIALOG) close();
                            }
                        });

        AlertDialog d = builder.create();
        d.show();
        TextView tv = d.findViewById(android.R.id.message);
        if (tv != null) {
            tv.setTextAppearance(this, R.style.LogAlertDialogStyle_Subhead);
            tv.setBackgroundColor(getResources().getColor(R.color.dark));
        }
    }

    private final BroadcastReceiver commandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getExtras() != null) {
                String cmd = intent.getExtras().getString(COMMAND);
                if (cmd == null) {
                    return;
                }
                Log.d("COMMAND", cmd);
                switch (cmd) {
                    case ON_INFO:
                        final String line = intent.getExtras().getString(LINE);
                        final int progress = intent.getExtras().getInt(PROGRESS);
                        setInfo(line, progress);
                        break;
                    case ON_DONE:
                        final String stopLine = intent.getExtras().getString(LINE);
                        final int result = intent.getExtras().getInt(RESULT);
                        done(stopLine, result);
                        break;
                    case ON_ABORT:
                        close();
                        break;
                    case ON_LOG:
                        final String log = intent.getExtras().getString(LOG);
                        setLog(log);
                        break;
                    case ON_LIST:
                        final  String listData =  intent.getExtras().getString(LIST_DATA);
                        createListMenu(listData);
                        break;
                }

            }
        }
    };


    private void startShellGuiActivity(){

        if(getMode() > 0){
            setResult(RESULT_OK, getIntent());
            finish();
        } else {
            Intent myIntent = new Intent(this, Gui.class);
            startActivity(myIntent);
            finish();
        }


    }


    private void createListMenu(String listData){

        if(listLayout.getChildCount() > 0){
            listLayout.removeAllViews();
        }

        final LayoutInflater inflater = getLayoutInflater();

        List<Job> jobs = BatchJob.getGobsFronJson(listData);

        for (Job job : jobs){
            View view = inflater.inflate(R.layout.job, null);
            ImageView iconView = view.findViewById(R.id.jobIcon);
            iconView.setImageResource(job.getStatus());
            TextView inView = view.findViewById(R.id.jobIn);
            TextView outView = view.findViewById(R.id.jobOut);
            inView.setText(StrUtil.getFileNamePretty(job.in));
            outView.setText(StrUtil.getFileNamePretty(job.out));
            inView.setSelected(true);
            outView.setSelected(true);
            final String id = job.getId();
            final ImageButton remove = view.findViewById(R.id.jobRemove);
            if(job.getStatus() == JsonInterface.S_WAIT || job.getStatus() == JsonInterface.S_CURRENT){
                remove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeJob(id);
                    }
                });

            } else {
                remove.setImageResource(R.drawable.remove_job2);
                remove.setEnabled(false);
            }


            listLayout.addView(view);
        }

    }
}
