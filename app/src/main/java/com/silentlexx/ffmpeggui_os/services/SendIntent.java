package com.silentlexx.ffmpeggui_os.services;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class SendIntent implements Commands {
    private final Intent intent;

    public SendIntent(Context context, String destenation, String command){
        intent = new Intent(destenation);
        intent.setPackage(context.getPackageName());
        intent.putExtra(COMMAND, command);
    }

    public SendIntent add(String key, String data){
        intent.putExtra(key, data);
        return this;
    }

    public SendIntent add(String key, boolean data){
        intent.putExtra(key, data);
        return this;
    }

    public SendIntent add(String key, int data){
        intent.putExtra(key, data);
        return this;
    }

    public SendIntent add(String key, float data){
        intent.putExtra(key, data);
        return this;
    }

    public SendIntent add(String key, long data){
        intent.putExtra(key, data);
        return this;
    }


    public void send(Context context){
        try {
            context.sendBroadcast(intent);
        } catch (RuntimeException e){
            e.printStackTrace();
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}
