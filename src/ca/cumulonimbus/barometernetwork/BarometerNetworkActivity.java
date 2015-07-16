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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import ca.cumulonimbus.barometernetwork.PressureNetApplication.TrackerName;
import ca.cumulonimbus.pressurenetsdk.CbApiCall;
import ca.cumulonimbus.pressurenetsdk.CbConfiguration;
import ca.cumulonimbus.pressurenetsdk.CbCurrentCondition;
import ca.cumulonimbus.pressurenetsdk.CbDb;
import ca.cumulonimbus.pressurenetsdk.CbObservation;
import ca.cumulonimbus.pressurenetsdk.CbScience;
import ca.cumulonimbus.pressurenetsdk.CbService;
import ca.cumulonimbus.pressurenetsdk.CbSettingsHandler;
import ca.cumulonimbus.pressurenetsdk.CbStatsAPICall;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;
import com.mixpanel.android.mpmetrics.MixpanelAPI;



public class BarometerNetworkActivity extends Activity implements
		SensorEventListener {
	
	public static final int NOTIFICATION_ID = 101325;

	double mLatitude = 0.0;
	double mLongitude = 0.0;
	double mAltitude = 0.0;
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

	private String mTendency = "";

	public String statusText = "";
	private int mapFontSize = 18; 

	Intent serviceIntent;

	// PressureNet 4.0
	// SDK communication
	boolean mBound;
	private Messenger mMessenger = new Messenger(new IncomingHandler());
	Messenger mService = null;

	Location bestLocation;
	CbObservation bestPressure;
	CbSettingsHandler activeSettings;

	private ArrayList<CbCurrentCondition> globalConditionRecents = new ArrayList<CbCurrentCondition>();
	
	
	private ArrayList<CbCurrentCondition> localConditionRecents = new ArrayList<CbCurrentCondition>();
	private ArrayList<CbCurrentCondition> conditionAnimationRecents = new ArrayList<CbCurrentCondition>();
	

	private int activeAPICallCount = 0;

	boolean dataReceivedToPlot = false;

	
	private int hoursAgoSelected = 12;

	private ProgressBar progressAPI;

	private ImageButton buttonSearchLocations;

	private ImageButton buttonMyLocation;
	private String locationButtonMode = "conditions";
	
	private SeekBar animationProgress;
	
	private Calendar calAnimationStartDate;
	private long animationDurationInMillis = 0;
	
	Handler timeHandler = new Handler();
	Handler mapDelayHandler = new Handler();
	Handler animationHandler = new Handler();

	private int animationStep = 0;

	private boolean temperatureAnimationPlaying = false;
	private int temperatureAnimationStep = 0;
	private TemperatureAnimator temperatureAnimator = new TemperatureAnimator();
	private ArrayList<MarkerOptions> temperatureAnimationMarkerOptions = new ArrayList<MarkerOptions>();
	private ArrayList<TemperatureForecast> forecastRecents = new ArrayList<TemperatureForecast>();
	private String activeForecastStartTime = "";
	
	String apiServerURL = CbConfiguration.SERVER_URL_PRESSURENET + "list/?";

	private boolean locationAvailable = true;
	
	double recentPressureReading = 0.0;
	double recentTemperatureReading = 1000; // TODO: fix default value hack
	double recentHumidityReading = 1000;
	private final int TYPE_AMBIENT_TEMPERATURE = 13;
	private final int TYPE_RELATIVE_HUMIDITY = 12;

	public static final int REQUEST_SETTINGS = 1;
	public static final int REQUEST_LOCATION_CHOICE = 2;
	public static final int REQUEST_MAILED_LOG = 3;
	public static final int REQUEST_DATA_CHANGED = 4;
	public static final int REQUEST_ANIMATION_PARAMS = 5;

	public static final String GA_CATEGORY_MAIN_APP = "app";
	public static final String GA_CATEGORY_NOTIFICATIONS = "notifications";
	public static final String GA_CATEGORY_WIDGET = "widgets";
	
	public static final String GA_ACTION_MODE = "mode";
	public static final String GA_ACTION_SEARCH = "search";
	public static final String GA_ACTION_BUTTON = "button_press";
	
	
	/**
	 * preferences
	 */
	private String preferencePressureUnit;
	private String preferenceTemperatureUnit;
	private String preferenceCollectionFrequency;
	private boolean preferenceShareData;
	private String preferenceShareLevel;
	private boolean preferenceSendNotifications;
	private boolean preferenceUseGPS;
	private boolean preferenceWhenCharging;
	private boolean preferenceMSLP;
	private String preferenceDistanceUnit;

	private GoogleMap mMap;
	private LatLngBounds visibleBound;

	// Search Locations
	private ImageButton buttonGoLocation;
	private EditText editLocation;

	private ImageButton imageButtonPlay;
	private TextView textAnimationInfoLeft;
	private TextView textAnimationInfoRight;

	private ArrayList<SearchLocation> searchedLocations = new ArrayList<SearchLocation>();

	private String activeMode = "map";
	private long lastGlobalApiCall = System.currentTimeMillis()
			- (1000 * 60 * 60);
	private long lastSearchLocationsAPICall = System.currentTimeMillis()
			- (1000 * 60 * 60);
	private long lastGlobalConditionsApiCall = System.currentTimeMillis()
			- (1000 * 60 * 60);
	private long lastMapDataUpdate = System.currentTimeMillis()
			- (1000 * 60 * 60);

	private boolean isConnected = false;

	private boolean hasBarometer = true;
	private boolean hasThermometer = false;
	private boolean hasHygrometer = false;
	
	private long lastSubmitStart = 0;

	private final int DEFAULT_ZOOM = 11;
	
	private static final String moon_phase_name[] = { "New Moon", // 0
			"Waxing crescent", // 1
			"First quarter", // 2
			"Waxing gibbous", // 3
			"Full Moon", // 4
			"Waning gibbous", // 5
			"Third quarter", // 6
			"Waning crescent" }; // 7

	private ArrayList<MarkerOptions> animationMarkerOptions = new ArrayList<MarkerOptions>();

	private boolean displayConditions = true;
	
	private boolean animationPlaying = false;
	private AnimationRunner animator = new AnimationRunner();
	
	Message statsMsg = null;

	private boolean isPrimaryApp = true;
	
	private long appStartTime = 0;
	
	private Handler appHintsHandler;
	
	private boolean userPrompted = false;
	
	MixpanelAPI mixpanel;
	
	private Menu mOptionsMenu;
	
	private Handler conditionsHandler = new Handler(); 
	private ConditionsAdder conditionsAdder = new ConditionsAdder();
	
	private DrawerLayout drawerLayout;
	private ListView drawerList;
	
	ArrayList<MarkerOptions> liveMarkerOptions = new ArrayList<MarkerOptions>();
	
	ArrayList<ForecastLocation> liveMapForecasts = new ArrayList<ForecastLocation>();
	private String mapStartTime = "";
	
	String[] drawerListContents = {"My data", "Locations", "Settings", "About", "Rate & review", "Invite your friends!"};
	private ActionBarDrawerToggle drawerToggle;
	
	public static final String DATA_DOWNLOAD_RESULTS = "ca.cumulonimbus.barometernetwork.DATA_DOWNLOAD";
	
	private long lastMapRefresh = 0;
	
	// Supported Geography limits (map & forecast temperatures)
	private double minSupportedLatitude = 20;
	private double maxSupportedLatitude = 70;
	private double minSupportedLongitude = -165;
	private double maxSupportedLongitude = -45;
	
	private LinearLayout layoutNoConditionsPrompt;
	private LinearLayout layoutNoConditionsThanks;
	private Button buttonNotifyMe;
	private ImageButton buttonCloseNoConditions;
	private Button inviteFriends3;
	private ImageButton buttonCloseNoConditionsPrompt;
	private EditText editTextEmail;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dataReceivedToPlot = false;
		appStartTime = System.currentTimeMillis();
		setContentView(R.layout.main);
		addDrawerLayout();
		checkNetwork();
		checkSensors();
		setLastKnownLocation();
		startLog();
		getStoredPreferences();
		registerForDownloadResults();
		setUpMap();
		setUpUIListeners();
		setId();
		setUpFiles();
		//setUpActionBar();
		checkDb();
		callExternalAPIs();
		setUpMixPanel();		
	}
	
	private void registerForDownloadResults() {
		LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(DATA_DOWNLOAD_RESULTS);
		bManager.registerReceiver(bReceiver, intentFilter);
	}

	private BroadcastReceiver bReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	SharedPreferences sharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());
			long lastGlobalForecastCall = sharedPreferences.getLong("lastGlobalForecastCall", 0);
			
		
			long waitDiff = 1000 * 60 * 60; // one hour wait
	    	long now = System.currentTimeMillis();
	        
	    	if(intent.getAction().equals(DATA_DOWNLOAD_RESULTS)) {
	        	if(!temperatureAnimationPlaying) {
	        		liveMapForecasts.clear();
	        		addTemperaturesToMap();
	        		addConditionMarkersToMap();
	        	}
	            double deltaExtra = intent.getDoubleExtra("delta", 0);
	            if(deltaExtra < 3) {
	            	// make another call, this time global
	            	if(now - lastGlobalForecastCall > waitDiff) {
	            		log("app says it's been more than 1h since last global forecast temperature call. go ahead!");
	            		
	            		SharedPreferences.Editor editor = sharedPreferences.edit();
	            		lastGlobalForecastCall = System.currentTimeMillis();
		            	editor.putLong("lastGlobalForecastCall", lastGlobalForecastCall);
		            	editor.commit();
	            		
	            		downloadTemperatureData(10);
	            		
	            	} else {
	            		log("app considered making global forecast call, but hasn't been 1h yet");
	            	}
	            	
	            } else {
	            	log("app returned from making global forecast call");
            		
	            }
	        }	        
	    }
	};
	
	private void downloadTemperatureData(double delta) {
		log("app starting to download temperature forecast data");
		Location userLocation;
		if(bestLocation != null) {
			userLocation = bestLocation;
		} else {
			LocationManager lm = (LocationManager) getApplicationContext()
					.getSystemService(Context.LOCATION_SERVICE);
			Location loc = lm
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			userLocation = loc; 
		}
		
		double latitude = userLocation.getLatitude();
		double longitude = userLocation.getLongitude();
		
		Intent tempIntent = new Intent(this, ForecastService.class);
		tempIntent.putExtra("latitude", latitude);
		tempIntent.putExtra("longitude", longitude);
		tempIntent.putExtra("delta", delta);
		
		startService(tempIntent);
	}
	
	private void addDrawerLayout() {
		
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        
        // Set the adapter for the list view
        drawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, drawerListContents));
        // Set the list's click listener
        drawerList.setOnItemClickListener(new DrawerItemClickListener());
        
        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        
        
        
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
            	hideKeyboard();
            	super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
            	hideKeyboard();
            	super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                
            }
            
            
        };

        drawerToggle.setDrawerIndicatorEnabled(true);
        
        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(drawerToggle);
   
	}
	
	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(editLocation.getWindowToken(), 0);
		editLocation.setCursorVisible(false);
	}
	
	
	@Override
	public void onPostCreate(Bundle savedInstanceState,
			PersistableBundle persistentState) {
		super.onPostCreate(savedInstanceState, persistentState);
		drawerToggle.syncState();
	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener {
	    @Override
	    public void onItemClick(AdapterView parent, View view, int position, long id) {
	    	log("navigation drawer selected item position " + position + ", " + drawerListContents[position]);
	    	selectItem(position);
	    	
	    	if(position == 0) {
	    		// My Data
	    		viewLog();
	    	} else if (position == 1) {
	    		// Locations
	    		Intent intent = new Intent(getApplicationContext(),
						SearchLocationsActivity.class);
				startActivityForResult(intent, REQUEST_LOCATION_CHOICE);
	    	} else if (position == 2) {
	    		// Settings
	    		Intent i = new Intent(getApplicationContext(), PreferencesActivity.class);
				startActivityForResult(i, REQUEST_SETTINGS);
				overridePendingTransition(R.anim.main_to_log_viewer_open, R.anim.main_to_log_viewer_close);
	    	} else if (position == 3) {
	    		// About
	    		Intent intent = new Intent(getApplicationContext(),
						AboutActivity.class);
				startActivity(intent);
				overridePendingTransition(R.anim.main_to_log_viewer_open, R.anim.main_to_log_viewer_close);
	    	} else if (position == 4) {
	    		// Rate & review
	    		ratePressureNET();
	    	} else if (position == 5) {
	    		// Tell your friends
	    		growPressureNET();
	    	} else {
	    		log("navigation drawer unknown event");
	    	}
	    	
	    }
	}
	

	/** Swaps fragments in the main content view */
	private void selectItem(int position) {
	    // Create a new fragment and specify the planet to show based on position


	    // Highlight the selected item, update the title, and close the drawer
		drawerList.setItemChecked(position, true);
	    //setTitle(mPlanetTitles[position]);
	    drawerLayout.closeDrawer(drawerList);
	    
	}

	
	/**
	 * Show a red icon in the Action bar if there are alerts.
	 * Show a default icon otherwise.
	 */
	private void setActionBarAlertIcon() {
		
		Drawable d = getResources().getDrawable(R.drawable.ic_wea_on_severe);
		
		PnDb db = new PnDb(getApplicationContext());
		db.open();
		
		Cursor c = db.fetchRecentForecastAlerts();
		if(c.getCount()>0) {

			int color = Color.parseColor("#ec2826");
			d.setColorFilter( color, Mode.MULTIPLY );
			mOptionsMenu.findItem(R.id.menu_test_forecastalerts).setIcon(d);
			log("main activity launching, setting action bar icon color for new forecast alerts");
		} else {
			log("main activity launching, NOT setting action bar icon color  (no forecast alerts)");
		}
		db.close();
	}
	
	/**
	 * Send a message to CbService to register with GCM for notifications
	 */
	private void registerForNotifications() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		boolean firstLaunch = sharedPreferences.getBoolean("firstLaunch", true);
		sendRegistrationInfo();
		if (firstLaunch) {
			// sendRegistrationInfo();	
		} else {
			log("app not registering for notifications, not first launch");
		}
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean("firstLaunch", false);
		editor.commit();
	}
	
	private void setUpMixPanel() {
		// Mixpanel project token, MIXPANEL_TOKEN, and a reference
		// to your application context.
		mixpanel = MixpanelAPI.getInstance(getApplicationContext(), PressureNETConfiguration.MIXPANEL_TOKEN);
		mixpanel.identify(getID());
		
		mixpanel.getPeople().identify(getID());
		mixpanel.getPeople().set("UserID", getID());
		
		JSONObject props = new JSONObject();

		JSONObject hashedUserIdProps = new JSONObject();
		try {
			hashedUserIdProps.put("user_id", getID());
			mixpanel.registerSuperProperties(hashedUserIdProps);
		} catch (JSONException e) {
			log("setupmixpanel json exception " + e.getMessage());
			e.printStackTrace();
		}
				
		mixpanel.track("App Launch", props);
		
	}
	
	/**
	 * Establish which sensors we can access
	 */
	private void checkSensors() {
		checkBarometer();
		checkThermometer();
		checkHygrometer();
	}
	
	/**
	 * Retrieve data from other weather services
	 */
	private void callExternalAPIs() {
	
	}

	/**
	 * Create a db object, open and close it. Forces a check for upgrades
	 */
	private void checkDb() {
		PnDb db = new PnDb(getApplicationContext());
		db.open();
		db.close();
	}
	
	/**
	 * Update local location data with the last known location.
	 */
	private void setLastKnownLocation() {

		LocationManager lm = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		Location loc = lm
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (loc == null) {
			loc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
			log("last known network location null, trying passive");
		}
		
		if (loc == null) {
			log("location still null, no other options");
			locationAvailable = false;
			return;
		}
		
		bestLocation = loc;

		double latitude = loc.getLatitude();
		double longitude = loc.getLongitude();
		double altitude = loc.getAltitude();
		mLatitude = latitude;
		mLongitude = longitude;
		mAltitude = altitude;
		
		buttonMyLocation = (ImageButton) findViewById(R.id.buttonMyLocation);
		try {
			buttonMyLocation.setImageAlpha(255);	
		} catch (NoSuchMethodError nsme) {
			log("app could not set image alpha");
		} catch(RuntimeException re) {
			log("my location button error");
		} catch (Exception e) {
			log("unknown imagealphaerror " + e.getMessage());
		}
		
		locationAvailable = true;
		
	}

	/**
	 * Check if we have a barometer. Use info to disable menu items, choose to
	 * run the service or not, etc.
	 */
	private boolean checkBarometer() {
		PackageManager packageManager = this.getPackageManager();
		hasBarometer = packageManager
				.hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER);
		return hasBarometer;
	}

	/**
	 * Check if we have a thermometer.
	 */
	private boolean checkThermometer() {
		SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		Sensor temp = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
		if(temp==null) {
			hasThermometer = false;
		} else {
			hasThermometer = true;
		}
		return hasThermometer;
	}
	
	/**
	 * Check if we have a hygrometer.
	 */
	private boolean checkHygrometer() {
		SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		Sensor humid = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
		if(humid==null) {
			hasHygrometer = false;
		} else {
			hasHygrometer = true;
		}
		return hasHygrometer;
	}


	
	/**
	 * Check if we're online
	 */
	private void checkNetwork() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if (activeNetwork != null) {
			isConnected = activeNetwork.isConnectedOrConnecting();
		} else {
			isConnected = false;
		}
	}

	/**
	 * Alert the user if PressureNet is offline
	 */
	private void displayNetworkOfflineToast() {
		if (!isConnected) {
			Toast.makeText(getApplicationContext(),
					getString(R.string.connectionErrorMsg),
					Toast.LENGTH_LONG).show();
		}
	}


	/**
	 * Get fresh conditions data for the global map
	 */
	private void makeGlobalConditionsMapCall() {
		long now = System.currentTimeMillis();
		long acceptableLocationTimeDifference = 1000 * 30;
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		// when was the last search locations API call?
		lastGlobalConditionsApiCall = sharedPreferences.getLong(
				"lastGlobalConditionsApiCall", now - (1000 * 60 * 2));
		if (now - lastGlobalConditionsApiCall > acceptableLocationTimeDifference) {
			log("making global conditions call");
			CbApiCall globalMapCall = new CbApiCall();
			globalMapCall.setMinLat(-90);
			globalMapCall.setMaxLat(90);
			globalMapCall.setMinLon(-180);
			globalMapCall.setMaxLon(180);
			globalMapCall.setLimit(1000);
			globalMapCall.setStartTime(System.currentTimeMillis()
					- (int) (1000 * 60 * 60 * 3));
			globalMapCall.setEndTime(System.currentTimeMillis());
			makeCurrentConditionsAPICall(globalMapCall);

			// Save the time in prefs
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putLong("lastGlobalConditionsApiCall",
					lastGlobalConditionsApiCall);
			editor.commit();
		} else {
			log("not making conditions global call, too soon");
		}
	}
	
	@SuppressWarnings("deprecation")
	public class ConditionsDownloader extends AsyncTask<String, String, String> {

		Messenger replyToApp = null;
		private CbApiCall apiCall;

		public CbApiCall getApiCall() {
			return apiCall;
		}

		public void setApiCall(CbApiCall apiCall) {
			this.apiCall = apiCall;
		}

		public Messenger getReplyToApp() {
			return replyToApp;
		}

		public void setReplyToApp(Messenger replyToApp) {
			this.replyToApp = replyToApp;
		}

		@Override
		protected String doInBackground(String... params) {
			String responseText = "";
			try {
				DefaultHttpClient client = new DefaultHttpClient();
				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				nvps.add(new BasicNameValuePair("min_latitude", apiCall
						.getMinLat() + "" + ""));
				nvps.add(new BasicNameValuePair("max_latitude", apiCall
						.getMaxLat() + "" + ""));
				nvps.add(new BasicNameValuePair("min_longitude", apiCall
						.getMinLon() + "" + ""));
				nvps.add(new BasicNameValuePair("max_longitude", apiCall
						.getMaxLon() + "" + ""));
				
				nvps.add(new BasicNameValuePair("api_key", apiCall.getApiKey()));
				nvps.add(new BasicNameValuePair("format", apiCall.getFormat()));
				nvps.add(new BasicNameValuePair("limit", apiCall.getLimit()
						+ ""));
				nvps.add(new BasicNameValuePair("global", apiCall.isGlobal()
						+ ""));
				nvps.add(new BasicNameValuePair("since_last_call", apiCall
						.isSinceLastCall() + ""));
				nvps.add(new BasicNameValuePair("sdk_version", CbConfiguration.SDK_VERSION));
				nvps.add(new BasicNameValuePair("source", "pressurenet"));

				

				String serverURL = CbConfiguration.SERVER_URL_CONDITIONS_QUERY + "?";

				
				nvps.add(new BasicNameValuePair("start_time", (apiCall
						.getStartTime() / 1000 )+ ""));
				nvps.add(new BasicNameValuePair("end_time",(apiCall
						.getEndTime() / 1000 )+ ""));

				
				String paramString = URLEncodedUtils.format(nvps, "utf-8");

				serverURL = serverURL + paramString;
				apiCall.setCallURL(serverURL);
				log("cbservice api sending " + serverURL);
				HttpGet get = new HttpGet(serverURL);
				// Execute the GET call and obtain the response
				HttpResponse getResponse = client.execute(get);
				HttpEntity responseEntity = getResponse.getEntity()	;
				log("response " + responseEntity.getContentLength());

				BufferedReader r = new BufferedReader(new InputStreamReader(
						responseEntity.getContent()));

				StringBuilder total = new StringBuilder();
				String line;
				if (r != null) {
					while ((line = r.readLine()) != null) {
						total.append(line);
					}
					responseText = total.toString();
				}
			} catch (Exception e) {
				// System.out.println("api error");
				//e.printStackTrace();
			}
			//log(responseText);
			
			globalConditionRecents = processJSONConditions(responseText);
			conditionsAdder = new ConditionsAdder();
			conditionsAdder.execute("");
			return responseText;
		}

		protected void onPostExecute(String result) {
			
		}

		
		private void log(String text) {
			if(PressureNETConfiguration.DEBUG_MODE) {
				System.out.println(text);
			}
		}

	}

	
	/**
	 * Take a JSON string and return the data in a useful structure
	 * 
	 * @param resultJSON
	 */
	private ArrayList<CbCurrentCondition> processJSONConditions(String resultJSON) {
		ArrayList<CbCurrentCondition> obsFromJSON = new ArrayList<CbCurrentCondition>();
		try {
			JSONArray jsonArray = new JSONArray(resultJSON);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
			
				//log("json condition " + jsonObject.toString());
				CbCurrentCondition current = new CbCurrentCondition();
				Location location = new Location("network");
				location.setLatitude(jsonObject.getDouble("latitude"));
				location.setLongitude(jsonObject.getDouble("longitude"));
				current.setLocation(location);
				current.setGeneral_condition(jsonObject
						.getString("general_condition"));
				current.setTime(jsonObject.getLong("daterecorded"));
				current.setTzoffset(jsonObject.getInt("tzoffset"));
				// current.setSharing_policy(jsonObject.getString("sharing"));
				// current.setUser_id(jsonObject.getString("user_id"));
				if(jsonObject.has("cloud_type")) {
					current.setCloud_type(jsonObject.getString("cloud_type"));
				}
				current.setWindy(jsonObject.getString("windy"));
				current.setFog_thickness(jsonObject
						.getString("fog_thickness"));
				current.setPrecipitation_type(jsonObject
						.getString("precipitation_type"));
				current.setPrecipitation_amount(jsonObject
						.getDouble("precipitation_amount"));
				current.setPrecipitation_unit(jsonObject
						.getString("precipitation_unit"));
				current.setThunderstorm_intensity(jsonObject
						.getString("thunderstorm_intensity"));
				current.setUser_comment(jsonObject.getString("user_comment"));
				//log("condition from API: \n" + current);
				obsFromJSON.add(current);

		
			}

		} catch (Exception e) {
			//e.printStackTrace();
		}
		return obsFromJSON;
	}
	
	/**
	 * Run map setup, update UI accordingly
	 */
	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the
		// map.
		if (mMap == null) {
			mMap = ((MapFragment) getFragmentManager().findFragmentById(
					R.id.map)).getMap();

			if (mMap != null) {

				mMap.getUiSettings().setZoomControlsEnabled(false);
				mMap.getUiSettings().setCompassEnabled(false);
				
				//clusterManager = new ClusterManager<MapItem>(this, mMap);
			//	mMap.setOnCameraChangeListener(clusterManager);
		    
				mMap.setOnCameraChangeListener(new OnCameraChangeListener() {
					
					@Override
					public void onCameraChange(CameraPosition cameraPos) {
						hideKeyboard();
						
						
						// if the user location is off the map, change the Current Conditions icon to the 
						// Go to My Location icon
						
						Location userLocation;
						
						if(bestLocation != null) {
							userLocation = bestLocation;

						} else {
							LocationManager lm = (LocationManager) getApplicationContext()
									.getSystemService(Context.LOCATION_SERVICE);
							Location loc = lm
									.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
							userLocation = loc; 
						}
						LatLngBounds bounds = mMap.getProjection()
								.getVisibleRegion().latLngBounds;
						visibleBound = bounds;
						
						double minLat;
						double maxLat;
						double minLon;
						double maxLon;
						
						if (visibleBound != null) {
							LatLng ne = visibleBound.northeast;
							LatLng sw = visibleBound.southwest;
							minLat = sw.latitude;
							maxLat = ne.latitude;
							minLon = sw.longitude;
							maxLon = ne.longitude;
						
							if( (userLocation.getLatitude() < minLat) || 
									(userLocation.getLatitude() > maxLat) ||
									(userLocation.getLongitude() < minLon) || 
									(userLocation.getLongitude() > maxLon)) {
									buttonMyLocation.setImageDrawable(getResources().getDrawable(R.drawable.ic_location_found));
									locationButtonMode = "locations";	
							} else {
								if(mMap.getCameraPosition().zoom >= DEFAULT_ZOOM) {
									buttonMyLocation.setImageDrawable(getResources().getDrawable(R.drawable.ic_current_map));
									locationButtonMode = "conditions";
								} else {
									buttonMyLocation.setImageDrawable(getResources().getDrawable(R.drawable.ic_location_found));
									locationButtonMode = "locations";	
								}
							}
							
							
						} 
						
						refreshMap();
						
					}
				});
		        
				mMap.setOnMarkerClickListener(new OnMarkerClickListener() {
					
					@Override
					public boolean onMarkerClick(Marker marker) {
						marker.showInfoWindow();
						return true;
					}
				});
				
				
				if(isWithinSupportedGeography()) {
					hideEmailNotify();
					downloadLocalData();
				} else {
					// show message that we don't support the region
					
					// only on first launch
					
					SharedPreferences sharedPreferences = PreferenceManager
							.getDefaultSharedPreferences(this);
					boolean firstLaunch = sharedPreferences.getBoolean("firstLaunch", true);
					
					if (firstLaunch) {
						showEmailNotify();
					} else {
						hideEmailNotify();
						downloadLocalData();
					}
					SharedPreferences.Editor editor = sharedPreferences.edit();
					editor.putBoolean("firstLaunch", false);
					editor.commit();
					
					
				}
				
				
			} else {
				Toast.makeText(getApplicationContext(), getString(R.string.mapError),
						Toast.LENGTH_SHORT).show();
			}

		}
		
	}	
	
	
	private void showEmailNotify() {
		layoutNoConditionsPrompt = (LinearLayout) findViewById(R.id.layoutNoConditionsPrompt);
		buttonMyLocation = (ImageButton) findViewById(R.id.buttonMyLocation);
		imageButtonPlay = (ImageButton) findViewById(R.id.imageButtonPlay);
		animationProgress = (SeekBar) findViewById(R.id.animationProgress);
		RelativeLayout layoutAnimationHoriz = (RelativeLayout) findViewById(R.id.layoutAnimationHoriz);
		
		layoutAnimationHoriz.setVisibility(View.GONE);
		
		layoutNoConditionsPrompt.setVisibility(View.VISIBLE);
		buttonMyLocation.setVisibility(View.GONE);
	}
	
	private void hideEmailNotify() {
		buttonMyLocation = (ImageButton) findViewById(R.id.buttonMyLocation);
		buttonMyLocation.setVisibility(View.VISIBLE);
		RelativeLayout layoutAnimationHoriz = (RelativeLayout) findViewById(R.id.layoutAnimationHoriz);
		layoutAnimationHoriz.setVisibility(View.VISIBLE);
		
		layoutNoConditionsPrompt = (LinearLayout) findViewById(R.id.layoutNoConditionsPrompt);
		layoutNoConditionsPrompt.setVisibility(View.GONE);
	}
	
	private void downloadLocalData() {
		downloadTemperatureData(2);
		downloadAndShowConditions();
	}

	/**
	 * Register with the server for push notifications
	 */
	@SuppressWarnings("unchecked")
	private void sendEmailRegistrationToServer(final String id, final String email) {
		
		log("cbservice sending token to server : " + email);
		
		 new AsyncTask(){
	            protected Object doInBackground(final Object... params) {
	            
	                String token;
	                try {
	                	String serverURL = CbConfiguration.SERVER_URL_EMAIL_REGISTRATION;
	                	String userID = getID();
	                	DefaultHttpClient client = new DefaultHttpClient();
	                	
	                	
	                	
	                	JSONObject object = new JSONObject();
	                	JSONObject data = new JSONObject();
	                	JSONArray jsonArray = new JSONArray();
	        			 //nvps.add(new BasicNameValuePair("device_id", id));
	        			 
	                	
	        			 object.put("email", email);
	        			 
	        			 Location userLocation;
        	 			 if(bestLocation != null) {
        			 		userLocation = bestLocation;

        				 } else {
        					LocationManager lm = (LocationManager) getApplicationContext()
        							.getSystemService(Context.LOCATION_SERVICE);
        					Location loc = lm
        							.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        					userLocation = loc; 
        				 }
        				
        				double latitude = userLocation.getLatitude();
        				double longitude = userLocation.getLongitude();
        				
        			 
        				object.put("latitude", latitude );
        				object.put("longitude", longitude );
        				
        				try {
        					
        					jsonArray.put(object);
        					data.put("data", jsonArray);
        				}catch(JSONException jsone) {
        					log("json error " + jsone.getMessage());
        				}
        				
	        			 HttpPost httppost = new HttpPost(serverURL);
	        			 
	        			 String message;
	    				 
	     				try {
	     			        message = object.toString();
	     				  
	     				  httppost.setEntity(new StringEntity(message, "UTF8"));
	     				  httppost.setHeader("Content-type", "application/json");
	     				  httppost.addHeader("Accept","application/json");
	     				} catch(Exception e) {
	     					
	     				}
	        			 
	        		
	     				log("POST Email: " + EntityUtils.toString(httppost.getEntity()));
	     				
	    				
	    				HttpResponse resp = client.execute(httppost);
	    				HttpEntity responseEntity = resp.getEntity();
	    	
	    				String addResp = "";
	    				BufferedReader r = new BufferedReader(new InputStreamReader(
	    						responseEntity.getContent()));
	    	
	    				StringBuilder total = new StringBuilder();
	    				String line;
	    				if (r != null) {
	    					while ((line = r.readLine()) != null) {
	    						total.append(line);
	    					}
	    					addResp = total.toString();
	    					
	    				}
	    				log("email notify registration sent to server " + addResp);
	                } 
	                catch (Exception e) {
	                    log("app email registration error " + e.getMessage());
	                }
	                return true;
	            }
	        }.execute(null, null, null);
	}
	
	
	private void sendEmailRegistration() {
		String id = getID();
		String email = editTextEmail.getText().toString();
		
		
		sendEmailRegistrationToServer(id, email);
		
		
	}
	
	private boolean isWithinSupportedGeography() {
		Location userLocation;
		
		if(bestLocation != null) {
			userLocation = bestLocation;

		} else {
			LocationManager lm = (LocationManager) getApplicationContext()
					.getSystemService(Context.LOCATION_SERVICE);
			Location loc = lm
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			userLocation = loc; 
		}
		
		if( (userLocation.getLatitude() > minSupportedLatitude) &&
				(userLocation.getLatitude() < maxSupportedLatitude) &&
				(userLocation.getLongitude() > minSupportedLongitude) &&
				(userLocation.getLongitude() < maxSupportedLongitude)) {
			return true;
		} else {
			return false;
		}
	}
	
	
	public class MapRefresher implements Runnable {

		@Override
		public void run() {
			if(!temperatureAnimationPlaying) {
				forecastRecents.clear();
				temperatureAnimationMarkerOptions.clear();
				liveMapForecasts.clear();
				
				addTemperaturesToMap();
				addLiveMarkersToMap();
					
			}
		}
		
	}
	
	private void refreshMap() {
		long now = System.currentTimeMillis();
		long waitBeforeRefresh = 200; 
		
		if(now - lastMapRefresh > waitBeforeRefresh) {
			Handler handler = new Handler();
			lastMapRefresh = System.currentTimeMillis();
			MapRefresher refresh = new MapRefresher();
			
			handler.postDelayed(refresh, 200);
			
		}
		
	}
	
	private void addLiveMarkersToMap() {
		for (MarkerOptions markerOptions : liveMarkerOptions) {
			mMap.addMarker(markerOptions);
		}
	}
	
	private void downloadAndShowConditions() {
		// go get the conditions and add them to the map
		log("app starting download of conditions");

		ConditionsDownloader conditionsDownloader = new ConditionsDownloader();
		CbApiCall globalMapCall = composeConditionsGlobalApiCall();
		conditionsDownloader.setApiCall(globalMapCall);
		conditionsDownloader.execute("");
	}
	
	private CbApiCall composeConditionsGlobalApiCall () {
		CbApiCall globalMapCall = new CbApiCall();
		globalMapCall.setMinLat(-90);
		globalMapCall.setMaxLat(90);
		globalMapCall.setMinLon(-180);
		globalMapCall.setMaxLon(180);
		globalMapCall.setLimit(1000);
		globalMapCall.setStartTime(System.currentTimeMillis()
				- (int) (1000 * 60 * 60));
		globalMapCall.setEndTime(System.currentTimeMillis());
		return globalMapCall;
	}
	
	/**
	 * Zoom into the user's location
	 */
	public void setUpMap() {
		setUpMapIfNeeded();

		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
				.getMap();

		// Set default coordinates (centered around the user's location)
		appStartGoToMyLocation();
	}
	
	/**
	 * Use the recent network location to go to the user's location on the map
	 */
	public void appStartGoToMyLocation() {
		try {
			if(bestLocation != null) {
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(bestLocation.getLatitude(), bestLocation.getLongitude()), DEFAULT_ZOOM));
			} else {
				LocationManager lm = (LocationManager) this
						.getSystemService(Context.LOCATION_SERVICE);
				Location loc = lm
						.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				if (loc.getLatitude() != 0) {
					mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), DEFAULT_ZOOM));
				} 
			}

		} catch (Exception e) {

		}
		buttonMyLocation.setImageDrawable(getResources().getDrawable(R.drawable.ic_current_map));
		locationButtonMode = "conditions";
	}


	/**
	 * Use the recent network location to go to the user's location on the map
	 */
	public void goToMyLocation() {
		try {
			if(bestLocation != null) {
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(bestLocation.getLatitude(), bestLocation.getLongitude()), DEFAULT_ZOOM));
			} else {
				LocationManager lm = (LocationManager) this
						.getSystemService(Context.LOCATION_SERVICE);
				Location loc = lm
						.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				if (loc.getLatitude() != 0) {
					mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(bestLocation.getLatitude(), bestLocation.getLongitude()), DEFAULT_ZOOM));
				} 
			}

		} catch (Exception e) {

		}
		buttonMyLocation.setImageDrawable(getResources().getDrawable(R.drawable.ic_current_map));
		locationButtonMode = "conditions";
	}

	/**
	 * Move and animate the map to a new location
	 * 
	 * @param latitude
	 * @param longitude
	 */
	private void moveMapTo(double latitude, double longitude) {
		setUpMapIfNeeded();

		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
				.getMap();
		try {
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
					latitude, longitude), DEFAULT_ZOOM));
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	/**
	 * Check the Android SharedPreferences for important values. Save relevant
	 * ones to CbSettings for easy access in submitting readings
	 */
	private void getStoredPreferences() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		preferencePressureUnit = sharedPreferences.getString("units",
				"millibars");
		preferenceDistanceUnit = sharedPreferences.getString("distance_units", "Kilometers (km)");
		preferenceMSLP = sharedPreferences.getBoolean("mslp", false);
		preferenceTemperatureUnit = sharedPreferences.getString(
				"temperature_units", "Celsius (°C)");
		preferenceCollectionFrequency = sharedPreferences.getString(
				"autofrequency", "10 minutes");
		preferenceShareData = sharedPreferences.getBoolean("autoupdate", true);
		preferenceShareLevel = sharedPreferences.getString(
				"sharing_preference", "Us, Researchers and Forecasters");
		preferenceSendNotifications = sharedPreferences.getBoolean(
				"send_notifications", false);
		preferenceUseGPS = sharedPreferences.getBoolean("use_gps", false);
		preferenceWhenCharging = sharedPreferences.getBoolean(
				"only_when_charging", false);

		CbSettingsHandler settings = new CbSettingsHandler(
				getApplicationContext());
		settings.setAppID(getPackageName());
		settings.setSharingData(preferenceShareData);
		settings.setDataCollectionFrequency(CbService
				.stringTimeToLongHack(preferenceCollectionFrequency));
		settings.setShareLevel(preferenceShareLevel);
		settings.setSendNotifications(preferenceSendNotifications);
		settings.setUseGPS(preferenceUseGPS);
		settings.setOnlyWhenCharging(preferenceWhenCharging);
		settings.setServerURL(CbConfiguration.SERVER_URL_PRESSURENET);
		settings.saveSettings();
		activeSettings = settings;
		sendNewSettings();
		log("app saved new settings (getStoredPreferences):" + settings);
	}

	/**
	 * Check the Android SharedPreferences for important values. Save relevant
	 * ones to CbSettings for easy access in submitting readings
	 */
	public String getUnitPreference() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		return sharedPreferences.getString("units", "millibars");
	}

	/**
	 * Check the Android SharedPreferences for important values. Save relevant
	 * ones to CbSettings for easy access in submitting readings
	 */
	private String getTempUnitPreference() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		return sharedPreferences
				.getString("temperature_units", "Celsius (°C)");
	}

	
	/**
	 * Round the api call location values to improve performance (caching)
	 * 
	 * @param rawApi
	 * @return
	 */
	private CbApiCall roundApiCallLocations(CbApiCall rawApi) {
		double newMinLat = Math.floor(rawApi.getMinLat() * 10) / 10;
		double newMaxLat = Math.floor(rawApi.getMaxLat() * 10) / 10;
		double newMinLon = Math.floor(rawApi.getMinLon() * 10) / 10;
		double newMaxLon = Math.floor(rawApi.getMaxLon() * 10) / 10;

		if (newMaxLat == newMinLat) {
			newMaxLat += .1;
		}
		if (newMaxLon == newMinLon) {
			newMaxLon += .1;
		}

		rawApi.setMinLat(newMinLat);
		rawApi.setMaxLat(newMaxLat);
		rawApi.setMinLon(newMinLon);
		rawApi.setMaxLon(newMaxLon);

		return rawApi;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	/**
	 * Attach listeners to UI elements
	 */
	private void setUpUIListeners() {
		Context context = getApplicationContext();
		mInflater = LayoutInflater.from(context);
		
		progressAPI = (ProgressBar) findViewById(R.id.progressBarAPICalls);

		buttonGoLocation = (ImageButton) findViewById(R.id.buttonGoLocation);
		editLocation = (EditText) findViewById(R.id.editGoLocation);

		
		buttonSearchLocations = (ImageButton) findViewById(R.id.buttonSearchLocations);
		buttonMyLocation = (ImageButton) findViewById(R.id.buttonMyLocation);

		imageButtonPlay = (ImageButton) findViewById(R.id.imageButtonPlay);
		animationProgress = (SeekBar) findViewById(R.id.animationProgress);
		
		textAnimationInfoLeft = (TextView) findViewById(R.id.textAnimationInfoLeft);
		textAnimationInfoRight = (TextView) findViewById(R.id.textAnimationInfoRight);
	
		layoutNoConditionsPrompt = (LinearLayout) findViewById(R.id.layoutNoConditionsPrompt);
		layoutNoConditionsThanks = (LinearLayout) findViewById(R.id.layoutNoConditionsThanks);

		buttonNotifyMe = (Button) findViewById(R.id.buttonNotifyMe);
		buttonCloseNoConditions = (ImageButton) findViewById(R.id.buttonCloseNoConditionsPrompt2);
		
		inviteFriends3 = (Button) findViewById(R.id.inviteFriends3);
		
		buttonCloseNoConditionsPrompt = (ImageButton) findViewById(R.id.buttonCloseNoConditionsPrompt);
		
		editTextEmail = (EditText) findViewById(R.id.editTextEmail);
		
		animationProgress.setEnabled(true);
	
		buttonCloseNoConditionsPrompt.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				closeNoConditionsAndLoadData();
				
			}
		});
		
		inviteFriends3.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				growPressureNET();
				closeNoConditionsAndLoadData();
			}
		});
		
		buttonCloseNoConditions.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				closeNoConditionsAndLoadData();
			}
		});
		
		buttonNotifyMe.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				layoutNoConditionsPrompt.setVisibility(View.GONE);
				layoutNoConditionsThanks.setVisibility(View.VISIBLE);
				
				if(editTextEmail.getText().length()<2) {
					displayMapToast("Please enter an email address");
					return;
				} 
				
				hideKeyboard();
				sendEmailRegistration();
			}
		});
		
		animationProgress.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
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
						//animator.showSpecificFrame(progress);
						temperatureAnimator.showTemperatureFrame(progress);
					}
			}
		});
		
		imageButtonPlay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				
				if (!temperatureAnimationPlaying) {
					log("play button pressed, not animating, starting temperature animation");
					if(prepareTemperatureAnimation()) {
						playTemperatureAnimation();	
						mixpanel.track("Play Temperature Animation", null);
					} else {
						Toast.makeText(getApplicationContext(), "Animation error", Toast.LENGTH_SHORT).show();
					}
					
				} else {
					log("play button pressed; currently animating temperatures, pausing");
					temperatureAnimator.pause();
				}
			
			}
			
		});

		buttonMyLocation.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if(locationButtonMode.equals("conditions")) {
					launchCurrentConditionsActivity();	
				} else {
					if(locationAvailable) {
						displayMapToast(getString(R.string.locatingUser));

						// Get tracker.
						Tracker t = ((PressureNetApplication) getApplication()).getTracker(
						    TrackerName.APP_TRACKER);
						// Build and send an Event.
						t.send(new HitBuilders.EventBuilder()
						    .setCategory(BarometerNetworkActivity.GA_CATEGORY_MAIN_APP)
						    .setAction(BarometerNetworkActivity.GA_ACTION_BUTTON)
						    .setLabel("my_location")
						    .build());
						
						
						goToMyLocation(); 
					} else {
						displayMapToast(getString(R.string.locationServicesError));
					}	
				}
			}
		});
		
		
		editLocation.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View arg0, boolean hasFocus) {
				log("editLocation focus change, has focus " + hasFocus);
				if (hasFocus) {
					editLocation.setCursorVisible(true);
				} else {
					editLocation.setCursorVisible(false);
				}

			}
		});

		buttonSearchLocations.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(),
						SearchLocationsActivity.class);
				startActivityForResult(intent, REQUEST_LOCATION_CHOICE);

			}
		});


		editLocation.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
			
				// Get tracker.
				Tracker t = ((PressureNetApplication) getApplication()).getTracker(
				    TrackerName.APP_TRACKER);
				// Build and send an Event.
				t.send(new HitBuilders.EventBuilder()
				    .setCategory(BarometerNetworkActivity.GA_CATEGORY_MAIN_APP)
				    .setAction(BarometerNetworkActivity.GA_ACTION_BUTTON)
				    .setLabel("search_location_focus")
				    .build());
				
				
				
				editLocation.setText("");
				editLocation.setCursorVisible(true);
			}
		});

		editLocation.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					buttonGoLocation.performClick();
					
					// Get tracker.
					Tracker t = ((PressureNetApplication) getApplication()).getTracker(
					    TrackerName.APP_TRACKER);
					// Build and send an Event.
					t.send(new HitBuilders.EventBuilder()
					    .setCategory(BarometerNetworkActivity.GA_CATEGORY_MAIN_APP)
					    .setAction(BarometerNetworkActivity.GA_ACTION_SEARCH)
					    .setLabel(editLocation.getEditableText().toString())
					    .build());
					
					
				}
				return false;
			}
		});

		buttonGoLocation.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				String location = editLocation.getText().toString().trim();
				if (location.equals("")) {
					displayMapToast(getString(R.string.locationPrompt));
					focusSearch();
					return;
				}
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(editLocation.getWindowToken(), 0);
				editLocation.setCursorVisible(false);
				Geocoder geocode = new Geocoder(getApplicationContext());
				try {
					List<Address> addr = geocode.getFromLocationName(location,
							2);
					if (addr.size() > 0) {
						displayMapToast(getString(R.string.goingTo) + " " + location);
						
						Address ad = addr.get(0);
						double latitude = ad.getLatitude();
						double longitude = ad.getLongitude();
						moveMapTo(latitude, longitude);

						SearchLocation loc = new SearchLocation(location,
								latitude, longitude);
						searchedLocations.add(loc);
						
						// Get tracker.
						Tracker t = ((PressureNetApplication) getApplication()).getTracker(
						    TrackerName.APP_TRACKER);
						// Build and send an Event.
						t.send(new HitBuilders.EventBuilder()
						    .setCategory(BarometerNetworkActivity.GA_CATEGORY_MAIN_APP)
						    .setAction(BarometerNetworkActivity.GA_ACTION_SEARCH)
						    .setLabel(location)
						    .build());
						
						PnDb pn = new PnDb(getApplicationContext());
						pn.open();
						pn.addLocation(location, latitude, longitude,
								System.currentTimeMillis());
						pn.close();

						CbApiCall api = buildSearchLocationAPICall(loc);
						makeAPICall(api);

						CbApiCall conditionApi = buildMapCurrentConditionsCall(2);
						makeCurrentConditionsAPICall(conditionApi);
					} else {
						displayMapToast(getString(R.string.noSearchResults));
					}

				} catch (IOException ioe) {
					displayMapToast(ioe.getMessage());
					ioe.printStackTrace();
					//displayMapToast("Unable to search Google Maps");
				}

			}
		});
	}
	
	private void closeNoConditionsAndLoadData() {
		layoutNoConditionsThanks.setVisibility(View.GONE);
		
		buttonMyLocation = (ImageButton) findViewById(R.id.buttonMyLocation);
		buttonMyLocation.setVisibility(View.VISIBLE);
		RelativeLayout layoutAnimationHoriz = (RelativeLayout) findViewById(R.id.layoutAnimationHoriz);
		layoutAnimationHoriz.setVisibility(View.VISIBLE);
		
		layoutNoConditionsPrompt = (LinearLayout) findViewById(R.id.layoutNoConditionsPrompt);
		layoutNoConditionsPrompt.setVisibility(View.GONE);
		
		downloadLocalData();
	}
	
	private void displayMapToast(String message) {
		Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.TOP|Gravity.CENTER, 0, 300);
		toast.show();
	}
	
	private void displayLongMapToast(String message) {
		Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.TOP|Gravity.CENTER, 0, 300);
		toast.show();
	}
	
	/**
	 * Tell CbService to send registration info to GCM
	 */
	private void sendRegistrationInfo() {
		if (mBound) {
			log("app directing CbService to send registration info");
			Message msg = Message.obtain(null,
					CbService.MSG_REGISTER_NOTIFICATIONS, 0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// e.printStackTrace();
			}
		} else {
			// log("error: not bound");
		}
	}

	/**
	 * Get simple counts from the database
	 * to tell the user how much they've contributed
	 */
	private void askForContributionData() {
		if (mBound) {
			log("asking for contribution data");
			Message msg = Message.obtain(null,
					CbService.MSG_GET_CONTRIBUTIONS, 0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// e.printStackTrace();
			}
		} else {
			// log("error: not bound");
		}
	}

	/**
	 * The user has requested the animation begin. Fetch the data to begin.
	 */
	private void playConditionsAnimation() {
		if (mMap == null) {
			return;
		}
		
		// If the animation isn't in-progress, start
		// at the beginning. Otherwise resume.
		if( (animationStep < 1) || (animationStep >= 99)) { 
			animationStep = 0;
			animationProgress.setProgress(0);
			animationProgress.setEnabled(true);
			conditionAnimationRecents.clear();
			animationMarkerOptions.clear();
			
			// user-specified start and end date
			if (calAnimationStartDate == null) {
				calAnimationStartDate = Calendar.getInstance();
			}

			long startTime = calAnimationStartDate.getTimeInMillis();
			long endTime = startTime + animationDurationInMillis;
			log("animation start " + startTime + ", + end " + endTime);
			long timeSpan = endTime - startTime;
			if(timeSpan > (1000 * 60 * 60 * 24)) {
				log("send toast");
				Toast.makeText(getApplicationContext(), getString(R.string.preparingAnimation), Toast.LENGTH_SHORT).show();
			} else {
				log("don't send toast");
			}
			
			
			makeCurrentConditionsAPICall(buildConditionsAnimationCall(startTime,
					endTime));
		} else {
			beginAnimationWithNewConditions(animationStep);
		}
	}

	/**
	 * The new condition data for animations has been received. Play the
	 * animation.
	 */
	private void beginAnimationWithNewConditions(int startFrame) {
		mMap.clear();
		animationStep = startFrame;
		if (conditionAnimationRecents == null) {
			animator.pause();
			return;
		}
		if (conditionAnimationRecents.size() == 0) {
			animator.pause();
			return;
		}
		animationPlaying = true;
		Collections.sort(conditionAnimationRecents,
				new CbScience.ConditionTimeComparator());

		long timeStart = conditionAnimationRecents.get(0).getTime();
		long timeEnd = conditionAnimationRecents.get(
				conditionAnimationRecents.size() - 1).getTime();
		long timeSpan = timeEnd - timeStart;
		long frameLength = timeSpan / 100;

		if (frameLength == 0) {
			log("barometernetworkactivity framelength = 0, bail on animation");
			Toast.makeText(getApplicationContext(), getString(R.string.animationErrorNoData), Toast.LENGTH_SHORT).show();
			animator.stop();
			animator.reset();
			return;
		}
		
		ConditionsDrawables draws = new ConditionsDrawables(getApplicationContext());
		
		for (CbCurrentCondition condition : conditionAnimationRecents) {
			long conditionTime = condition.getTime();
			long timeOffsetFromStart = conditionTime - timeStart;
			long frameNumber = timeOffsetFromStart / frameLength;
			int frame = (int) frameNumber;
			condition.setAnimateGroupNumber(frame);
			log("setting condition frame " + frame);

			LatLng point = new LatLng(condition.getLocation().getLatitude(),
					condition.getLocation().getLongitude());
			log("getting layer drawable for condition "
					+ condition.getGeneral_condition());
			
			
			LayerDrawable drLayer = draws.getCurrentConditionDrawable(condition, null);
			if (drLayer == null) {
				log("drlayer null, next!");
				
				continue;
			}
			Drawable draw = draws.getSingleDrawable(drLayer);

			Bitmap image = drawableToBitmap(draw, null);
			MarkerOptions thisMarkerOptions = new MarkerOptions().position(
					point).icon(BitmapDescriptorFactory.fromBitmap(image));

			animationMarkerOptions.add(thisMarkerOptions);
		}

		animationHandler.post(animator);

	}

	private void displayAnimationFrame(int frame) {
		animationStep = frame;
		Iterator<CbCurrentCondition> conditionIterator = conditionAnimationRecents
				.iterator();
		int num = 0;
		int e = 10;
		mMap.clear();
		while (conditionIterator.hasNext()) {
			CbCurrentCondition condition = conditionIterator.next();
			try {
				MarkerOptions markerOpts = animationMarkerOptions.get(num);
				if (Math.abs(condition.getAnimateGroupNumber() - frame) < e) {
					mMap.addMarker(markerOpts);
				}
			} catch(IndexOutOfBoundsException ioobe) {
				// 
			}
			num++;
		}
	}

	private class AnimationRunner implements Runnable {

		public void stop() {
			log("stoping animation");
			imageButtonPlay.setImageResource(R.drawable.ic_play);
			animationPlaying = false;
		}
		
		public void reset() {
			log("resetting animation");
			animationStep = 0;
			conditionAnimationRecents.clear();
			animationProgress.setProgress(animationStep);
			imageButtonPlay.setImageResource(R.drawable.ic_play);
			animationPlaying = false;
		}
		
		public void showSpecificFrame(int frameNumber) {
			pause();
			displayAnimationFrame(frameNumber);
		}
		
		public void pause() {
			animationPlaying = false;
			animationHandler.removeCallbacks(this);
			imageButtonPlay.setImageResource(R.drawable.ic_play);
		}
		
		public void run() {
			if (activeMode.equals("animation")) {
				displayAnimationFrame(animationStep);
				imageButtonPlay.setImageResource(R.drawable.ic_pause);
				animationProgress.setProgress(animationStep);
				animationStep++;
				if (animationStep < 100) {
					animationHandler.postDelayed(this, 50);
				} else {
					stop();
				}
			} else {
				stop();
				reset();
			}
		}
	}
	

	/**
	 * Initiate the CbService
	 */
	private void startCbService() {
		log("start cbservice");
		try {
			serviceIntent = new Intent(this, CbService.class);
			startService(serviceIntent);
			log("app started service");
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	/**
	 * Query the database for locally stored current conditions
	 * 
	 * @param api
	 */
	private void askForCurrentConditionRecents(CbApiCall api) {

		if (mBound) {
			log("asking for current conditions");
			Message msg = Message.obtain(null,
					CbService.MSG_GET_CURRENT_CONDITIONS, api);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// e.printStackTrace();
			}
		} else {
			// log("error: not bound");
		}
	}

	/**
	 * Query the database for locally stored current conditions
	 * 
	 * @param api
	 */
	private void askForLocalConditionRecents() {

		if (mBound) {
			log("asking for current conditions");
			CbApiCall mapApiCall = buildMapAPICall(2);
			Message msg = Message.obtain(null,
					CbService.MSG_GET_LOCAL_CONDITIONS, mapApiCall);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// e.printStackTrace();
			}
		} else {
			// log("error: not bound");
		}
	}

	/**
	 * Query the Service for best location
	 */
	private void askForBestLocation() {
		if (mBound) {
			log("app asking for best location");

			Message msg = Message.obtain(null, CbService.MSG_GET_BEST_LOCATION, 0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// e.printStackTrace();
			}
		} else {
			// log("error: not bound");
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
		if (!mBound) {
			bindService(new Intent(getApplicationContext(), CbService.class),
					mConnection, Context.BIND_AUTO_CREATE);

		}
	}

	/**
	 * Get the settings from the Cb database
	 */
	private void askForSettings() {
		if (mBound) {
			log("asking for settings");

			Message msg = Message
					.obtain(null, CbService.MSG_GET_SETTINGS, 0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// e.printStackTrace();
			}
		} else {
			// log("error: not bound");
		}
	}

	/**
	 * Handle communication with CbService. Listen for messages and act when
	 * they're received, sometimes responding with answers.
	 * 
	 * @author jacob
	 * 
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CbService.MSG_BEST_LOCATION:
				bestLocation = (Location) msg.obj;
				if (bestLocation != null) {
					log("Client Received new location from service "
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
				activeSettings.getSettings();
				if (activeSettings != null) {
					log("received msg_settings, setting activeSettings "
							+ activeSettings);
					log("Client Received from service "
							+ activeSettings.getServerURL());
				} else {
					log("settings null");
				}
				break;
			case CbService.MSG_API_RECENTS:
				log("app received MSG_API_RECENTS; useless");
				break;
			case CbService.MSG_STATS:
				log("app receiving MSG_STATS; useless");
				updateAPICount(-1);
				break;
			case CbService.MSG_API_RESULT_COUNT:
				int count = msg.arg1;
				updateAPICount(-1);
				if (activeMode.equals("map")) {

					//CbApiCall apiConditions = buildMapCurrentConditionsCall(2);
					//askForCurrentConditionRecents(apiConditions);
				} else if (activeMode.equals("animation")) {
					if(calAnimationStartDate != null) {
						long startTime = calAnimationStartDate.getTimeInMillis();
						long endTime = startTime + animationDurationInMillis;
						CbApiCall api = buildConditionsAnimationCall(startTime,
								endTime);
						askForCurrentConditionRecents(api);
					}
				}

				break;
			case CbService.MSG_CURRENT_CONDITIONS:
				ArrayList<CbCurrentCondition> receivedList = (ArrayList<CbCurrentCondition>) msg.obj;
				if (receivedList != null) {
					if (receivedList.size() > 0) {
						if (!activeMode.equals("animation")) {
							//currentConditionRecents = receivedList;
							//conditionsHandler.post(conditionsAdder);
							addConditionMarkersToMap();
						} else {
							conditionAnimationRecents.clear();
							conditionAnimationRecents = receivedList;
							beginAnimationWithNewConditions(animationStep);
						}
					} else {
						log("app received conditions size 0");
						if(activeMode.equals("animation")) {
							Toast.makeText(getApplicationContext(), getString(R.string.animationErrorNoData), Toast.LENGTH_SHORT).show();
							animator.stop();
							animator.reset();
						}
						break;
					}
					
				} else {
					log("app received null conditions");
					break;
				}

				
				break;
			case CbService.MSG_CHANGE_NOTIFICATION:
				String change = (String) msg.obj;
				// deliverNotification(change);
				break;
			case CbService.MSG_DATA_RESULT:
				// Used to be a Toast notification, now handled in NotificationSender
				break;
			case CbService.MSG_CONTRIBUTIONS:
				// Contribution UI removed
				break;
			case CbService.MSG_IS_PRIMARY:
				if (msg.arg1 == 1) {
					isPrimaryApp = true;
				} else {
					isPrimaryApp = false;
				}
				break;
			case CbService.MSG_LOCAL_CONDITIONS:
				localConditionRecents = (ArrayList<CbCurrentCondition>) msg.obj;
				log("app receiving " + localConditionRecents.size() + " local conditions");
				break;
			default:
				log("received default message");
				super.handleMessage(msg);
			}
		}
	}

	/** 
	 * Use the app preferences to tell the SDK to poll
	 * for nearby conditions or not.
	 */
	private void setNotificationDeliverySDKPreference() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		boolean pressureNetOn = sharedPreferences.getBoolean("send_condition_notifications", false);
		tellSDKToPollConditions(pressureNetOn);
	}
	
	private void tellSDKToPollConditions(boolean pollConditions) {
		if (mBound) {
			log("telling SDK about condition delivery prefs " + pollConditions);

			Message msg;
			if(pollConditions) {
				msg = Message
						.obtain(null, CbService.MSG_LOCAL_CONDITION_OPT_IN, 0, 0);
			} else {
				msg = Message
						.obtain(null, CbService.MSG_LOCAL_CONDITION_OPT_OUT, 0, 0);
			}
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// e.printStackTrace();
			}
		} else {
			// log("error: not bound");
		}
	}
	
	/**
	 * Communicate with CbService
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			log("client says : service connected");
			mService = new Messenger(service);
			mBound = true;
			Message msg = Message.obtain(null, CbService.MSG_OKAY);
			log("client received " + msg.arg1 + " " + msg.arg2);
			askForSettings();
			
			sendChangeNotification();
			getStoredPreferences();
			askForBestLocation();
						
			setNotificationDeliverySDKPreference();
			
			registerForNotifications();
			
			// Refresh the data unless we're in animation mode
			if(!activeMode.equals("animation")) {
				try {
					//mMap.clear();
					//makeGlobalConditionsMapCall();
				} catch(NullPointerException npe) {
					
				}
			}
			
			
		}

		/**
		 * Send a message with a good replyTo for the Service to send
		 * notifications through.
		 */
		private void sendChangeNotification() {
			if (mBound) {
				log("send change notif request");

				Message msg = Message.obtain(null,
						CbService.MSG_CHANGE_NOTIFICATION, 0, 0);
				try {
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// e.printStackTrace();
				}
			} else {
				// log("error: not bound");
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			log("client: service disconnected");
			mMessenger = null;
			mBound = false;
		}
	};

	/**
	 * Log session init
	 */
	private void startLog() {
		String version = "";
		PackageInfo pInfo;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = pInfo.versionName;
		} catch (NameNotFoundException e) {
		}

		log("oncreate main activity v: " + version);
	}

	/**
	 * Get a unique ID by fetching the phone ID and hashing it
	 * 
	 * @return
	 */
	private String getID() {
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
	
	/**
	 * Some devices have barometers, other's don't. Fix up the UI a bit so that
	 * most useful elements show for the right users
	 */
	private void cleanUI(Menu menu) {
		// hide some menu items that are barometer-specific
		if (!hasBarometer) {
			menu.removeItem(R.id.menu_submit_reading);
			menu.removeItem(R.id.menu_log_viewer);
		}
		if (!PressureNETConfiguration.DEBUG_MODE) {
			// hide menu item
			menu.removeItem(R.id.send_debug_log);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.settings, menu);
		
		mOptionsMenu = menu;
		setActionBarAlertIcon();
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		cleanUI(menu);
		return super.onPrepareOptionsMenu(menu);
	}

	private void launchCurrentConditionsActivity() {
		Intent intent = new Intent(getApplicationContext(),
				CurrentConditionsActivity.class);
		try {
			if (mLatitude == 0.0) {
				LocationManager lm = (LocationManager) this
						.getSystemService(Context.LOCATION_SERVICE);
				Location loc = lm
						.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				mLatitude = loc.getLatitude();
				mLongitude = loc.getLongitude();
				mAltitude = loc.getAltitude();
				mLocationAccuracy = loc.getAccuracy();
			}

			intent.putExtra("appdir", mAppDir);
			intent.putExtra("latitude", mLatitude);
			intent.putExtra("longitude", mLongitude);
			log("starting condition " + mLatitude + " , " + mLongitude);
			startActivity(intent);
			overridePendingTransition(R.anim.open_current_conditions, 0);
			mixpanel.track("Open Current Conditions", null);
		} catch (NullPointerException e) {
			Toast.makeText(getApplicationContext(),
					getString(R.string.noLocationFound),
					Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	     if (drawerToggle.onOptionsItemSelected(item)) {
	            return true;
	        }
		if (item.getItemId() == R.id.menu_settings) {
			Intent i = new Intent(this, PreferencesActivity.class);
			startActivityForResult(i, REQUEST_SETTINGS);
		} else if (item.getItemId() == R.id.menu_log_viewer) {
			viewLog();
		} else if (item.getItemId() == R.id.menu_current_conditions) {
			launchCurrentConditionsActivity();
		} else if (item.getItemId() == R.id.menu_about) {
			Intent intent = new Intent(getApplicationContext(),
					AboutActivity.class);
			startActivity(intent);

		} else if (item.getItemId() == R.id.menu_help) {
			Intent intent = new Intent(getApplicationContext(),
					HelpActivity.class);
			startActivity(intent);

		} else if (item.getItemId() == R.id.menu_submit_reading) {
			// submit a single reading
			sendSingleObservation();

			// Get tracker.
			// Get tracker.
			Tracker t = ((PressureNetApplication) getApplication()).getTracker(
			    TrackerName.APP_TRACKER);
			// Build and send an Event.
			t.send(new HitBuilders.EventBuilder()
			    .setCategory(BarometerNetworkActivity.GA_CATEGORY_MAIN_APP)
			    .setAction(BarometerNetworkActivity.GA_ACTION_BUTTON)
			    .setLabel("send_single_reading")
			    .build());
			
		} else if (item.getItemId() == R.id.menu_grow_pressurenet) {
			growPressureNET();
	

			// Get tracker.
			Tracker t = ((PressureNetApplication) getApplication()).getTracker(
			    TrackerName.APP_TRACKER);
			// Build and send an Event.
			t.send(new HitBuilders.EventBuilder()
			    .setCategory(BarometerNetworkActivity.GA_CATEGORY_MAIN_APP)
			    .setAction(BarometerNetworkActivity.GA_ACTION_BUTTON)
			    .setLabel("spread_the_word")
			    .build());
			
		} else if (item.getItemId() == R.id.menu_rate_pressurenet) {
			ratePressureNET();
			// Get tracker.
			Tracker t = ((PressureNetApplication) getApplication()).getTracker(
			    TrackerName.APP_TRACKER);
			// Build and send an Event.
			t.send(new HitBuilders.EventBuilder()
			    .setCategory(BarometerNetworkActivity.GA_CATEGORY_MAIN_APP)
			    .setAction(BarometerNetworkActivity.GA_ACTION_BUTTON)
			    .setLabel("rate_pressurenet")
			    .build());
		} else if (item.getItemId() == R.id.menu_test_forecastalerts) {
			Intent intent = new Intent(this, ForecastDetailsActivity.class);
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Open the Google Play PressureNet page
	 */
	private void ratePressureNET() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri
				.parse("market://details?id=ca.cumulonimbus.barometernetwork"));
		startActivity(intent);
	}

	/**
	 * Send a share intent to encourage network growth
	 */
	private void growPressureNET() {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent
				.putExtra(
						Intent.EXTRA_TEXT,
						getString(R.string.shareIntentText));
		sendIntent.setType("text/plain");
		startActivity(sendIntent);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_MAILED_LOG) {
			// Clear the log
			String strFile = mAppDir + "/log.txt";
			File file = new File(strFile);
			if (file.exists())
				file.delete();
		} else if (requestCode == REQUEST_LOCATION_CHOICE) {
			if (data != null) {
				
				long rowId = data.getLongExtra("location_id", -1L);
				if (rowId >= 1) {
					PnDb pn = new PnDb(getApplicationContext());
					pn.open();
					Cursor c = pn.fetchLocation(rowId);
					pn.close();
					String search = "";
					double lat = 0;
					double lon = 0;
					if (c.moveToFirst()) {
						search = c.getString(1);
						lat = c.getDouble(2);
						lon = c.getDouble(3);

						if (lat != 0) {
							editLocation.setText(search,
									TextView.BufferType.EDITABLE);
							displayMapToast(getString(R.string.goingTo) + " " + search);
							moveMapTo(lat, lon);
						}
					}
					

				} else if (rowId == -2L) {
					log("onactivityresult -2");
					displayMapToast(getString(R.string.noSavedLocations));

					focusSearch();
				} else {
					log("onactivity result " + rowId);
				}
			} else {
				log("request location data is null");
			}
		} else if (requestCode == REQUEST_DATA_CHANGED) {
			// allow for immediate call of global data
			if (resultCode == RESULT_OK) {
				lastGlobalApiCall = System.currentTimeMillis()
						- (1000 * 60 * 15);
				// Save the time in prefs
				SharedPreferences sharedPreferences = PreferenceManager
						.getDefaultSharedPreferences(this);
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putLong("lastGlobalAPICall", lastGlobalApiCall);
				editor.commit();
			}
		} else {
				log("barometernetworkactivity received data intent null:");
		}
		 
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void focusSearch() {
		editLocation.setText("");
		editLocation.setFocusableInTouchMode(true);
		if (editLocation.requestFocus()) {
			editLocation.setCursorVisible(true);
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(editLocation, 0);
		}
	}

	// Give a quick overview of recent
	// submissions
	private void viewLog() {
		try {
			Intent intent = new Intent(this, LogViewerActivity.class);
			startActivity(intent);
			overridePendingTransition(R.anim.main_to_log_viewer_open, R.anim.main_to_log_viewer_close);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	private void setUpActionBar() {
		ActionBar bar = getActionBar();
		bar.setDisplayUseLogoEnabled(true);
		bar.setTitle("");

	}

	@Override
	protected void onRestart() {
		super.onRestart();

	}

	/**
	 * Set a unique identifier so that updates from the same user are seen as
	 * updates and not new data.
	 */
	private void setId() {
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
			// e.printStackTrace();
		}
	}

	/**
	 * Prepare to write a log to SD card. Not used unless logging enabled.
	 */
	private void setUpFiles() {
		try {
			File homeDirectory = getExternalFilesDir(null);
			if (homeDirectory != null) {
				mAppDir = homeDirectory.getAbsolutePath();

			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	public class MapWindowAdapter implements GoogleMap.InfoWindowAdapter {
		private Context context = null;

		public MapWindowAdapter(Context context) {
			this.context = context;
		}

		// Hack to prevent info window from displaying: use a 0dp/0dp frame
		@Override
		public View getInfoWindow(Marker marker) {
			View v = ((Activity) context).getLayoutInflater().inflate(
					R.layout.info_window, null);
			return v;
		}

		@Override
		public View getInfoContents(Marker marker) {
			return null;
		}
	}

	private Bitmap drawableToBitmap(Drawable drawable, CbObservation obs) {
		/*
		 * if (drawable instanceof BitmapDrawable) { return ((BitmapDrawable)
		 * drawable).getBitmap(); }
		 */
		if (obs == null) {
			return ((BitmapDrawable) drawable).getBitmap();
		}

		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight(), Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight());

		String display = displayPressureValue(obs.getObservationValue(), obs.getLocation().getAltitude());
		if (display.contains(" ")) {
			display = display.split(" ")[0];
		}

		int x = 0;
		int y = canvas.getHeight();

		// Use the screen data to determine text size
		int defaultTextSize = 16;
		int defaultTextXOffset = 48;
		int defaultTextYOffset = 30;

		int textSize = defaultTextSize;
		int textXOffset = defaultTextXOffset;
		int textYOffset = defaultTextYOffset;

		// Check screen metrics
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		switch (metrics.densityDpi) {
		case DisplayMetrics.DENSITY_LOW:
			textSize = defaultTextSize - 10;
			textXOffset = defaultTextXOffset - 30;
			textYOffset = defaultTextYOffset - 25;
			break;
		case DisplayMetrics.DENSITY_MEDIUM:
			textSize = defaultTextSize - 8;
			textXOffset = defaultTextXOffset - 24;
			textYOffset = defaultTextYOffset - 15;
			break;
		case DisplayMetrics.DENSITY_HIGH:
			textSize = defaultTextSize - 4;
			textXOffset = defaultTextXOffset - 12;
			textYOffset = defaultTextYOffset - 8;

			break;
		case DisplayMetrics.DENSITY_XHIGH:
			textSize = defaultTextSize + 2;
			textXOffset = defaultTextXOffset + 3;
			textYOffset = defaultTextYOffset;
			break;
		case DisplayMetrics.DENSITY_XXHIGH:
			textSize = defaultTextSize + 16;
			textXOffset = defaultTextXOffset + 22;
			textYOffset = defaultTextYOffset + 14;
			break;
		default:
			break;

		}

		// Paint
		Paint paint = new Paint();
		paint.setTextAlign(Paint.Align.LEFT);
		paint.setTextSize(textSize);
		paint.setShadowLayer(15, 5, 5, 0);
		paint.setAntiAlias(true);
		paint.setColor(Color.BLACK);
		paint.setFlags(Paint.ANTI_ALIAS_FLAG);

		int xMax = canvas.getWidth();
		int yMax = canvas.getHeight();

		drawable.draw(canvas);

		canvas.drawText(display, textXOffset, textYOffset, paint);

		return bitmap;
	}

	/**
	 * The new condition data for animations has been received. Play the
	 * animation.
	 */
	private void beginTemperatureAnimation() {
		log("app starting temperature animation with " + forecastRecents.size() + " total items");
		
		

		animationHandler.post(temperatureAnimator);

	}
	
	private void prepareTemperatureAnimationMarkers(int startFrame) {
		temperatureAnimationStep = startFrame;
		temperatureAnimationMarkerOptions.clear();
		
		IconGenerator iconFactory = new IconGenerator(getApplicationContext());

		iconFactory.setBackground(null);
		// iconFactory.setColor(Color.rgb(230, 230, 230));
		iconFactory.setStyle(R.style.MapTemperatureBackground);
		iconFactory.setTextAppearance(R.style.MapTemperatureText);
				
		
		TemperatureForecast previous = forecastRecents.get(0);
		for (TemperatureForecast forecast : forecastRecents) {
			LatLng position = new LatLng(forecast.getLatitude(), forecast.getLongitude());

			String displayValue = forecast.getDisplayTempValue();
			
			if(previous.getTemperatureValue() < forecast.getTemperatureValue()) {
				displayValue += " ↑";
			} else if(previous.getTemperatureValue() > forecast.getTemperatureValue()) {
				displayValue += " ↓";
			} else {	
				displayValue += "  ";
				
			}
			
	        MarkerOptions markerOptions = new MarkerOptions().
	                icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(displayValue))).
	                position(position).
	                anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());

	        temperatureAnimationMarkerOptions.add(markerOptions);
	        previous = forecast;
		}
		log("added markers to list, count " + temperatureAnimationMarkerOptions.size());
	}
	
	private boolean prepareTemperatureAnimation() {
		if (mMap == null) {
			return false;
		}
		forecastRecents.clear();
		temperatureAnimationMarkerOptions.clear();
		
		if(liveMapForecasts.size()<1) {
			log("app attempting to play temperature animation; size 0, bailing");
			noAnimationResults();
			return false;
		} else {
			animationProgress.setEnabled(true);
			imageButtonPlay.setEnabled(true);
			try {
				imageButtonPlay.setImageAlpha(255);	
			} catch (NoSuchMethodError nsme) {
				log("app could not set image alpha");
			} catch(RuntimeException re) {
				log("my location button error");
			} catch (Exception e) {
				log("unknown imagealphaerror " + e.getMessage());
			}
		}
		
		PnDb db = new PnDb(getApplicationContext());
		db.open();
		for(ForecastLocation location : liveMapForecasts) {
			Cursor forecastCursor = db.getTemperatureForecastsById(location.getLocationID());
			ArrayList<TemperatureForecast> currentLocationTemperatures = new ArrayList<TemperatureForecast>();
			while(forecastCursor.moveToNext()) {
				// id = 1
				String mapStartTime = forecastCursor.getString(2);
				int mapHour = forecastCursor.getInt(3);
				int mapScale = forecastCursor.getInt(4);
				double mapValue = forecastCursor.getDouble(5);
				TemperatureForecast forecast = new TemperatureForecast(location.getLocationID(), mapScale, mapValue, mapStartTime, mapHour);
				forecast.setLatitude(location.getLatitude());
				forecast.setLongitude(location.getLongitude());
				forecast.setContext(getApplicationContext());
				forecast.prepareDisplayValue();
				currentLocationTemperatures.add(forecast);
				// log("forecast temp details " + mapValue + " " + mapStartTime + " " + mapHour);
				forecastRecents.add(forecast);
			}
			location.setTemperatures(currentLocationTemperatures);
			log("location " + location.getLocationID() + " has " + location.getTemperatures().size() + " temperature forecasts");
		}
		
		if(liveMapForecasts.size()>0){
			if(liveMapForecasts.get(0).getTemperatures().size()>0) {
				
				
				
				int animationLength = liveMapForecasts.get(0).getTemperatures().size() - 1;
				activeForecastStartTime = liveMapForecasts.get(0).getTemperatures().get(0).getStartTime();
				animationProgress.setMax(animationLength);
				updateAnimationTime("left", activeForecastStartTime, 0);
				updateAnimationTime("right", activeForecastStartTime, animationLength);
			} 
		} 
		
		db.close();
		
		prepareTemperatureAnimationMarkers(temperatureAnimationStep);
		return true;
	
	}
	
	private void noAnimationResults() {
		log("there are no forecast results");
		animationProgress.setEnabled(false);
		imageButtonPlay.setEnabled(false);
		try {
			imageButtonPlay.setImageAlpha(200);	
		} catch (NoSuchMethodError nsme) {
			log("app could not set image alpha");
		} catch(RuntimeException re) {
			log("my location button error");
		} catch (Exception e) {
			log("unknown imagealphaerror " + e.getMessage());
		}
	}
	
	private void playTemperatureAnimation() {
		
		//updateAnimationTime(liveMapForecasts.get(0).getTemperatures().get(0).getStartTime(), 0);
		temperatureAnimationStep = 0;
		animationProgress.setProgress(0);
		
		
		animationProgress.setEnabled(true);
		
		temperatureAnimationPlaying = true;
		
		mMap.clear();
		
		prepareTemperatureAnimationMarkers(temperatureAnimationStep);
		beginTemperatureAnimation();
	
	}
	
	private void updateAnimationTime(String whichText, String startTime, int hour) {
		try {
			String dateString = startTime; 
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
			df.setTimeZone(TimeZone.getTimeZone("UTC"));
			Date d = df.parse(dateString);
			
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			
			// time zone conversion
			cal.setTime(d);
			cal.add(Calendar.HOUR, hour);
			
			TimeZone local = TimeZone.getDefault();
			
			if(cal.get(Calendar.MINUTE) >= 30) {
				cal.add(Calendar.HOUR, 1);
				cal.set(Calendar.MINUTE, 0);
			} else {
				cal.set(Calendar.MINUTE, 0);
			}
			
			SimpleDateFormat display = new SimpleDateFormat("h a");
			display.setTimeZone(local);
			
			if(whichText.equals("left")) {
				textAnimationInfoLeft.setText(display.format(cal.getTime()));	
			} else if (whichText.equals("right")) {
				textAnimationInfoRight.setText(display.format(cal.getTime()));
			}
				
		} catch (java.text.ParseException e) {
			log("app parse exception " + e.getMessage());
		}
	}
	
	private void displayTemperatureFrame(int frame) {
		temperatureAnimationStep = frame;
		Iterator<TemperatureForecast> temperatureIterator = forecastRecents
				.iterator();
		int num = 0;
		mMap.clear();
		while (temperatureIterator.hasNext()) {
			TemperatureForecast forecast = temperatureIterator.next();
			try {
				MarkerOptions markerOpts = temperatureAnimationMarkerOptions.get(num);
				if (forecast.getForecastHour() == frame) {
					mMap.addMarker(markerOpts);
				}
			} catch(IndexOutOfBoundsException ioobe) {
				// 
			}
			num++;
		}
		
		//updateAnimationTime(activeForecastStartTime, frame);
		
	}
	
	private class TemperatureAnimator implements Runnable {

		public void stop() {
			log("stoping temp animation");
			imageButtonPlay.setImageResource(R.drawable.ic_play);
			temperatureAnimationPlaying = false;
		}
		
		public void reset() {
			log("resetting temp animation");
			temperatureAnimationStep = 0;
			
			animationProgress.setProgress(animationStep);
			imageButtonPlay.setImageResource(R.drawable.ic_play);
			temperatureAnimationPlaying = false;
		}
		
		public void showTemperatureFrame(int frameNumber) {
			pause();
			displayTemperatureFrame(frameNumber);
		}
		
		public void pause() {
			temperatureAnimationPlaying = false;
			animationHandler.removeCallbacks(this);
			imageButtonPlay.setImageResource(R.drawable.ic_play);
		}
		
		public void run() {
			displayTemperatureFrame(temperatureAnimationStep);
			imageButtonPlay.setImageResource(R.drawable.ic_pause);
			animationProgress.setProgress(temperatureAnimationStep);
			temperatureAnimationStep++;
			if (temperatureAnimationStep < 8) {
				animationHandler.postDelayed(this, 500);
			} else {
				stop();
			}
		}
	}
	
	
	
	/***
	 * 
	 * 
	 * 
	 * Split this up
	 * 
	 * Do the database access (and quadrant calculations?) in another thread
	 * Store the results of the marker details that we'll have to add in the UI thread
	 * In a new AsyncTask, run the database access in the background
	 * and in onPostExecute, add the data to the map
	 * 
	 */
	
	private void addTemperaturesToMap() {
		log("adding temperature icons to map");
		IconGenerator iconFactory = new IconGenerator(getApplicationContext());

		forecastRecents.clear();
		
		iconFactory.setBackground(null);
		// iconFactory.setColor(Color.rgb(230, 230, 230));
		iconFactory.setStyle(R.style.MapTemperatureBackground);
		iconFactory.setTextAppearance(R.style.MapTemperatureText);
	
		if(visibleBound == null) {
			visibleBound = mMap.getProjection()
					.getVisibleRegion().latLngBounds;
			
		}
	
		LatLng ne = visibleBound.northeast;
		LatLng sw = visibleBound.southwest;
		double minLat = sw.latitude;
		double maxLat = ne.latitude;
		double minLon = sw.longitude;
		double maxLon = ne.longitude;

		
		PnDb db = new PnDb(getApplicationContext());
		
		try {
			
			db.open();
			Cursor cursor = db.getMapTemperatures(minLat, minLon, maxLat, maxLon);
			
			
			double lat = 0;
			double lon = 0;
			double value = 0;
			int scale = 0;
			String startTime = "";
			
			int count = 0;
			log("received " + cursor.getCount() + " temperatures");
			
			if(cursor.getCount() != 0) {
				mMap.clear();
				//conditionsAdder = new ConditionsAdder();
				//conditionsAdder.execute("");
				addConditionMarkersToMap();
			}
			
			// limit a few per map quadrant
			int q1Count = 0;
			int q2Count = 0;
			int q3Count = 0;
			int q4Count = 0;
			int maxQ = 2;
			
			String forecastID = "";
			
			while(cursor.moveToNext()) {
				forecastID = cursor.getString(0);
				lat = cursor.getDouble(1);
				lon = cursor.getDouble(2);
				value = cursor.getDouble(3);
				scale = cursor.getInt(4);
				startTime = cursor.getString(5);
				
				
				String displayTempValue = "";
				if(scale == 2) {
					// Temperature arrived in degrees F
					double tempInC = ((value - 32)*5)/9;
					displayTempValue = displayTemperatureValue(tempInC, "##");
				} else if (scale == 1) {
					// Temperature arrived in degrees C
					displayTempValue = displayTemperatureValue(value, "##");
				} else {
					// unknown scale! 
					continue;
				}
				
				int q = whichMapQ(lat, lon);
				
				if (q == 1) {
					q1Count++;
					if(q1Count > maxQ) {
						//log("too many temp in q1");
						continue;
					}
				} else if (q == 2) {
					q2Count++;
					if(q2Count > maxQ) {
						//log("too many temp in q2");
						continue;
					}
				} else if (q == 3) {
					q3Count++;
					if(q3Count > maxQ) {
						//log("too many temp in q3");
						continue;
					}
				} else if (q == 4) {
					q4Count++;
					if(q4Count > 0) {
						//log("too many temp in q4");
						continue;
					}
				}
				
				addIcon(iconFactory, displayTempValue, new LatLng(lat, lon));
				
				ForecastLocation location = new ForecastLocation(forecastID, lat, lon);
				liveMapForecasts.add(location);
				

				log("adding temp icon for value " + value);
				count++;
				
			}
			mapStartTime = startTime;

			
			
			
			
			db.close();
		} catch (SQLiteDatabaseLockedException locke) {
			log ("app db locked, cannot add forecast map temps");
		} catch (Exception e) {
			log ("app sql error? " + e.getMessage());
		} finally {
			db.close();
		}
		
		
		
		animationProgress = (SeekBar) findViewById(R.id.animationProgress);
		imageButtonPlay = (ImageButton) findViewById(R.id.imageButtonPlay);
		
		
		prepareTemperatureAnimation();
		

		temperatureAnimator.reset();
		updateAnimationTime("left", activeForecastStartTime, 0);
		updateAnimationTime("right", activeForecastStartTime, animationProgress.getMax());
		

	}
	
	
	private int whichMapQ(double lat, double lon) {
		LatLng center = mMap.getCameraPosition().target;
		double mapCenterLat = center.latitude;
		double mapCenterLon = center.longitude;
		
		if( (lat < mapCenterLat) && (lon < mapCenterLon)) {
			return 1;
		} else if( (lat > mapCenterLat) && (lon < mapCenterLon)) {
			return 2;
		} else if( (lat > mapCenterLat) && (lon > mapCenterLon)) {
			return 3;
		} else if( (lat < mapCenterLat) && (lon > mapCenterLon)) {
			return 4;
		} else {
			return 0;	
		}
	}
	
    private void addIcon(IconGenerator iconFactory, String text, LatLng position) {
        MarkerOptions markerOptions = new MarkerOptions().
                icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(text))).
                position(position).
                anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());

        Marker addedTemp = mMap.addMarker(markerOptions);
        
        //log("added temp icon");
    }
	 
	private CbApiCall buildLocalCurrentConditionsCall(double hoursAgo) {
		log("building map conditions call for hours: "
				+ hoursAgo);
		long startTime = System.currentTimeMillis()
				- (int) ((hoursAgo * 60 * 60 * 1000));
		long endTime = System.currentTimeMillis();
		CbApiCall api = new CbApiCall();

		double minLat = 0;
		double maxLat = 0;
		double minLon = 0;
		double maxLon = 0;
		
		try {
			Location userLocation;
			if(bestLocation != null) {
				userLocation = bestLocation;
			} else {
				LocationManager lm = (LocationManager) getApplicationContext()
						.getSystemService(Context.LOCATION_SERVICE);
				Location loc = lm
						.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				userLocation = loc; 
			}
			if(userLocation.getLatitude() != 0) {
				minLat = userLocation.getLatitude() - .1;
				maxLat = userLocation.getLatitude() + .1;
				minLon = userLocation.getLongitude() - .1;
				maxLon = userLocation.getLongitude() + .1;
			} else {
				log("no location, bailing on csll");
				return null;
			}
				
			api.setMinLat(minLat);
			api.setMaxLat(maxLat);
			api.setMinLon(minLon);
			api.setMaxLon(maxLon);
			api.setStartTime(startTime);
			api.setEndTime(endTime);
			api.setLimit(500);
			api.setCallType("Conditions");
		} catch(NullPointerException npe) {
			// 
		}
		return api;
	}
    
	private ArrayList<CbCurrentCondition> getMyCurrentConditionsFromLocalAPI(CbApiCall currentConditionAPI) {
		ArrayList<CbCurrentCondition> conditions = new ArrayList<CbCurrentCondition>();
		CbDb db = new CbDb(getApplicationContext());
		try {
			db.open();
			Cursor ccCursor = db.getMyCurrentConditions(
					currentConditionAPI.getMinLat(),
					currentConditionAPI.getMaxLat(),
					currentConditionAPI.getMinLon(),
					currentConditionAPI.getMaxLon(),
					currentConditionAPI.getStartTime(),
					currentConditionAPI.getEndTime(), 1000, getID());

			while (ccCursor.moveToNext()) {
				CbCurrentCondition cur = new CbCurrentCondition();
				Location location = new Location("network");
				double latitude = ccCursor.getDouble(1);
				double longitude = ccCursor.getDouble(2);
				location.setLatitude(latitude);
				location.setLongitude(longitude);
				cur.setLat(latitude);
				cur.setLon(longitude);
				location.setAltitude(ccCursor.getDouble(3));
				location.setAccuracy(ccCursor.getInt(4));
				location.setProvider(ccCursor.getString(5));
				cur.setLocation(location);
				cur.setSharing_policy(ccCursor.getString(6));
				cur.setTime(ccCursor.getLong(7));
				cur.setTzoffset(ccCursor.getInt(8));
				cur.setUser_id(ccCursor.getString(9));
				cur.setGeneral_condition(ccCursor.getString(10));
				cur.setWindy(ccCursor.getString(11));
				cur.setFog_thickness(ccCursor.getString(12));
				cur.setCloud_type(ccCursor.getString(13));
				cur.setPrecipitation_type(ccCursor.getString(14));
				cur.setPrecipitation_amount(ccCursor.getDouble(15));
				cur.setPrecipitation_unit(ccCursor.getString(16));
				cur.setThunderstorm_intensity(ccCursor.getString(17));
				cur.setUser_comment(ccCursor.getString(18));
				conditions.add(cur);
			}
		} catch (Exception e) {
			log("app get_current_conditions failed " + e.getMessage());
		} finally {
			db.close();
		}
		return conditions;
	}
	
	/**
	 * Display user-submitted current conditions on the map
	 */
	private void prepareConditionMarkersForMap() {
		int currentCur = 0;
		int totalAllowed = 3000;
		
		liveMarkerOptions.clear();
		log("about to add current conditions to map: "
				+ globalConditionRecents.size());
		try {
			
			
			CbApiCall apiCall = buildLocalCurrentConditionsCall(1);
			ArrayList<CbCurrentCondition> dbRecents = getMyCurrentConditionsFromLocalAPI(apiCall);
			globalConditionRecents.addAll(dbRecents);
			
		} catch(Exception e) {
			log("app failed to add recent condition deliveries to the map " + e.getMessage());
		}
		
		
		
		if (globalConditionRecents != null) {
			log("adding current conditions to map: "
					+ globalConditionRecents.size());
						
			ConditionsDrawables draws = new ConditionsDrawables(getApplicationContext());
			
			// Add Current Conditions
			for (CbCurrentCondition condition : globalConditionRecents) {

				LatLng point = new LatLng(
						condition.getLocation().getLatitude(), condition
								.getLocation().getLongitude());
				
				log("getting layer drawable for condition "
						+ condition.getGeneral_condition() + " id " + condition.getUser_id());
				
				LayerDrawable drLayer = draws.getCurrentConditionDrawable(condition,
						null);
				if (drLayer == null) {
					log("drlayer null, next!");
					continue;
				}
				Drawable draw = draws.getSingleDrawable(drLayer);

				Bitmap image = drawableToBitmap(draw, null);
				
				long timeSinceSubmit = System.currentTimeMillis() - condition.getTime();
				long minutesAgo = timeSinceSubmit / (1000 * 60);

				String markerTitle = condition.getGeneral_condition();
				if(markerTitle.equals(getString(R.string.extreme))) {
					markerTitle = condition.getUser_comment();
				} else if (markerTitle.equals(getString(R.string.precipitation))) {
					markerTitle = condition.getPrecipitation_type();
				}
				
				String minutesAgoMessage = "";
				if (minutesAgo == 1) {
					minutesAgoMessage = minutesAgo + " " + getString(R.string.minuteAgo);
				} else {
					minutesAgoMessage = minutesAgo + " " + getString(R.string.minutesAgo);
				}
				
				MarkerOptions options = new MarkerOptions()
				.position(point)
				.title(markerTitle)
				.snippet(minutesAgoMessage)
				.icon(BitmapDescriptorFactory.fromBitmap(image));
				
				//Marker marker = mMap.addMarker(options);
				liveMarkerOptions.add(options);
				
				currentCur++;
				if (currentCur > totalAllowed) {
					break;
				}
			}
			
			
			//globalConditionRecents.clear();
			
			log("app done adding conditions to map");
		} else {
			log("addDatatomap conditions recents is null");
		}
	}

	public class ConditionsAdder extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... arg0) {
			prepareConditionMarkersForMap();
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			for(MarkerOptions options : liveMarkerOptions) {
				Marker marker = mMap.addMarker(options);	
			}
			super.onPostExecute(result);
		}
	}
	
	private void addConditionMarkersToMap() {
		try {
			for(MarkerOptions options : liveMarkerOptions) {
				Marker marker = mMap.addMarker(options);	
			}	
		} catch(ConcurrentModificationException cme) {
			log("concurrentmodificationexception adding markers to map " + cme.getMessage());
		}
		
	}
	
	/**
	 * 
	 * Users will save locations and we will cache data for those locations.
	 * Build a general API call to cache a location.
	 * 
	 * @param locationRowId
	 * @return
	 */
	private CbApiCall buildSearchLocationAPICall(SearchLocation loc) {
		long startTime = System.currentTimeMillis()
				- (int) ((2 * 60 * 60 * 1000));
		long endTime = System.currentTimeMillis();
		CbApiCall api = new CbApiCall();

		api.setMinLat(loc.getLatitude() - .1);
		api.setMaxLat(loc.getLatitude() + .1);
		api.setMinLon(loc.getLongitude() - .1);
		api.setMaxLon(loc.getLongitude() + .1);
		api.setStartTime(startTime);
		api.setEndTime(endTime);
		api.setLimit(1000);
		return api;
	}

	private CbApiCall buildLocalConditionsAPICall(double hoursAgo) {
		long startTime = System.currentTimeMillis()
				- (int) ((hoursAgo * 60 * 60 * 1000));
		long endTime = System.currentTimeMillis();
		CbApiCall api = new CbApiCall();

		double minLat = 0;
		double maxLat = 0;
		double minLon = 0;
		double maxLon = 0;
		
		if (visibleBound != null) {
			LatLng ne = visibleBound.northeast;
			LatLng sw = visibleBound.southwest;
			minLat = sw.latitude;
			maxLat = ne.latitude;
			minLon = sw.longitude;
			maxLon = ne.longitude;
		} else {
			log("no map center, bailing on map call");
			return api;
		}

		api.setMinLat(minLat);
		api.setMaxLat(maxLat);
		api.setMinLon(minLon);
		api.setMaxLon(maxLon);
		api.setStartTime(startTime);
		api.setEndTime(endTime);
		api.setLimit(500);
		return api;
	}
	
	private CbApiCall buildMapAPICall(double hoursAgo) {
		long startTime = System.currentTimeMillis()
				- (int) ((hoursAgo * 60 * 60 * 1000));
		long endTime = System.currentTimeMillis();
		CbApiCall api = new CbApiCall();

		double minLat = 0;
		double maxLat = 0;
		double minLon = 0;
		double maxLon = 0;

		if (visibleBound != null) {
			LatLng ne = visibleBound.northeast;
			LatLng sw = visibleBound.southwest;
			minLat = sw.latitude;
			maxLat = ne.latitude;
			minLon = sw.longitude;
			maxLon = ne.longitude;
		} else {
			log("no map center, bailing on map call");
			return api;
		}

		api.setMinLat(minLat);
		api.setMaxLat(maxLat);
		api.setMinLon(minLon);
		api.setMaxLon(maxLon);
		api.setStartTime(startTime);
		api.setEndTime(endTime);
		api.setLimit(500);
		return api;
	}
	
	private CbStatsAPICall buildStatsAPICall(double hoursAgo) {
		long startTime = System.currentTimeMillis()
				- (int) ((hoursAgo * 60 * 60 * 1000));
		long endTime = System.currentTimeMillis();
		CbStatsAPICall api = new CbStatsAPICall();

		double minLat = 0;
		double maxLat = 0;
		double minLon = 0;
		double maxLon = 0;

		if (visibleBound != null) {
			LatLng ne = visibleBound.northeast;
			LatLng sw = visibleBound.southwest;
			minLat = sw.latitude;
			maxLat = ne.latitude;
			minLon = sw.longitude;
			maxLon = ne.longitude;
		} else {
			log("no map center, bailing on map call");
			return api;
		}

		api.setMinLatitude(minLat);
		api.setMaxLatitude(maxLat);
		api.setMinLongitude(minLon);
		api.setMaxLongitude(maxLon);
		api.setStartTime(startTime);
		api.setEndTime(endTime);
		api.setLogDuration("hourly");
		return api;
	}

	private CbApiCall buildConditionsAnimationCall(long startTime, long endTime) {
		Date start = new Date(startTime);
		Date end = new Date(endTime);
		log("building conditions animation call, start : "
				+ start.toLocaleString() + ", end " + end.toLocaleString());
		CbApiCall api = new CbApiCall();

		double minLat = 0;
		double maxLat = 0;
		double minLon = 0;
		double maxLon = 0;

		LatLngBounds bounds = mMap.getProjection()
				.getVisibleRegion().latLngBounds;
		visibleBound = bounds;
		
		if (visibleBound != null) {
			LatLng ne = visibleBound.northeast;
			LatLng sw = visibleBound.southwest;
			minLat = sw.latitude;
			maxLat = ne.latitude;
			minLon = sw.longitude;
			maxLon = ne.longitude;
		} else {
			log("no map center, bailing");
			return null;
		}

		api.setMinLat(minLat);
		api.setMaxLat(maxLat);
		api.setMinLon(minLon);
		api.setMaxLon(maxLon);
		api.setStartTime(startTime);
		api.setEndTime(endTime);
		api.setLimit(500);
		api.setCallType("Conditions");
		return api;
	}

	private CbApiCall buildMapCurrentConditionsCall(double hoursAgo) {
		log("building map conditions call for hours: " + hoursAgo);
		long startTime = System.currentTimeMillis()
				- (int) ((hoursAgo * 60 * 60 * 1000));
		long endTime = System.currentTimeMillis();
		CbApiCall api = new CbApiCall();

		double minLat = 0;
		double maxLat = 0;
		double minLon = 0;
		double maxLon = 0;

		if (visibleBound != null) {
			LatLng ne = visibleBound.northeast;
			LatLng sw = visibleBound.southwest;
			minLat = sw.latitude;
			maxLat = ne.latitude;
			minLon = sw.longitude;
			maxLon = ne.longitude;
		} else {
			log("no map center, bailing");
			return null;
		}

		api.setMinLat(minLat);
		api.setMaxLat(maxLat);
		api.setMinLon(minLon);
		api.setMaxLon(maxLon);
		api.setStartTime(startTime);
		api.setEndTime(endTime);
		api.setLimit(500);
		api.setCallType("Conditions");
		return api;
	}

	private void sendSingleObservation() {
		// check location and bail/notify if it's unavailable
		if (mLatitude == 0.0) {
			Toast.makeText(getApplicationContext(),
					getString(R.string.locationUnavailable), Toast.LENGTH_LONG)
					.show();
			return;
		}

		if (mBound) {
			Message msg = Message.obtain(null, CbService.MSG_SEND_OBSERVATION,
					0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// e.printStackTrace();
			}
		} else {
			log("app failed to send single obs; data management error: not bound");
		}
	}

	private void sendNewSettings() {
		log("app sending new settings");
		if (mBound) {
			Message msg = Message.obtain(null, CbService.MSG_SET_SETTINGS,
					activeSettings);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				log("app can't send new settings, remote exception "
						+ e.getMessage());
				// e.printStackTrace();
			}
		} else {
			log("app failed to send single obs; data management error: not bound");
		}
	}

	private void updateAPICount(int value) {
		activeAPICallCount += value;
		if (activeAPICallCount <= 0) {
			activeAPICallCount = 0;
		}
		updateProgressBar();
	}

	private void updateProgressBar() {
		if (activeAPICallCount > 0) {
			progressAPI.setVisibility(View.VISIBLE);
		} else {
			progressAPI.setVisibility(View.GONE);
		}
	}

	private void makeAPICall(CbApiCall apiCall) {
		if (mBound) {
			Message msg = Message.obtain(null, CbService.MSG_MAKE_API_CALL,
					apiCall);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
				updateAPICount(1);
			} catch (RemoteException e) {
				// e.printStackTrace();
				updateAPICount(-1);
			}
		} else {
			log("make api call; app failed api call; data management error: not bound");
		}
	}
	
	private void makeStatsAPICall(CbStatsAPICall apiCall) {
		if (mBound) {
			statsMsg = Message.obtain(null, CbService.MSG_MAKE_STATS_CALL,
					apiCall);
			try {
				statsMsg.replyTo = mMessenger;
				mService.send(statsMsg);
				updateAPICount(1);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			log("make api call; app failed api call; data management error: not bound");
		}
	}

	private void makeCurrentConditionsAPICall(CbApiCall apiCall) {
		log("making current conditions api call");
		if (mBound) {
			Message msg = Message.obtain(null,
					CbService.MSG_MAKE_CURRENT_CONDITIONS_API_CALL, apiCall);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
				updateAPICount(1);
			} catch (RemoteException e) {
				// e.printStackTrace();
				updateAPICount(-1);
			}
		} else {
			System.out
					.println("data management error: not bound for condition api");
		}
	}

	private void loadRecents() {
		CbApiCall conditionsAPI = buildMapAPICall(1.5);
		askForCurrentConditionRecents(conditionsAPI);

	}

	// Stop listening to the barometer when our app is paused.
	@Override
	protected void onPause() {
		super.onPause();
		unBindCbService();
		stopSensorListeners();
	}

	// Register a broadcast listener
	@Override
	protected void onResume() {
		super.onResume();

		checkNetwork();
		displayNetworkOfflineToast();

		getStoredPreferences();

		registerForDownloadResults();
		

		setUpMapIfNeeded();
		
		checkSensors();
		updateVisibleReading();

		invalidateOptionsMenu();
		
		drawerList = (ListView) findViewById(R.id.left_drawer);
		drawerList.setItemChecked(-1, false);
		drawerList.clearChoices();
		
		
		if(hasBarometer) {
			startSensorListeners();
		}

		if (!isCbServiceRunning()) {
			log("onresume cbservice is not already running, ");
			startCbService();
		} else {
			log("onresume cbservice is already running");
		}
		bindCbService();
		editLocation.setText("");
	}

	private boolean isCbServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (CbService.class.getName()
					.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onStart() {
		dataReceivedToPlot = false;
		// bindCbService();
		super.onStart();
		
		// Get tracker.
		Tracker t = ((PressureNetApplication) getApplication()).getTracker(
		    TrackerName.APP_TRACKER);


		// Set screen name.
		t.setScreenName("PressureNet Main App");

		// Send a screen view.
		t.send(new HitBuilders.ScreenViewBuilder().build());
	}

	@Override
	protected void onStop() {
		stopSensorListeners();
		dataReceivedToPlot = false;
		unBindCbService();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		dataReceivedToPlot = false;
		unBindCbService();
		mixpanel.flush();
		super.onDestroy();
	}
	
	private String displayPressureValue(double value, double altitude) {
		if(preferenceMSLP) {
			value = CbScience.estimateMSLP(value, altitude, 15);
		}
		
		DecimalFormat df = new DecimalFormat("####.0");
		PressureUnit unit = new PressureUnit(preferencePressureUnit);
		unit.setValue(value);
		unit.setAbbreviation(preferencePressureUnit);
		double pressureInPreferredUnit = unit.convertToPreferredUnit();
		return df.format(pressureInPreferredUnit) + " " + unit.fullToAbbrev();
	}

	private String displayTemperatureValue(double value, String format) {
		DecimalFormat df = new DecimalFormat(format);
		TemperatureUnit unit = new TemperatureUnit(preferenceTemperatureUnit);
		unit.setValue(value);
		unit.setAbbreviation(preferenceTemperatureUnit);
		double temperatureInPreferredUnit = unit.convertToPreferredUnit();
		return df.format(temperatureInPreferredUnit) + " "
				+ unit.fullToAbbrev();
	}

	private String displayHumidityValue(double value) {
		DecimalFormat df = new DecimalFormat("##.00");
		return df.format(value) + "%";
	}

	private String getBestPressureDisplay () {
		String toPrint = "";
	
		if(bestLocation!=null) {
			toPrint = displayPressureValue(recentPressureReading, bestLocation.getAltitude());
		} else {
			toPrint = displayPressureValue(recentPressureReading, 0);
		}			
	
		return toPrint;
	}
	
	/**
	 * Display visible reading to the user
	 */
	private void updateVisibleReading() {
		checkSensors();
		preferencePressureUnit = getUnitPreference();
		preferenceTemperatureUnit = getTempUnitPreference();
		
		if (hasBarometer) {
			String toPrint = getBestPressureDisplay();
			
	
			if (toPrint.length() > 2) {
				// buttonBarometer.setText(getBestPressureDisplay());
				
				ActionBar bar = getActionBar();
				bar.setTitle(toPrint);
				int actionBarTitleId = getResources().getSystem()
						.getIdentifier("action_bar_title", "id", "android");
				TextView actionBarTextView = (TextView) findViewById(actionBarTitleId);
				actionBarTextView.setTextColor(Color.WHITE);
			} else {
				//buttonBarometer.setText("No barometeer detected.");
			}
		} else {
			//buttonBarometer.setText("No barometer detected.");
		}
	}

	/**
	 * Log data to SD card for debug purposes. To enable logging, ensure the
	 * Manifest allows writing to SD card.
	 * 
	 * @param text
	 */
	private void logToFile(String text) {
		try {
			OutputStream output = new FileOutputStream(mAppDir + "/log.txt",
					true);
			String logString = (new Date()).toString() + ": " + text + "\n";
			output.write(logString.getBytes());
			output.close();
		} catch (FileNotFoundException e) {
			// e.printStackTrace();
		} catch (IOException ioe) {
			// ioe.printStackTrace();
		}
	}

	private void log(String text) {
		if (PressureNETConfiguration.DEBUG_MODE) {
			// logToFile(text);
			System.out.println(text);
		}
	}

	private void startSensorListeners() {
		try {
			updateVisibleReading();
			sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			Sensor pressureSensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
			Sensor temperatureSensor = sm
					.getDefaultSensor(TYPE_AMBIENT_TEMPERATURE);
			Sensor humiditySensor = sm.getDefaultSensor(TYPE_RELATIVE_HUMIDITY);

			if (pressureSensor != null) {
				if (android.os.Build.VERSION.SDK_INT == 19) {
					sm.registerListener(this, pressureSensor,
							SensorManager.SENSOR_DELAY_UI, 1000000);
				} else {
					sm.registerListener(this, pressureSensor,
							SensorManager.SENSOR_DELAY_UI);
				}
			} else {
				recentPressureReading = 0.0;
			}
			if (temperatureSensor != null) {
				if (android.os.Build.VERSION.SDK_INT == 19) {
					sm.registerListener(this, temperatureSensor,
							SensorManager.SENSOR_DELAY_UI, 1000000);
				} else {
					sm.registerListener(this, temperatureSensor,
							SensorManager.SENSOR_DELAY_UI);
				}
			} else {
				recentTemperatureReading = 1000.0;
			}
			if (humiditySensor != null) {
				if (android.os.Build.VERSION.SDK_INT == 19) {
					sm.registerListener(this, humiditySensor,
							SensorManager.SENSOR_DELAY_UI, 1000000);
				} else {
					sm.registerListener(this, humiditySensor,
							SensorManager.SENSOR_DELAY_UI);
				}
			} else {
				recentHumidityReading = 1000.0;
			}
		} catch (Exception e) {
			log("app sensor error " + e.getMessage());
		}
	}

	public void stopSensorListeners() {
		sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sm.unregisterListener(this);
		sm = null;
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
			recentPressureReading = event.values[0];
			updateVisibleReading();
		} else if (event.sensor.getType() == TYPE_AMBIENT_TEMPERATURE) {
			recentTemperatureReading = event.values[0];
			updateVisibleReading();
		} else if (event.sensor.getType() == TYPE_RELATIVE_HUMIDITY) {
			recentHumidityReading = event.values[0];
			updateVisibleReading();
		}
	}
}
