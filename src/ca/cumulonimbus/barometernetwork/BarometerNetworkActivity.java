package ca.cumulonimbus.barometernetwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import ca.cumulonimbus.pressurenetsdk.CbApiCall;
import ca.cumulonimbus.pressurenetsdk.CbCurrentCondition;
import ca.cumulonimbus.pressurenetsdk.CbObservation;
import ca.cumulonimbus.pressurenetsdk.CbScience;
import ca.cumulonimbus.pressurenetsdk.CbService;
import ca.cumulonimbus.pressurenetsdk.CbSettingsHandler;
import ca.cumulonimbus.pressurenetsdk.CbWeather;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class BarometerNetworkActivity extends MapActivity {

	double mLatitude = 0.0;
	double mLongitude = 0.0;
	double mReading = 0.0;
	double mTimeOfReading = 0.0;
	float mReadingAccuracy = 0.0f;
	float mLocationAccuracy = 0.0f;
	SensorManager sm;

	LayoutInflater mInflater;

	private String mAppDir = "";
	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;

	private String android_id;

	private boolean debugMode = true;

	private String mTendency = "";

	public String statusText = "";
	private int mapFontSize = 18;

	Intent serviceIntent;

	// pressureNET 4.0
	// SDK communication

	boolean mBound;
	private Messenger mMessenger = new Messenger(new IncomingHandler());
	Messenger mService = null;

	Location bestLocation;
	CbObservation bestPressure;
	CbSettingsHandler activeSettings;

	ArrayList<CbObservation> recents = new ArrayList<CbObservation>();
	ArrayList<CbObservation> apiCache = new ArrayList<CbObservation>();
	ArrayList<CbObservation> graphContents = new ArrayList<CbObservation>();

	boolean dataReceivedToPlot = false;

	private SeekBar seekTime;
	private ImageButton buttonPlay;
	private Spinner spinnerTime;
	private TextView textCallLog;
	private int hoursAgoSelected = 1;

	Handler timeHandler = new Handler();

	// API call parameters.
	private ArrayList<CbObservation> apiCbObservationResults = new ArrayList<CbObservation>();

	private ArrayList<CbCurrentCondition> currentConditions = new ArrayList<CbCurrentCondition>();

	String apiServerURL = "https://pressurenet.cumulonimbus.ca/live/?";

	private int currentTimeProgress = 0;
	private boolean animateState = false;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dataReceivedToPlot = false;
		setContentView(R.layout.main);
		// migratePreferences();
		startLog();
		// getStoredPreferences();
		setUpUIListeners();
		setId();
		setUpFiles();
		setUpMap();
		showWelcomeActivity();
		setUpActionBar();
		startCbService();
		bindCbService();

	}

	Runnable animate = new Runnable() {
		@Override
		public void run() {
			if (currentTimeProgress < 100) {
			
				
				currentTimeProgress++;
				seekTime.setProgress(currentTimeProgress);
				updateMapWithSeekTimeData();
				timeHandler.postDelayed(animate, 50);
			} else {
				Drawable play = getResources().getDrawable(R.drawable.ic_menu_play);
				buttonPlay.setImageDrawable(play);
				currentTimeProgress = 0;
				seekTime.setProgress(currentTimeProgress);
				animateState = false;
				
			}

		}
	};

	public boolean isCloseToFrame(int a, int b) {
		return Math.abs( a - b) < 3;
	}
	
	public void updateMapWithSeekTimeData() {
		BarometerMapView mv = (BarometerMapView) findViewById(R.id.mapview);
		List<Overlay> mapOverlays = mv.getOverlays();
				
		ArrayList<CbWeather> thisFrameCondition = new ArrayList<CbWeather>();
		for(CbCurrentCondition c : currentConditions) {
			if(isCloseToFrame(c.getAnimateGroupNumber(), currentTimeProgress)) {
				thisFrameCondition.add(c);
			} else {
				
			}
		}
		
		ArrayList<CbWeather> thisFrameObservation = new ArrayList<CbWeather>();
		for(CbObservation r : recents) {
			if(isCloseToFrame(r.getAnimateGroupNumber(), currentTimeProgress)) {
				thisFrameObservation.add(r);
			} else {
				
			}
		}
		
		addDataFrameToMap(thisFrameCondition, thisFrameObservation);
	}

	private void setUpUIListeners() {
		Context context = getApplicationContext();
		mInflater = LayoutInflater.from(context);
		spinnerTime = (Spinner) findViewById(R.id.spinnerChartTime);
		buttonPlay = (ImageButton) findViewById(R.id.buttonPlay);
		seekTime = (SeekBar) findViewById(R.id.seekBarTime);
		textCallLog = (TextView) findViewById(R.id.textViewCallLog);
		
		ArrayAdapter<CharSequence> adapterTime = ArrayAdapter
				.createFromResource(this, R.array.display_time_chart,
						android.R.layout.simple_spinner_item);
		adapterTime
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerTime.setAdapter(adapterTime);
		spinnerTime.setSelection(2);

		seekTime.setProgress(100);
		
		seekTime.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if(fromUser) {
					currentTimeProgress = progress;
					updateMapWithSeekTimeData();
				}
				
			}
		});
		
		buttonPlay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				animateState = !animateState;
				if(animateState== true) {
					Drawable pause = getResources().getDrawable(R.drawable.ic_menu_pause);
					buttonPlay.setImageDrawable(pause);
				} else {
					timeHandler.removeCallbacks(animate);
					Drawable play = getResources().getDrawable(R.drawable.ic_menu_play);
					buttonPlay.setImageDrawable(play);
					return;
				}

				
				if (currentConditions.size() == 0) {
					return;
				}
				// currentTimeProgress = 0;
				seekTime.setProgress(currentTimeProgress);

				Collections.sort(currentConditions,
						new CbScience.ConditionTimeComparator());
				Collections.sort(recents,
						new CbScience.TimeComparator());
				
				long msAgoSelected = hoursAgoSelected * 60 * 60 * 1000;
				long singleTimeSpan = msAgoSelected / 100;
				long startTime = currentConditions.get(0).getTime();

				ArrayList<ArrayList<CbCurrentCondition>> collectionConditions = new ArrayList<ArrayList<CbCurrentCondition>>();

				log(currentConditions.size() + " current conditions over "
						+ hoursAgoSelected);
				for (CbCurrentCondition c : currentConditions) {
					long time = c.getTime();
					int group = (int) ((time - startTime) / singleTimeSpan);
					c.setAnimateGroupNumber(group);
					

					log("group " + group);
				}

				for (CbObservation o : recents) {
					long time = o.getTime();
					int group = (int) (Math.abs((time - startTime)) / singleTimeSpan);
					o.setAnimateGroupNumber(group);
					

					log("o group " + group);
				}

				timeHandler.postDelayed(animate, 0);
			}
		});

		spinnerTime.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				String selected = arg0.getSelectedItem().toString();
				// TODO: Fix hack
				CbApiCall apiCall = new CbApiCall();
				if (selected.equals("1 hour")) {
					hoursAgoSelected = 1;
				} else if (selected.equals("3 hours")) {
					hoursAgoSelected = 3;
				} else if (selected.equals("6 hours")) {
					hoursAgoSelected = 6;
				} else if (selected.equals("1 day")) {

					hoursAgoSelected = 24;
				}
				makeMapApiCallAndLoadRecents();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}

		});
	}

	private void startCbService() {
		log("start cbservice");
		try {
			serviceIntent = new Intent(this, CbService.class);
			startService(serviceIntent);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void askForCurrentConditions(CbApiCall api) {

		if (mBound) {
			log("asking for current conditions");
			Message msg = Message.obtain(null,
					CbService.MSG_GET_CURRENT_CONDITIONS, api);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			log("error: not bound");
		}
	}

	private void askForBestPressure() {
		if (mBound) {
			log("asking for best pressure");
			Message msg = Message.obtain(null, CbService.MSG_GET_BEST_PRESSURE,
					0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
				updateVisibleReading();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			log("error: not bound");
		}
	}

	private void askForBestLocation() {
		if (mBound) {
			log("asking for best location");
			Message msg = Message.obtain(null, CbService.MSG_GET_BEST_LOCATION,
					0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			log("error: not bound");
		}
	}

	private void askForRecents(CbApiCall apiCall) {
		if (mBound) {
			log("asking for recents");

			Message msg = Message.obtain(null, CbService.MSG_GET_API_RECENTS,
					apiCall);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			log("error: not bound");
		}
	}

	public void unBindCbService() {
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}

	public void bindCbService() {
		log("bind cbservice");
		bindService(new Intent(getApplicationContext(), CbService.class),
				mConnection, Context.BIND_AUTO_CREATE);

	}

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CbService.MSG_BEST_LOCATION:
				bestLocation = (Location) msg.obj;
				if (bestLocation != null) {
					log("Client Received from service "
							+ bestLocation.getLatitude());
				} else {
					log("location null");
				}
				break;
			case CbService.MSG_BEST_PRESSURE:
				bestPressure = (CbObservation) msg.obj;
				if (bestPressure != null) {
					log("Client Received from service "
							+ bestPressure.getObservationValue());
				} else {
					log("pressure null");
				}
				updateVisibleReading();
				break;
			case CbService.MSG_SETTINGS:
				activeSettings = (CbSettingsHandler) msg.obj;
				if (activeSettings != null) {
					log("Client Received from service "
							+ activeSettings.getServerURL());
				} else {
					log("settings null");
				}
				break;
			case CbService.MSG_DATA_STREAM:
				bestPressure = (CbObservation) msg.obj;
				if (bestPressure != null) {
					// log("received " + bestPressure.getObservationValue());
				} else {
					// log("received null observation");
				}
				updateVisibleReading();
				break;
			case CbService.MSG_API_RECENTS:
				apiCache.clear();
				apiCache = (ArrayList<CbObservation>) msg.obj;
				if (recents != null) {
					log("received " + recents.size()
							+ " recent observations in buffer.");

				} else {
					log("received recents: NULL");
				}
				dataReceivedToPlot = true;
				recents = apiCache;
				createAndShowChart();
				break;
			case CbService.MSG_API_RESULT_COUNT:
				int count = msg.arg1;
				textCallLog.setText( count + " API results cached");

				break;
			case CbService.MSG_CURRENT_CONDITIONS:
				log("receiving current conditions");
				currentConditions = (ArrayList<CbCurrentCondition>) msg.obj;
				if (currentConditions != null) {
					log(" size " + currentConditions.size());
					addDataToMap();
				} else {
					log("conditions ARE NuLL");
				}

				break;
			default:
				log("received default message");
				super.handleMessage(msg);
			}
		}
	}

	public void createAndShowChart() {
		if(recents == null ) {
			log("recents null RETURNING");
			return;
		} else if ( recents.size() == 0) {
			log("recents 0, RETURNING");
			return;
		}
		if (dataReceivedToPlot) {
			// draw chart
			log("plotting... " + recents.size());
			Chart chart = new Chart(getApplicationContext());
			View chartView = chart.drawChart(recents);

			LinearLayout mainLayout = (LinearLayout) findViewById(R.id.mainParentLayout);

			try {
				View testChartView = findViewById(100); // TODO: ...
				mainLayout.removeView(testChartView);
			} catch (Exception e) {
				e.printStackTrace();
			}

			chartView.setId(100); // TODO: what's safe?

			// add to layout
			LayoutParams lparams = new LayoutParams(LayoutParams.MATCH_PARENT,
					400);

			chartView.setLayoutParams(lparams);
			if (mainLayout == null) {
				log("chartlayout null");
				return;
			}
			mainLayout.addView(chartView);
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			log("client says : service connected");
			mService = new Messenger(service);
			mBound = true;
			Message msg = Message.obtain(null, CbService.MSG_OKAY);
			log("client received " + msg.arg1 + " " + msg.arg2);

		}

		public void onServiceDisconnected(ComponentName className) {
			log("client: service disconnected");
			mMessenger = null;
			mBound = false;
		}
	};

	public void startDataStream() {
		if (mBound) {
			log("pN-4 starting data stream");
			Message msg = Message.obtain(null, CbService.MSG_START_DATA_STREAM,
					0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			log("error: not bound");
		}
	}

	public void stopDataStream() {
		if (mBound) {
			log("pN-4 stopping data stream");
			Message msg = Message.obtain(null, CbService.MSG_STOP_DATA_STREAM,
					0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			log("error: not bound");
		}
	}

	public void startLog() {
		// Log
		String version = "";
		PackageInfo pInfo;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = pInfo.versionName;
		} catch (NameNotFoundException e) {
		}

		log("oncreate main activity v: " + version);
	}

	// Get the phone ID and hash it
	public String getID() {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");

			String actual_id = Secure.getString(getApplicationContext()
					.getContentResolver(), Secure.ANDROID_ID);
			byte[] bytes = actual_id.getBytes();
			byte[] digest = md.digest(bytes);
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < digest.length; i++) {
				hexString.append(Integer.toHexString(0xFF & digest[i]));
			}
			return hexString.toString();
		} catch (Exception e) {
			return "--";
		}
	}

	public void deleteUserData() {
		// show a dialog, listen for its response.
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getResources().getString(R.string.deleteWarning))
				.setPositiveButton("Continue", dialogDeleteClickListener)
				.setNegativeButton("Cancel", dialogDeleteClickListener).show();
	}

	DialogInterface.OnClickListener dialogDeleteClickListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				// TODO: Implement
				break;

			case DialogInterface.BUTTON_NEGATIVE:
				break;
			}
		}
	};

	/**
	 * Migrate from the old 'pressureNETprefs' system to the new one. 3.0 ->
	 * 3.0.1
	 */
	private void migratePreferences() {
		// Log
		String version = "";
		PackageInfo pInfo;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = pInfo.versionName;
		} catch (NameNotFoundException e) {
		}

		log("migrate prefs to " + version);

		// Migrate old preferences
		SharedPreferences oldSettings = getSharedPreferences(
				"pressureNETPrefs", 0);
		if (oldSettings.contains("autoupdate")) {
			// Load
			boolean autoUpdate = oldSettings.getBoolean("autoupdate", true);
			String unit = oldSettings.getString("units", "Millibars (mbar)");
			String autoFrequency = oldSettings.getString("autofrequency",
					"10 minutes");
			String sharing = oldSettings.getString("sharing_preference",
					"Us, Researchers and Forecasters");
			int firstRun = oldSettings.getInt("first_run", 1);

			firstRun++;

			// Store
			SharedPreferences newSharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = newSharedPreferences.edit();
			editor.putBoolean("autoupdate", autoUpdate);
			editor.putString("units", unit);
			editor.putString("autoFrequency", autoFrequency);
			editor.putString("sharing_preference", sharing);
			editor.putInt("first_run", firstRun);
			editor.commit();
		}
	}

	/**
	 * Welcome the user to pressureNET and explain the privacy options
	 */
	public void showWelcomeActivity() {
		// has this been shown yet?
		// TODO: store in preferences
		int firstRun = 0;

		if (firstRun == 0) {
			Intent intent = new Intent(this,
					ca.cumulonimbus.barometernetwork.WelcomeActivity.class);
			// startActivityForResult(intent, 0);
		}

	}

	/**
	 * Some devices have barometers, other's don't. Fix up the UI a bit so that
	 * most useful elements show for the right users
	 */
	public void cleanUI(Menu menu) {
		if (false) { // TODO: Fix: was barometerdetected
			// keep the UI as-is. default assumes barometer exists :)
			// ensure the right items are always visible, though, in case of
			// detection error
		} else {
			// hide some menu items that are barometer-specific
			menu.removeItem(R.id.menu_my_info);
			menu.removeItem(R.id.menu_submit_reading);
			menu.removeItem(R.id.menu_log_viewer);
		}

		if (!debugMode) {
			// hide menu item
			menu.removeItem(R.id.send_debug_log);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		cleanUI(menu);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_settings) {
			Intent i = new Intent(this, PreferencesActivity.class);
			i.putExtra("hasBarometer", false); // TODO: fix, was
												// barometerdetected
			startActivityForResult(i, 1);
		} else if (item.getItemId() == R.id.menu_my_info) {
			// TODO: Implement
		} else if (item.getItemId() == R.id.menu_submit_reading) {
			// TODO: Implement
		} else if (item.getItemId() == R.id.menu_log_viewer) {
			showRecentHistory();
		} else if (item.getItemId() == R.id.send_debug_log) {
			// send logs to Cumulonimbus
			emailLogs();
		} else if (item.getItemId() == R.id.menu_current_conditions) {
			// Current conditions
			Intent intent = new Intent(getApplicationContext(),
					CurrentConditionsActivity.class);
			try {
				if (mLatitude == 0.0) {
					LocationManager lm = (LocationManager) this
							.getSystemService(Context.LOCATION_SERVICE);
					Location loc = lm
							.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					double latitude = loc.getLatitude();
					double longitude = loc.getLongitude();
					mLatitude = latitude;
					mLongitude = longitude;
					mLocationAccuracy = loc.getAccuracy();
				}

				intent.putExtra("appdir", mAppDir);
				intent.putExtra("latitude", mLatitude);
				intent.putExtra("longitude", mLongitude);
				log("starting condition " + mLatitude + " , " + mLongitude);
				startActivity(intent);
			} catch (NullPointerException e) {
				Toast.makeText(getApplicationContext(),
						"No location found. Please try again soon.",
						Toast.LENGTH_SHORT).show();
			}
		} else if (item.getItemId() == R.id.menu_load_data_vis) {
			// Load up pressurenet.cumulonimbus.ca with the user's location
			// and current timeframe
			try {
				LocationManager lm = (LocationManager) this
						.getSystemService(Context.LOCATION_SERVICE);
				Location loc = lm
						.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				double latitude = loc.getLatitude();
				double longitude = loc.getLongitude();
				Intent intent = new Intent(getApplicationContext(),
						PNDVActivity.class);
				intent.putExtra("latitude", latitude);
				intent.putExtra("longitude", longitude);
				startActivity(intent);
			} catch (NullPointerException npe) {
				// Android 4.2 NPEs here. Try again but still be careful
				try {
					Intent intent = new Intent(getApplicationContext(),
							PNDVActivity.class);
					intent.putExtra("latitude", mLatitude);
					intent.putExtra("longitude", mLongitude);
					startActivity(intent);
				} catch (Exception e) {
					Intent intent = new Intent(getApplicationContext(),
							PNDVActivity.class);
					startActivity(intent);
				}
			}
		} else if (item.getItemId() == R.id.menu_data_management) {
			Intent intent = new Intent(getApplicationContext(),
					DataManagementActivity.class);
			startActivity(intent);

		}

		/*
		 * else if(item.getItemId()==R.id.menu_about) { }
		 * Toast.makeText(getApplicationContext(),
		 * "About pressureNET and Cumulonimbus", Toast.LENGTH_SHORT).show();
		 * 
		 * }
		 *//*
			 * else if(item.getItemId()==R.id.menu_reload) { loadAndShowData();
			 * }/* // Show a graph of local data points. Local is defined by //
			 * visible map region. Intended for viewing tendencies else
			 * if(item.getItemId()==R.id.menu_showLocalGraph) { Intent intent =
			 * new Intent(getApplication(), LocalChartActivity.class);
			 * intent.putExtra("appdir", mAppDir);
			 * intent.putExtra("regioninfo",""); startActivityForResult(intent,
			 * 0); }
			 */

		return super.onOptionsItemSelected(item);
	}

	/**
	 * 
	 * Email debug logs to Cumulonimbus.
	 * 
	 */
	public void emailLogs() {
		try {
			String strFile = mAppDir + "/log.txt";

			File file = new File(strFile);
			if (!file.exists())
				file.mkdirs();

			final Intent emailIntent = new Intent(
					android.content.Intent.ACTION_SEND);

			PackageInfo pInfo = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			String version = pInfo.versionName;

			String address = "software@cumulonimbus.ca";
			String subject = "pressureNET " + version + " Debug Log";
			String emailtext = "Debug log sent "
					+ (new Date()).toLocaleString();

			emailIntent.setType("plain/text");

			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
					new String[] { address });

			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);

			emailIntent.putExtra(Intent.EXTRA_STREAM,
					Uri.parse("file://" + strFile));

			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, emailtext);

			startActivityForResult(
					Intent.createChooser(emailIntent, "Send mail..."), 105);

		} catch (Throwable t) {
			Toast.makeText(this, "Request failed: " + t.toString(),
					Toast.LENGTH_LONG).show();
		}
	}

	private String fullUnitToRealAbbrev(String unit) {
		if (unit.contains("mbar")) {
			return "mbar";
		} else if (unit.contains("mmHg")) {
			return "mmHg";
		} else if (unit.contains("inHg")) {
			return "inHg";
		} else if (unit.contains("hPa")) {
			return "hPa";
		} else if (unit.contains("kPa")) {
			return "kPa";
		} else if (unit.contains("atm")) {
			return "atm";
		} else {
			return "mbar";
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 105) {
			// Clear the log
			String strFile = mAppDir + "/log.txt";
			File file = new File(strFile);
			if (file.exists())
				file.delete();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	// Give a quick overview of recent
	// submissions
	public void showRecentHistory() {
		String log = "";
		ArrayList<CbObservation> recents = new ArrayList<CbObservation>();
		try {
			// TODO: Implement with CbService
			Intent intent = new Intent(this, LogViewerActivity.class);
			intent.putExtra("log", log);
			startActivity(intent);
		} catch (Exception e) {

		}
	}

	public void setUpActionBar() {
		// TODO: Compatible Action Bar
		ActionBar bar = getActionBar();
		bar.setDisplayUseLogoEnabled(true);
		bar.setTitle("");

	}

	@Override
	protected void onRestart() {
		super.onRestart();

	}

	// Set a unique identifier so that updates from the same user are
	// seen as updates and not new data. MD5 to minimize privacy problems. (?)
	public void setId() {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");

			String actual_id = Secure.getString(getApplicationContext()
					.getContentResolver(), Secure.ANDROID_ID);
			byte[] bytes = actual_id.getBytes();
			byte[] digest = md.digest(bytes);
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < digest.length; i++) {
				hexString.append(Integer.toHexString(0xFF & digest[i]));
			}
			android_id = hexString.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Used to write a log to SD card. Not used unless logging enabled.
	public void setUpFiles() {
		try {
			File homeDirectory = getExternalFilesDir(null);
			if (homeDirectory != null) {
				mAppDir = homeDirectory.getAbsolutePath();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Zoom into the user's location, add pinch zoom controls
	public void setUpMap() {
		log("setting up map");
		// Add zoom
		BarometerMapView mapView = (BarometerMapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);

		// Set default coordinates (centered around the user's location)

		try {
			MapController mc = mapView.getController();
			LocationManager lm = (LocationManager) this
					.getSystemService(Context.LOCATION_SERVICE);
			Location loc = lm
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			mc.setZoom(15);
			if (loc.getLatitude() != 0) {
				// log("setting center " + loc.getLatitude() + " " +
				// loc.getLongitude());
				mc.animateTo(new GeoPoint((int) (loc.getLatitude() * 1E6),
						(int) (loc.getLongitude() * 1E6)));
			} else {
				log("no known last location");
			}

			mapView.invalidate();
			mapView.refreshDrawableState();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// Custom map overlays for barometer readings
	public class MapOverlay extends ItemizedOverlay<OverlayItem> {

		private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
		Context mContext;
		private int mTextSize;

		@Override
		protected boolean onTap(int index) {
			// TODO: Implement

			return true;
		}

		public MapOverlay(Drawable defaultMarker, Context context, int textSize) {
			super(boundCenterBottom(defaultMarker));
			mContext = context;
			mTextSize = textSize;
		}

		public MapOverlay(Drawable defaultMarker, Context context) {
			super(defaultMarker);
			mContext = context;
		}

		public MapOverlay(Drawable defaultMarker) {
			super(boundCenterBottom(defaultMarker));
		}

		public void addOverlay(OverlayItem overlay) {
			mOverlays.add(overlay);
			populate();
		}

		public void removeOverlay(OverlayItem overlay) {
			mOverlays.remove(overlay);
			populate();
		}

		@Override
		protected OverlayItem createItem(int i) {
			return mOverlays.get(i);
		}

		@Override
		public int size() {
			return mOverlays.size();
		}

		// Draw all the overlay data points onto the map. Include an icon as
		// well as
		@Override
		public void draw(android.graphics.Canvas canvas, MapView mapView,
				boolean shadow) {
			shadow = false;
			super.draw(canvas, mapView, shadow);

			if (shadow == false) {
				// cycle through all overlays
				for (int index = 0; index < mOverlays.size(); index++) {
					try {
						OverlayItem item = mOverlays.get(index);

						// Converts lat/lng-Point to coordinates on the screen
						GeoPoint point = item.getPoint();
						Point ptScreenCoord = new Point();
						mapView.getProjection().toPixels(point, ptScreenCoord);

						// Paint
						Paint paint = new Paint();
						paint.setTextAlign(Paint.Align.CENTER);
						paint.setTextSize(mTextSize);
						paint.setShadowLayer(15, 5, 5, 0);
						paint.setARGB(255, 0, 0, 0); // alpha, r, g, b (Black,
														// semi see-through)
						paint.setAntiAlias(true);

						// String toPrint = item.getTitle().substring(0,
						// item.getTitle().length() - 5);
						String toPrint = item.getTitle().split(" ")[0];
						// Double value = Double.parseDouble(toPrint);
						// DecimalFormat df = new DecimalFormat("####.00");
						// toPrint = df.format(value);

						// show text to the right of the icon
						float textWidth = paint.measureText(toPrint);
						Paint bgPaint = new Paint();
						bgPaint.setColor(Color.WHITE);

						Rect rect = new Rect((int) (ptScreenCoord.x
								- (textWidth / 2) - 2), ptScreenCoord.y,
								(int) (ptScreenCoord.x + (textWidth / 2) + 2),
								ptScreenCoord.y + mTextSize + 5);

						if (toPrint.length() == 0) {
							canvas.drawRoundRect(new RectF(rect), 6, 6, bgPaint);
						}
						canvas.drawText(toPrint, ptScreenCoord.x,
								ptScreenCoord.y + mTextSize, paint);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	// Assume that matching latitude and longitude can only be you.
	public boolean obsIsMe(CbObservation ob) {
		return ((ob.getUser_id().equals(android_id)));
	}

	public MapOverlay getCurrentConditionOverlay(CbCurrentCondition condition,
			Drawable drawable) {
		MapOverlay overlay = new MapOverlay(drawable, this, mapFontSize);

		Drawable weatherBackgroundDrawable = resizeDrawable(this.getResources()
				.getDrawable(R.drawable.bg_wea_square));

		if (condition.getGeneral_condition().equals(getString(R.string.sunny))) {
			Drawable sunDrawable = this.getResources().getDrawable(
					R.drawable.ic_wea_col_sun);
			Drawable[] layers = { weatherBackgroundDrawable,
					resizeDrawable(sunDrawable) };
			LayerDrawable layerDrawable = new LayerDrawable(layers);
			overlay = new MapOverlay(layerDrawable, this, mapFontSize);
		} else if (condition.getGeneral_condition().equals(
				getString(R.string.precipitation))) {
			if (condition.getPrecipitation_type().equals(
					getString(R.string.rain))) {
				if (condition.getPrecipitation_amount() == 0.0) {
					Drawable rainDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_rain1);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(rainDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				} else if (condition.getPrecipitation_amount() == 1.0) {
					Drawable rainDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_rain2);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(rainDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				} else if (condition.getPrecipitation_amount() == 2.0) {
					Drawable rainDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_rain3);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(rainDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				}
			} else if (condition.getPrecipitation_type().equals(
					getString(R.string.snow))) {
				if (condition.getPrecipitation_amount() == 0.0) {
					Drawable snowDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_snow1);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(snowDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				} else if (condition.getPrecipitation_amount() == 1.0) {
					Drawable snowDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_snow2);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(snowDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				} else if (condition.getPrecipitation_amount() == 2.0) {
					Drawable snowDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_snow3);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(snowDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				}
			} else if (condition.getPrecipitation_type().equals(
					getString(R.string.hail))) {
				if (condition.getPrecipitation_amount() == 0.0) {
					Drawable hailDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_hail1);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(hailDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				} else if (condition.getPrecipitation_amount() == 1.0) {
					Drawable hailDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_hail2);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(hailDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				} else if (condition.getPrecipitation_amount() == 2.0) {
					Drawable hailDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_hail3);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(hailDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				}
			}
		} else if (condition.getGeneral_condition().equals(
				getString(R.string.cloudy))) {
			if (condition.getCloud_type().equals(
					getString(R.string.partly_cloudy))) {
				Drawable cloudDrawable = this.getResources().getDrawable(
						R.drawable.ic_wea_col_cloud1);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(cloudDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			} else if (condition.getCloud_type().equals(
					getString(R.string.mostly_cloudy))) {
				Drawable cloudDrawable = this.getResources().getDrawable(
						R.drawable.ic_wea_col_cloud2);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(cloudDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			} else if (condition.getCloud_type().equals(
					getString(R.string.very_cloudy))) {
				Drawable cloudDrawable = this.getResources().getDrawable(
						R.drawable.ic_wea_col_cloud);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(cloudDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			} else {
				Drawable cloudDrawable = this.getResources().getDrawable(
						R.drawable.ic_wea_col_cloud);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(cloudDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			}
		} else if (condition.getGeneral_condition().equals(
				getString(R.string.foggy))) {
			if (condition.getFog_thickness().equals(
					getString(R.string.light_fog))) {
				Drawable fogDrawable = this.getResources().getDrawable(
						R.drawable.ic_wea_col_fog1);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(fogDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			} else if (condition.getFog_thickness().equals(
					getString(R.string.moderate_fog))) {
				Drawable fogDrawable = this.getResources().getDrawable(
						R.drawable.ic_wea_col_fog2);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(fogDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			} else if (condition.getFog_thickness().equals(
					getString(R.string.heavy_fog))) {
				Drawable fogDrawable = this.getResources().getDrawable(
						R.drawable.ic_wea_col_fog3);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(fogDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			} else {
				Drawable fogDrawable = this.getResources().getDrawable(
						R.drawable.ic_wea_col_fog2);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(fogDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			}
		} else if (condition.getGeneral_condition().equals(
				getString(R.string.thunderstorm))) {
			if (Double.parseDouble(condition.getThunderstorm_intensity()) == 0.0) {
				Drawable thunderstormDrawable = this.getResources()
						.getDrawable(R.drawable.ic_wea_col_r_l1);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(thunderstormDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			} else if (Double
					.parseDouble(condition.getThunderstorm_intensity()) == 1.0) {
				Drawable thunderstormDrawable = this.getResources()
						.getDrawable(R.drawable.ic_wea_col_r_l2);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(thunderstormDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			} else if (Double
					.parseDouble(condition.getThunderstorm_intensity()) == 2.0) {
				Drawable thunderstormDrawable = this.getResources()
						.getDrawable(R.drawable.ic_wea_col_r_l3);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(thunderstormDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			}
		} else {
			// there is no current condition. show just the barometer icon
			// perhaps with a tendency arrow
			// log("No condition found, default: " +
			// getString(R.string.precipitation) + " " +
			// condition.getGeneral_condition());

		}

		return overlay;
	}

	// The gesture threshold expressed in dp
	// http://developer.android.com/guide/practices/screens_support.html#density-independence
	private static final float GESTURE_THRESHOLD_DP = 16.0f;

	// resize drawables on demand.
	// High-res bitmaps on Android? Be careful of memory issues
	private Drawable resizeDrawable(Drawable image) {
		Bitmap d = ((BitmapDrawable) image).getBitmap();
		final float scale = getResources().getDisplayMetrics().density;
		int p = (int) (GESTURE_THRESHOLD_DP * scale + 0.5f);
		Bitmap bitmapOrig = Bitmap.createScaledBitmap(d, p * 4, p * 4, false);
		return new BitmapDrawable(bitmapOrig);
	}

	// Put a bunch of barometer readings and current conditions on the map.
	public void addDataFrameToMap(ArrayList<CbWeather> frameConditions, ArrayList<CbWeather> frameObservations) {
		BarometerMapView mv = (BarometerMapView) findViewById(R.id.mapview);
		List<Overlay> mapOverlays = mv.getOverlays();

		Drawable drawable = this.getResources().getDrawable(
				R.drawable.ic_marker);
		mapOverlays.clear();
		
		int totalEachAllowed = 30;
		int currentObs = 0;
		int currentCur = 0;
		try {
			// Add Barometer Readings and associated current Conditions
			// Add Barometer Readings and associated current Conditions
			for(CbWeather weatherObs : frameObservations) {
				CbObservation obs = (CbObservation) weatherObs;
				
				MapOverlay overlay;

				// Pick an overlay icon depending on the reading and
				// the current conditions. reading alone? reading with tendency?
				// current condition alone? current condition with tendency?

				// is there a current condition from the same user as this
				// reading?
				overlay = new MapOverlay(drawable, this, mapFontSize);
				

				GeoPoint point = new GeoPoint(
						(int) ((obs.getLocation().getLatitude()) * 1E6),
						(int) ((obs.getLocation().getLongitude()) * 1E6));
				String snippet = "s"; // condition.getUser_id();
				String textForTitle = obs.getTrend();
				OverlayItem overlayitem = new OverlayItem(point, textForTitle,
						snippet);
				overlay.addOverlay(overlayitem);
				mapOverlays.add(overlay);

				mv.invalidate();
				currentObs++;
				if(currentObs > totalEachAllowed) {
					break;
				}
			}

			// Add singleton Current Conditions
			for (CbWeather weather: frameConditions) {
				MapOverlay overlay;
				CbCurrentCondition condition = (CbCurrentCondition) weather;

				// Pick an overlay icon depending on the reading and
				// the current conditions. reading alone? reading with tendency?
				// current condition alone? current condition with tendency?

				// is there a current condition from the same user as this
				// reading?
				overlay = new MapOverlay(drawable, this, mapFontSize);
				overlay = getCurrentConditionOverlay(condition, drawable);

				GeoPoint point = new GeoPoint(
						(int) ((condition.getLocation().getLatitude()) * 1E6),
						(int) ((condition.getLocation().getLongitude()) * 1E6));
				String snippet = "singleton_condition"; // condition.getUser_id();
				String textForTitle = "";
				OverlayItem overlayitem = new OverlayItem(point, textForTitle,
						snippet);
				overlay.addOverlay(overlayitem);
				mapOverlays.add(overlay);

				mv.invalidate();
				currentCur++;
				if(currentCur > totalEachAllowed) {
					break;
				}
			}
		} catch (Exception e) {
			log("add data error: " + e.getMessage());
		}
	}

	// Put a bunch of barometer readings and current conditions on the map.
	public void addDataToMap() {
		int totalEachAllowed = 30;
		int currentObs = 0;
		int currentCur = 0;
		BarometerMapView mv = (BarometerMapView) findViewById(R.id.mapview);
		List<Overlay> mapOverlays = mv.getOverlays();

		Drawable drawable = this.getResources().getDrawable(
				R.drawable.ic_marker);
		mapOverlays.clear();

		try {

			// Add Barometer Readings and associated current Conditions
			for(CbObservation obs : recents) {
				MapOverlay overlay;

				// Pick an overlay icon depending on the reading and
				// the current conditions. reading alone? reading with tendency?
				// current condition alone? current condition with tendency?

				// is there a current condition from the same user as this
				// reading?
				overlay = new MapOverlay(drawable, this, mapFontSize);
				

				GeoPoint point = new GeoPoint(
						(int) ((obs.getLocation().getLatitude()) * 1E6),
						(int) ((obs.getLocation().getLongitude()) * 1E6));
				String snippet = "s"; // condition.getUser_id();
				String textForTitle = obs.getTrend();
				OverlayItem overlayitem = new OverlayItem(point, textForTitle,
						snippet);
				overlay.addOverlay(overlayitem);
				mapOverlays.add(overlay);

				mv.invalidate();
				currentObs++;
				if(currentObs > totalEachAllowed) {
					break;
				}
			}
			
			// Add singleton Current Conditions
			for (CbCurrentCondition condition : currentConditions) {
				MapOverlay overlay;

				// Pick an overlay icon depending on the reading and
				// the current conditions. reading alone? reading with tendency?
				// current condition alone? current condition with tendency?

				// is there a current condition from the same user as this
				// reading?
				overlay = new MapOverlay(drawable, this, mapFontSize);
				overlay = getCurrentConditionOverlay(condition, drawable);

				GeoPoint point = new GeoPoint(
						(int) ((condition.getLocation().getLatitude()) * 1E6),
						(int) ((condition.getLocation().getLongitude()) * 1E6));
				String snippet = "singleton_condition"; // condition.getUser_id();
				String textForTitle = "";
				OverlayItem overlayitem = new OverlayItem(point, textForTitle,
						snippet);
				overlay.addOverlay(overlayitem);
				mapOverlays.add(overlay);

				mv.invalidate();
				currentCur++;
				if(currentCur > totalEachAllowed) {
					break;
				}
			}
		} catch (Exception e) {
			log("add data error: " + e.getMessage());
		}
	}

	// Runnable to refresh the map. Can be called when another
	// thread wishes to refresh the view.
	private final Runnable refreshMap = new Runnable() {
		public void run() {
			BarometerMapView mapView = (BarometerMapView) findViewById(R.id.mapview);
			mapView.invalidate();
			mapView.refreshDrawableState();
		}
	};

	public CbApiCall buildMapAPICall(int hoursAgo) {
		BarometerMapView mapView = (BarometerMapView) findViewById(R.id.mapview);
		GeoPoint gp = mapView.getMapCenter();
		int latE6 = gp.getLatitudeE6();
		int lonE6 = gp.getLongitudeE6();
		double latitude = latE6 / 1E6;
		double longitude = lonE6 / 1E6;
		double latitudeSpan = mapView.getLatitudeSpan();
		double longitudeSpan = mapView.getLongitudeSpan();
		long startTime = System.currentTimeMillis()
				- (hoursAgo * 60 * 60 * 1000);
		long endTime = System.currentTimeMillis();
		CbApiCall api = new CbApiCall();
		api.setMinLat(latitude - (latitudeSpan / 1E6));
		api.setMaxLat(latitude + (latitudeSpan / 1E6));
		api.setMinLon(longitude - (longitudeSpan / 1E6));
		api.setMaxLon(longitude + (longitudeSpan / 1E6));
		api.setStartTime(startTime);
		api.setEndTime(endTime);
		api.setApiKey(PressureNETConfiguration.API_KEY);
		api.setLimit(500);
		return api;
	}

	private void makeAPICall(CbApiCall apiCall) {
		if (mBound) {
			Message msg = Message.obtain(null, CbService.MSG_MAKE_API_CALL,
					apiCall);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("data management error: not bound");
		}
	}

	public void makeMapApiCallAndLoadRecents() {
		CbApiCall api = buildMapAPICall(hoursAgoSelected);
		askForRecents(api);
		askForCurrentConditions(api);

		// make a fresh call with extra nearby data
		api.setMinLat(api.getMinLat() - .5);
		api.setMaxLat(api.getMaxLat() + .5);
		api.setMinLon(api.getMinLon() - .5);
		api.setMaxLon(api.getMaxLon() + .5);
		makeAPICall(api);
		createAndShowChart();
	}

	private BroadcastReceiver receiveForMap = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(BarometerMapView.CUSTOM_INTENT)) {
				BarometerMapView mapView = (BarometerMapView) findViewById(R.id.mapview);

				makeMapApiCallAndLoadRecents();

			}
		}
	};

	// Stop listening to the barometer when our app is paused.
	@Override
	protected void onPause() {
		super.onPause();
		stopDataStream();
		unBindCbService();
		unregisterReceiver(receiveForMap);
	}

	// Register a broadcast listener
	@Override
	protected void onResume() {
		super.onResume();
		bindCbService();
		registerReceiver(receiveForMap, new IntentFilter(
				BarometerMapView.CUSTOM_INTENT));

	}

	// Must exist for the MapView.
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	protected void onStart() {
		dataReceivedToPlot = false;
		bindCbService();
		super.onStart();
	}

	@Override
	protected void onStop() {
		dataReceivedToPlot = false;
		stopDataStream();
		unBindCbService();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		dataReceivedToPlot = false;
		stopDataStream();
		unBindCbService();
		super.onDestroy();
	}

	public void updateVisibleReading() {

		if (bestPressure != null) {
			double value = bestPressure.getObservationValue();
			TextView textView = (TextView) findViewById(R.id.textReading);
			if (bestPressure.getObservationValue() != 0.0) {
				textView.setVisibility(View.VISIBLE);
				DecimalFormat df = new DecimalFormat("####.00");
				String toPrint = df.format(value);
				textView.setText(toPrint + " "
						+ bestPressure.getObservationUnit() + " " + mTendency
						+ " ");
			} else {
				textView.setText("No barometer detected.");
			}
		}
	}

	// Log data to SD card for debug purposes.
	// To enable logging, ensure the Manifest allows writing to SD card.
	public void logToFile(String text) {
		try {
			OutputStream output = new FileOutputStream(mAppDir + "/log.txt",
					true);
			String logString = (new Date()).toString() + ": " + text + "\n";
			output.write(logString.getBytes());
			output.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void log(String text) {
		// logToFile(text);
		System.out.println(text);
	}
}
