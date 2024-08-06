package com.silentlexx.ffmpeggui_os.services;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.silentlexx.ffmpeggui_os.R;
import com.silentlexx.ffmpeggui_os.activities.Gui;
import com.silentlexx.ffmpeggui_os.activities.Shell;
import com.silentlexx.ffmpeggui_os.config.Config;
import com.silentlexx.ffmpeggui_os.model.BatchJob;
import com.silentlexx.ffmpeggui_os.model.EncodeCommand;
import com.silentlexx.ffmpeggui_os.model.FileInfo;
import com.silentlexx.ffmpeggui_os.model.Job;
import com.silentlexx.ffmpeggui_os.parts.AsyncTaskHelper;
import com.silentlexx.ffmpeggui_os.parts.Bin;
import com.silentlexx.ffmpeggui_os.parts.NotificationHelper;
import com.silentlexx.ffmpeggui_os.utils.FileUtil;
import com.silentlexx.ffmpeggui_os.utils.MyUtil;
import com.silentlexx.ffmpeggui_os.utils.StrUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EncodeService extends Service implements AsyncTaskHelper, Commands {



    private static final int ALL = -1;
    private static final int ERROR = 5;
    private static final int SUCCES = 2;

    private static boolean isRun = false;
    private boolean serviceAlive = false;
    //private static boolean isSuccess = false;

    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    //public static final String SERVICE_PARAM = "service_param";
    // public static final String SERVICE_NAME = "FFmpegEncodeService";
    public final String TAG = "EncodeService";
    private static String line;
    private static List<String> log = new ArrayList<>();
    private PowerManager.WakeLock wakeLock = null;
    private final int TIMER_DELAY = 1000;
    private final Handler h = new Handler();
    private Outer mOuter;
    private Timer mTimer;
    private NotificationHelper mNotificationHelper;
    private String filesInfo = "";
    private String batchInfo = "";
    //  private NotificationHelper mNotificationHelperOnce;


    private BatchJob batchJob;
    private static long startTime;
    private String strTime = "";
    private String strLog = "";
    private String strProg = "";
    private String stopLine;
    private FileInfo inputFileDetailedInfo;
    private long stopTime;

    public String getLog() {
        return getLine(ALL);
    }


    public String getLine() {
        return line;
    }

    public String getLine(int n) {
        if (log == null) {
            return "";
        }

        if (n < 0 || n > log.size()) {
            n = log.size();
        }
        int l = log.size() - n;
        if (l < 0) {
            l = 0;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = l; i < log.size(); i++) {
            sb.append(log.get(i));
            sb.append("\n");
        }

        return sb.toString();
    }


    @Override
    public void onExecuteString(String str) {

    }

    @Override
    public void onExecuteFile(File file) {

    }

    @Override
    public void onExecuteInt(int result) {
        Log.d("PROC_res", Integer.toString(result));

        final int res = serviceAlive ? result : Bin.ABORTED_CODE;
        batchJob.setCurrentJobDone(res);
        if (batchJob.unDone() > 0 && serviceAlive) {
            execJob();
        } else {
            stopService(res);
        }
    }

    private class Outer extends TimerTask {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                info();
            }
        };

        @Override
        public void run() {
            h.post(r);
        }
    }


    private final static String SD = String.valueOf(Gui.SPACE_MASK);

    private static String parseArgs(final String args) {
        int i = 0;
        StringBuilder sb = new StringBuilder();
        for (String s : args.split("\"")) {
            i++;
            if (MyUtil.dividesByTwo(i)) {
                sb.append(s.replaceAll(" ", SD));
            } else {
                sb.append(s);
            }
        }
        return sb.toString();
    }


    private static String[] getCmd(Context context, String agrs) {
        if (agrs.startsWith(Bin.FFMPEG)) {
            agrs = agrs.substring(Bin.FFMPEG.length());
        }
        agrs = parseArgs(agrs);
        //Log.d("PARSE", agrs);
        final String[] a = agrs.split(" +");
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add(Bin.getBin(context)[0]);
        if(!Bin.getBin(context)[1].isEmpty()) cmd.add(Bin.getBin(context)[1]);
        for (String anA : a) {
            if (anA != null && !anA.isEmpty()) {
                String s = anA.replaceAll(SD, "\\ ");
                //Log.d("AGR", s);
                cmd.add(s);
            }
        }
        String[] arr = new String[cmd.size()];
        for (int i = 0; i < cmd.size(); i++) {
            arr[i] = cmd.get(i);
        }
        return arr;
    }

    private int getProgress() {

        if (inputFileDetailedInfo==null){
            return 0;
        }

        String frames;
        long frame_current = -1;
        long frame_size = inputFileDetailedInfo.frames;
        long dur = inputFileDetailedInfo.duration;


        Matcher m = Pattern.compile("frame=(.*[0-9]+).*fps")
                .matcher(line);
        if (m.find()) {
            frames = (m.toMatchResult().group(1)).replaceAll("[^\\d+]", "");
            if (frames.matches("[0-9]+")) {
                try {
                    frame_current = Integer.parseInt(frames);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }


        if (frame_current > -1 && frame_size > 0) {
            float buf = (float) frame_size / 100;
            float proc = (float) frame_current / buf;
            //Log.i(TAG,"Frame: "+ Float.toString(frame_size)+"   "+Float.toString(frame_current) + "   "+ Float.toString(proc));
            if (proc > 100) proc = 100;
            return (int) (proc);
        }


        String ts = null;
        m = Pattern
                .compile("time=([0-9]+:[0-9]+:[0-9]+)\\..*")
                .matcher(line);
        if (m.find()) {
            ts = m.toMatchResult().group(1);
        }

        if (ts != null) {
            long time = StrUtil.strToTime(ts);
            if (time > -1 && dur > 0) {
                float buf = (float) dur / 100;
                float proc = (float) time / buf;

                //Log.i(TAG,"Time: "+ Float.toString(dur)+" - "+Float.toString(time)+ "   "+ Float.toString(proc));


                if (proc > 100) proc = 100;
                return (int) (proc);
            }


        }


        return -1;
    }


    private static int processStart(Context context, String args) {
        String[] cmd = getCmd(context, args);
        Log.d("COMMAND", StrUtil.arrToString(cmd));

        int exit = 1;
        try {
            final Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader in_stream = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            String line;

            try {

                while ((line = in_stream.readLine()) != null) {
                    //if(line!=null) {
                    addToLog(line);
                    //}
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            //if(in_stream!=null){
            in_stream.close();
            //}
            p.waitFor();
            exit = p.exitValue();
            //if (p != null) {
            p.getOutputStream().close();
            p.getInputStream().close();
            p.getErrorStream().close();
            //		}

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return exit;
    }

    private static class Enc extends AsyncTask<EncodeCommand, Void, Integer> {

        @Override
        protected Integer doInBackground(EncodeCommand... cmd) {
            isRun = true;
            startTime = System.currentTimeMillis();
            return processStart(cmd[0].context, cmd[0].command);
        }

        @Override
        protected void onPostExecute(Integer result) {
            isRun = false;
            if (asyncTaskHelper != null) {
                asyncTaskHelper.onExecuteInt(result);
            }
            super.onPostExecute(result);
        }

    }


    private static AsyncTaskHelper asyncTaskHelper = null;

    public String getStopLine(boolean res, boolean all) {
        int last = ALL;
        if (!all) {
            if (res) {
                last = SUCCES;
            } else {
                last = ERROR;
            }
        }

        stopLine = StrUtil.strings(batchInfo, "\n\n", filesInfo, "\n\n", strTime, ": ", StrUtil.msToTime(stopTime), "  \n", strLog, ":\n ", getLine(last));
        return stopLine;
    }

    public void stopService(int result) {


        if (wakeLock != null){
            try {
                wakeLock.release();
            } catch (RuntimeException e)  {
                Log.e(TAG, e.toString(), e);
            }
        }

        sendList();

        stopBroadcast();
        //mNotificationHelperOnce = new NotificationHelper(this, NOTY_DONE);
        String m;
        if (result == 0) {
            m = getString(R.string.job_good);
        } else if (result == Bin.ABORTED_CODE) {
            m = getString(R.string.job_abort);
        } else {
            m = getString(R.string.job_bad);

        }


        if (!Shell.isAborted) {
            if (line == null) {
                line = "Error";
            }
            Config cfg = new Config(this);
            cfg.setEnd(true, result == 0, line);
            stopTime = System.currentTimeMillis() - startTime;
            String line = StrUtil.strings(strTime, ": ", StrUtil.msToTime(stopTime), ". ", strLog, ": ", getLine());
            mNotificationHelper.createNotificationComplete(StrUtil.strings("FFmpeg: ", m), line, result);
            sendOnDone(getStopLine(result == 0, false), result);

        } else {
            sendOnAbort();
            stopForeground(true);
            System.exit(0);
            return;

        }


        stopForeground(true);


    }


    @SuppressLint({"InvalidWakeLockTag", "WakelockTimeout"})
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return startId;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, new IntentFilter(SERVICE), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(commandReceiver, new IntentFilter(SERVICE));
        }


        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        strTime = getString(R.string.time_el);
        strLog = getString(R.string.log);
        strProg = getString(R.string.progress);
        String in = intent.getStringExtra(Bin.IN);
        String out = intent.getStringExtra(Bin.OUT);
        String agrs = intent.getStringExtra(Bin.AGRS);
        if (in != null && out != null && agrs != null) {
            serviceAlive = true;
            batchJob = new BatchJob();
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            wakeLock.acquire();
            log.clear();
            addJob(in, out, agrs);
            execJob();
            startBroadcast();
        }
        return START_STICKY;
    }

    private void addJob(String in, String out, String agrs) {
        //Log.d("ADDJOB", agrs);
        if (in.contains("*")) {
            addDirBatch(in, out, agrs);
        } else {
            batchJob.add(in, out, agrs);
        }
        batchInfo = getBatchInfo(this, batchJob);

    }

    private void addDirBatch(String in, String out, String agrs) {
        final File[] files = FileUtil.getDirFileList(in);
        if(files != null && files.length > 0) {
            for (File file : files) {
                final String fileName = StrUtil.cropTo(file.getName(), '.');
                final String inFile = file.getAbsolutePath();
                final String outFile = out.replace("*", fileName);


                //FIXME
                final String agrs2 = agrs.replace(in, inFile).replace(out, outFile);

                //Log.d("Files", agrs + " --> " + agrs2 );

                batchJob.add(inFile, outFile, agrs2);
            }
        }
    }


    private void execJob() {
        final Job job = batchJob.getNextJob().getJob();

        sendList();

        if (job == null) {
            sendOnAbort();
            return;
        }

        batchInfo = getBatchInfo(this, batchJob);
        filesInfo = getFileInfo(this, job.in, job.out);
        inputFileDetailedInfo = Bin.getFileInfo(this, job.in);
        addToLog(job.agrs + "\n");
        asyncTaskHelper = this;
        new Enc().execute(new EncodeCommand(this, job.agrs));
    }

    private static void addToLog(String l) {
        line = l;

        if(log.size() < MAX_ARRAY_SIZE) {
            try {
                log.add(l);
            } catch (OutOfMemoryError e){
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
       // createNotificationChannel();
    }



    @Override
    public void onDestroy() {
        Bin.killAllKill(this);

        if (mNotificationHelper != null && mNotificationHelper.isAlive()) {
            mNotificationHelper.completed();
        }
        try {
            unregisterReceiver(commandReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //	if(mNotificationHelperOnce !=null && mNotificationHelperOnce.isAlive()){
        //		   mNotificationHelperOnce.completed();
        //		}

        Log.d("Service", "Destroyed!");
        super.onDestroy();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void info() {
        if (!isRun) return;
        long time = System.currentTimeMillis() - startTime;
        int procent = getProgress();
        String line =
                StrUtil.strings(batchInfo, "\n\n", filesInfo, "\n\n", strTime, ": ", StrUtil.msToTime(time), "\n\n", strLog, ":\n", getLine());

        //	if(shell!=null){
        //		shell.onSetText(line, procent);
        //	}
        sendInfo(line, procent);
        if (!Shell.isAborted) {
            line = getBatchInfo(this, batchJob) + ".  ";
            if (procent > -1) {
                line += StrUtil.strings(strProg, ": ", Integer.toString(procent), "% / ", strTime, ": ", StrUtil.msToTime(time));
            } else {
                line += StrUtil.strings(strTime, ": ", StrUtil.msToTime(time), ". ", strLog, ": ", getLine());
            }
            mNotificationHelper.progressUpdate(line);
        }


    }


    private void startBroadcast() {
        mNotificationHelper = new NotificationHelper(this);
        Notification notification = mNotificationHelper.createNotification();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            if (notification != null) {
                startForeground(NotificationHelper.PROGRESS, notification);
            }
        }
        mTimer = new Timer();
        mOuter = new Outer();
        mTimer.schedule(mOuter, 0, TIMER_DELAY);
    }

    private void stopBroadcast() {
        if (mOuter != null) {
            mOuter.cancel();
            mTimer.cancel();
            mTimer.purge();
            mOuter = null;
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
                    case GET_LOG:
                        sendLog();
                        break;
                    case PING:
                        if (batchJob.isAllDone()) {
                            sendOnDone(stopLine, batchJob.getLastResult());
                        }
                        break;
                    case KILL:
                        stopServiceRemote();
                        Bin.killAllKill(EncodeService.this);
                        break;
                    case STOP:
                        stopServiceRemote();
                        Bin.killAll(EncodeService.this);
                        break;
                    case ADD_JOB:
                        final String in = intent.getExtras().getString(IN_FILE);
                        final String out = intent.getExtras().getString(OUT_FILE);
                        final String agrs = intent.getExtras().getString(AGRS);
                        addJob(in, out, agrs);
                        break;
                    case GET_LIST:
                       sendList();
                        break;
                    case REMOVE_JOB:
                        final String id = intent.getExtras().getString(JOB_ID);
                        removeJob(id);
                        break;
                }

            }
        }
    };

    private void removeJob(String id) {
        Log.d("JOB_ID", id);
        if(batchJob.removeJob(id)){
            Bin.killAll(this);
        }
        sendList();
    }


    private void sendList() {
        new SendIntent(this, Commands.SHELL, Commands.ON_LIST)
                .add(Commands.LIST_DATA, batchJob.getJsonList())
                .send(this);

    }

    private void sendLog() {
        new SendIntent(this, Commands.SHELL, Commands.ON_LOG)
                .add(LOG, getLog())
                .send(this);

    }

    private void sendInfo(String curLine, int procent) {
        new SendIntent(this, Commands.SHELL, Commands.ON_INFO)
                .add(Commands.LINE, curLine)
                .add(PROGRESS, procent)
                //.add(BATCH_INFO, batchInfo)
                //.add(FILES_INFO, filesInfo)
                .send(this);

    }

    private void sendOnDone(String stopLine, int result) {
        new SendIntent(this, Commands.SHELL, Commands.ON_DONE)
                .add(LINE, stopLine)
                .add(RESULT, result)
                .send(this);
        new SendIntent(this, Commands.GUI, Commands.ON_DONE)
                .add(LINE, stopLine)
                .add(RESULT, result)
                .send(this);
    }

    private void sendOnAbort() {
        new SendIntent(this, Commands.SHELL, Commands.ON_ABORT)
                .send(this);
    }


    public static boolean isRun(Context c) {
        if(Config.USE_CHECK_BY_PROCESS_ONLY){
            return Bin.isProcessRunning(c);
        } else {

            if(Bin.isProcessRunning(c)) {
                ActivityManager manager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
                for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if (EncodeService.class.getName().equals(service.service.getClassName())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public static String getBatchInfo(Context context, BatchJob jobs) {
        return context.getString(R.string.converting) +
                ": " +
                Integer.toString(jobs.done() + 1) +
                " " +
                context.getString(R.string.from) +
                " " +
                Integer.toString(jobs.all() + 1);
    }

    public static String getFileInfo(Context context, String in, String out) {
        String secFile = "";

        if(in.contains("\n")){
            String arr[] = in.split("\n");
            if(arr.length>1){
                in = arr[0];
                secFile = context.getString(R.string.secondfile) + ": " + arr[1] + "\n\n";
            }
        }


        return context.getString(R.string.input_file) +
                ": " +
                in +
                "\n\n" +
                secFile +
                context.getString(R.string.output_file) +
                ": " +
                out;
    }

    public void stopServiceRemote() {
        serviceAlive = false;
        batchJob.abortAll();
    }

}
