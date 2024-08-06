package com.silentlexx.ffmpeggui_os.model;

import android.content.Context;
import android.net.Uri;

public class UriWithContext {
    public Uri uri;
    public Context context;

    public UriWithContext(Context context, Uri uri){
        this.context = context;
        this.uri = uri;
    }
}
