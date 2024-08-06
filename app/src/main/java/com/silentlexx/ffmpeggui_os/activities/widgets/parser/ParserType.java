package com.silentlexx.ffmpeggui_os.activities.widgets.parser;

public enum ParserType {
    NONE("none"),
    VIDEO_LIB("vlib"),
    AUDIO_LIB("alib"),
    QUALITY("quality"),
    DIGITAL("digit"),
    BIT("bit"),
    ANY("any"),
    HZ("hz"),
    VIDEO_FORMAT("vformat"),
    AUDIO_FORMAT("aformat");

    private final String key;

    ParserType(final String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }

    public static ParserType getParseType(final String str){
        if(str == null || str.isEmpty()){
            return NONE;
        }
        for(ParserType type : ParserType.values()){
            if(type.toString().equals(str)){
                return type;
            }
        }

     return  NONE;
    }


}
