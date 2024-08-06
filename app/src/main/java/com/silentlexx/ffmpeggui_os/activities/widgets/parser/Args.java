package com.silentlexx.ffmpeggui_os.activities.widgets.parser;

import android.content.Context;

import com.silentlexx.ffmpeggui_os.R;

public class Args extends ParserItem  {

    private Context context;

    public Args(Context context) {
        this.context = context;
    }

    public Args setDefault(){
        String[] arr = context.getResources().getStringArray(R.array.args);
        for (String line : arr){
            String[] p = line.split("\\|");
            if(p.length == 3) {
                try {
                    put(p[0], p[1], p[2]);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return this;
    }


    public Args add(String arg, String name, ParserType type){
        put(arg, name, type);
        return this;
    }




}
