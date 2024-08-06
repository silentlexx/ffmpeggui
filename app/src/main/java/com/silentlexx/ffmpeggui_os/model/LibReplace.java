package com.silentlexx.ffmpeggui_os.model;

import java.util.ArrayList;
import java.util.List;

public class LibReplace {

    private List<String> fromList = new ArrayList<>();
    private List<String> toList = new ArrayList<>();

    LibReplace(){
        clear();
    }

    private void clear() {
        fromList.clear();
        toList.clear();
    }

    public LibReplace add(String from, String to){
        fromList.add(" "+from+" ");
        toList.add(" "+to+" ");
        return this;
    }

    String replace(String str){
        int n = fromList.size();
        if(n > 0 && n == toList.size()){
            for (int i = 0; i < n; i++){
                str = str.replaceAll(fromList.get(i), toList.get(i));
            }
            return str;
        } else {
            return str;
        }
    }

}
