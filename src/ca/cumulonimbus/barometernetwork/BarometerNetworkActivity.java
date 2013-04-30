package ca.cumulonimbus.barometernetwork;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

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
import android.os.AsyncTask;
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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import ca.cumulonimbus.pressurenetsdk.CbApiCall;
import ca.cumulonimbus.pressurenetsdk.CbObservation;
import ca.cumulonimbus.pressurenetsdk.CbService;
import ca.cumulonimbus.pressurenetsdk.CbSettingsHandler;

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

	boolean dataReceivedToPlot = false;

	Button buttonChart;
	Button buttonCallAPI;

	// API call parameters. 
	private double apiLatitude = 0.0;
	private double apiLongitude = 0.0;
	private double apiLatitudeSpan = 0.0;
	private double apiLongitudeSpan = 0.0;

	private long apiStartTime = 0;
	private long apiEndTime = 0;
	private String apiFormat = "json";
	private ArrayList<CbObservation> apiCbObservationResults = new ArrayList<CbObservation>();

	String apiServerURL = "https://pressurenet.cumulonimbus.ca/live/?";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dataReceivedToPlot = false;
		setContentView(R.layout.main);
		// migratePreferences();
		startLog();
		// getStoredPreferences();
		setId();
		setUpFiles();
		setUpMap();
		showWelcomeActivity();
		setUpActionBar();
		startCbService();
		bindCbService();
		setUpUIListeners();
	}
	
	private void makeAPICall(CbApiCall apiCall) {
		if (mBound) {
			log("making Live API Call");
			Message msg = Message.obtain(null, CbService.MSG_MAKE_API_CALL, apiCall);
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

	private void setUpUIListeners() {
		Context context=getApplicationContext();
		mInflater = LayoutInflater.from(context);
		buttonCallAPI = (Button) findViewById(R.id.buttonCallAPI);
		buttonChart = (Button) findViewById(R.id.buttonDrawChart);
		
		buttonChart.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				CbApiCall apiCall = buildMapAPICall();
				askForRecents(apiCall);
				createAndShowChart();
			}
		});
		
		buttonCallAPI.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// CbApiCall api = buildMapAPICall();
				// askForRecents(api);
				
				CbApiCall apiCall = buildMapAPICall();
				makeAPICall(apiCall);
			}
		});
	}

	private void startCbService() {
		log("start cbservice");
		try {
			serviceIntent = new Intent(this, CbService.class);
			serviceIntent.putExtra("serverURL", "http://localhost:8000/");

			startService(serviceIntent);

		} catch (Exception e) {
			e.printStackTrace();
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
			
			
			Message msg = Message.obtain(null, CbService.MSG_GET_API_RECENTS, apiCall);
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
			default:
				log("received default message");
				super.handleMessage(msg);
			}
		}
	}

	
	public void createAndShowChart() {
		if (dataReceivedToPlot) {
			// draw chart
			log("plotting...");
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
			LayoutParams lparams = new LayoutParams(LayoutParams.FILL_PARENT,
					500);
			
			chartView.setLayoutParams(lparams);
			if(mainLayout == null ) {
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
			startActivityForResult(intent, 0);
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
			menu.removeItem(R.id.menu_delete_data);
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
		} else if (item.getItemId() == R.id.menu_delete_data) {
			deleteUserData();
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

	// Assemble a list of CurrentConditions. This is the opposite of function
	// currentConditionToWeb in the servlet.
	public ArrayList<CurrentCondition> csvToCurrentConditions(
			String[] conditions) {
		ArrayList<CurrentCondition> conditionsList = new ArrayList<CurrentCondition>();
		for (int i = 0; i < conditions.length; i++) {
			try {
				String[] values = conditions[i].split("\\|");
				CurrentCondition cc = new CurrentCondition();
				cc.setLatitude(Double.parseDouble(values[0]));
				cc.setLongitude(Double.parseDouble(values[1]));
				cc.setGeneral_condition(values[2]);
				cc.setTime(Double.parseDouble(values[3]));
				cc.setTzoffset((Integer.parseInt(values[4])));
				cc.setWindy(values[5]);
				cc.setPrecipitation_type(values[6]);
				cc.setPrecipitation_amount(Double.parseDouble(values[7]));
				cc.setThunderstorm_intensity(values[8]);
				cc.setCloud_type(values[9]);
				cc.setFog_thickness(values[10]);
				cc.setUser_id(values[11]);
				conditionsList.add(cc);
			} catch (NumberFormatException nfe) {
				// Likely, tomcat returned an error.
				log("Server error? " + nfe.getMessage());
			}
		}
		return conditionsList;
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
			log(e.getMessage() + "");
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
			log(e.getMessage());
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
			log(e.getMessage() + "");
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
						Double value = Double.parseDouble(toPrint);
						DecimalFormat df = new DecimalFormat("####.00");
						toPrint = df.format(value);

						// show text to the right of the icon
						float textWidth = paint.measureText(toPrint);
						Paint bgPaint = new Paint();
						bgPaint.setColor(Color.WHITE);

						Rect rect = new Rect((int) (ptScreenCoord.x
								- (textWidth / 2) - 2), ptScreenCoord.y,
								(int) (ptScreenCoord.x + (textWidth / 2) + 2),
								ptScreenCoord.y + mTextSize + 5);
						canvas.drawRoundRect(new RectF(rect), 6, 6, bgPaint);
						canvas.drawText(toPrint, ptScreenCoord.x,
								ptScreenCoord.y + mTextSize, paint);
					} catch (Exception e) {
						log(e.getMessage() + "");
					}
				}
			}
		}
	}

	// Assume that matching latitude and longitude can only be you.
	public boolean obsIsMe(CbObservation ob) {
		return ((ob.getUser_id().equals(android_id)));
	}

	public MapOverlay getCurrentConditionOverlay(CurrentCondition condition,
			Drawable drawable) {
		MapOverlay overlay = new MapOverlay(drawable, this, mapFontSize);

		Drawable weatherBackgroundDrawable = resizeDrawable(this.getResources()
				.getDrawable(R.drawable.bg_wea_square));

		if (condition.getGeneral_condition().equals(getString(R.string.sunny))) {
			Drawable sunDrawable = this.getResources().getDrawable(
					R.drawable.ic_col_sun);
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
							R.drawable.ic_col_rain1);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(rainDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				} else if (condition.getPrecipitation_amount() == 1.0) {
					Drawable rainDrawable = this.getResources().getDrawable(
							R.drawable.ic_col_rain2);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(rainDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				} else if (condition.getPrecipitation_amount() == 2.0) {
					Drawable rainDrawable = this.getResources().getDrawable(
							R.drawable.ic_col_rain3);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(rainDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				}
			} else if (condition.getPrecipitation_type().equals(
					getString(R.string.snow))) {
				if (condition.getPrecipitation_amount() == 0.0) {
					Drawable snowDrawable = this.getResources().getDrawable(
							R.drawable.ic_col_snow1);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(snowDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				} else if (condition.getPrecipitation_amount() == 1.0) {
					Drawable snowDrawable = this.getResources().getDrawable(
							R.drawable.ic_col_snow2);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(snowDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				} else if (condition.getPrecipitation_amount() == 2.0) {
					Drawable snowDrawable = this.getResources().getDrawable(
							R.drawable.ic_col_snow3);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(snowDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				}
			} else if (condition.getPrecipitation_type().equals(
					getString(R.string.hail))) {
				if (condition.getPrecipitation_amount() == 0.0) {
					Drawable hailDrawable = this.getResources().getDrawable(
							R.drawable.ic_col_hail1);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(hailDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				} else if (condition.getPrecipitation_amount() == 1.0) {
					Drawable hailDrawable = this.getResources().getDrawable(
							R.drawable.ic_col_hail2);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(hailDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					overlay = new MapOverlay(layerDrawable, this, mapFontSize);
				} else if (condition.getPrecipitation_amount() == 2.0) {
					Drawable hailDrawable = this.getResources().getDrawable(
							R.drawable.ic_col_hail3);
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
						R.drawable.ic_col_cloudy1);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(cloudDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			} else if (condition.getCloud_type().equals(
					getString(R.string.mostly_cloudy))) {
				Drawable cloudDrawable = this.getResources().getDrawable(
						R.drawable.ic_col_cloudy2);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(cloudDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			} else if (condition.getCloud_type().equals(
					getString(R.string.very_cloudy))) {
				Drawable cloudDrawable = this.getResources().getDrawable(
						R.drawable.ic_col_cloudy);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(cloudDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			} else {
				Drawable cloudDrawable = this.getResources().getDrawable(
						R.drawable.ic_col_cloudy);
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
						R.drawable.ic_col_fog1);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(fogDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			} else if (condition.getFog_thickness().equals(
					getString(R.string.moderate_fog))) {
				Drawable fogDrawable = this.getResources().getDrawable(
						R.drawable.ic_col_fog2);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(fogDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			} else if (condition.getFog_thickness().equals(
					getString(R.string.heavy_fog))) {
				Drawable fogDrawable = this.getResources().getDrawable(
						R.drawable.ic_col_fog3);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(fogDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			} else {
				Drawable fogDrawable = this.getResources().getDrawable(
						R.drawable.ic_col_fog2);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(fogDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			}
		} else if (condition.getGeneral_condition().equals(
				getString(R.string.thunderstorm))) {
			if (Double.parseDouble(condition.getThunderstorm_intensity()) == 0.0) {
				Drawable thunderstormDrawable = this.getResources()
						.getDrawable(R.drawable.ic_col_r_l1);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(thunderstormDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			} else if (Double
					.parseDouble(condition.getThunderstorm_intensity()) == 1.0) {
				Drawable thunderstormDrawable = this.getResources()
						.getDrawable(R.drawable.ic_col_r_l2);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(thunderstormDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				overlay = new MapOverlay(layerDrawable, this, mapFontSize);
			} else if (Double
					.parseDouble(condition.getThunderstorm_intensity()) == 2.0) {
				Drawable thunderstormDrawable = this.getResources()
						.getDrawable(R.drawable.ic_col_r_l3);
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
	public void addDataToMap(ArrayList<CbObservation> readingsList,
			ArrayList<CurrentCondition> conditionsList, boolean showTendencies,
			HashMap<String, String> tendencies) {
		log("add data to map " + readingsList.size());
		BarometerMapView mv = (BarometerMapView) findViewById(R.id.mapview);
		List<Overlay> mapOverlays = mv.getOverlays();

		Drawable drawable = this.getResources().getDrawable(
				R.drawable.ic_marker);
		mapOverlays.clear();

		try {
			// Add Barometer Readings and associated current Conditions
			for (CbObservation ob : readingsList) {
				MapOverlay overlay;

				String snippet = ob.getUser_id();

				// Pick an overlay icon depending on the reading and
				// the current conditions. reading alone? reading with tendency?
				// current condition alone? current condition with tendency?

				// is there a current condition from the same user as this
				// reading?
				Drawable pressureBackgroundDrawable = resizeDrawable(this
						.getResources().getDrawable(R.drawable.bg_pre_rect));
				boolean onlyPressure = true;
				overlay = new MapOverlay(drawable, this, mapFontSize);
				for (CurrentCondition condition : conditionsList) {
					if (condition.getUser_id().equals(ob.getUser_id())) {
						// pick and hold the overlay
						overlay = getCurrentConditionOverlay(condition,
								drawable);
						// remove this condition from the list and break out of
						// the loop
						// this leaves all non-barometer device conditions in
						// the list
						// for processing just after.
						conditionsList.remove(condition);
						onlyPressure = false;
						break;
					}
				}

				if (onlyPressure) {
					// overlay = new MapOverlay(pressureBackgroundDrawable,
					// this, mapFontSize);
				}

				GeoPoint point = new GeoPoint(
						(int) ((ob.getLocation().getLatitude()) * 1E6),
						(int) ((ob.getLocation().getLongitude()) * 1E6));

				String textForTitle = ob.getObservationValue() + " "
						+ ob.getObservationUnit();
				OverlayItem overlayitem = new OverlayItem(point, textForTitle,
						snippet);
				overlay.addOverlay(overlayitem);
				mapOverlays.add(overlay);

				mv.invalidate();
			}

			// Add singleton Current Conditions
			for (CurrentCondition condition : conditionsList) {
				MapOverlay overlay;

				// Pick an overlay icon depending on the reading and
				// the current conditions. reading alone? reading with tendency?
				// current condition alone? current condition with tendency?

				// is there a current condition from the same user as this
				// reading?
				overlay = new MapOverlay(drawable, this, mapFontSize);
				overlay = getCurrentConditionOverlay(condition, drawable);

				GeoPoint point = new GeoPoint(
						(int) ((condition.getLatitude()) * 1E6),
						(int) ((condition.getLongitude()) * 1E6));
				String snippet = "singleton_condition"; // condition.getUser_id();
				String textForTitle = "";
				OverlayItem overlayitem = new OverlayItem(point, textForTitle,
						snippet);
				overlay.addOverlay(overlayitem);
				mapOverlays.add(overlay);

				mv.invalidate();
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

	public CbApiCall buildMapAPICall() {
		BarometerMapView mapView = (BarometerMapView) findViewById(R.id.mapview);
		GeoPoint gp = mapView.getMapCenter();
		int latE6 = gp.getLatitudeE6();
		int lonE6 = gp.getLongitudeE6();
		double latitude = latE6 / 1E6;
		double longitude = lonE6 / 1E6;
		double latitudeSpan = mapView.getLatitudeSpan();
		double longitudeSpan = mapView.getLongitudeSpan();
		long startTime = System.currentTimeMillis() - (3 * 60 * 60 * 1000);
		long endTime = System.currentTimeMillis();
		CbApiCall api = new CbApiCall();
		api.setMinLat(latitude - (latitudeSpan/1E6));
		api.setMaxLat(latitude + (latitudeSpan/1E6));
		api.setMinLon(longitude - (longitudeSpan/1E6));
		api.setMaxLon(longitude + (longitudeSpan/1E6));
		api.setStartTime(startTime);
		api.setEndTime(endTime);
		api.setApiKey(PressureNETConfiguration.API_KEY);
		return api;
	}
	
	private BroadcastReceiver receiveForMap = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(BarometerMapView.CUSTOM_INTENT)) {
				BarometerMapView mapView = (BarometerMapView) findViewById(R.id.mapview);
				GeoPoint gp = mapView.getMapCenter();
				int latE6 = gp.getLatitudeE6();
				int lonE6 = gp.getLongitudeE6();
				double latitude = latE6 / 1E6;
				double longitude = lonE6 / 1E6;
				double latitudeSpan = mapView.getLatitudeSpan();
				double longitudeSpan = mapView.getLongitudeSpan();
				long startTime = System.currentTimeMillis() - (3 * 60 * 60 * 1000);
				long endTime = System.currentTimeMillis();
				CbApiCall api = new CbApiCall();
				api.setMinLat(latitude - (latitudeSpan/1E6));
				api.setMaxLat(latitude + (latitudeSpan/1E6));
				api.setMinLon(longitude - (longitudeSpan/1E6));
				api.setMaxLon(longitude + (longitudeSpan/1E6));
				api.setStartTime(startTime);
				api.setEndTime(endTime);
				askForRecents(api);
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
		logToFile(text);
		System.out.println(text);
	}
}
