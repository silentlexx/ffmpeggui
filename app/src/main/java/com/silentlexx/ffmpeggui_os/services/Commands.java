package com.silentlexx.ffmpeggui_os.services;

public interface Commands {

    String COMMAND = "command";
    //-----------------------------------
    String SERVICE = "com.silentlexx.ffmpeggui.services.EncodeService";

    String GET_LOG = "getLog";
    String STOP_JOB = "stopJob";
    String PING = "ping";
    String STOP = "stop";
    String KILL = "kill";
    String GET_LIST = "getList";
    String REMOVE_JOB = "removeJob";

    String JOB_ID = "jobId";


    //----------------------------------------------------------------

    String SHELL = "com.silentlexx.ffmpeggui.services.Shell";

    String ON_INFO = "onInfo";
    String ON_DONE = "onDone";
    String ON_ABORT = "onAbort";
    String ON_LOG = "onLog";
    String ON_LIST = "onList";


    String LINE = "line";
    String PROGRESS = "progress";
    String RESULT = "result";
    String LOG = "log";



    String ADD_JOB = "addJob";

    String IN_FILE = "inFile";
    String OUT_FILE = "OUT_FILE";
    String AGRS = "agrs";

    String LIST_DATA = "listData";

    //----------------------------------------------------------------

    String GUI = "com.silentlexx.ffmpeggui.services.Gui";


}
