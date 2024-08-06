package com.silentlexx.ffmpeggui_os.model;

public class TextFragment {
    public String fullText;
    public String fragment;
    public int startPosition;
    public int endPosition;

    public TextFragment(){
    }

    public TextFragment(String fullText, String fragment, int startPosition, int endPosition){
        this.fullText = fullText;
        this.fragment = fragment;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }


    public TextFragment getFragment(){
        this.fragment = fullText.substring(startPosition, endPosition);
        return this;
    }
}
