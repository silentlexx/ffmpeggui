package com.silentlexx.ffmpeggui_os.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.silentlexx.ffmpeggui_os.R;
import com.silentlexx.ffmpeggui_os.activities.widgets.InputDialog;
import com.silentlexx.ffmpeggui_os.activities.widgets.Parser;
import com.silentlexx.ffmpeggui_os.activities.widgets.parser.ParserType;
import com.silentlexx.ffmpeggui_os.config.Config;
import com.silentlexx.ffmpeggui_os.model.EncodeCommand;
import com.silentlexx.ffmpeggui_os.model.FFmpeg;
import com.silentlexx.ffmpeggui_os.model.FFmpegChooser;
import com.silentlexx.ffmpeggui_os.model.Preset;
import com.silentlexx.ffmpeggui_os.model.TextFragment;
import com.silentlexx.ffmpeggui_os.model.UriWithContext;
import com.silentlexx.ffmpeggui_os.parts.AsyncTaskHelper;
import com.silentlexx.ffmpeggui_os.parts.Bin;
import com.silentlexx.ffmpeggui_os.services.Commands;
import com.silentlexx.ffmpeggui_os.services.EncodeService;
import com.silentlexx.ffmpeggui_os.utils.FileUtil;
import com.silentlexx.ffmpeggui_os.utils.MyUtil;
import com.silentlexx.ffmpeggui_os.utils.StrUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Gui extends AppActivity implements GuiWidgetInterface, AsyncTaskHelper {

    // STATIC

    public static final String TAG = "ffmpeggui";

    public static final String SHARED_PREFS_NAME = TAG + "-cfg";

    public static final int FILTER_MAN = 11;

    public static final char SPACE_MASK = '#';

    public static final char FILTERING_CHARS[] = {'\n'}; // , ';', '<', '>', '|', ';', '&', '$'};

    public static final int QCIF[] = {176, 144};
    public static final int CIF[] = {352, 288};
    public static final int QVGA[] = {320, 240};

    public static final int HD[] = {1280, 720};
    public static final int FHD[] = {1920, 1080};
    public static final int QHD[] = {2560, 1440};
    private static final int POST_NOTIFICATION = 221;

    private boolean is2In = false;

    private FloatingActionButton fabStartJob;
    private FloatingActionButton fabCurJob;

    private final static int DO_NOTHING = 10;
    private final static int DO_OPEN = 11;
    private final static int DO_OPEN2 = 13;
    private final static int DO_SAVE = 12;
    private final static int DO_SETTINGS = 14;

    private static final int NORMAL = 0;
    private static final int SECONDARY = 1;

    private boolean spinnerPresetIni = false;

    private long clickTime = System.currentTimeMillis();

    @SuppressLint("InlinedApi")
    private static final String[] PERM_SD = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private boolean isServiceRun = false;

    private enum GuiMode {
        BASIC,
        CONSOLE
    }

    private GuiMode guiMode = GuiMode.BASIC;

    @TargetApi(Build.VERSION_CODES.M)
    private boolean canAccessSD() {
        //FIXME workaround for Android 13
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return
                PackageManager.PERMISSION_GRANTED == checkSelfPermission(PERM_SD[0]) &&
                        PackageManager.PERMISSION_GRANTED == checkSelfPermission(PERM_SD[1]);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions,grantResults);
        if (requestCode == DO_NOTHING || requestCode == DO_OPEN || requestCode == DO_SAVE) {
            if (canAccessSD()) {
                doAfter(requestCode);
            }

        }
    }


    @Override
    public void onExecuteInt(int i) {
    }


    public void doAfter(int code) {
        switch (code) {
            case DO_NOTHING:
                firstRun();
                initFFChooser = false;
                setFFChooser();
                renameOut(false);
                break;
            case DO_OPEN:
                openFileDialog(REQUEST_LOAD, NORMAL);
                break;
            case DO_OPEN2:
                openFileDialog(REQUEST_LOAD, SECONDARY);
                break;
            case DO_SAVE:
                openFileDialog(REQUEST_SAVE, NORMAL);
                break;

        }
    }

    public void checkPerms(int code) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!canAccessSD()) {
                requestPermissions(PERM_SD, code);
            } else {
                doAfter(code);
            }
        } else {
            doAfter(code);
            if (code == DO_NOTHING) {
                firstRun();
                renameOut(false);
            }
        }
    }


    // LAYOUT


    // MAIN
    private Spinner spinnerPresets;
    private Spinner spinnerExt;
    private int currentPreset = 0;
    private final Handler h = new Handler();
    private CheckBox resChekbox;

    private List<Preset> presets;


    private boolean doResetFF = false;

    public void settings() {
        doResetFF = true;
        Intent myIntent = new Intent(this, Settings.class);
        startActivity(myIntent);
    }

    public static InputFilter inputFilters[] = new InputFilter[]{new InputFilter() {
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {

            for (int i = start; i < end; i++)
                for (char FILTERING_CHAR : FILTERING_CHARS) {
                    if (source.charAt(i) == FILTERING_CHAR) {
                        return "";
                    }
                }

            return null;
        }
    }};

    public static InputFilter numberFilter[] = new InputFilter[]{new InputFilter() {
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {

            for (int i = start; i < end; i++) {
                if (!Character.isDigit(source.charAt(i))) {
                    return "";
                }
            }

            return null;
        }
    }};

    @Override
    public int getRootView() {
        return R.layout.gui;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //overridePendingTransition(R.anim.in2, R.anim.out2);

        try {
            Class.forName("android.os.AsyncTask");
        } catch (Throwable ignore) {
            // ignored
        }

        is2In = config.get(Config.SECOND_INPUT, false);

        setSwitchInButton();

        findViewById(SECOND_IN_BTN).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                is2In = !is2In;
                setSwitchInButton();
            }
        });


        ((EditText) findViewById(IN)).setFilters(inputFilters);
        ((EditText) findViewById(IN)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                checkBatchJob(s.toString());
                checkInputFile(IN);
            }
        });

        ((EditText) findViewById(IN2)).setFilters(inputFilters);
        ((EditText) findViewById(IN2)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                // checkBatchJob(s.toString());
                checkInputFile(IN2);
            }
        });

        ((EditText) findViewById(OUT)).setFilters(inputFilters);
        ((EditText) findViewById(RES_X)).setFilters(numberFilter);
        ((EditText) findViewById(RES_Y)).setFilters(numberFilter);
        ((EditText) findViewById(AGRS)).setFilters(inputFilters);
        ((EditText) findViewById(CONSOLE)).setFilters(inputFilters);

        findViewById(BUILD_INFO).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMsgBox(Gui.this.getString(R.string.build_info), Bin.getBinVersion(Gui.this), false);
            }
        });

        findViewById(FILE_INFO).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileInfo(IN);
            }
        });

        findViewById(FILE_INFO2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileInfo(IN2);
            }
        });


        final View.OnClickListener switchGui = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchGui();
            }
        };
        findViewById(SWITCH_GUI).setOnClickListener(switchGui);

        spinnerExt = findViewById(SPINNER_EXT);

        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, Config.getExtensions(this));
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExt.setAdapter(adapter2);
        spinnerExt.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                renameOut(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        final String args = config.get(Config.AGRS, "");

        if (args.isEmpty()) {
            spinnerPresetIni = true;
        } else {
            setEditText(AGRS, args);
            spinnerExt.setSelection(Config.getExtPosition(config.get(Config.EXT)));
        }

        resChekbox = findViewById(RES_S);

        resChekbox.setChecked(config.getBool(Config.RES_BOX));
        setResBox();
        resChekbox.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                setResBox();
            }
        });

        presets = config.getPresets();

        currentPreset = config.getInt(Config.CURRENT);
        if (presets.size() <= currentPreset) {
            currentPreset = 0;
        }

        spinnerPresets = findViewById(R.id.preset);
        setPresets();
        spinnerPresets.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v,
                                       int position, long id) {
                if (spinnerPresetIni) {
                    setPresetPos(position);
                } else {
                    spinnerPresetIni = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }

        });

        spinnerPresets.setSelection(currentPreset);

        setEditText(IN, config.get(Config.IN, ""));
        setEditText(IN2, config.get(Config.IN2, ""));
        setEditText(OUT, config.get(Config.OUT, ""));
        String res[] = getRes();
        setEditText(RES_X, config.get(Config.RES_X, res[0]));
        setEditText(RES_Y, config.get(Config.RES_Y, res[1]));


        findViewById(RES_0)
                .setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        showRes();
                    }
                });

        findViewById(IN_BTN)
                .setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        checkPerms(DO_OPEN);

                    }
                });


        findViewById(IN_BTN2)
                .setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        checkPerms(DO_OPEN2);

                    }
                });

        findViewById(OUT_BTN)
                .setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        checkPerms(DO_SAVE);

                    }
                });

        findViewById(ADD)
                .setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        addPreset();
                    }
                });

        findViewById(DEL)
                .setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        deletePreset();
                    }
                });

        findViewById(HELP)
                .setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Help();
                    }
                });

        findViewById(BAK)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openBackupMenu();
                    }
                });

        if (Config.DEBUG) {
            findViewById(HELP).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    FFmpegChooser.removeBin(Gui.this);
                    initFfmpeg();
                    Toast.makeText(Gui.this, "Binary has refreshed!", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        fabCurJob = findViewById(CURJOB);
        fabCurJob.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startShellActivityTop();
            }
        });

        fabStartJob = findViewById(RUN);

        fabStartJob.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                run();
            }
        });


        findViewById(BATCH_JOB)
                .setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        prepareButchJob();
                    }
                });


        if (Config.EXPEREMENTAL) {
            final EditText editTextArgs = findViewById(AGRS);
            editTextArgs.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    long time = System.currentTimeMillis();
                    if ((time - clickTime) <= Config.DOUBLE_CL_TIME) {
                        String text = editTextArgs.getText().toString();
                        TextFragment fragmet = StrUtil.getWord(text, editTextArgs.getSelectionStart());
                        new Parser(Gui.this, fragmet, AGRS);
                    } else {
                        clickTime = time;
                    }
                }
            });
        }

        findViewById(VC_BTN).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doParse(ParserType.VIDEO_LIB);
            }
        });

        findViewById(AC_BTN).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doParse(ParserType.AUDIO_LIB);
            }
        });

        checkPerms(DO_NOTHING);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(Gui.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(Gui.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, POST_NOTIFICATION);
        }
    }

    private void doParse(ParserType type) {
        new Parser(this, AGRS)
                .lib(type);
    }


    private void initFfmpeg() {
        if (config != null) {
            config.initFfmpeg();
            if (config.getCurrent().isFail()) {
                errorDevice();
            }
        }
    }

    private void resetFFChooser() {
        if (config != null) {
            config.clearFfmpeg();
        }
        initFFChooser = false;
        setFFChooser();
    }


    private boolean initFFChooser = false;

    private void setFFChooser() {
        initFfmpeg();
        final Spinner ffVer = findViewById(SET_FF_VER);
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, config.getCurrent().getNames());
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ffVer.setAdapter(adapter1);
        ffVer.setSelection(config.getCurrent().getCurrentCode());
        ffVer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v,
                                       int position, long id) {
                if (initFFChooser) {
                    if (config.getFFmpeg(position).getType() != FFmpeg.Type.MESSAGE) {
                        FFmpegChooser.removeBin(Gui.this);
                        config.setCurrentBin(position);
                    } else {
                        showMessage(config.getFFmpeg(position));
                        initFFChooser = false;
                        ffVer.setSelection(config.getCurrent().getCurrentCode());
                    }
                } else {
                    initFFChooser = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }

        });

    }


    private void showMessage(FFmpeg fFmpeg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(fFmpeg.getMessage())
                .setTitle(fFmpeg.getName())
                .setIcon(R.drawable.information)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showFileInfo(final int in) {
        final String s = trimSpace(getEditText(in));
        final File file = new File(s);

        if (s.contains("://") || (file.exists() && file.isFile())) {
            showMsgBox(Gui.this.getString(R.string.file_info), Bin.getBinFileInfo(this, s), true);
            if (in == IN) {
                asyncTaskHelper = this;
                new SyncRes().execute(new EncodeCommand(this, getEditText(in).trim())); //FIXME
            }
        } else if (!s.contains("*")) {
            Toast.makeText(this, getString(R.string.open_file), Toast.LENGTH_SHORT).show();
            checkPerms(DO_OPEN);
        }
    }


    private void setResBox() {
        checkIsResEnable();
        boolean b = resChekbox.isChecked();
        findViewById(RES_X).setEnabled(b);
        findViewById(RES_Y).setEnabled(b);
        //((ImageButton) findViewById(RES_0)).setEnabled(b);

    }

    // BUTTONS IMPL

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        onIntent();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, new IntentFilter(Commands.GUI), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(commandReceiver, new IntentFilter(Commands.GUI));
        }

        isServiceRun = EncodeService.isRun(this);

        if (isServiceRun) {

            if (!Config.MULTIWIN) startShellActivityTop();

        } else {
            if (config.getBool(Config.GUI_MODE) && guiMode == GuiMode.BASIC) {
                switchGui();
                setEditText(CONSOLE, config.get(Config.LAST_CONSOLE, ""));
            }


            h.postDelayed(new Runnable() {

                @Override
                public void run() {
                    checkInputFile(IN); //FIXME is this needed???
                    if (is2In) checkInputFile(IN2); //FIXME is this needed???
                }
            }, 1000);
        }

        switchState();
        setFFChooser();
        onIntent();
    }
/*
	private void startShell() {
		config.setEnd(false, false);
		Intent myIntent = new Intent(this, Shell.class);
		startActivity(myIntent);
	}
*/


    private int setPresets() {
        String array[] = config.getNamesArray(presets);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, array);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPresets.setAdapter(adapter);
        return array.length;
    }


    public void onEnter(String name) {
        String arr[] = config.getNamesArray(presets);
        int n = -1;
        for (int i = 0; i < arr.length; i++) {
            if (name.equals(arr[i])) {
                n = i;
            }
        }
        String agrs = getEditText(AGRS);
        String ext = spinnerExt.getSelectedItem().toString();
        if (n > -1) {
            if (n > Preset.getSysPresetsCount() - 1) {
                presets.get(n).setArgs(agrs);
                presets.get(n).setType(ext);
                config.setPresets(presets);
                setPresets();
                spinnerPresets.setSelection(n);
            } else {
                Toast.makeText(this, getString(R.string.one_preset),
                        Toast.LENGTH_LONG).show();
            }
        } else {
            presets.add(new Preset(name, agrs, ext));
            config.setPresets(presets);
            n = setPresets();
            spinnerPresets.setSelection(n - 1);
        }

    }

    private void sendEmail() {
        String[] addr = new String[1];
        addr[0] = "silentlexx@gmail.com";

        try {

            Intent sendIntent = new Intent(Intent.ACTION_SEND,
                    Uri.parse("mailto:"));
            sendIntent.setClassName("com.google.android.gm",
                    "com.google.android.gm.ComposeActivityGmail");
            sendIntent.putExtra(Intent.EXTRA_EMAIL, addr);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                    TAG + " v." + MyUtil.getAppVer(this));
            startActivity(sendIntent);

        } catch (Exception e) {
            Toast.makeText(this, "Gmail not installed!", Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gui_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mail:
                sendEmail();
                break;
            case R.id.declam:
                showAbout();
                break;
            case R.id.settings:
                settings();
                break;
            case R.id.fsaccess:
                openFsAccessDialog();
                break;
            /*
             * case R.id.market: openMarket(); break;
             */
            case R.id.pp:
                //Ads.showPrivatePolicy(this, config, false);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAbout() {
        final String text = getString(R.string.info_text) +
                "\n\n" +
                getString(R.string.addbuild_message);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(text)
                .setTitle(TAG + " v." + MyUtil.getAppVer(this))
                .setIcon(R.drawable.information)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private String ffmpegAgrs = "";

    private String getArgs() {
        String in = getEditText(IN);
        String out = getEditText(OUT) + "."
                + spinnerExt.getSelectedItem().toString();
        String res_x = getEditText(RES_X);
        String res_y = getEditText(RES_Y);

        String agrs = getEditText(AGRS);
        final String SPACE = " ";
        StringBuilder cmd = new StringBuilder();
        cmd.append(Bin.FFMPEG);
        cmd.append(SPACE);
        cmd.append(Bin.PREFIX); //TODO create editor
        cmd.append(SPACE);
        cmd.append("-i");
        cmd.append(SPACE);
        cmd.append(StrUtil.trimFileName(in));
        cmd.append(SPACE);

        if (is2In) {
            String in2 = getEditText(IN2);
            cmd.append("-i");
            cmd.append(SPACE);
            cmd.append(StrUtil.trimFileName(in2));
            cmd.append(SPACE);
        }

        if (resChekbox.isChecked() && !agrs.contains("-s ")) {
            cmd.append("-s ");
            cmd.append(res_x);
            cmd.append("x");
            cmd.append(res_y);
            cmd.append(SPACE);
        } else if (resChekbox.isChecked()) {
            checkIsResEnable();
        }
        cmd.append(agrs);
        cmd.append(SPACE);
        cmd.append(StrUtil.trimFileName(out));
        return cmd.toString();
    }


    protected void run() {

        if (guiMode == GuiMode.BASIC) {
            final String outf = getEditText(OUT);
            final String in = getEditText(IN);
            final String in2 = getEditText(IN2);
            final String out = outf + "."
                    + spinnerExt.getSelectedItem().toString();


            if (in.contains("*")) {

                File inDir = new File(StrUtil.cropTo(in, '/'));

                if (!inDir.exists() || !inDir.isDirectory() || !inDir.canRead()) {
                    Toast.makeText(this, getString(R.string.incorrect_path) + " " + getString(R.string.input_file), Toast.LENGTH_LONG).show();
                    return;
                }

                File outDir = new File(StrUtil.cropTo(out, '/'));
                if (!outDir.exists() || !outDir.isDirectory() || !outDir.canWrite()) {
                    if (!outDir.mkdirs()) {
                        Toast.makeText(this, getString(R.string.incorrect_path) + " " + getString(R.string.output_file), Toast.LENGTH_LONG).show();
                        return;
                    }
                }

            } else {


                if (in.isEmpty() || !FileUtil.isPathIsCorrect(in) || !FileUtil.isPathIsCorrect(out)) {
                    Toast.makeText(this, getString(R.string.err), Toast.LENGTH_LONG).show();
                    renameOut(false);
                    return;
                }


                if(!in.contains("://")) {
                    File f = new File(in);
                    if (!f.exists() || f.isDirectory() || !f.canRead()) {
                        Toast.makeText(this, getString(R.string.choose_file), Toast.LENGTH_LONG).show();
                        openFileDialog(REQUEST_LOAD, NORMAL);
                        return;
                    }
                }


                if (is2In) {
                    File f = new File(in2);
                    if (!f.exists() || f.isDirectory() || !f.canRead()) {
                        Toast.makeText(this, getString(R.string.choose_file), Toast.LENGTH_LONG).show();
                        openFileDialog(REQUEST_LOAD, SECONDARY);
                        return;
                    }
                }

                if (in.equals(out)) {
                    Toast.makeText(this, getString(R.string.inout_same),
                            Toast.LENGTH_LONG).show();

                    return;
                }

            }

            ffmpegAgrs = getArgs();


            File test = new File(out);


            if (test.isDirectory()) {
                Toast.makeText(this, getString(R.string.error_output),
                        Toast.LENGTH_LONG).show();

                return;
            } else {
                File dir = test.getParentFile();
                if (!dir.canWrite()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.cantwrite)
                            .setTitle(R.string.warning)
                            .setIcon(R.drawable.question)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.yes,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int id) {

                                            String neuOut = FileUtil.getOutputPath(Gui.this) + "/" + StrUtil.getFileName(out, false);
                                            setTextOnView(OUT, neuOut);
                                            run();
                                        }
                                    })
                            .setNegativeButton(android.R.string.no,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int id) {
                                            dialog.cancel();
                                        }
                                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    return;
                }

            }

            if (test.exists()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.overwrite)
                        .setTitle(R.string.warning)
                        .setIcon(R.drawable.question)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        startShellActivity();
                                    }
                                })
                        .setNegativeButton(android.R.string.no,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        ffmpegAgrs = "";
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            } else {
                startShellActivity();
            }

        } else {
            ffmpegAgrs = getEditText(CONSOLE);
            startShellActivity();
        }
    }

    private void startShellActivityTop() {
        if (isServiceRun) {
            config.setEnd(false, false, "");
            Intent myIntent = new Intent(this, Shell.class);
            myIntent.putExtra(ACTIVITY_MODE, VIEW_ENCODING);
            startActivityForResult(myIntent, VIEW_ENCODING);
            try {
                unregisterReceiver(commandReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // if (Config.FINISH_ON_CLOSE) finish();
        }
    }


    private void startShellActivity() {
        //    progressDialog = ProgressDialog.show(this, null, getString(R.string.init), true);
        config.setEnd(false, false, "");
        Intent myIntent = new Intent(this, Shell.class);
        Bundle b = new Bundle();
        b.putString(Bin.AGRS, ffmpegAgrs);
        b.putString(Bin.IN, trimSpace(getEditText(IN)));
        b.putString(Bin.OUT, trimSpace(getEditText(OUT) + "."
                + spinnerExt.getSelectedItem().toString()));
        myIntent.putExtras(b);
        myIntent.putExtra(ACTIVITY_MODE, START_ENCODING);
        startActivityForResult(myIntent, START_ENCODING);
        ffmpegAgrs = "";
        try {
            unregisterReceiver(commandReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // if (Config.FINISH_ON_CLOSE) finish();
    }

    protected void Help() {

        final CharSequence[] items = this.getResources().getTextArray(
                R.array.help_array);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.show);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                    case 0:
                        showMsgBox(items[item], Bin.getBinHelp(Gui.this), true);
                        break;
                    case 1:
                        showMsgBox(items[item], Bin.getBinFormats(Gui.this), true);
                        break;
                    case 2:
                        showMsgBox(items[item], Bin.getBinCodecs(Gui.this), true);
                        break;

                    case 3:
                        showMsgBox(items[item], Bin.getBinDecoders(Gui.this), true);
                        break;
                    case 4:
                        showMsgBox(items[item], Bin.getBinEncoders(Gui.this), true);
                        break;
                    case 5:
                        showMsgBox(items[item], Bin.getBinFilters(Gui.this), true);
                        break;
                    case 6:
                        showMsgBox(items[item], Bin.getBinPixfmts(Gui.this), true);
                        break;
                    case 7:
                        showMsgBox(items[item], Bin.getBinVersion(Gui.this), false);
                        break;
                    default:
                        showMsgBox(items[1], Bin.getBinHelp(Gui.this), true);
                }
            }
        }).show();

    }

    public void showMsgBox(CharSequence title, String msg, boolean trim) {
        String output;
        if (trim) {
            String arr[] = msg.split("\n");
            StringBuilder sb = new StringBuilder();
            for (int i = FILTER_MAN; i < arr.length; i++) {
                sb.append(arr[i]);
                sb.append("\n");
            }
            output = sb.toString();
        } else {
            output = msg;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.LogAlertDialogStyle);
        builder.setMessage(output)
                .setCancelable(true)
                .setIcon(R.drawable.terminal)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog d = builder.create();
        d.show();
        TextView tv = d.findViewById(android.R.id.message);
        if (tv != null) {
            tv.setTextAppearance(this, R.style.LogAlertDialogStyle_Subhead);
            tv.setBackgroundColor(getResources().getColor(R.color.dark));
        }
    }

    protected void deletePreset() {
        final Context c = this;
        if (currentPreset > Preset.getSysPresetsCount() - 1) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.delete_preset)
                    .setTitle(R.string.warning)
                    .setIcon(R.drawable.question)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {

                                    presets.remove(currentPreset);
                                    config.setPresets(presets);
                                    setPresets();
                                    int n = currentPreset - 1;
                                    if (n < 0)
                                        n = 0;
                                    spinnerPresets.setSelection(n);

                                }
                            })
                    .setNegativeButton(android.R.string.no,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    dialog.cancel();
                                }
                            });
            builder.create().show();
        } else {
            Toast.makeText(c, R.string.one_preset, Toast.LENGTH_LONG).show();
        }
    }

    private void addPreset() {
        final int cur = spinnerPresets.getSelectedItemPosition();
        final boolean isSys = cur >= Preset.getSysPresetsCount();

        String name = presets.get(currentPreset).getName();

        if (!isSys) {
            List<String> list = new ArrayList<>();
            for (Preset preset : config.getPresets()) {
                list.add(preset.getName());
            }
            name = StrUtil.getNewName(name, list);
        }


        InputDialog dialog = new InputDialog(this, this, name);
        dialog.show();
    }

    // OTHER

    private void showRes() {
        final CharSequence[] items = this.getResources().getTextArray(
                R.array.screen_array);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.resize);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                int res[] = null;
                int orig[] = getResFromFile();
                switch (item) {
                    case 0:
                        res = orig;
                        break;
                    case 1:
                        res = makeRes(orig, getResInt());
                        break;
                    case 2:
                        res = makeRes(orig, getCustomRes());
                        break;
                    case 3:
                        res = getResInt();
                        break;
                    case 4:
                        res = QVGA;
                        break;
                    case 5:
                        res = QCIF;
                        break;
                    case 6:
                        res = CIF;
                        break;
                    case 7:
                        res = HD;
                        break;
                    case 8:
                        res = FHD;
                        break;
                    case 9:
                        res = QHD;
                        break;
                }


                resChekbox.setChecked(item != 0);
                setResBox();

                if (res != null && res.length == 2) {
                    setRes(res, orig);
                }

            }
        }).show();

    }

    private int[] getCustomRes() {
        int r[] = new int[2];
        try {
            r[0] = Integer.parseInt(getEditText(RES_X));
            r[1] = Integer.parseInt(getEditText(RES_Y));
        } catch (Exception e) {
            return null;
        }

        return r;
    }

    private void setRes(int[] r, int[] v) {
        if (r != null) {
            setEditText(RES_X, Integer.toString(r[0]));
            setEditText(RES_Y, Integer.toString(r[1]));
        }
        if (v != null) {
            setTextOnView(RES_ORIG,
                    "(" + getString(R.string.orig_res) + ": " + Integer.toString(v[0])
                            + "x" + Integer.toString(v[1]) + ")");
        } else {
            setTextOnView(RES_ORIG, "");
            // Toast.makeText(this, getString(R.string.cant_info),
            // Toast.LENGTH_LONG).show();
        }
    }


    private static AsyncTaskHelper asyncTaskHelper;

    static class SyncRes extends AsyncTask<EncodeCommand, Void, String> {

        @Override
        protected String doInBackground(EncodeCommand... cmd) {
            return Bin.getBinVideoInfo(cmd[0].context, cmd[0].command);
        }

        @Override
        protected void onPostExecute(String result) {
            if (asyncTaskHelper != null) {
                asyncTaskHelper.onExecuteString(result);
            }
            super.onPostExecute(result);
        }
    }

    @Override
    public void onExecuteString(String str) {
        setResFromFile(str);
    }

    private void checkInputFile(final int in) {
        final String str = getEditText(in);
        boolean fileExist = false;
        if (!str.isEmpty() && !str.equals("*")) {
            File file = new File(str);

            if (str.contains("://") || (file.exists() && !file.isDirectory() && file.canRead())) {
                fileExist = true;
                if (in == IN) {
                    asyncTaskHelper = this;
                    new SyncRes().execute(new EncodeCommand(this, getEditText(in).trim()));
                }
            }

        }


        if (!fileExist && in == IN) {
            setTextOnView(RES_ORIG, "");
        }

        final int btn = in == IN2 ? FILE_INFO2 : FILE_INFO; //FIXME

        findViewById(btn).setVisibility(fileExist ? View.VISIBLE : View.GONE);

    }

    private int[] getResFromFile() {
        return Bin.getRes(Bin.getBinVideoInfo(this, getEditText(IN).trim()));
    }

    private void setResFromFile(String s) {
        if (s == null) return;
        int v[] = Bin.getRes(s);
        if (v != null)
            setRes(null, v);
    }

    private int[] makeRes(int v[], int s[]) {
        if (v == null) {
            return null;
        }
        return calculateRes(v, s);
    }

    private int[] calculateRes(int[] f, int[] s) {
        int r[] = new int[2];
        int h;
        try {
            float scale = (float) f[0] / (float) s[0];
            h = Math.round((float) f[1] / scale);
            if ((h % 2) != 0) {
                h++;
            }
        } catch (NullPointerException e) {
            return r;
        }
        r[0] = s[0];
        r[1] = h;
        return r;
    }

    private String[] getRes() {
        int ir[] = getResInt();
        String r[] = new String[2];
        r[0] = Integer.toString(ir[0]);
        r[1] = Integer.toString(ir[1]);
        return r;
    }

    private int[] getResInt() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int x = size.x;
        final int y = size.y;
        int r[] = new int[2];
        if (x > y) {
            r[0] = x;
            r[1] = y;
        } else {
            r[1] = x;
            r[0] = y;
        }
        return r;
    }

    private void setPresetPos(int position) {
        currentPreset = position;
        config.set(Config.CURRENT, currentPreset);
        setEditText(AGRS, presets.get(position).getArgs());
        checkIsResEnable();
        int pos = presets.get(position).getPos();
        spinnerExt.setSelection(pos);
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(commandReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        save();
        // if (progressDialog != null && progressDialog.isShowing()) { progressDialog.dismiss(); }
        super.onPause();
    }


    private String trimSpace(String s) {
        //String o = s.replaceAll(" ", "\\ ").replaceAll("'", "\\'");//.replaceAll("\(", "\\(").replaceAll("\)", "\\)");
        return s.trim();
    }

    private void save() {
        if (config == null || (config.getCurrent() != null && config.getCurrent().isFail()))//FIXME
        {
            return;
        }

        config.set(Config.CURRENT, currentPreset);
        config.set(Config.IN, getEditText(IN));
        config.set(Config.IN2, getEditText(IN2));
        config.set(Config.OUT, getEditText(OUT));
        config.set(Config.RES_X, getEditText(RES_X));
        config.set(Config.RES_Y, getEditText(RES_Y));
        config.set(Config.AGRS, getEditText(AGRS));
        config.set(Config.EXT, spinnerExt.getSelectedItem().toString());
        config.set(Config.RES_BOX, resChekbox.isChecked());
        config.set(Config.GUI_MODE, guiMode == GuiMode.CONSOLE);
        config.set(Config.LAST_CONSOLE, getEditText(CONSOLE));
        config.set(Config.SECOND_INPUT, is2In);
    }

    private final int REQUEST_SAVE = 1;
    private final int REQUEST_LOAD = 2;


    private void openFileDialog(int req, int extra) {
        String startPath = "";
        String title = "";
        String newFile = "";
        int mode = FileDialog.SelectionMode.MODE_OPEN;
        switch (req) {
            case REQUEST_LOAD:
                startPath = StrUtil.cropTo(getEditText(IN), '/');
                title = extra == SECONDARY ? getString(R.string.secondfile) : getString(R.string.input_file);
                mode = FileDialog.SelectionMode.MODE_OPEN;
                break;
            case REQUEST_SAVE:
                startPath = StrUtil.cropTo(getEditText(OUT), '/');
                title = getString(R.string.output_file);
                mode = FileDialog.SelectionMode.MODE_CREATE;
                newFile = getEditText(IN);
                break;
        }

        if (startPath.isEmpty() || !new File(startPath).isDirectory() || !FileUtil.isPathIsCorrect(startPath)) {
            startPath = Config.getSdCardPath(this);
        }

        Intent intent = new Intent(getBaseContext(), FileDialog.class);
        intent.putExtra(FileDialog.START_PATH, startPath);

        intent.putExtra(FileDialog.SELECTION_MODE, mode);

        intent.putExtra(FileDialog.EXTRA_INT, extra);
        // can user select directories or not
        intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
        //if (req == REQUEST_LOAD)
        intent.putExtra(FileDialog.OPEN_ONLY, mode == FileDialog.SelectionMode.MODE_OPEN);
        // alternatively you can set file filter
        intent.putExtra(FileDialog.DIRECT_SELECTION, true);
        intent.putExtra(FileDialog.TITLE, title);
        intent.putExtra(FileDialog.NEW_FILE, newFile);

        startActivityForResult(intent, req);
    }

    public static final String DUMMY_EXT = "dummy";


    private void errorDevice() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.error_device)
                .setTitle(R.string.sorry)
                .setIcon(R.drawable.screen_error)
                .setCancelable(false)

                .setNegativeButton(R.string.close,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                finish();
                            }
                        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void switchGui() {
        switchGui(false);
    }

    private void switchGui(boolean force) {
        switch (guiMode) {
            case CONSOLE:

                if (!force && !getArgs().equals(getEditText(CONSOLE))) {
                    showConsoleQuitDialog();
                    return;
                }

                findViewById(WRP_GUI).setVisibility(View.VISIBLE);
                findViewById(WRP_CONSOLE).setVisibility(View.GONE);
                setEditText(CONSOLE, "");
                setIconOnFab(SWITCH_GUI, R.drawable.gui_console);
                guiMode = GuiMode.BASIC;
                break;
            case BASIC:
                findViewById(WRP_GUI).setVisibility(View.GONE);
                findViewById(WRP_CONSOLE).setVisibility(View.VISIBLE);
                setEditText(CONSOLE, getArgs());
                setIconOnFab(SWITCH_GUI, R.drawable.gui_basic);
                guiMode = GuiMode.CONSOLE;
                break;
        }

    }

    private void showConsoleQuitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.console_quit_dialog_msg)
                //.setTitle(R.string)
                .setIcon(R.drawable.question)
                .setCancelable(true)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switchGui(true);
                        dialog.cancel();
                    }
                })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    @Override
    public void onBackPressed() {
        // if (guiMode == GuiMode.CONSOLE) {
        //     switchGui();
        // } else {

        if (isServiceRun) {
            startShellActivityTop();
        } else {
            config.clearFfmpeg();
            finish();
            super.onBackPressed();
            // System.exit(0); //FIXME
            //super.onBackPressed();
        }
        // }
    }


    private void onIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        Uri uri = getIntent().getData() != null ? getIntent().getData() : (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);


        if (uri != null && (Intent.ACTION_VIEW.equals(action) ||
                Intent.ACTION_SEND.equals(action) ||
                Intent.ACTION_SENDTO.equals(action) ||
                Intent.ACTION_INSERT.equals(action)
        )) {

            if (guiMode == GuiMode.CONSOLE) {
                switchGui(true);
            }


            Log.d("Intent", "action: " + action + ", uri: " + uri.getPath());

            progressDialog = ProgressDialog.show(this, null,
                    getString(R.string.init), true);

            asyncTaskHelper = this;
            new GetIntentTask().execute(new UriWithContext(this, uri));


        }

    }

    static class GetIntentTask extends AsyncTask<UriWithContext, Void, File> {

        @Override
        protected File doInBackground(UriWithContext... uris) {
            return FileUtil.from(uris[0].context, uris[0].uri);
        }

        @Override
        protected void onPostExecute(File file) {
            if (asyncTaskHelper != null) {
                asyncTaskHelper.onExecuteFile(file);
            }
            super.onPostExecute(file);
        }
    }

    @Override
    public void onExecuteFile(File file) {
        if (file != null && file.exists()) {
            Log.d("Intent", "File: " + file.getAbsolutePath());
            setEditText(IN, file.getAbsolutePath());
            renameOut(true);
        }

        setIntent(new Intent());

        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (IllegalArgumentException e){
                Log.e(TAG, e.toString(), e);
            }
        }
    }


    private void renameOut(boolean forceRename) {
        String in = getEditText(IN);
        String out = getEditText(OUT);
        String ext = spinnerExt.getSelectedItem().toString();

        if (in.contains("*") && in.equals(out + "." + ext)) {
            out = out + "-1"; //FIXME
            setEditText(OUT, out);
            return;
        }

        if (!out.contains("://") && !out.contains("*")) {
            if (out.isEmpty() || !FileUtil.isPathIsCorrect(out)) {
                out = FileUtil.getOutputPath(this) + "/" + StrUtil.getFileName(in, false);
                setEditText(OUT, out);
            }
        }


        if (!in.contains("://") && !in.contains("*")) {
            File fileIn = new File(in);
            if (in.isEmpty() || !fileIn.exists() || fileIn.isDirectory() || !FileUtil.isPathIsCorrect(in)) {
                setEditText(IN, Config.getSamplePath(this, false));
                setEditText(OUT, StrUtil.cropTo(Config.getSamplePath(this, true) , '.'));
            }

            if (!fileIn.exists() || fileIn.isDirectory()) {
                return;
            }

        }


        if (forceRename || in.equals(out + "." + ext)) {

            String path = StrUtil.cropTo(out, '/');
            out = path + "/" + StrUtil.getFileName(in, false);
            setEditText(OUT, MyUtil.getNewName(out, ext));
        }

    }


    private void checkIsResEnable() {
        final String agrs = getEditText(AGRS);
        if (resChekbox.isChecked() && agrs.contains("-s ")) {
            Toast.makeText(this, getString(R.string.preset_res), Toast.LENGTH_SHORT).show();
            resChekbox.setChecked(false);
        }
    }

    private void prepareButchJob() {
        String in = getEditText(IN);
        String inMask = StrUtil.getBatchMask(in, true);
        setEditText(IN, inMask);
    }

    private void checkBatchJob(String str) {
        final boolean isBatch = str.contains("*");
        findViewById(BATCH_JOB).setEnabled(!isBatch);
        String out = getEditText(OUT);
        if (!out.contains("*")) {
            String outMask = StrUtil.getBatchMask(out, false);
            setEditText(OUT, outMask);
            renameOut(false);
        }

        findViewById(SECOND_IN_BTN).setEnabled(!isBatch);

        if (isBatch) {
            is2In = false;
            setSwitchInButton();
        }
    }


    private void setSwitchInButton() {
        setVisible(SECOND_IN_WRP, is2In);
        ((Button) findViewById(SECOND_IN_BTN)).setText(!is2In ? R.string.two_in : R.string.one_in);
        if (is2In) checkInputFile(IN2);
    }

    @SuppressLint("RestrictedApi")
    private void switchState() {
        fabCurJob.setVisibility(isServiceRun ? View.VISIBLE : View.GONE);
        fabStartJob.setImageResource(isServiceRun ? R.drawable.startjob2 : R.drawable.startjob);
    }

    private BroadcastReceiver commandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getExtras() != null) {
                String cmd = intent.getExtras().getString(Commands.COMMAND);
                if (cmd == null) {
                    return;
                }
                Log.d("COMMAND", cmd);
                switch (cmd) {
                    case Commands.ON_DONE:
                        isServiceRun = false;
                        final String stopLine = intent.getExtras().getString(Commands.LINE);
                        final int result = intent.getExtras().getInt(Commands.RESULT);
                        done(stopLine, result);
                        stopService(new Intent(Gui.this, EncodeService.class));
                        switchState();
                        break;
                }

            }
        }
    };


    private void openBackupMenu() {
        //final List<String> fileNames = new ArrayList<>();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.backup_title);
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        final String newFile = "ffmpeg_presets" + Config.BACKUP_EXT;


        //fileNames.add(newFile);
        arrayAdapter.add(getString(R.string.save_to));
        arrayAdapter.add(getString(R.string.load_from));

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    createFile(newFile, CREATE_BACKUP_FILE);
                } else  if (which == 1) {
                    openFile(OPEN_BACKUP_FILE);
                }
            }
        });
        builder.show();
    }


    private static final int CREATE_BACKUP_FILE = 55;

    private void createFile(String name, final int type) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/*");
        intent.putExtra(Intent.EXTRA_TITLE, name);
        //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, );

        startActivityForResult(intent, type);
    }

    private static final int OPEN_BACKUP_FILE = 56;

    private void openFile(final int type) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/*");
        //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, );

        startActivityForResult(intent, type);
    }


    public synchronized void onActivityResult(final int requestCode,
                                              int resultCode, final Intent data) {


        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == REQUEST_SAVE) {
                String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
                String pathWithoutExt;

                if (getEditText(IN).contains("*")) {
                    pathWithoutExt = StrUtil.cropTo(filePath, '/') + "/*";
                } else {
                    pathWithoutExt = StrUtil.cropTo(filePath, '.');
                }

                setEditText(OUT, pathWithoutExt);
                renameOut(false);
            } else if (requestCode == REQUEST_LOAD) {
                String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
                int extra = data.getIntExtra(FileDialog.EXTRA_INT, NORMAL);

                switch (extra) {
                    case NORMAL:
                        setEditText(IN, filePath);
                        renameOut(true);
                        break;
                    case SECONDARY:
                        setEditText(IN2, filePath);
                        break;

                }

            } else if (requestCode == START_ENCODING || requestCode == VIEW_ENCODING) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(commandReceiver, new IntentFilter(Commands.GUI), Context.RECEIVER_NOT_EXPORTED);
                } else {
                    registerReceiver(commandReceiver, new IntentFilter(Commands.GUI));
                }


            } else if (requestCode == CREATE_BACKUP_FILE) {
                if (data != null) {
                    Uri u = data.getData();
                    if (u != null) {
                        config.savePresetsToFile(this, u);
                    }
                }
            } else if (requestCode == OPEN_BACKUP_FILE) {
                if (data != null) {
                    Uri u = data.getData();
                    if (u != null) {
                        if(config.restorePresetsFromFile(this, u)) {
                            presets = config.getPresets();
                            setPresets();
                            Toast.makeText(Gui.this,
                                    getText(R.string.loaded_from), Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            Toast.makeText(Gui.this,
                                    getText(R.string.wrong_file), Toast.LENGTH_LONG)
                                    .show();
                        }
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}