package com.silentlexx.ffmpeggui_os.model;

import android.content.Context;

public class EncodeCommand {
   final public Context context;
   final public String command;

   public EncodeCommand(Context context, String command){
       this.context = context;
       this.command = command;
   }
}
