package com.silentlexx.ffmpeggui_os.activities.widgets;

import android.content.DialogInterface;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.silentlexx.ffmpeggui_os.R;
import com.silentlexx.ffmpeggui_os.activities.AppActivity;
import com.silentlexx.ffmpeggui_os.activities.widgets.parser.Args;
import com.silentlexx.ffmpeggui_os.activities.widgets.parser.DefaultArgs;
import com.silentlexx.ffmpeggui_os.activities.widgets.parser.ParserSerializable;
import com.silentlexx.ffmpeggui_os.activities.widgets.parser.ParserType;
import com.silentlexx.ffmpeggui_os.model.TextFragment;
import com.silentlexx.ffmpeggui_os.parts.Bin;
import com.silentlexx.ffmpeggui_os.utils.StrUtil;

public class Parser implements DefaultArgs {

    private enum Action {
        NEW,
        ARG,
        DAT
    }

    private Args args;

    private Action action = Action.NEW;

    private AppActivity appActivity;
    private TextFragment fragment;
    private int viewId;
    private ParserType type = ParserType.NONE;

    public Parser(AppActivity activity,  int textEditId) {
        this.appActivity = activity;
        this.viewId = textEditId;
         args = new Args(activity);

    }

    public Parser lib(ParserType type){

        this.type = type;

        String prefixLib = "", prefixArg = "", title = null;

        switch (type){
            case VIDEO_LIB:
                title = appActivity.getString(R.string.vc_btn);
                prefixArg = VIDEO_CODEC_PREFIX;
                prefixLib = "V";
                args.add(VIDEO_CODEC_NONE, NONE, ParserType.VIDEO_LIB);
                break;
            case AUDIO_LIB:
                title = appActivity.getString(R.string.ac_btn);
                prefixArg = AUDIO_CODEC_PREFIX;
                prefixLib = "A";
                args.add(AUDIO_CODEC_NONE, NONE, ParserType.AUDIO_LIB);
                break;
        }

        args.add(prefixArg + " " + CODEC_COPY, CODEC_COPY, type);


        try {

            String outs[] = Bin.getBinEncoders(appActivity).split("\n");
            for (String line : outs){
                line = line.replaceAll("D",""); //TODO workaround D
                line = StrUtil.normalizeText(line);
                if(line.startsWith(prefixLib)){
                    String name = StrUtil.normalizeText(StrUtil.cropFrom(line , '.'));
                    String lib = name.split(" +")[0];
                    ///Log.d("LINE", line);
                    if(!lib.startsWith("=")) {
                        args.add(prefixArg + " " + lib, name, type);
                    }
                }
            }


        } catch (Exception e){
            e.printStackTrace();
        }


        openDialog(title, args);

        return this;
    }


    @Deprecated
    public Parser(AppActivity activity, TextFragment fragment, int viewId){

        this.appActivity = activity;
        this.fragment = fragment;
        this.viewId = viewId;

        if(fragment.fragment.isEmpty()){

            action = Action.NEW;

            Args parse = new Args(activity);
            openDialog(null, parse);


        } else if(fragment.fragment.startsWith("-")){
            action = Action.ARG;
            Args parse = new Args(activity);
            parse.removeByArg(fragment.fragment);
            openDialog(null, parse);
        } else {
            action = Action.DAT;

        }

        //TODO
       Toast.makeText(appActivity, fragment.fragment, Toast.LENGTH_SHORT).show();
    }


    private void openDialog(String title, final ParserSerializable parserItem){
        AlertDialog.Builder builder = new AlertDialog.Builder(appActivity);
        if(title!=null && !title.isEmpty()) {
            builder.setTitle(title);
        }
        builder.setItems(parserItem.getNames(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ParserType type =  parserItem.getType(which);
                if(type == ParserType.VIDEO_LIB || type == ParserType.AUDIO_LIB) {
                    onChooseLib(parserItem.getArg(which), type);
                }
            }
        });
        builder.show();
    }

    private void onChooseLib(String str, ParserType type){
        String text = appActivity.getEditText(viewId);

        String prefCodec = "", prefNone = "";

        switch (type){
            case VIDEO_LIB:
                prefCodec = VIDEO_CODEC_PREFIX;
                prefNone = VIDEO_CODEC_NONE;
                break;
            case AUDIO_LIB:
                prefCodec = AUDIO_CODEC_PREFIX;
                prefNone = AUDIO_CODEC_NONE;
                break;
        }

        try {

            text = StrUtil.normalizeText(text);

            if(text.contains(prefCodec)){
                final String word = prefCodec + " " + StrUtil.getNextWord(text, prefCodec);
                text = text.replace(word, " " + str + " ");

            } else if(text.contains(prefNone)){
                text = text.replace(prefNone, " " + str + " ");
            } else {
                text = text + " " + str;
            }

            text = StrUtil.normalizeText(text);

        } catch (Exception e){
            e.printStackTrace();
        }



        appActivity.setEditText(viewId, text);
    }



}
