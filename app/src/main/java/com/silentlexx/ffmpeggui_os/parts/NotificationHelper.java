package com.silentlexx.ffmpeggui_os.parts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.silentlexx.ffmpeggui_os.R;
import com.silentlexx.ffmpeggui_os.activities.Shell;
import com.silentlexx.ffmpeggui_os.config.Config;


public class NotificationHelper {
    //public static final String NOTIFICATION_MODE = "NOTIFICATION_MODE";
    private Context context;
    //private int notificationId;

    private static final String NOTY_CHANNEL_ID = "ffmpeg_channel_1";
    private static final String SOUND_CHANNEL_ID = "ffmpeg_channel_2";

    public static int PROGRESS = 1;
    public static int COMPLETE = 2;

    private int id = PROGRESS;

    private NotificationCompat.Builder notification;
    private NotificationManager notificationManager;

    private Config config;

    public NotificationHelper(Context context) {
        this.context = context;
        config = new Config(context);
    }


    public Notification createNotification() {
        id = PROGRESS;
        return createNotification(context.getString(R.string.work), "", R.drawable.job, false, false);
    }

    public synchronized void createNotificationComplete(String title, String text, int res) {
        notificationManager.cancel(PROGRESS);
        int icon;
        if (res == 0) {
            icon = R.drawable.job_done;
        } else if(res == Bin.ABORTED_CODE){
            icon = R.drawable.job_abort;
        } else {
            icon = R.drawable.job_error;
        }
        id = COMPLETE;
        createNotification(title, text, icon, res==0, true);
    }


    private Notification createNotification(String title, String text, int icon, boolean success, boolean once) {
        final Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getApplicationContext().getPackageName() + "/" + R.raw.ding);
        final boolean mute = config.isMute();
        //get the notification manager
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            if (success && once && !mute) {
               // notificationManager.deleteNotificationChannel(NOTY_CHANNEL_ID);
                CharSequence name2 = context.getString(R.string.complete);
                NotificationChannel mChannel2 = new NotificationChannel(SOUND_CHANNEL_ID, name2, NotificationManager.IMPORTANCE_DEFAULT);
                mChannel2.enableLights(false);
                mChannel2.enableVibration(false);


                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build();
                    mChannel2.setSound(soundUri, audioAttributes);

                notificationManager.createNotificationChannel(mChannel2);
            } else {
             //   notificationManager.deleteNotificationChannel(SOUND_CHANNEL_ID);
                CharSequence name1 = context.getString(R.string.progress);
                NotificationChannel mChannel1 = new NotificationChannel(NOTY_CHANNEL_ID, name1, NotificationManager.IMPORTANCE_LOW);
                mChannel1.enableLights(false);
                mChannel1.enableVibration(false);
                notificationManager.createNotificationChannel(mChannel1);
            }


        }

        //CharSequence tickerText = context.getString(R.string.work); //Initial text that appears in the status bar
        long when = System.currentTimeMillis();

        if (success && once && !mute) {
            notification = new NotificationCompat.Builder(context, SOUND_CHANNEL_ID);
        } else {
            notification = new NotificationCompat.Builder(context, NOTY_CHANNEL_ID);
        }



        Intent startIntent = new Intent(context, Shell.class);
        startIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);


        Intent cancelIntent = new Intent(context, Shell.class);
        cancelIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        cancelIntent.setAction(Shell.CANCEL);
        //       notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent mContentIntent = PendingIntent.getActivity(context, 0, startIntent, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent mCancelIntent = PendingIntent.getActivity(context, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE);
        notification.setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setWhen(when)
                .setOngoing(!once)
                .setAutoCancel(true)
                .setContentIntent(mContentIntent);

        if (success && once && !mute) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                try {
                    notification.setSound(soundUri);
                } catch (Exception e) {
                    Log.e("SOUND", e.toString());
                    e.printStackTrace();
                }
            }
        } else if(!once) {
            notification.addAction(R.drawable.cancel, context.getString(R.string.cancel), mCancelIntent);
        }

        Notification notification = this.notification.build();
        //show the notification

        notificationManager.notify(id, notification);

        return notification;
    }


    public synchronized void progressUpdate(String text) {
        if (notification != null && id != COMPLETE) {
            notification.setContentText(text);
            notificationManager.notify(PROGRESS, notification.build());
        }
    }


    public boolean isAlive() {
        return notification != null;
    }


    public void completed() {
        //remove the notification from the status bar
        notificationManager.cancel(PROGRESS);
        notificationManager.cancel(COMPLETE);
        notification = null;
        //notificationManager=null;
    }


}
