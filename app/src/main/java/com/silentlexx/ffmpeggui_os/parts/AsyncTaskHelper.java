package com.silentlexx.ffmpeggui_os.parts;

import java.io.File;

public interface AsyncTaskHelper {
    void onExecuteString(String str);
    void onExecuteFile(File file);
    void onExecuteInt(int i);
}
