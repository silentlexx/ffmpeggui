package com.silentlexx.ffmpeggui_os.model;

import android.util.Log;

import com.silentlexx.ffmpeggui_os.parts.Bin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BatchJob implements JsonInterface {

    private List<Job> unDoneJobs = new ArrayList<>();
    private List<Job> doneJobs = new ArrayList<>();
    private Job current = null;

    private int lastResult = -1;

    public BatchJob() {
        unDoneJobs.clear();
        doneJobs.clear();
    }

    public BatchJob add(String in, String out, String agrs) {
        Log.d("ADD", agrs);

        Job job = new Job(in, out, agrs);

        boolean isPresent = current != null && current.getId().equals(job.getId());

        if(!isPresent)
        for(Job jb : unDoneJobs){
            if(jb.getId().equals(job.getId())){
                isPresent = true;
                break;
            }
        }

        if(!isPresent)
        for(Job jb : doneJobs){
            if(jb.getId().equals(job.getId())){
                isPresent = true;
                break;
            }
        }


        if(!isPresent) {
            unDoneJobs.add(job);
        }
        return this;
    }

    public int all() {
        return unDoneJobs.size() + doneJobs.size();
    }

    public boolean isAllDone(){
        return current == null && unDoneJobs.size() == 0;
    }

    public int done() {
        return doneJobs.size();
    }

    public int unDone() {
        return unDoneJobs.size();
    }

    public boolean isJobGo() {
        return current != null;
    }

    public Job getJob() {
        //Log.d("JOBS", "in: " + current.in + ", out: " + current.out);
        return current;
    }

    public BatchJob getNextJob() {
        if (unDone() > 0) {
            current = unDoneJobs.get(0);
            unDoneJobs.remove(0);
        } else {
            current = null;
        }
        return this;
    }

    public BatchJob setCurrentJobDone(int result) {
      //  Log.d("CURRENT", current.agrs);
        if(current==null){
            return this;
        }
        this.lastResult = result;
        current.setResult(result);
        doneJobs.add(current);
        current = null;
        return this;
    }


    public String getJsonList() {
        JSONArray json = new JSONArray();

        for (Job job : doneJobs) {

            int res = S_DONE;

            if(job.getResult() > 0){
                if(job.getResult() == Bin.ABORTED_CODE){
                    res = S_ABORT;
                } else {
                    res = S_FAIL;
                }
            }

            json.put(getJsonFromJob(job, res));
        }


        if(current!=null) {
            json.put(getJsonFromJob(current, S_CURRENT));
        }

        for (Job job : unDoneJobs) {
            json.put(getJsonFromJob(job, S_WAIT));
        }



        return json.toString();
    }


    private JSONObject getJsonFromJob(Job job, int status) {
        JSONObject json = new JSONObject();
        try {
            json.put(INPUT, job.in);
            json.put(OUTPUT, job.out);
            json.put(AGRS, job.agrs);
            json.put(STATUS, status);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static List<Job> getGobsFronJson(String str){
        List<Job> jobs = new ArrayList<>();

        try {
            JSONArray jsonArr = new JSONArray(str);

            for (int i = 0; i < jsonArr.length(); i++){
                JSONObject json = jsonArr.getJSONObject(i);
                String in = json.getString(INPUT);
                String out = json.getString(OUTPUT);
                String agrs = json.getString(AGRS);
                int status = json.getInt(STATUS);
                Job job = new Job(in, out, agrs, status);
                jobs.add(job);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jobs;
    }


    public int getLastResult(){
        return lastResult;
    }

    public boolean removeJob(String id){
        if(current != null && current.getId().equals(id)){
            return true;
        } else {
            Iterator<Job> iterator = unDoneJobs.iterator();
            while(iterator.hasNext()){
                Job job = iterator.next();
                if(job.getId().equals(id)){
                    job.setResult(Bin.ABORTED_CODE);
                    doneJobs.add(job);
                    iterator.remove();
                }
            }
        }

        return false;
    }


    public void abortAll() {
      //  if(current!=null){
      //      current.setResult(Bin.ABORTED_CODE);
      //      doneJobs.add(current);
      //      current = null;
     //   }
        Iterator<Job> iterator = unDoneJobs.iterator();
        while(iterator.hasNext()){
            Job job = iterator.next();
            job.setResult(Bin.ABORTED_CODE);
            doneJobs.add(job);
            iterator.remove();
        }
    }
}
