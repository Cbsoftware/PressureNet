package ca.cumulonimbus.barometernetwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
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
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
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
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import ca.cumulonimbus.pressurenetsdk.CbApiCall;
import ca.cumulonimbus.pressurenetsdk.CbConfiguration;
import ca.cumulonimbus.pressurenetsdk.CbCurrentCondition;
import ca.cumulonimbus.pressurenetsdk.CbObservation;
import ca.cumulonimbus.pressurenetsdk.CbService;
import ca.cumulonimbus.pressurenetsdk.CbSettingsHandler;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class BarometerNetworkActivity extends Activity implements
		SensorEventListener {

	public static final int NOTIFICATION_ID = 101325;

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

	private ArrayList<CbObservation> listRecents = new ArrayList<CbObservation>();
	private ArrayList<CbObservation> graphRecents = new ArrayList<CbObservation>();
	private ArrayList<CbCurrentCondition> currentConditionRecents = new ArrayList<CbCurrentCondition>();

	private int activeAPICallCount = 0;

	boolean dataReceivedToPlot = false;

	private Button buttonBarometer;
	private Button buttonThermometer;
	private Button buttonHygrometer;
	private int hoursAgoSelected = 12;

	private ProgressBar progressAPI;

	private Button mapMode;
	private Button graphMode;
	private Button sensorMode;

	private LinearLayout layoutMapInfo;
	private LinearLayout layoutGraph;
	private LinearLayout layoutSensors;

	private TextView mapLatitudeMinText;
	private TextView mapLongitudeMinText;
	private TextView mapLatitudeMaxText;
	private TextView mapLongitudeMaxText;
	private TextView mapDataPointsText;

	private ImageButton buttonSearchLocations;
	private TextView textChartTimeInfo;
	private RelativeLayout layoutGraphButtons;

	private CheckBox satelliteView;

	private ImageButton buttonGoBackwards;
	private ImageButton buttonGoForwards;
	
	Handler timeHandler = new Handler();
	Handler mapDelayHandler = new Handler();

	String apiServerURL = CbConfiguration.SERVER_URL + "list/?";

	double recentPressureReading = 0.0;
	double recentTemperatureReading = 1000; // TODO: fix default value hack
	double recentHumidityReading = 1000;
	private final int TYPE_AMBIENT_TEMPERATURE = 13;
	private final int TYPE_RELATIVE_HUMIDITY = 12;

	public static final int REQUEST_SETTINGS = 1;
	public static final int REQUEST_LOCATION_CHOICE = 2;
	public static final int REQUEST_MAILED_LOG = 3;
	public static final int REQUEST_DATA_CHANGED = 4;

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

	private GoogleMap mMap;
	private LatLngBounds visibleBound;

	// Search Locations
	private ImageButton buttonGoLocation;
	private EditText editLocation;

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

	private LocationManager networkLocationManager;
	private LocationListener locationListener;

	private long lastSubmitStart = 0;

	private static final String moon_phase_name[] = { "New Moon", // 0
			"Waxing crescent", // 1
			"First quarter", // 2
			"Waxing gibbous", // 3
			"Full Moon", // 4
			"Waning gibbous", // 5
			"Third quarter", // 6
			"Waning crescent" }; // 7

	ChartController charts = new ChartController();
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dataReceivedToPlot = false;
		setContentView(R.layout.main);
		checkNetwork();
		checkBarometer();
		setLastKnownLocation();
		startAppLocationListener();
		startSensorListeners();
		startLog();
		getStoredPreferences();
		setUpMap();
		setUpUIListeners();
		setId();
		setUpFiles();
		showWelcomeActivity();
		setUpActionBar();
	}

	/**
	 * Controller for the chart viewing experience. Keep some data cached
	 */
	private class ChartController {
		private int cachedSegments;
		private int currentSegment;
		
		/**
		 * New data is being loaded, so increment the cache count
		 * and set the current pointer to the most recent value
		 */
		public void addAndLoadSegment() {
			cachedSegments++;
			currentSegment = cachedSegments;
			
			CbApiCall api = getActiveChartCacheCall();
			makeAPICall(api);
		}
		
		public CbApiCall getActiveChartCacheCall() {
			long startTime = System.currentTimeMillis() - (currentSegment * 12 * 60 * 60 * 1000);
			long endTime = startTime + (12 * 60 * 60 * 1000);
			
			CbApiCall api = buildMapAPICall(12);
			api.setStartTime(startTime);
			api.setEndTime(endTime);
			api.setLimit(3000);
			return api;
		}
		
		public void loadCachedSegment() {
			CbApiCall api = getActiveChartCacheCall();
			askForGraphRecents(api);
			
		}
		
		public void reset() {
			cachedSegments = 0;
			currentSegment = 0;
		}
		
		public void goBack() {
			currentSegment++;
			if(currentSegment > cachedSegments) {
				addAndLoadSegment();
			} else {
				loadCachedSegment();
			}
			buttonGoForwards.setEnabled(true);
			buttonGoForwards.setAlpha(1F);
		}
		
		public void goForward() {
			currentSegment--;
			if(currentSegment <= 1) {
				buttonGoForwards.setEnabled(false);
				buttonGoForwards.setAlpha(.5F);
			}
			loadCachedSegment();
		}
		
		public int getCachedSegments() {
			return cachedSegments;
		}
		public void setCachedSegments(int cachedSegments) {
			this.cachedSegments = cachedSegments;
		}
		public int getCurrentSegment() {
			return currentSegment;
		}
		public void setCurrentSegment(int currentSegment) {
			this.currentSegment = currentSegment;
		}
	}
	
	/**
	 * Start the network location listener for use outside the SDK
	 */
	private void startAppLocationListener() {
		networkLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		startGettingLocations();
	}

	/**
	 * Stop all location listeners
	 * 
	 * @return
	 */
	public boolean stopGettingLocations() {
		try {
			if (locationListener != null) {
				if (networkLocationManager != null) {
					networkLocationManager.removeUpdates(locationListener);
				}
			}
			networkLocationManager = null;
			return true;
		} catch (Exception e) {
			// e.printStackTrace();
			return false;
		}
	}

	/**
	 * Get the user's location from the location service
	 * 
	 * @return
	 */
	public boolean startGettingLocations() {
		locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				bestLocation = location;
				mLatitude = location.getLatitude();
				mLongitude = location.getLongitude();
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};

		// Register the listener with the Location Manager to receive location
		// updates
		try {
			networkLocationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 1000 * 60 * 60 * 5, 300,
					locationListener);
		} catch (Exception e) {
			// e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Update local location data with the last known location.
	 */
	private void setLastKnownLocation() {
		try {
			LocationManager lm = (LocationManager) this
					.getSystemService(Context.LOCATION_SERVICE);
			Location loc = lm
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			bestLocation = loc;

			double latitude = loc.getLatitude();
			double longitude = loc.getLongitude();
			mLatitude = latitude;
			mLongitude = longitude;
		} catch (Exception e) {
			// everything stays as previous, likely 0
		}

	}

	/**
	 * Send an Android notification to the user with a notice of pressure
	 * tendency change.
	 * 
	 * @param tendencyChange
	 */
	private void deliverNotification(String tendencyChange) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);

		long lastNotificationTime = sharedPreferences.getLong(
				"lastNotificationTime", System.currentTimeMillis()
						- (1000 * 60 * 60 * 10));
		long now = System.currentTimeMillis();
		long waitDiff = 1000 * 60 * 60 * 6;
		if (now - lastNotificationTime < waitDiff) {
			log("bailing on notification, not 6h wait yet");
			return;
		}

		String deliveryMessage = "";
		if (!tendencyChange.contains(",")) {
			// not returning to directional values? don't deliver notification
			return;
		}

		String first = tendencyChange.split(",")[0];
		String second = tendencyChange.split(",")[1];

		if ((first.contains("Rising")) && (second.contains("Falling"))) {
			deliveryMessage = "The pressure is dropping";
		} else if ((first.contains("Steady")) && (second.contains("Falling"))) {
			deliveryMessage = "The pressure is dropping";
		} else if ((first.contains("Steady")) && (second.contains("Rising"))) {
			deliveryMessage = "The pressure is rising";
		} else if ((first.contains("Falling")) && (second.contains("Rising"))) {
			deliveryMessage = "The pressure is rising";
		} else {
			deliveryMessage = "The pressure is steady";
			// don't deliver this message
			log("bailing on notification, pressure is steady");
			return;
		}

		Notification.Builder mBuilder = new Notification.Builder(
				getApplicationContext()).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("pressureNET").setContentText(deliveryMessage);
		// Creates an explicit intent for an activity
		Intent resultIntent = new Intent(getApplicationContext(),
				CurrentConditionsActivity.class);
		// Current Conditions activity likes to know the location in the Intent
		double notificationLatitude = 0.0;
		double notificationLongitude = 0.0;
		try {
			LocationManager lm = (LocationManager) this
					.getSystemService(Context.LOCATION_SERVICE);
			Location loc = lm
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (loc.getLatitude() != 0) {
				notificationLatitude = loc.getLatitude();
				notificationLongitude = loc.getLongitude();
			}
		} catch (Exception e) {

		}

		resultIntent.putExtra("latitude", notificationLatitude);
		resultIntent.putExtra("longitude", notificationLongitude);
		resultIntent.putExtra("cancelNotification", true);

		TaskStackBuilder stackBuilder = TaskStackBuilder
				.create(getApplicationContext());

		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the
		// notification later on.
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

		// save the time
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putLong("lastNotificationTime", now);
		editor.commit();

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
	 * Alert the user if pressureNET is offline
	 */
	private void displayNetworkOfflineToast() {
		if (!isConnected) {
			Toast.makeText(getApplicationContext(),
					"Cannot connect to network.", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Get fresh data for each of the user's saved locations
	 */
	private void makeLocationAPICalls() {
		long now = System.currentTimeMillis();
		long acceptableLocationTimeDifference = 1000 * 60 * 5;
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		// when was the last search locations API call?
		lastSearchLocationsAPICall = sharedPreferences.getLong("lastSearchLocationsAPICall", now - (1000 * 60 * 10));
		if(now - lastSearchLocationsAPICall > acceptableLocationTimeDifference) {
			log("running makeLocationAPICalls");
			PnDb pn = new PnDb(getApplicationContext());
			pn.open();
			Cursor cursor = pn.fetchAllLocations();

			while (cursor.moveToNext()) {
				String name = cursor.getString(1);
				double latitude = cursor.getDouble(2);
				double longitude = cursor.getDouble(3);
				SearchLocation location = new SearchLocation(name, latitude,
						longitude);
				CbApiCall locationApiCall = buildSearchLocationAPICall(location);
				log("making api call for " + name + " at " + latitude + " "
						+ longitude);
				makeAPICall(locationApiCall);
			}
			pn.close();
			
			lastSearchLocationsAPICall = now;
			// Save the time in prefs
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putLong("lastSearchLocationsAPICall", lastSearchLocationsAPICall);
			editor.commit();
		} else {
			log("not making locations calls, too soon");
		}
	}

	/**
	 * Get fresh data for the global map
	 */
	private void makeGlobalMapCall() {
		long currentTime = System.currentTimeMillis();
		long acceptableTimeDiff = 1000 * 60 * 5;
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		// when was the last global API call?
		lastGlobalApiCall = sharedPreferences.getLong("lastGlobalAPICall", currentTime - (1000 * 60 * 10));
		if (currentTime - lastGlobalApiCall > (acceptableTimeDiff)) {
			// System.out.println("making global map api call");

			CbApiCall globalMapCall = new CbApiCall();
			globalMapCall.setMinLat(-90);
			globalMapCall.setMaxLat(90);
			globalMapCall.setMinLon(-180);
			globalMapCall.setMaxLon(180);
			globalMapCall.setLimit(3000);
			globalMapCall.setStartTime(System.currentTimeMillis()
					- (int) (1000 * 60 * 60 * .25));
			globalMapCall.setEndTime(System.currentTimeMillis());
			makeAPICall(globalMapCall);

			// Save the time in prefs
			lastGlobalApiCall = currentTime;
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putLong("lastGlobalAPICall", lastGlobalApiCall);
			editor.commit();

		} else {
			log("not making global map call time diff "
					+ (currentTime - lastGlobalApiCall));
		}
	}

	/**
	 * Get fresh conditions data for the global map
	 */
	private void makeGlobalConditionsMapCall() {
		long now = System.currentTimeMillis();
		long acceptableLocationTimeDifference = 1000 * 60 * 5;
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		// when was the last search locations API call?
		lastGlobalConditionsApiCall = sharedPreferences.getLong("lastGlobalConditionsApiCall", now - (1000 * 60 * 10));
		if(now - lastGlobalConditionsApiCall > acceptableLocationTimeDifference) {
			log("making global conditions call");
			CbApiCall globalMapCall = new CbApiCall();
			globalMapCall.setMinLat(-90);
			globalMapCall.setMaxLat(90);
			globalMapCall.setMinLon(-180);
			globalMapCall.setMaxLon(180);
			globalMapCall.setLimit(1000);
			globalMapCall.setStartTime(System.currentTimeMillis()
					- (int) (1000 * 60 * 60 * 2));
			globalMapCall.setEndTime(System.currentTimeMillis());
			makeCurrentConditionsAPICall(globalMapCall);
			
			// Save the time in prefs
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putLong("lastGlobalConditionsApiCall", lastGlobalConditionsApiCall);
			editor.commit();
		} else {
			log("not making conditions global call, too soon");
		}
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

			mMap.getUiSettings().setZoomControlsEnabled(false);
			mMap.getUiSettings().setCompassEnabled(false);

			mMap.setInfoWindowAdapter(new MapWindowAdapter(this));

			mMap.setOnCameraChangeListener(new OnCameraChangeListener() {

				@Override
				public void onCameraChange(CameraPosition position) {
					// change button ability based on zoom level
					if (position.zoom >= 9) {
						graphMode.setEnabled(true);
						graphMode.setTextColor(Color.BLACK);
					} else {
						graphMode.setEnabled(false);
						graphMode.setTextColor(Color.GRAY);
					}

					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(editLocation.getWindowToken(),
							0);
					editLocation.setCursorVisible(false);

					LatLngBounds bounds = mMap.getProjection()
							.getVisibleRegion().latLngBounds;
					visibleBound = bounds;

					if (activeMode.equals("graph")) {
						mapMode.performClick();
						layoutMapInfo.setVisibility(View.GONE);
					} else if (activeMode.equals("map")) {
						loadRecents();
					} else if (activeMode.equals("sensors")) {
						mapMode.performClick();
						layoutMapInfo.setVisibility(View.GONE);
					}

					updateMapInfoText();
				}
			});

		}

	}

	/**
	 * Zoom into the user's location
	 */
	public void setUpMap() {
		setUpMapIfNeeded();

		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
				.getMap();

		// Set default coordinates (centered around the user's location)

		try {
			LocationManager lm = (LocationManager) this
					.getSystemService(Context.LOCATION_SERVICE);
			Location loc = lm
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (loc.getLatitude() != 0) {
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
						new LatLng(loc.getLatitude(), loc.getLongitude()), 11));
				updateMapInfoText();
			} else {

			}

		} catch (Exception e) {

		}
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
					latitude, longitude), 11));
			updateMapInfoText();
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
		preferenceTemperatureUnit = sharedPreferences.getString(
				"temperature_units", "Celsius (¡C)");
		preferenceCollectionFrequency = sharedPreferences.getString(
				"autofrequency", "10 minutes");
		preferenceShareData = sharedPreferences.getBoolean("autoupdate", true);
		preferenceShareLevel = sharedPreferences.getString(
				"sharing_preference", "Us, Researchers and Forecasters");
		preferenceSendNotifications = sharedPreferences.getBoolean(
				"send_notifications", false);
		preferenceUseGPS = sharedPreferences.getBoolean("use_gps", true);
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
		settings.setServerURL(CbConfiguration.SERVER_URL);
		settings.saveSettings();
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
				.getString("temperature_units", "Celsius (Â°C)");
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

	/**
	 * During animation, check if a condition/reading's time is close enough to
	 * the current frame to show it on the map
	 * 
	 * @param groupNumber
	 * @param currentTimeProgress
	 * @return
	 */
	public boolean isCloseToFrame(int groupNumber, int currentTimeProgress) {
		if (currentTimeProgress >= groupNumber) {
			if (currentTimeProgress - groupNumber < 10) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	/**
	 * Attach listeners to UI elements
	 */
	private void setUpUIListeners() {
		Context context = getApplicationContext();
		mInflater = LayoutInflater.from(context);
		buttonBarometer = (Button) findViewById(R.id.imageButtonBarometer);
		buttonThermometer = (Button) findViewById(R.id.imageButtonThermometer);
		buttonHygrometer = (Button) findViewById(R.id.imageButtonHygrometer);

		progressAPI = (ProgressBar) findViewById(R.id.progressBarAPICalls);

		buttonGoLocation = (ImageButton) findViewById(R.id.buttonGoLocation);
		editLocation = (EditText) findViewById(R.id.editGoLocation);

		mapLatitudeMinText = (TextView) findViewById(R.id.latitudeValueMinMapInfoText);
		mapLongitudeMinText = (TextView) findViewById(R.id.longitudeValueMinMapInfoText);
		mapLatitudeMaxText = (TextView) findViewById(R.id.latitudeValueMaxMapInfoText);
		mapLongitudeMaxText = (TextView) findViewById(R.id.longitudeValueMaxMapInfoText);
		mapDataPointsText = (TextView) findViewById(R.id.dataPointsValueMapInfoText);

		mapMode = (Button) findViewById(R.id.buttonMapMode);
		graphMode = (Button) findViewById(R.id.buttonGraphMode);
		sensorMode = (Button) findViewById(R.id.buttonSensorMode);

		layoutMapInfo = (LinearLayout) findViewById(R.id.layoutMapInformation);
		layoutGraph = (LinearLayout) findViewById(R.id.layoutGraph);
		layoutSensors = (LinearLayout) findViewById(R.id.layoutSensorInfo);

		buttonSearchLocations = (ImageButton) findViewById(R.id.buttonSearchLocations);

		satelliteView = (CheckBox) findViewById(R.id.checkSatellite);

		layoutGraphButtons = (RelativeLayout) findViewById(R.id.layoutGraphButtons);
		textChartTimeInfo = (TextView) findViewById(R.id.textChartTime);
		buttonGoBackwards = (ImageButton) findViewById(R.id.buttonGoBackwards);
		buttonGoForwards = (ImageButton) findViewById(R.id.buttonGoForwards);
		
		mapMode.setTypeface(null, Typeface.BOLD);

		buttonGoBackwards.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				charts.goBack();
			}
		});
	

		buttonGoForwards.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				charts.goForward();
			}
		});
		
		satelliteView.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
				} else {
					mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
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

		mapMode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (activeMode.equals("map")) {
					int visible = layoutMapInfo.getVisibility();
					if (visible == View.VISIBLE) {
						layoutMapInfo.setVisibility(View.GONE);
					} else {
						layoutMapInfo.setVisibility(View.VISIBLE);
					}
				} else {
					// UI switch
					layoutGraph.setVisibility(View.GONE);
					layoutGraphButtons.setVisibility(View.GONE);
					layoutMapInfo.setVisibility(View.VISIBLE);
					layoutSensors.setVisibility(View.GONE);

					// buttonChartTimeInfo.setVisibility(View.GONE);

					mapMode.setTypeface(null, Typeface.BOLD);
					graphMode.setTypeface(null, Typeface.NORMAL);
					sensorMode.setTypeface(null, Typeface.NORMAL);

					removeChartFromLayout();

					// set mode and load data
					activeMode = "map";
					addDataToMap();
				}

			}
		});

		graphMode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				int visible = layoutGraph.getVisibility();
				if (visible == View.VISIBLE) {
					layoutGraph.setVisibility(View.GONE);
					layoutGraphButtons.setVisibility(View.GONE);
				} else {
					graphMode.setEnabled(false);
					graphMode.setTextColor(Color.GRAY);
					Toast.makeText(getApplicationContext(), "Loading graph...",
							Toast.LENGTH_SHORT).show();
					layoutGraph.setVisibility(View.VISIBLE);
					activeMode = "graph";
					removeChartFromLayout();

					charts.reset();
					
					hoursAgoSelected = 12;

					log("making api call 12h for graph");
					CbApiCall api = buildMapAPICall(hoursAgoSelected);
					api.setLimit(5000);
					makeAPICall(api);
					
					charts.addAndLoadSegment();

					layoutGraph.setVisibility(View.VISIBLE);
					layoutMapInfo.setVisibility(View.GONE);
					layoutSensors.setVisibility(View.GONE);

					mapMode.setTypeface(null, Typeface.NORMAL);
					graphMode.setTypeface(null, Typeface.BOLD);
					sensorMode.setTypeface(null, Typeface.NORMAL);
				}

			}
		});

		sensorMode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (activeMode.equals("sensors")) {
					int visible = layoutSensors.getVisibility();
					if (visible == View.VISIBLE) {
						layoutSensors.setVisibility((View.GONE));
					} else {
						layoutSensors.setVisibility(View.VISIBLE);
					}
				} else {
					activeMode = "sensors";

					// UI switch
					layoutGraph.setVisibility(View.GONE);
					layoutGraphButtons.setVisibility(View.GONE);
					layoutMapInfo.setVisibility(View.GONE);
					layoutSensors.setVisibility(View.VISIBLE);

					layoutGraphButtons.setVisibility(View.GONE);

					mapMode.setTypeface(null, Typeface.NORMAL);
					graphMode.setTypeface(null, Typeface.NORMAL);
					sensorMode.setTypeface(null, Typeface.BOLD);

				}

			}
		});

		editLocation.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
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
				}
				return false;
			}
		});

		buttonGoLocation.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				String location = editLocation.getText().toString();
				if (location.equals("")) {
					Toast toast = Toast.makeText(getApplicationContext(),
							"Please enter a location to search",
							Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
					toast.show();
					focusSearch();
					return;
				}
				location = location.trim();
				Toast.makeText(getApplicationContext(), "Going to " + location,
						Toast.LENGTH_SHORT).show();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(editLocation.getWindowToken(), 0);
				editLocation.setCursorVisible(false);
				Geocoder geocode = new Geocoder(getApplicationContext());
				try {
					List<Address> addr = geocode.getFromLocationName(location,
							2);
					if (addr.size() > 0) {
						Address ad = addr.get(0);
						double latitude = ad.getLatitude();
						double longitude = ad.getLongitude();
						moveMapTo(latitude, longitude);

						SearchLocation loc = new SearchLocation(location,
								latitude, longitude);
						searchedLocations.add(loc);

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
						Toast.makeText(getApplicationContext(),
								"Error: cannot search Google Maps",
								Toast.LENGTH_SHORT).show();
					}

				} catch (IOException ioe) {
					// ioe.printStackTrace();
				}

			}
		});

	}

	/**
	 * Get basic information and format it to display on screen Show the map
	 * coordinates, number of unique data points visible, etc
	 */
	private void updateMapInfoText() {
		String updatedText = "";
		DecimalFormat latlngFormat = new DecimalFormat("###.0000");

		LatLng ne = visibleBound.northeast;
		LatLng sw = visibleBound.southwest;
		double maxLat = Math.max(ne.latitude, sw.latitude);
		double minLat = Math.min(ne.latitude, sw.latitude);
		double maxLon = Math.max(ne.longitude, sw.longitude);
		double minLon = Math.min(ne.longitude, sw.longitude);
		int visibleCount = listRecents.size();

		String minLatitude = latlngFormat.format(minLat);
		String maxLatitude = latlngFormat.format(maxLat);
		String minLongitude = latlngFormat.format(minLon);
		String maxLongitude = latlngFormat.format(maxLon);

		mapLatitudeMinText.setText(minLatitude);
		mapLatitudeMaxText.setText(maxLatitude);
		mapLongitudeMinText.setText(minLongitude);
		mapLongitudeMaxText.setText(maxLongitude);
		mapDataPointsText.setText(visibleCount + "");
	}

	/**
	 * Initiate the CbService
	 */
	private void startCbService() {
		log("start cbservice");
		try {
			if (hasBarometer) {
				serviceIntent = new Intent(this, CbService.class);
				startService(serviceIntent);
				log("app started service");
			} else {
				log("app detects no barometer, not starting service");
			}
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
	 * Query the database for locally stored observations with the intent to
	 * display on a time series chart
	 * 
	 * @param apiCall
	 */
	private void askForGraphRecents(CbApiCall apiCall) {
		if (mBound) {
			log("asking for graph recents");

			Message msg = Message.obtain(null,
					CbService.MSG_GET_API_RECENTS_FOR_GRAPH, apiCall);
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
	 * Query the database for locally stored observations
	 * 
	 * @param apiCall
	 */
	private void askForRecents(CbApiCall apiCall) {
		if (mBound) {
			log("asking for recents");

			Message msg = Message.obtain(null, CbService.MSG_GET_API_RECENTS,
					apiCall);
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
				listRecents = (ArrayList<CbObservation>) msg.obj;
				addDataToMap();
				if (activeMode.equals("graph")) {
					createAndShowChart();
				}
				break;
			case CbService.MSG_API_RECENTS_FOR_GRAPH:
				graphRecents = (ArrayList<CbObservation>) msg.obj;
				createAndShowChart();
				break;
			case CbService.MSG_API_RESULT_COUNT:
				int count = msg.arg1;
				updateAPICount(-1);
				if (activeMode.equals("map")) {
					CbApiCall api = buildMapAPICall(.5);
					askForRecents(api);

					CbApiCall apiConditions = buildMapAPICall(.5);
					apiConditions.setCallType("Conditions");
					askForCurrentConditionRecents(apiConditions);
				} else if (activeMode.endsWith("graph")) {
					CbApiCall api = charts.getActiveChartCacheCall();
					askForGraphRecents(api);
				}
				break;
			case CbService.MSG_CURRENT_CONDITIONS:
				// updateAPICount(-1);
				ArrayList<CbCurrentCondition> receivedList = (ArrayList<CbCurrentCondition>) msg.obj;
				if (receivedList != null) {
					if (receivedList.size() > 0) {
						currentConditionRecents = receivedList;
					} else {
						log("app received conditions size 0");
					}
				} else {
					log("app received null conditions");
				}

				addDataToMap();
				break;
			case CbService.MSG_CHANGE_NOTIFICATION:
				String change = (String) msg.obj;
				deliverNotification(change);
				break;
			case CbService.MSG_DATA_RESULT:
				String errors = (String) msg.obj;
				if (errors.contains("error")) {
					Toast.makeText(getApplicationContext(),
							"Error sending data.", Toast.LENGTH_SHORT).show();
				} else if ((!errors.contains("error")) && (errors.length() > 1)) {
					String condition = errors;
					Toast.makeText(getApplicationContext(),
							"Sent " + condition + "!", Toast.LENGTH_SHORT)
							.show();
				} else {
					// pressure toast
					String toPrint = displayPressureValue(recentPressureReading);
					Toast.makeText(getApplicationContext(),
							"Sent " + toPrint + "!", Toast.LENGTH_SHORT).show();
				}
				askForCurrentConditionRecents(buildMapCurrentConditionsCall(1));
				break;
			default:
				log("received default message");
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * Take the chart away.
	 */
	private void removeChartFromLayout() {
		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.layoutGraph);

		try {
			View testChartView = findViewById(100); // TODO: set a better
													// constant
			mainLayout.removeView(testChartView);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	/**
	 * Display a chart
	 */
	private void createAndShowChart() {
		if (!activeMode.equals("graph")) {
			log("createandshowchart called outside of graph mode");
			return;
		}
		if (graphRecents == null) {
			log("graph recents null RETURNING, no chart");
			return;
		} else if (graphRecents.size() == 0) {
			log("graph recents 0, RETURNING, no chart");
			return;
		}

		// draw chart
		log("plotting... " + graphRecents.size());
		Chart chart = new Chart(getApplicationContext());

		// set units according to preference
		ArrayList<CbObservation> displayRecents = new ArrayList<CbObservation>();
		for (CbObservation ob : graphRecents) {
			double rawValue = ob.getObservationValue();

			PressureUnit unit = new PressureUnit(preferencePressureUnit);
			unit.setValue(rawValue);
			unit.setAbbreviation(preferencePressureUnit);
			double pressureInPreferredUnit = unit.convertToPreferredUnit();

			ob.setObservationUnit(preferencePressureUnit);
			ob.setObservationValue(pressureInPreferredUnit);
			displayRecents.add(ob);
		}

		View chartView = chart.drawChart(displayRecents);

		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.layoutGraph);

		removeChartFromLayout();

		chartView.setId(100); // TODO: what's safe?

		// add to layout
		// Check screen metrics and orientation, set chart height accordingly
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int chartHeight = 380;
		try {
			DisplayMetrics displayMetrics = new DisplayMetrics();
			displayMetrics = getResources().getDisplayMetrics();
			log("setting text size, density is " + displayMetrics.densityDpi);
			switch (displayMetrics.densityDpi) {
			case DisplayMetrics.DENSITY_LOW:
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
					chartHeight = 100;
				} else {
					chartHeight = 200;
				}
				break;
			case DisplayMetrics.DENSITY_MEDIUM:
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
					chartHeight = 230;
				} else {
					chartHeight = 300;
				}
				break;
			case DisplayMetrics.DENSITY_HIGH:
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
					chartHeight = 200;
				} else {
					chartHeight = 350;
				}
				break;
			case DisplayMetrics.DENSITY_XHIGH:
				chartHeight = 400;
				break;
			case DisplayMetrics.DENSITY_XXHIGH:
				chartHeight = 450;
				break;
			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		LayoutParams lparams = new LayoutParams(LayoutParams.MATCH_PARENT,
				chartHeight);

		chartView.setBackgroundColor(Color.rgb(238, 238, 238));
		chartView.setLayoutParams(lparams);
		if (mainLayout == null) {
			log("chartlayout null");
			return;
		}
		layoutGraphButtons.setVisibility(View.VISIBLE);
		// TODO: bring the chart back
		mainLayout.addView(chartView);

		graphMode.setEnabled(true);
		graphMode.setTextColor(Color.BLACK);
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
			//makeLocationAPICalls();
			makeGlobalMapCall();
			makeGlobalConditionsMapCall();
			sendChangeNotification();
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
	 * Delete user data
	 */
	private void deleteUserData() {
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
	 * Welcome the user to pressureNET and explain the privacy options. Only
	 * show on the first run
	 */
	private void showWelcomeActivity() {
		// has this been shown yet?
		int runCount = 0;
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (sharedPreferences != null) {
			runCount = sharedPreferences.getInt("runCount", 0);
		}

		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putInt("runCount", runCount + 1);
		editor.commit();

		if (runCount == 0) {

			Intent intent = new Intent(this,
					ca.cumulonimbus.barometernetwork.WelcomeActivity.class);
			startActivityForResult(intent, 0);
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
			startActivityForResult(i, REQUEST_SETTINGS);
		} else if (item.getItemId() == R.id.menu_log_viewer) {
			viewLog();
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
			startActivityForResult(intent, REQUEST_DATA_CHANGED);

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
		} else if (item.getItemId() == R.id.menu_grow_pressurenet) {
			growPressureNET();
		} else if (item.getItemId() == R.id.menu_send_feedback) {
			sendFeedback();
		} else if (item.getItemId() == R.id.menu_rate_pressurenet) {
			ratePressureNET();
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Open the Google Play pressureNET page
	 */
	private void ratePressureNET() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri
				.parse("market://details?id=ca.cumulonimbus.barometernetwork"));
		startActivity(intent);
	}

	/**
	 * Email software@cumulonimbus.ca for feedback
	 */
	private void sendFeedback() {
		String address = "software@cumulonimbus.ca";
		String subject = "pressureNET feedback";
		String emailtext = "";
		final Intent emailIntent = new Intent(
				android.content.Intent.ACTION_SEND);

		emailIntent.setType("plain/text");

		emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
				new String[] { address });

		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);

		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, emailtext);

		startActivityForResult(
				Intent.createChooser(emailIntent, "Send mail..."),
				REQUEST_MAILED_LOG);
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
						"pressureNET crowdsources Android sensor data to improve weather forecasting. Free app: https://play.google.com/store/apps/details?id=ca.cumulonimbus.barometernetwork");
		sendIntent.setType("text/plain");
		startActivity(sendIntent);
	}

	/**
	 * 
	 * Email debug logs to Cumulonimbus.
	 * 
	 */
	private void emailLogs() {
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
					Intent.createChooser(emailIntent, "Send mail..."),
					REQUEST_MAILED_LOG);

		} catch (Throwable t) {
			Toast.makeText(this, "Request failed: " + t.toString(),
					Toast.LENGTH_LONG).show();
		}
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
				mapMode.performClick();
				layoutMapInfo.setVisibility(View.GONE);
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
							moveMapTo(lat, lon);
						}
					}
					layoutMapInfo.setVisibility(View.GONE);
				} else if (rowId == -2L) {
					log("onactivityresult -2");
					Toast toast = Toast.makeText(getApplicationContext(),
							"No saved locations. Please enter a location.",
							Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
					toast.show();

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
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putLong("lastGlobalAPICall", lastGlobalApiCall);
				editor.commit();
			}
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
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	private void setUpActionBar() {
		// TODO: Compatible Action Bar
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

	/**
	 * Check if an observation is from the current device
	 * 
	 * @param ob
	 * @return
	 */
	private boolean obsIsMe(CbObservation ob) {
		return ((ob.getUser_id().equals(android_id)));
	}

	/**
	 * Moon phase info
	 */
	private int getMoonPhaseIndex() {
		MoonPhase mp = new MoonPhase(Calendar.getInstance());
		return mp.getPhaseIndex();
	}

	/**
	 * Create neat drawables for weather conditions depending on the type of
	 * weather, the time, etc.
	 * 
	 * @param condition
	 * @param drawable
	 * @return
	 */
	private LayerDrawable getCurrentConditionDrawable(
			CbCurrentCondition condition, Drawable drawable) {

		Drawable weatherBackgroundDrawable = resizeDrawable(this.getResources()
				.getDrawable(R.drawable.bg_wea_square));

		int moonNumber = getMoonPhaseIndex() + 1;

		if (condition.getGeneral_condition().equals(getString(R.string.sunny))) {
			Drawable sunDrawable = this.getResources().getDrawable(
					R.drawable.ic_wea_col_sun);
			if (!CurrentConditionsActivity.isDaytime(condition.getLocation()
					.getLatitude(), condition.getLocation().getLongitude())) {
				switch (moonNumber) {
				case 1:
					sunDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_moon1);
					break;
				case 2:
					sunDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_moon2);
					break;
				case 3:
					sunDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_moon3);
					break;
				case 4:
					sunDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_moon4);
					break;
				case 5:
					sunDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_moon5);
					break;
				case 6:
					sunDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_moon6);
					break;
				case 7:
					sunDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_moon7);
					break;
				case 8:
					sunDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_moon8);
					break;
				default:
					sunDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_moon2);
					break;
				}

			}
			Drawable[] layers = { weatherBackgroundDrawable,
					resizeDrawable(sunDrawable) };
			LayerDrawable layerDrawable = new LayerDrawable(layers);
			return layerDrawable;
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
					return layerDrawable;
				} else if (condition.getPrecipitation_amount() == 1.0) {
					Drawable rainDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_rain2);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(rainDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				} else if (condition.getPrecipitation_amount() == 2.0) {
					Drawable rainDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_rain3);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(rainDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				}
			} else if (condition.getPrecipitation_type().equals(
					getString(R.string.snow))) {
				if (condition.getPrecipitation_amount() == 0.0) {
					Drawable snowDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_snow1);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(snowDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				} else if (condition.getPrecipitation_amount() == 1.0) {
					Drawable snowDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_snow2);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(snowDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				} else if (condition.getPrecipitation_amount() == 2.0) {
					Drawable snowDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_snow3);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(snowDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				}
			} else if (condition.getPrecipitation_type().equals(
					getString(R.string.hail))) {
				if (condition.getPrecipitation_amount() == 0.0) {
					Drawable hailDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_hail1);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(hailDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				} else if (condition.getPrecipitation_amount() == 1.0) {
					Drawable hailDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_hail2);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(hailDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				} else if (condition.getPrecipitation_amount() == 2.0) {
					Drawable hailDrawable = this.getResources().getDrawable(
							R.drawable.ic_wea_col_hail3);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(hailDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
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
				return layerDrawable;
			} else if (condition.getCloud_type().equals(
					getString(R.string.mostly_cloudy))) {
				Drawable cloudDrawable = this.getResources().getDrawable(
						R.drawable.ic_wea_col_cloud2);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(cloudDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else if (condition.getCloud_type().equals(
					getString(R.string.very_cloudy))) {
				Drawable cloudDrawable = this.getResources().getDrawable(
						R.drawable.ic_wea_col_cloud);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(cloudDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else {
				Drawable cloudDrawable = this.getResources().getDrawable(
						R.drawable.ic_wea_col_cloud);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(cloudDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
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
				return layerDrawable;
			} else if (condition.getFog_thickness().equals(
					getString(R.string.moderate_fog))) {
				Drawable fogDrawable = this.getResources().getDrawable(
						R.drawable.ic_wea_col_fog2);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(fogDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else if (condition.getFog_thickness().equals(
					getString(R.string.heavy_fog))) {
				Drawable fogDrawable = this.getResources().getDrawable(
						R.drawable.ic_wea_col_fog3);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(fogDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else {
				Drawable fogDrawable = this.getResources().getDrawable(
						R.drawable.ic_wea_col_fog2);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(fogDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			}
		} else if (condition.getGeneral_condition().equals(
				getString(R.string.thunderstorm))) {
			if (Double.parseDouble(condition.getThunderstorm_intensity()) == 0.0) {
				Drawable thunderstormDrawable = this.getResources()
						.getDrawable(R.drawable.ic_wea_col_r_l1);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(thunderstormDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else if (Double
					.parseDouble(condition.getThunderstorm_intensity()) == 1.0) {
				Drawable thunderstormDrawable = this.getResources()
						.getDrawable(R.drawable.ic_wea_col_r_l2);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(thunderstormDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else if (Double
					.parseDouble(condition.getThunderstorm_intensity()) == 2.0) {
				Drawable thunderstormDrawable = this.getResources()
						.getDrawable(R.drawable.ic_wea_col_r_l3);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(thunderstormDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			}
		} else {

		}

		return null;
	}

	// The gesture threshold expressed in dp
	// http://developer.android.com/guide/practices/screens_support.html#density-independence
	private static final float GESTURE_THRESHOLD_DP = 16.0f;

	/**
	 * Resize drawables on demand. High-res bitmaps on Android? Be careful of
	 * memory issues
	 * 
	 * @param image
	 * @return
	 */
	private Drawable resizeDrawable(Drawable image) {
		Bitmap d = ((BitmapDrawable) image).getBitmap();
		final float scale = getResources().getDisplayMetrics().density;
		int p = (int) (GESTURE_THRESHOLD_DP * scale + 0.5f);
		Bitmap bitmapOrig = Bitmap.createScaledBitmap(d, p * 4, p * 4, false);
		return new BitmapDrawable(bitmapOrig);
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

		String display = displayPressureValue(obs.getObservationValue());
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
			textSize = defaultTextSize;
			textXOffset = defaultTextXOffset + 3;
			textYOffset = defaultTextYOffset;
			break;
		case DisplayMetrics.DENSITY_XXHIGH:
			textSize = defaultTextSize + 8;
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

	private Drawable getSingleDrawable(LayerDrawable layerDrawable) {

		int resourceBitmapHeight = layerDrawable.getMinimumHeight(), resourceBitmapWidth = layerDrawable
				.getMinimumWidth();

		float widthInInches = 0.2f;

		int widthInPixels = (int) (widthInInches * getResources()
				.getDisplayMetrics().densityDpi);
		int heightInPixels = (int) (widthInPixels * resourceBitmapHeight / resourceBitmapWidth);

		int insetLeft = 10, insetTop = 10, insetRight = 10, insetBottom = 10;

		layerDrawable.setLayerInset(1, insetLeft, insetTop, insetRight,
				insetBottom);

		Bitmap bitmap = Bitmap.createBitmap(widthInPixels, heightInPixels,
				Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmap);
		layerDrawable.setBounds(0, 0, widthInPixels, heightInPixels);
		layerDrawable.draw(canvas);

		BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(),
				bitmap);
		bitmapDrawable.setBounds(0, 0, widthInPixels, heightInPixels);

		return bitmapDrawable;
	}

	// Put a bunch of barometer readings and current conditions on the map.
	private void addDataToMap() {

		// TODO: add delay so that the map isn't fully refreshed every touch
		log("add data to map");

		int totalEachAllowed = 30;
		int currentObs = 0;
		int currentCur = 0;

		int maxUpdateFrequency = 500; // 500ms
		long now = System.currentTimeMillis();

		Drawable drawable = this.getResources().getDrawable(
				R.drawable.bg_pre_marker);

		if (listRecents.size() > 0) {

			try {
				if ((now - lastMapDataUpdate) < (maxUpdateFrequency)) {
					log("adding data to map too frequently, bailing");
					return;
				} else {
					log("adding data to map, last update "
							+ (now - lastMapDataUpdate));
				}
				mMap.clear();
				lastMapDataUpdate = now;
				System.out.println("adding data to map, list recents size "
						+ listRecents.size());
				for (CbObservation observation : listRecents) {
					LatLng point = new LatLng(observation.getLocation()
							.getLatitude(), observation.getLocation()
							.getLongitude());

					Bitmap image = drawableToBitmap(drawable, observation);

					String valueToPrint = displayPressureValue(observation
							.getObservationValue());

					long timeRecorded = observation.getTime();
					long timeNow = System.currentTimeMillis();
					long msAgo = now - timeRecorded;
					int minutesAgo = (int) (msAgo / (1000 * 60));

					mMap.addMarker(new MarkerOptions().position(point)
							.title(minutesAgo + " minutes ago")
							.icon(BitmapDescriptorFactory.fromBitmap(image)));

					currentObs++;
					if (currentObs > totalEachAllowed) {
						break;
					}
				}
				updateMapInfoText();
			} catch (Exception e) {
				log("error adding observations to map " + e.getMessage());
			}
		}

		try {

			// System.out.println("adding current conditions to map: " +
			// currentConditionRecents.size());
			// Add Current Conditions
			for (CbCurrentCondition condition : currentConditionRecents) {

				LatLng point = new LatLng(
						condition.getLocation().getLatitude(), condition
								.getLocation().getLongitude());
				LayerDrawable drLayer = getCurrentConditionDrawable(condition,
						null);

				Drawable draw = getSingleDrawable(drLayer);

				Bitmap image = drawableToBitmap(draw, null);

				Marker marker = mMap.addMarker(new MarkerOptions().position(
						point).icon(BitmapDescriptorFactory.fromBitmap(image)));
				marker.showInfoWindow();

				currentCur++;
				if (currentCur > totalEachAllowed) {
					break;
				}
			}
		} catch (Exception e) {
			log("error adding conditions to map " + e.getMessage());
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
				- (int) ((.5 * 60 * 60 * 1000));
		long endTime = System.currentTimeMillis();
		CbApiCall api = new CbApiCall();

		api.setMinLat(loc.getLatitude() - .1);
		api.setMaxLat(loc.getLatitude() + .1);
		api.setMinLon(loc.getLongitude() - .1);
		api.setMaxLon(loc.getLongitude() + .1);
		api.setStartTime(startTime);
		api.setEndTime(endTime);
		api.setLimit(100);
		return api;
	}

	private CbApiCall buildMapAPICall(double hoursAgo) {
		// TODO: Don't override hoursAgo. One method for map overlays
		// and one for graph generation; map overlays is static 1 hour ago
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

	private CbApiCall buildMapCurrentConditionsCall(double hoursAgo) {
		System.out.println("building map conditions call for hours: "
				+ hoursAgo);
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
			log("no map center, bailing on condition api");
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
		if (mBound) {
			Message msg = Message.obtain(null, CbService.MSG_SET_SETTINGS,
					activeSettings);
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
		CbApiCall api = buildMapAPICall(1);
		askForRecents(api);
		askForCurrentConditionRecents(api);
	}

	// Stop listening to the barometer when our app is paused.
	@Override
	protected void onPause() {
		super.onPause();
		unBindCbService();
		stopGettingLocations();
		stopSensorListeners();
	}

	// Register a broadcast listener
	@Override
	protected void onResume() {
		super.onResume();

		checkNetwork();
		displayNetworkOfflineToast();

		getStoredPreferences();

		addDataToMap();

		startSensorListeners();
		startGettingLocations();

		startCbService();
		bindCbService();

		editLocation.setText("");
	}

	@Override
	protected void onStart() {
		dataReceivedToPlot = false;
		// bindCbService();
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	protected void onStop() {
		dataReceivedToPlot = false;
		unBindCbService();
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	protected void onDestroy() {
		dataReceivedToPlot = false;
		unBindCbService();
		stopGettingLocations();
		super.onDestroy();
	}

	private String displayPressureValue(double value) {
		DecimalFormat df = new DecimalFormat("####.0");
		PressureUnit unit = new PressureUnit(preferencePressureUnit);
		unit.setValue(value);
		unit.setAbbreviation(preferencePressureUnit);
		double pressureInPreferredUnit = unit.convertToPreferredUnit();
		return df.format(pressureInPreferredUnit) + " " + unit.fullToAbbrev();
	}

	private String displayTemperatureValue(double value) {
		DecimalFormat df = new DecimalFormat("##.0");
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

	/**
	 * Display visible reading to the user
	 */
	private void updateVisibleReading() {
		preferencePressureUnit = getUnitPreference();
		preferenceTemperatureUnit = getTempUnitPreference();

		if (recentPressureReading != 0.0) {
			String toPrint = displayPressureValue(recentPressureReading);
			if (toPrint.length() > 2) {
				buttonBarometer.setText(toPrint);
				ActionBar bar = getActionBar();
				bar.setTitle(toPrint);
				int actionBarTitleId = getResources().getSystem()
						.getIdentifier("action_bar_title", "id", "android");
				TextView actionBarTextView = (TextView) findViewById(actionBarTitleId);
				actionBarTextView.setTextColor(Color.WHITE);
			} else {
				buttonBarometer.setText("No barometeer detected.");
			}
		} else {
			buttonBarometer.setText("No barometer detected.");
		}

		// TODO: fix default value hack
		if (recentTemperatureReading != 1000) {
			String toPrint = displayTemperatureValue(recentTemperatureReading);
			buttonThermometer.setText(toPrint);
		} else {
			buttonThermometer.setText("No thermometer detected.");
		}

		if (recentHumidityReading != 1000) {
			String toPrint = displayHumidityValue(recentHumidityReading);
			buttonHygrometer.setText(toPrint);

		} else {
			buttonHygrometer.setText("No hygrometer detected.");
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
			//logToFile(text);
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
				sm.registerListener(this, pressureSensor,
						SensorManager.SENSOR_DELAY_UI);
			} else {
				recentPressureReading = 0.0;
			}
			if (temperatureSensor != null) {
				sm.registerListener(this, temperatureSensor,
						SensorManager.SENSOR_DELAY_UI);
			} else {
				recentTemperatureReading = 1000.0;
			}
			if (humiditySensor != null) {
				sm.registerListener(this, humiditySensor,
						SensorManager.SENSOR_DELAY_UI);
			} else {
				recentHumidityReading = 1000.0;
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	public void stopSensorListeners() {
		sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sm.unregisterListener(this);
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
