package com.silentlexx.ffmpeggui_os.activities.widgets.parser;

import java.util.ArrayList;
import java.util.List;

public abstract class ParserItem implements ParserSerializable {

    private List<String> args = new ArrayList<>();
    private List<String> names = new ArrayList<>();
    private List<ParserType> types = new ArrayList<>();

    protected void put(String arg, String name, String type){
        args.add(arg);
        names.add(name);
        types.add(ParserType.getParseType(type));
    }

    protected void put(String arg, String name, ParserType type){
        args.add(arg);
        names.add(name);
        types.add(type);
    }

    public String getName(int position){
        if(position >= 0 && position < names.size()){
            return names.get(position);
        } else return "";
    }

    public String getArg(int position){
        if(position >= 0 && position < args.size()){
            return args.get(position);
        } else return "";
    }

    public ParserType getType(int position){
        if(position >= 0 && position < types.size()){
            return types.get(position);
        } else return ParserType.NONE;
    }

    public void remove(int position){
        if(position >= 0 && position < args.size()){
            args.remove(position);
            names.remove(position);
            types.remove(position);
        }
    }

    public int findPositionByArg(String arg){
        int i=0;
        for (String s : args){
            if(s.equals(arg)){
                return i;
            }
            i++;
        }

        return -1;
    }

    public void removeByArg(String arg){
        int pos = findPositionByArg(arg);
        if(pos > -1){
            remove(pos);
        }
    }

    public String[] getNames(){
        int i=0;
        String[] arr = new String[names.size()];
        for (String name : names){
            //String str = args.get(i) + "  " + name;
            arr[i] = name;
            i++;
        }
        return arr;
    }
}
