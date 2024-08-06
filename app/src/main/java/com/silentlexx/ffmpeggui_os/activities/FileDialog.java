package com.silentlexx.ffmpeggui_os.activities;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.silentlexx.ffmpeggui_os.R;
import com.silentlexx.ffmpeggui_os.config.Config;
import com.silentlexx.ffmpeggui_os.utils.FileUtil;
import com.silentlexx.ffmpeggui_os.utils.StrUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * Activity para escolha de arquivos/diretorios.
 * 
 * @author android
 * 
 */
public class FileDialog extends AppActivity {


	class SelectionMode {
         static final int MODE_CREATE = 0;
         static final int MODE_OPEN = 1;
    }

	/**
	 * Chave de um item da lista de paths.
	 */
	private static final String ITEM_KEY = "key";

	/**
	 * Imagem de um item da lista de paths (diretorio ou arquivo).
	 */
	private static final String ITEM_IMAGE = "image";

	/**
	 * Diretorio raiz.
	 */
	private static final String ROOT = "/";

	/**
	 * Parametro de entrada da Activity: path inicial. Padrao: ROOT.
	 */
	public static final String START_PATH = "START_PATH";

	/**
	 * Parametro de entrada da Activity: filtro de formatos de arquivos. Padrao:
	 * null.
	 */
	public static final String FORMAT_FILTER = "FORMAT_FILTER";

	/**
	 * Parametro de saida da Activity: path escolhido. Padrao: null.
	 */
	public static final String RESULT_PATH = "RESULT_PATH";

	public static final String EXTRA_INT = "EXTRA_INT";

	public static final String SELECTION_MODE = "SELECTION_MODE";

	public static final String DIRECT_SELECTION = "DIRECT_SELECTION";


	public static final String CAN_SELECT_DIR = "CAN_SELECT_DIR";
	
	public static final String OPEN_ONLY = "OPEN_ONLY";

	public static final String TITLE = "TITLE";
	public static final String NEW_FILE = "NEW_FILE";

	private List<String> path = null;
	private TextView myPath;
	private EditText mFileName;
	private EditText mDirName;
	private ArrayList<HashMap<String, Object>> mList;

	private Button selectButton;
	private ImageButton selectAllButton;

	private LinearLayout layoutSelect;
	private LinearLayout layoutCreate;
	private LinearLayout layoutCreateDir;
	private InputMethodManager inputManager;
	private String parentPath;
	private String currentPath = ROOT;

	private int extra = 0;

	private String[] formatFilter = null;

	private boolean canSelectDir = false;
	private File selectedFile;
	private HashMap<String, Integer> lastPositions = new HashMap<>();
	private boolean directSelection = false;
	private ListView listView;

	private boolean isUpper = true;

	private boolean spinnerInited = false;

	private String newFile = "New File";
	private String newDir = "New Folder";

	private boolean  forWrite = false;

	/**
	 * Called when the activity is first created. Configura todos os parametros
	 * de entrada e das VIEWS..
	 */


	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return true;
	}

	@Override
	public int getRootView() {
		return R.layout.file_dialog_main;
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//overridePendingTransition(R.anim.in, R.anim.out);

		setResult(RESULT_CANCELED, getIntent());

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		listView = findViewById(R.id.list);
		listView.setOnItemClickListener(onItemClickListener);
		myPath = findViewById(R.id.path);
		mFileName = findViewById(R.id.fdEditTextFile);
		mFileName.setFilters(Gui.inputFilters);
		mDirName = findViewById(R.id.fdEditTextDir);
		mDirName.setFilters(Gui.inputFilters);
		inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

		newFile = getIntent().getStringExtra(NEW_FILE);

		int selectionMode = getIntent().getIntExtra(SELECTION_MODE, SelectionMode.MODE_CREATE);
		forWrite = selectionMode == SelectionMode.MODE_CREATE;

		extra =  getIntent().getIntExtra(EXTRA_INT, 0);

		directSelection = getIntent().getBooleanExtra(DIRECT_SELECTION, true);

		formatFilter = getIntent().getStringArrayExtra(FORMAT_FILTER);

		canSelectDir = getIntent().getBooleanExtra(CAN_SELECT_DIR, false);

		boolean openOnly = getIntent().getBooleanExtra(OPEN_ONLY, false);

		selectButton = findViewById(R.id.fdButtonSelect);
		selectButton.setEnabled(false);
		selectButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				selectionFileDone();
			}
		});


		selectAllButton = findViewById(R.id.fdButtonSelectAll);
		selectAllButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				selectionAllDone();
			}
		});


		selectButton.setVisibility(directSelection ? View.GONE : View.VISIBLE);
		//selectAllButton.setVisibility(directSelection ? View.GONE : View.VISIBLE);

		final Button newButton = findViewById(R.id.fdButtonNew);
		newButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setCreateVisible(v);
				mFileName.setText(StrUtil.getFileName(newFile, false));
				mFileName.requestFocus();
			}
		});

		final ImageButton newDirButton = findViewById(R.id.fdNewDir);
		newDirButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setCreateDirVisible(v);
				mDirName.setText(StrUtil.getFileName(newDir, false));
				mDirName.requestFocus();
			}
		});


		if (selectionMode == SelectionMode.MODE_OPEN ) {
			 newButton.setEnabled(false);
		}

		String title = getIntent().getStringExtra(TITLE);

		if(getSupportActionBar()!=null) {
			getSupportActionBar().setTitle(title);
		}

		if(openOnly){
			newButton.setVisibility(View.GONE);
			newDirButton.setVisibility(View.GONE);
			selectAllButton.setVisibility(extra == 0 ? View.VISIBLE : View.GONE); //FIXME
		} else {
			newButton.setVisibility(View.VISIBLE);
			newDirButton.setVisibility(View.VISIBLE);
			selectAllButton.setVisibility(View.GONE);
		}

		layoutSelect = findViewById(R.id.fdLinearLayoutSelect);
		layoutCreate = findViewById(R.id.fdLinearLayoutCreate);
		layoutCreate.setVisibility(View.GONE);

		layoutCreateDir = findViewById(R.id.fdLinearLayoutCreateDir);
		layoutCreateDir.setVisibility(View.GONE);


		final Button closeButton = findViewById(R.id.fdCloseButton);
		closeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED, getIntent());
				finish();
			}

		});
		
		final Button cancelButton = findViewById(R.id.fdButtonCancel);
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setSelectVisible(v);
			}

		});

		final Button cancelButtonDir = findViewById(R.id.fdButtonCancelDir);
		cancelButtonDir.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setSelectVisible(v);
			}

		});

		final Button createButton = findViewById(R.id.fdButtonCreate);
		createButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mFileName.getText().length() > 0) {
					getIntent().putExtra(RESULT_PATH, currentPath + "/" + mFileName.getText()+"."+Gui.DUMMY_EXT);
					setResult(RESULT_OK, getIntent());
					finish();
				}
			}
		});


		final Button createButtonDir = findViewById(R.id.fdButtonCreateDir);
		createButtonDir.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				createNewDir();
			}
		});

		String startPath = getIntent().getStringExtra(START_PATH);
		startPath = startPath != null ? startPath : ROOT;
		if (canSelectDir) {
			selectedFile = new File(startPath);
			selectButton.setEnabled(true);


		}


		final List<String> listVolumes = FileUtil.getDisks(this, forWrite );

		int i = 0;

		for(String vol : listVolumes){
			if(startPath.startsWith(vol)){
				break;
			} else {
				i++;
			}
		}


		final Spinner spinner = findViewById(R.id.spinner_volumes);

		ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
				android.R.layout.simple_spinner_item, listVolumes);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(i);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if(spinnerInited) {
					final String dir = listVolumes.get(position);
					getDir(dir);
				} else {
					spinnerInited = true;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		ImageView home = (ImageView) findViewById(R.id.phome);
		home.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				spinner.setSelection(0);
				getDir(Config.getSdCardPath(FileDialog.this));
			}
		});

		try {
			getDir(startPath);
		} catch (NullPointerException e){
			Toast.makeText(this,  e.toString(), Toast.LENGTH_LONG).show();
			finish();
		}

	}

	private void createNewDir() {
		String newDir = mDirName.getText().toString();
		if(!newDir.isEmpty()){
			String path = currentPath + "/" + newDir;
			File dir = new File(path);
			if( (dir.exists() && dir.isDirectory())  || dir.mkdirs()) {
				layoutCreateDir.setVisibility(View.GONE);
				layoutSelect.setVisibility(View.VISIBLE);

				getDir(path);

			} else {
				Toast.makeText(this, getString(R.string.cantcreatedir), Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void selectionAllDone() {
			getIntent().putExtra(RESULT_PATH, currentPath + "/*");
			getIntent().putExtra(EXTRA_INT, extra);
			setResult(RESULT_OK, getIntent());
			finish();
	}


	private void selectionFileDone() {
		if (selectedFile != null) {
			getIntent().putExtra(RESULT_PATH, selectedFile.getPath());
			getIntent().putExtra(EXTRA_INT, extra);
			setResult(RESULT_OK, getIntent());
			finish();
		}
	}

	private void getDir(String dirPath) throws NullPointerException {

		boolean useAutoSelection = dirPath.length() < currentPath.length();

		Integer position = lastPositions.get(parentPath);

		getDirImpl(dirPath);

		if (position != null && useAutoSelection) {
			listView.setSelection(position);
		}

	}

	/**
	 * Monta a estrutura de arquivos e diretorios filhos do diretorio fornecido.
	 * 
	 * @param dirPath
	 *            Diretorio pai.
	 */
	private void getDirImpl(final String dirPath) throws NullPointerException {

		if(dirPath==null){
			return;
		}

		currentPath = dirPath;

		//final List<String> item = new ArrayList<>();
		path = new ArrayList<>();
		mList = new ArrayList<>();

		File f = new File(currentPath);
		File[] files = f.listFiles();
		if (files == null) {
			currentPath = ROOT;
			f = new File(currentPath);
			files = f.listFiles();
		}

		final String local = getText(R.string.location) + ": " + currentPath;
		myPath.setText( local );

		if (!currentPath.equals(ROOT) && (
				( !forWrite && f.getParentFile().canRead() ) ||
						(forWrite && f.getParentFile().canWrite())
		)
		) {

			//item.add(ROOT);
			//addItem(ROOT, R.drawable.folder);
			//path.add(ROOT);

			//item.add("../");

				addItem("..", R.drawable.folder_up);
				path.add(f.getParent());

		    	parentPath = f.getParent();
		    	isUpper = false;

		} else {
			isUpper = true;
		}

		TreeMap<String, String> dirsMap = new TreeMap<>();
		TreeMap<String, String> dirsPathMap = new TreeMap<>();
		TreeMap<String, String> filesMap = new TreeMap<>();
		TreeMap<String, String> filesPathMap = new TreeMap<>();
		if(files!=null) {
			for (File file : files) {
				if (file.isDirectory()) {
					String dirName = file.getName();
					dirsMap.put(dirName, dirName);
					dirsPathMap.put(dirName, file.getPath());
				} else {
					final String fileName = file.getName();
					final String fileNameLwr = fileName.toLowerCase();
					// se ha um filtro de formatos, utiliza-o
					if (formatFilter != null) {
						boolean contains = false;
						for (String aFormatFilter : formatFilter) {
							final String formatLwr = aFormatFilter.toLowerCase();
							if (fileNameLwr.endsWith(formatLwr)) {
								contains = true;
								break;
							}
						}
						if (contains) {
							filesMap.put(fileName, fileName);
							filesPathMap.put(fileName, file.getPath());
						}
						// senao, adiciona todos os arquivos
					} else {
						filesMap.put(fileName, fileName);
						filesPathMap.put(fileName, file.getPath());
					}
				}
			}
		}
		//item.addAll(dirsMap.tailMap("").values());
		//item.addAll(filesMap.tailMap("").values());
		path.addAll(dirsPathMap.tailMap("").values());
		path.addAll(filesPathMap.tailMap("").values());

		SimpleAdapter fileList = new SimpleAdapter(this, mList, R.layout.file_dialog_row, new String[] {
				ITEM_KEY, ITEM_IMAGE }, new int[] { R.id.fdrowtext, R.id.fdrowimage });

		for (String dir : dirsMap.tailMap("").values()) {
			addItem(dir, R.drawable.folder);
		}

		for (String file : filesMap.tailMap("").values()) {
			if(isMedia(file)){
				addItem(file, R.drawable.file);				
			} else {
			 addItem(file, R.drawable.file_other);
			}
		}

		fileList.notifyDataSetChanged();

		listView.setAdapter(fileList);


	}
	
	private boolean isMedia(String s){
		String ext[] = Config.getExtensions(this);
		for (String anExt : ext) {
			if (s.toLowerCase().endsWith("." + anExt)) {
				return true;
			}
		}
		return false;
	}

	private void addItem(String fileName, int imageId) {
		HashMap<String, Object> item = new HashMap<>();
		item.put(ITEM_KEY, fileName);
		item.put(ITEM_IMAGE, imageId);
		mList.add(item);
	}



	private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
			onListItemClick(view, i, l);
		}
	};

	void onListItemClick(View v, int position, long id) {

		File file = new File(path.get(position));

		setSelectVisible(v);

		if (file.isDirectory()) {
			selectButton.setEnabled(false);

			if (  (forWrite && file.canWrite()) ||
					(!forWrite && file.canRead())
			) {
				lastPositions.put(currentPath, position);
				getDir(path.get(position));
				if (canSelectDir) {
					selectedFile = file;
					v.setSelected(true);
					selectButton.setEnabled(true);

					if(directSelection){
						selectionFileDone();
					}
				}
			} else {
				Toast.makeText(this,  getText(R.string.cant_read_folder), Toast.LENGTH_SHORT ).show();
				/*
				new AlertDialog.Builder(this).setIcon(R.drawable.cancel)
						.setTitle("[" + file.getName() + "] " + getText(R.string.cant_read_folder))
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {

							}
						}).show();
						*/
			}
		} else {
			selectedFile = file;
			v.setSelected(true);
			selectButton.setEnabled(true);

			if(directSelection){
				selectionFileDone();
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			selectButton.setEnabled(false);


			if(isUpper) {
				return super.onKeyDown(keyCode, event);
			}

			if (layoutCreateDir.getVisibility() == View.VISIBLE) {
				layoutCreateDir.setVisibility(View.GONE);
				layoutSelect.setVisibility(View.VISIBLE);
			} else
			if (layoutCreate.getVisibility() == View.VISIBLE) {
				layoutCreate.setVisibility(View.GONE);
				layoutSelect.setVisibility(View.VISIBLE);
			} else {
				if (!currentPath.equals(ROOT) ) {
					try {
						getDir(parentPath);
					} catch (NullPointerException e){
						return super.onKeyDown(keyCode, event);
					}
				} else {
					return super.onKeyDown(keyCode, event);
				}
			}

			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}


	private void setCreateDirVisible(View v) {
		layoutCreateDir.setVisibility(View.VISIBLE);
		layoutSelect.setVisibility(View.GONE);

		inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
		selectButton.setEnabled(false);

	}


	private void setCreateVisible(View v) {
		layoutCreate.setVisibility(View.VISIBLE);
		layoutSelect.setVisibility(View.GONE);

		inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
		selectButton.setEnabled(false);

	}


	private void setSelectVisible(View v) {
		layoutCreate.setVisibility(View.GONE);
		layoutCreateDir.setVisibility(View.GONE);
		layoutSelect.setVisibility(View.VISIBLE);

		inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
		selectButton.setEnabled(false);

	}
}
