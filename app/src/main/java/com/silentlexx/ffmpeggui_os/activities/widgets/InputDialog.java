package com.silentlexx.ffmpeggui_os.activities.widgets;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.silentlexx.ffmpeggui_os.R;
import com.silentlexx.ffmpeggui_os.activities.Gui;

public class InputDialog extends Dialog {
	private EditText text;

	private String name;
	
	private Gui gui;

	
	public InputDialog(Context context, Gui g, String s) {
		super(context);
		gui = g;
		name = s;
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.input);

		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			setTitle(R.string.new_preset_name);
			findViewById(R.id.fake_title).setVisibility(View.GONE);
		}


		setCancelable(true);
		Button ok = findViewById(R.id.in_ok);

		Button cancel = findViewById(R.id.in_cancel);


		text = findViewById(R.id.in_text);
		text.setText(name);
		if(!name.isEmpty()) {
			int n = name.length();
			try {
				text.setSelection(n);
			} catch (Exception e){
				e.printStackTrace();
			}
		}

		
		ok.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				onEnter(text.getText().toString());
				Close();
			}

		});
		
		cancel.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Close();
			}
		});

		
	    text.setFilters(Gui.inputFilters);
		
	}
	
	    
	 private void onEnter(String text){
		 gui.onEnter(text);
	 }
		
	

	private void Close() {
		text.setText("");
		dismiss();		
	}
	

}
