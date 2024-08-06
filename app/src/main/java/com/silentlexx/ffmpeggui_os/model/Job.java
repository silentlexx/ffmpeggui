package com.silentlexx.ffmpeggui_os.model;

import com.silentlexx.ffmpeggui_os.parts.Bin;
import com.silentlexx.ffmpeggui_os.utils.StrUtil;

public class Job {
    public String in;
    public String out;
    public String agrs;
    private int status = JsonInterface.S_WAIT;
    private int res = -1;

    public Job(String in, String out, String agrs){
        this.in = in;
        this.out = out;
        this.agrs = agrs;
    }

    public Job(String in, String out, String agrs, int status){
        this.status = status;
        this.in = in;
        this.out = out;
        this.agrs = agrs;
    }

    public void setResult(int res){
        this.res = res;
    }

    public int getResult(){
        return res;
    }

    public boolean isDone(){
        return res > -1;
    }

    public boolean isSuccess(){
        return res == 0;
    }

    public boolean isAborted(){
        return res == Bin.ABORTED_CODE;
    }

    public String getId(){
        return StrUtil.getMd5(agrs);
    }

    public int getStatus() {
        return status;
    }
}