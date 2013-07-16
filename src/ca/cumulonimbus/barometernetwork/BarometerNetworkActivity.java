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
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

public class BarometerNetworkActivity extends Activity implements
		SensorEventListener {

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

	private ArrayList<CbObservation> uniqueRecents = new ArrayList<CbObservation>();
	private ArrayList<CbObservation> listRecents = new ArrayList<CbObservation>();
	private ArrayList<CbObservation> fullRecents = new ArrayList<CbObservation>();
	private ArrayList<CbObservation> globalMapRecents = new ArrayList<CbObservation>();
	private ArrayList<CbCurrentCondition> currentConditionRecents = new ArrayList<CbCurrentCondition>();
	private ArrayList<CbCurrentCondition> currentConditionAnimation = new ArrayList<CbCurrentCondition>();

	
	boolean dataReceivedToPlot = false;

	private SeekBar seekTime;
	private ImageButton buttonPlay;
	private Button buttonBarometer;
	private Spinner spinnerTime;
	private int hoursAgoSelected = 1;

	private Button mapMode;
	private Button animationMode;
	private Button graphMode;
	private Button sensorMode;
	
	private LinearLayout layoutAnimationControlContainer;
	private LinearLayout layoutMapInfo;
	private LinearLayout layoutGraph;
	private LinearLayout layoutSensors;
	
	private TextView mapInfoText;
	private ImageButton buttonSearchLocations;
	
	private TextView textAnimationInformation;
	
	Handler timeHandler = new Handler();
	Handler mapDelayHandler = new Handler();

	String apiServerURL = "https://pressurenet.cumulonimbus.ca/list/?";

	private int currentTimeProgress = 0;
	private boolean animateState = false;
	private boolean graphVisible = false;

	double recentPressureReading = 0.0;
	private final int TYPE_AMBIENT_TEMPERATURE = 13;
	private final int TYPE_RELATIVE_HUMIDITY = 12;

	public static final int REQUEST_SETTINGS = 1;
	public static final int REQUEST_LOCATION_CHOICE = 2;
	public static final int REQUEST_MAILED_LOG = 3;

	boolean activeAnimation = false;

	private boolean pressureReadingsActive = false;
	private boolean humidityReadingsActive = false;
	private boolean temperatureReadingsActive = false;

	
	/**
	 * preferences
	 */
	private String preferenceUnit;
	private String preferenceCollectionFrequency;
	private boolean preferenceShareData;
	private String preferenceShareLevel;

	private GoogleMap mMap;
	private LatLngBounds visibleBound;

	// Search Locations
	private ImageButton buttonGoLocation;
	private EditText editLocation;

	private ArrayList<SearchLocation> searchedLocations = new ArrayList<SearchLocation>();

	private long lastMapMove = System.currentTimeMillis();
	private long lastMapDataUpdate = System.currentTimeMillis();
	
	private String activeMode = "map";
	private long lastGlobalApiCall = System.currentTimeMillis() - (1000 * 60 * 10);
	private long lastGraphApiCall = System.currentTimeMillis() - (1000 * 60 * 10);
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dataReceivedToPlot = false;
		setContentView(R.layout.main);
		// migratePreferences();
		startLog();
		getStoredPreferences();
		setUpMap();
		setUpUIListeners();
		setId();
		setUpFiles();
		showWelcomeActivity();
		setUpActionBar();
		startCbService();
		bindCbService();
		startSensorListeners();
	}

	/**
	 * Get fresh data for each of the user's saved locations
	 */
	public void makeLocationAPICalls() {
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
			System.out.println("making api call for " + name + " at "
					+ latitude + " " + longitude);
			makeAPICall(locationApiCall);
		}
		pn.close();
	}
	

	/**
	 * Get fresh data for the global map
	 */
	public void makeGlobalMapCall() {
		long currentTime = System.currentTimeMillis();
		if(currentTime - lastGlobalApiCall > (1000 * 60 * 5)) {
			
			CbApiCall globalMapCall = buildMapAPICall(.2);
			globalMapCall.setMinLat(-90);
			globalMapCall.setMaxLat(90);
			globalMapCall.setMinLon(-180);
			globalMapCall.setMaxLon(180);
			globalMapCall.setLimit(2000);
			makeAPICall(globalMapCall);
			lastGlobalApiCall = currentTime;
		} 
	}

	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the
		// map.
		if (mMap == null) {
			mMap = ((MapFragment) getFragmentManager().findFragmentById(
					R.id.map)).getMap();

			mMap.getUiSettings().setZoomControlsEnabled(false);
			
			mMap.setOnMarkerClickListener(new OnMarkerClickListener() {
				
				@Override
				public boolean onMarkerClick(Marker arg0) {
					lastMapDataUpdate = System.currentTimeMillis();
					return false;
				}
			});
			
			mMap.setOnCameraChangeListener(new OnCameraChangeListener() {
				
				
				
				@Override
				public void onCameraChange(CameraPosition position) {
					// change button ability based on zoom level
					if(position.zoom >= 10) {
						graphMode.setEnabled(true);
						graphMode.setTextColor(Color.BLACK);
					} else {
						graphMode.setEnabled(false);
						graphMode.setTextColor(Color.GRAY);
					}
					
					// dismiss the keyboard
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(editLocation.getWindowToken(),
							0);

					LatLngBounds bounds = mMap.getProjection()
							.getVisibleRegion().latLngBounds;
					visibleBound = bounds;

					if(activeMode.equals("graph")) {
						mapMode.performClick();
						/*
						makeMapApiCallAndLoadRecents();
						CbApiCall api = buildMapAPICall(hoursAgoSelected);
						api.setApiName("list");
						api.setLimit(2000);
						askForRecents(api);
						
						createAndShowChart();
						addDataToMap(false);
						*/
					} else if(activeMode.equals("animation")) {
						CbApiCall api = buildMapAPICall(1);
						askForCurrentConditionAnimation(api);
						addDataToMap(true);
					} else if (activeMode.equals("map")) {
						makeMapApiCallAndLoadRecents();
					} else {
						
					}
					
					updateMapInfoText();
				}
			});
			
			

			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				// The Map is verified. It is now safe to manipulate the map.
			}
		}

	}

	// Zoom into the user's location, add pinch zoom controls
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
						new LatLng(loc.getLatitude(), loc.getLongitude()), 13));
				updateMapInfoText();
			} else {

			}

		} catch (Exception e) {

		}

	}

	public void moveMapTo(double latitude, double longitude) {
		setUpMapIfNeeded();

		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
				.getMap();
		try {
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
					latitude, longitude), 13));
			updateMapInfoText();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Check the Android SharedPreferences for important values. Save relevant
	 * ones to CbSettings for easy access in submitting readings
	 */
	public void getStoredPreferences() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		preferenceUnit = sharedPreferences.getString("units", "millibars");
		preferenceCollectionFrequency = sharedPreferences.getString(
				"autofrequency", "10 minutes");
		preferenceShareData = sharedPreferences.getBoolean("autoupdate", true);
		preferenceShareLevel = sharedPreferences.getString(
				"sharing_preference", "Us, Researchers and Forecasters");

		CbSettingsHandler settings = new CbSettingsHandler(
				getApplicationContext());
		settings.setSharingData(preferenceShareData);
		settings.setDataCollectionFrequency(CbService.stringTimeToLongHack(preferenceCollectionFrequency));
		settings.setShareLevel(preferenceShareLevel);
		settings.saveSettings();
		log("saved new settings; sharing " + preferenceShareLevel);

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

	Runnable apiCallRunnable = new Runnable() {
		@Override
		public void run() {
			System.out.println("making api call (runnable)");
			CbApiCall api = buildMapAPICall(.2);
			api = roundApiCallLocations(api);
			makeAPICall(api);
		}
	};

	Runnable animate = new Runnable() {
		@Override
		public void run() {
			mMap.clear();
			activeAnimation = true;
			if (currentTimeProgress <= 100) {

				currentTimeProgress++;
				seekTime.setProgress(currentTimeProgress);
				updateMapWithSeekTimeData();
				timeHandler.postDelayed(animate, 50);
			} else {
				Drawable play = getResources().getDrawable(
						R.drawable.ic_menu_light_play);
				buttonPlay.setImageDrawable(play);
				animateState = false;
				currentTimeProgress = 0;

				activeAnimation = false;
				addDataToMap(true);
			}

		}
	};

	/**
	 * Round the api call location values to improve performance (caching)
	 * 
	 * @param rawApi
	 * @return
	 */
	public CbApiCall roundApiCallLocations(CbApiCall rawApi) {
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

	// TODO: clean up
	public boolean isCloseToFrame(int groupNumber, int currentTimeProgress) {
		if (currentTimeProgress >= groupNumber) {
			if(currentTimeProgress - groupNumber < 10) {
				return true;
			}
		}
		return false;
	}

	public void updateMapWithSeekTimeData() {

		ArrayList<CbWeather> thisFrameCondition = new ArrayList<CbWeather>();
		for (CbCurrentCondition c : currentConditionAnimation) {
			if (isCloseToFrame(c.getAnimateGroupNumber(), currentTimeProgress)) {
				thisFrameCondition.add(c);
			} else {

			}
		}

		ArrayList<CbWeather> thisFrameObservation = new ArrayList<CbWeather>();		
		/*
		System.out.println("full recents count " + fullRecents.size());
		for (CbObservation o : fullRecents) {
			if (isCloseToFrame(o.getAnimateGroupNumber(), currentTimeProgress)) {
				thisFrameObservation.add(o);
			} else {

			}
		}
		*/

		addDataFrameToMap(thisFrameCondition, thisFrameObservation);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	}
	
	private void setUpUIListeners() {
		Context context = getApplicationContext();
		mInflater = LayoutInflater.from(context);
		spinnerTime = (Spinner) findViewById(R.id.spinnerChartTime);
		buttonPlay = (ImageButton) findViewById(R.id.buttonPlay);
		seekTime = (SeekBar) findViewById(R.id.seekBarTime);
		buttonBarometer = (Button) findViewById(R.id.imageButtonBarometer);

		buttonGoLocation = (ImageButton) findViewById(R.id.buttonGoLocation);
		editLocation = (EditText) findViewById(R.id.editGoLocation);

		mapMode = (Button) findViewById(R.id.buttonMapMode);
		animationMode = (Button) findViewById(R.id.buttonAnimationMode);
		graphMode = (Button) findViewById(R.id.buttonGraphMode);
		sensorMode = (Button) findViewById(R.id.buttonSensorMode);
		
		
		layoutAnimationControlContainer = (LinearLayout) findViewById(R.id.layoutAnimationControlContainer);
		layoutMapInfo = (LinearLayout) findViewById(R.id.layoutMapInformation);
		layoutGraph = (LinearLayout) findViewById(R.id.layoutGraph);
		layoutSensors = (LinearLayout) findViewById(R.id.layoutSensorInfo);
		
		mapInfoText = (TextView) findViewById(R.id.textMapInfo);
		
		buttonSearchLocations = (ImageButton) findViewById(R.id.buttonSearchLocations);
		
		ArrayAdapter<CharSequence> adapterTime = ArrayAdapter
				.createFromResource(this, R.array.display_time_chart,
						android.R.layout.simple_spinner_item);
		adapterTime
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerTime.setAdapter(adapterTime);
		spinnerTime.setSelection(0);
		seekTime.setProgress(100);

		textAnimationInformation = (TextView) findViewById(R.id.textAnimationInformation);
		
		mapMode.setTypeface(null, Typeface.BOLD);
		
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
				if(activeMode.equals("map")) {
					int visible = layoutMapInfo.getVisibility();
					if(visible == View.VISIBLE) {
						layoutMapInfo.setVisibility(View.GONE);
					} else {
						layoutMapInfo.setVisibility(View.VISIBLE);						
					}
				} else {
					// UI switch
					layoutAnimationControlContainer.setVisibility(View.GONE);
					layoutGraph.setVisibility(View.GONE);
					layoutMapInfo.setVisibility(View.VISIBLE);
					layoutSensors.setVisibility(View.GONE);
					
					mapMode.setTypeface(null, Typeface.BOLD);
					graphMode.setTypeface(null, Typeface.NORMAL);
					animationMode.setTypeface(null, Typeface.NORMAL);
					sensorMode.setTypeface(null, Typeface.NORMAL);
					
					
					// set mode and load data
					activeMode = "map";
					addDataToMap(false);
				}
				
				
			}
		});

		animationMode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(activeMode.equals("animation")) {
					int visible = layoutAnimationControlContainer.getVisibility();
					if(visible == View.VISIBLE) {
						layoutAnimationControlContainer.setVisibility(View.GONE);
					} else {
						layoutAnimationControlContainer.setVisibility(View.VISIBLE);						
					}
					
				} else {
					mMap.clear();
					
					CbApiCall api = buildMapAPICall(24 * 7);
					askForCurrentConditionAnimation(api);
					
					activeMode = "animation";
					
					layoutAnimationControlContainer.setVisibility(View.VISIBLE);
					layoutGraph.setVisibility(View.GONE);
					layoutMapInfo.setVisibility(View.GONE);
					layoutSensors.setVisibility(View.GONE);
					
					mapMode.setTypeface(null, Typeface.NORMAL);
					graphMode.setTypeface(null, Typeface.NORMAL);
					animationMode.setTypeface(null, Typeface.BOLD);
					sensorMode.setTypeface(null, Typeface.NORMAL);

				}
			}
		});

		graphMode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(activeMode.equals("graph")) {
					int visible = layoutGraph.getVisibility();
					if (visible == View.VISIBLE) {
						layoutGraph.setVisibility(View.GONE);
					} else {
						layoutGraph.setVisibility(View.VISIBLE);
					}
				} else {
					activeMode = "graph";
					
					spinnerTime.setSelection(0);
					hoursAgoSelected = 1;

					CbApiCall apiGraph = buildMapAPICall(hoursAgoSelected);
					apiGraph.setLimit(500);
					askForUniqueRecents(apiGraph);
					
					
					// don't make this call repeatedly if user keeps tapping
					// say, five second limit?
					long currentTime = System.currentTimeMillis();
					if(currentTime - lastGraphApiCall > (1000 * 5)) {
						System.out.println("making api call 3h for graph");
						CbApiCall api = buildMapAPICall(3);
						//api.setApiName("list");
						api.setLimit(500);
						makeAPICall(api);
						
						lastGraphApiCall = 	currentTime;
					}
					
					layoutAnimationControlContainer.setVisibility(View.GONE);
					layoutGraph.setVisibility(View.VISIBLE);
					layoutMapInfo.setVisibility(View.GONE);
					layoutSensors.setVisibility(View.GONE);
					
					mapMode.setTypeface(null, Typeface.NORMAL);
					graphMode.setTypeface(null, Typeface.BOLD);
					animationMode.setTypeface(null, Typeface.NORMAL);
					sensorMode.setTypeface(null, Typeface.NORMAL);

				}
			}
		});

		
		sensorMode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(activeMode.equals("sensors")) {
					int visible = layoutSensors.getVisibility();
					if(visible == View.VISIBLE) {
						layoutSensors.setVisibility((View.GONE));
					} else {
						layoutSensors.setVisibility(View.VISIBLE);
					}
				} else {
					activeMode = "sensors";
					
					// UI switch
					layoutAnimationControlContainer.setVisibility(View.GONE);
					layoutGraph.setVisibility(View.GONE);
					layoutMapInfo.setVisibility(View.GONE);
					layoutSensors.setVisibility(View.VISIBLE);
					
					mapMode.setTypeface(null, Typeface.NORMAL);
					graphMode.setTypeface(null, Typeface.NORMAL);
					animationMode.setTypeface(null, Typeface.NORMAL);
					sensorMode.setTypeface(null, Typeface.BOLD);

				}
	
			}
		});

		editLocation.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				editLocation.setText("");
			}
		});

		buttonGoLocation.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(buttonSearchLocations.getVisibility() == View.INVISIBLE) {
					buttonSearchLocations.setVisibility(View.VISIBLE);
					editLocation.setVisibility(View.VISIBLE);
					return;
				}
				
				String location = editLocation.getEditableText().toString();
				if (location.equals("")) {
					return;
				}
				location = location.trim();
				Toast.makeText(getApplicationContext(), "Going to " + location,
						Toast.LENGTH_SHORT).show();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(editLocation.getWindowToken(), 0);
				Geocoder geocode = new Geocoder(getApplicationContext());
				try {
					List<Address> addr = geocode.getFromLocationName(location,
							1);
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

						CbApiCall conditionApi = buildMapCurrentConditionsCall(72);
						makeCurrentConditionsAPICall(conditionApi);
					}

				} catch (IOException ioe) {
					ioe.printStackTrace();
				}

			}
		});

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
				if (fromUser) {

					currentTimeProgress = progress;
					updateMapWithSeekTimeData();
				}

			}
		});

		buttonPlay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				animateState = !animateState;
				if (animateState == true) {
					Drawable pause = getResources().getDrawable(
							R.drawable.ic_menu_light_pause);
					buttonPlay.setImageDrawable(pause);
				} else {
					timeHandler.removeCallbacks(animate);
					Drawable play = getResources().getDrawable(
							R.drawable.ic_menu_light_play);
					buttonPlay.setImageDrawable(play);
					return;
				}
				
				textAnimationInformation.setText(currentConditionAnimation.size() + " data points (last week)");
				
				// currentTimeProgress = 0;
				seekTime.setProgress(currentTimeProgress);

				long msAgoSelected = (int) (7 * 24 * 60 * 60 * 1000);
				long singleTimeSpan = msAgoSelected / 100;

				long universalStartTime = System.currentTimeMillis()
						- msAgoSelected;

				if (currentConditionAnimation.size() > 1) {
					Collections.sort(currentConditionAnimation,
							new CbScience.ConditionTimeComparator());

					for (CbCurrentCondition c : currentConditionAnimation) {
						long time = c.getTime();
						int group = (int) ((time - universalStartTime) / singleTimeSpan);
						System.out.println("cond group " + group + " for time "
								+ time);
						c.setAnimateGroupNumber(group);
					}

				}
				/*

				if (fullRecents.size() > 1) {
					Collections.sort(fullRecents,
							new CbScience.TimeComparator());

					for (CbObservation ob : fullRecents) {
						long time = ob.getTime();
						int group = (int) ((time - universalStartTime) / singleTimeSpan);
						System.out.println("rec group " + group + " for time "
								+ time);
						ob.setAnimateGroupNumber(group);
					}

				}
				*/

				timeHandler.postDelayed(animate, 0);
			}
		});

		spinnerTime.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				String selected = arg0.getSelectedItem().toString();
				// TODO: Fix hack
				if (selected.contains("1 hour")) {
					hoursAgoSelected = 1;
				} else if (selected.contains("3 hours")) {
					hoursAgoSelected = 3;
				} else if (selected.contains("6 hours")) {
					hoursAgoSelected = 6;
				} else if (selected.contains("12 hours")) {
					hoursAgoSelected = 12;
				} 
				log("selected " + hoursAgoSelected + " hours ago");
				CbApiCall api = buildMapAPICall(hoursAgoSelected);
				//api.setApiName("list");
				api.setLimit(500);
				askForRecents(api);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}

		});
	}

	/**
	 * Get basic information and format it to display on screen
	 * Show the map coordinates, number of unique data points visible, etc
	 */
	private void updateMapInfoText() {
		String updatedText = "";
		DecimalFormat latlngFormat = new DecimalFormat("###.####");
		
		LatLng ne = visibleBound.northeast;
		LatLng sw = visibleBound.southwest;
		double maxLat = Math.max(ne.latitude, sw.latitude);
		double minLat = Math.min(ne.latitude, sw.latitude);
		double maxLon = Math.max(ne.longitude, sw.longitude);
		double minLon = Math.min(ne.longitude, sw.longitude);
		int visibleCount = uniqueRecents.size();
		
		String latitudeRange = "Lat: " + latlngFormat.format(minLat) + " to " + latlngFormat.format(maxLat);
		String longitudeRange = "Lon: " + latlngFormat.format(minLon) + " to " + latlngFormat.format(maxLon);
		
		updatedText = latitudeRange + "\n" + longitudeRange + "\n" + visibleCount + " data points";
		mapInfoText.setText(updatedText);
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

	private void askForCurrentConditionRecents(CbApiCall api) {

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
	
	private void askForCurrentConditionAnimation(CbApiCall api) {

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
	
	private void askForUniqueRecents(CbApiCall apiCall) {
		if (mBound) {
			log("asking for recents");

			Message msg = Message.obtain(null,
					CbService.MSG_GET_API_UNIQUE_RECENTS, apiCall);
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

	public void askForSettings() {
		if (mBound) {
			log("asking for settings");

			Message msg = Message.obtain(null, CbService.MSG_GET_SETTINGS,
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
				System.out.println("got settings, share level "
						+ activeSettings.getShareLevel());
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
				if(activeMode.equals("graph")) {
					listRecents.clear();
					listRecents = (ArrayList<CbObservation>) msg.obj;
					log("received " + listRecents.size() + " list recents");
					createAndShowChart();
				} else if (activeMode.equals("map")) {
					//globalMapRecents.clear();
					//globalMapRecents = (ArrayList<CbObservation>) msg.obj;
					//log("fetched global map 30 minutes, total size " + globalMapRecents.size());
					//addDataToMap(false);
				} else {
					//fullRecents.clear();
					//fullRecents = (ArrayList<CbObservation>) msg.obj;					
					//log("received " + fullRecents.size() + " full recents");
					//addDataToMap(false);
				}
				break;
			case CbService.MSG_API_RESULT_COUNT:
				int count = msg.arg1;
				//System.out.println("Call result: " + count + " API results cached");
				if(activeMode.equals("map")) {
					CbApiCall api = buildMapAPICall(.2);
					askForRecents(api);
				}
				break;
			case CbService.MSG_CURRENT_CONDITIONS:
				if (currentConditionRecents != null) {
					log("currentConditionRecents size "
							+ currentConditionRecents.size());
				} else {
					log("conditions ARE NuLL");
				}

				if(activeMode.equals("animation")) {
					currentConditionAnimation = (ArrayList<CbCurrentCondition>) msg.obj;
					addDataToMap(true);
				} else {
					currentConditionRecents = (ArrayList<CbCurrentCondition>) msg.obj;
					//addDataToMap(false);
				}
				break;
			case CbService.MSG_API_UNIQUE_RECENTS:
				uniqueRecents = (ArrayList<CbObservation>) msg.obj;
				if (uniqueRecents != null) {
					log("received " + uniqueRecents.size()
							+ " unique recent observations in buffer.");
					addDataToMap(false);
				} else {
					log("received unique recents: NULL");
				}
				dataReceivedToPlot = true;
				break;
			default:
				log("received default message");
				super.handleMessage(msg);
			}
		}
	}

	public void createAndShowChart() {
		if(!activeMode.equals("graph")) {
			log("createandshowchart called outside of graph mode");
			return;
		}
		if (listRecents == null) {
			log("list recents null RETURNING, no chart");
			return;
		} else if (listRecents.size() == 0) {
			log("list recents 0, RETURNING, no chart");
			return;
		}
	
		// draw chart
		log("plotting... " + listRecents.size());
		Chart chart = new Chart(getApplicationContext());
		
		// set units according to preference
		ArrayList<CbObservation> displayRecents = new ArrayList<CbObservation>();
		for(CbObservation ob : listRecents) {
			double rawValue = ob.getObservationValue();
			
			PressureUnit unit = new PressureUnit(preferenceUnit);
			unit.setValue(rawValue);
			unit.setAbbreviation(preferenceUnit);
			double pressureInPreferredUnit = unit
					.convertToPreferredUnit();
			
			ob.setObservationUnit(preferenceUnit);
			ob.setObservationValue(pressureInPreferredUnit);
			displayRecents.add(ob);
		}
		
		View chartView = chart.drawChart(displayRecents);

		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.layoutGraph);

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

		chartView.setBackgroundColor(Color.BLACK);
		chartView.setLayoutParams(lparams);
		if (mainLayout == null) {
			log("chartlayout null");
			return;
		}
		// TODO: bring the chart back
		mainLayout.addView(chartView);
	
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			log("client says : service connected");
			mService = new Messenger(service);
			mBound = true;
			Message msg = Message.obtain(null, CbService.MSG_OKAY);
			log("client received " + msg.arg1 + " " + msg.arg2);

			// makeLocationAPICalls();
			makeGlobalMapCall();
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
			editor.putString("autofrequency", autoFrequency);
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
		// TODO: implement
		// hide some menu items that are barometer-specific
		// menu.removeItem(R.id.menu_my_info);
		// menu.removeItem(R.id.menu_submit_reading);
		// menu.removeItem(R.id.menu_log_viewer);

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
			startActivity(intent);

		} else if (item.getItemId() == R.id.menu_about) {
			Intent intent = new Intent(getApplicationContext(), About.class);
			startActivity(intent);

		} 
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
				long rowId = data.getLongExtra("location_id", -1);
				if (rowId != -1) {
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
					}
					if (!search.equals("")) {
						editLocation.setText(search);
					}
					if (lat != 0) {
						moveMapTo(lat, lon);
					}
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	// Give a quick overview of recent
	// submissions
	public void viewLog() {
		try {
			Intent intent = new Intent(this, LogViewerActivity.class);
			startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
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

	/*
	 * // Custom map overlays for barometer readings public class MapOverlay
	 * extends ItemizedOverlay<OverlayItem> {
	 * 
	 * private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	 * Context mContext; private int mTextSize;
	 * 
	 * @Override protected boolean onTap(int index) { // TODO: Implement
	 * 
	 * return true; }
	 * 
	 * public MapOverlay(Drawable defaultMarker, Context context, int textSize)
	 * { super(boundCenterBottom(defaultMarker)); mContext = context; mTextSize
	 * = textSize; }
	 * 
	 * public MapOverlay(Drawable defaultMarker, Context context) {
	 * super(defaultMarker); mContext = context; }
	 * 
	 * public MapOverlay(Drawable defaultMarker) {
	 * super(boundCenterBottom(defaultMarker)); }
	 * 
	 * public void addOverlay(OverlayItem overlay) { mOverlays.add(overlay);
	 * populate(); }
	 * 
	 * public void removeOverlay(OverlayItem overlay) {
	 * mOverlays.remove(overlay); populate(); }
	 * 
	 * @Override protected OverlayItem createItem(int i) { return
	 * mOverlays.get(i); }
	 * 
	 * @Override public int size() { return mOverlays.size(); }
	 * 
	 * // Draw all the overlay data points onto the map. Include an icon as //
	 * well as
	 * 
	 * @Override public void draw(android.graphics.Canvas canvas, MapView
	 * mapView, boolean shadow) { shadow = false; super.draw(canvas, mapView,
	 * shadow);
	 * 
	 * if (shadow == false) { // cycle through all overlays for (int index = 0;
	 * index < mOverlays.size(); index++) { try { OverlayItem item =
	 * mOverlays.get(index);
	 * 
	 * // Converts lat/lng-Point to coordinates on the screen GeoPoint point =
	 * item.getPoint(); Point ptScreenCoord = new Point();
	 * mapView.getProjection().toPixels(point, ptScreenCoord);
	 * 
	 * // Paint Paint paint = new Paint();
	 * paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(mTextSize);
	 * paint.setShadowLayer(15, 5, 5, 0); paint.setARGB(255, 0, 0, 0); // alpha,
	 * r, g, b (Black, // semi see-through) paint.setAntiAlias(true);
	 * 
	 * // String toPrint = item.getTitle().substring(0, //
	 * item.getTitle().length() - 5); String toPrint =
	 * item.getTitle().split(" ")[0]; // Double value =
	 * Double.parseDouble(toPrint); // DecimalFormat df = new
	 * DecimalFormat("####.00"); // toPrint = df.format(value);
	 * 
	 * // show text to the right of the icon float textWidth =
	 * paint.measureText(toPrint); Paint bgPaint = new Paint();
	 * bgPaint.setColor(Color.WHITE);
	 * 
	 * Rect rect = new Rect((int) (ptScreenCoord.x - (textWidth / 2) - 2),
	 * ptScreenCoord.y, (int) (ptScreenCoord.x + (textWidth / 2) + 2),
	 * ptScreenCoord.y + mTextSize + 5);
	 * 
	 * if (toPrint.length() == 0) { canvas.drawRoundRect(new RectF(rect), 6, 6,
	 * bgPaint); } canvas.drawText(toPrint, ptScreenCoord.x, ptScreenCoord.y +
	 * mTextSize, paint); } catch (Exception e) { e.printStackTrace(); } } } } }
	 */

	// Assume that matching latitude and longitude can only be you.
	public boolean obsIsMe(CbObservation ob) {
		return ((ob.getUser_id().equals(android_id)));
	}

	public LayerDrawable getCurrentConditionDrawable(
			CbCurrentCondition condition, Drawable drawable) {

		Drawable weatherBackgroundDrawable = resizeDrawable(this.getResources()
				.getDrawable(R.drawable.bg_wea_square));

		if (condition.getGeneral_condition().equals(getString(R.string.sunny))) {
			Drawable sunDrawable = this.getResources().getDrawable(
					R.drawable.ic_wea_col_sun);
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
	public void addDataFrameToMap(ArrayList<CbWeather> frameConditions,
			ArrayList<CbWeather> frameObservations) {

		int totalEachAllowed = 30;
		int currentObs = 0;
		int currentCur = 0;
		try {
			// Add Barometer Readings and associated current Conditions

			// Add singleton Current Conditions
			for (CbWeather weather : frameConditions) {
				CbCurrentCondition condition = (CbCurrentCondition) weather;
				LatLng point = new LatLng(
						condition.getLocation().getLatitude(), condition
								.getLocation().getLongitude());
				LayerDrawable drLayer = getCurrentConditionDrawable(condition,
						null);

				Drawable draw = getSingleDrawable(drLayer);

				Bitmap image = drawableToBitmap(draw);

				mMap.addMarker(new MarkerOptions().position(point).icon(
						BitmapDescriptorFactory.fromBitmap(image)));

				currentCur++;
				if (currentCur > totalEachAllowed) {
					break;
				}
			}

			// Add Recent Readings
			Drawable drawable = this.getResources().getDrawable(
					R.drawable.ic_marker);
			System.out.println("frame observations " + frameObservations.size());
			for (CbWeather weatherObs : frameObservations) {
				CbObservation observation = (CbObservation) weatherObs;
				LatLng point = new LatLng(observation.getLocation()
						.getLatitude(), observation.getLocation()
						.getLongitude());

				Bitmap image = drawableToBitmap(drawable);

				mMap.addMarker(new MarkerOptions().position(point)
						.title(observation.getObservationValue() + "")
						.icon(BitmapDescriptorFactory.fromBitmap(image)));

				currentObs++;
				if (currentObs > totalEachAllowed) {
					break;
				}
			}

		} catch (Exception e) {
			log("add data error: " + e.getMessage());
		}
	}

	public static Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		}

		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}

	public Drawable getSingleDrawable(LayerDrawable layerDrawable) {

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
	public void addDataToMap(boolean onlyConditions) {
		// TODO: add delay so that the map isn't fully refreshed every touch
		
		int totalEachAllowed = 60;
		int currentObs = 0;
		int currentCur = 0;
		
		if(activeAnimation) {
			// map markers are handled by addDataFrameToMap for animations
			return; 
		}
		
		int maxUpdateFrequency = 1 * 1000; 
		long now = System.currentTimeMillis();
		if(now - lastMapDataUpdate > maxUpdateFrequency) {
			//System.out.println("clearing map");
			//mMap.clear();
			lastMapDataUpdate = now;
		} else {
			System.out.println("adding data, not clearing map " + (now - lastMapDataUpdate));
		}

		Drawable drawable = this.getResources().getDrawable(
				R.drawable.ic_marker);

		try {
			// Add Recent Readings
			if(!onlyConditions) {
				if(uniqueRecents.size()> 0 ) {
					mMap.clear();
					log("clearing map, adding new data");
				}
				
				for (CbObservation observation : uniqueRecents) {
					LatLng point = new LatLng(observation.getLocation()
							.getLatitude(), observation.getLocation()
							.getLongitude());
	
					Bitmap image = drawableToBitmap(drawable);
	
					String valueToPrint = displayPressureValue(observation.getObservationValue());
					
					mMap.addMarker(new MarkerOptions().position(point)
							.title(valueToPrint)
							.icon(BitmapDescriptorFactory.fromBitmap(image)));
	
					currentObs++;
					if (currentObs > totalEachAllowed) {
						break;
					}
				}
			}
			
			// Add Current Conditions
			for (CbCurrentCondition condition : currentConditionRecents) {

				LatLng point = new LatLng(
						condition.getLocation().getLatitude(), condition
								.getLocation().getLongitude());
				LayerDrawable drLayer = getCurrentConditionDrawable(condition,
						null);

				Drawable draw = getSingleDrawable(drLayer);

				Bitmap image = drawableToBitmap(draw);

				mMap.addMarker(new MarkerOptions().position(point).icon(
						BitmapDescriptorFactory.fromBitmap(image)));

				currentCur++;
				if (currentCur > totalEachAllowed) {
					break;
				}
			}
			
			updateMapInfoText();

		} catch (Exception e) {
			log("add data error: " + e.getMessage());
		}
	}

	/**
	 * 
	 * Users will save locations and we will cache data for those locations.
	 * Build a general API call to cache a location. Use /live/
	 * 
	 * @param locationRowId
	 * @return
	 */
	public CbApiCall buildSearchLocationAPICall(SearchLocation loc) {
		long startTime = System.currentTimeMillis()
				- (int) ((1 * 60 * 60 * 1000));
		long endTime = System.currentTimeMillis();
		CbApiCall api = new CbApiCall();

		api.setMinLat(loc.getLatitude() - .05);
		api.setMaxLat(loc.getLatitude() + .05);
		api.setMinLon(loc.getLongitude() - .05);
		api.setMaxLon(loc.getLongitude() + .05);
		api.setStartTime(startTime);
		api.setEndTime(endTime);
		api.setApiKey(PressureNETConfiguration.API_KEY);
		api.setLimit(5000);
		api.setApiName("live");
		return api;
	}

	public CbApiCall buildMapAPICall(double hoursAgo) {
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
		}

		api.setMinLat(minLat);
		api.setMaxLat(maxLat);
		api.setMinLon(minLon);
		api.setMaxLon(maxLon);
		api.setStartTime(startTime);
		api.setEndTime(endTime);
		api.setApiKey(PressureNETConfiguration.API_KEY);
		api.setLimit(500);
		api.setApiName("live");
		System.out.println("building call for time " + api.getStartTime() + " " + api.getEndTime());
		return api;
	}

	public CbApiCall buildMapCurrentConditionsCall(double hoursAgo) {
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
		api.setApiKey(PressureNETConfiguration.API_KEY);
		api.setLimit(500);
		api.setCallType("Conditions");
		return api;
	}

	public void makeAPICall(CbApiCall apiCall) {
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

	private void makeCurrentConditionsAPICall(CbApiCall apiCall) {
		if (mBound) {
			Message msg = Message.obtain(null,
					CbService.MSG_MAKE_CURRENT_CONDITIONS_API_CALL, apiCall);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			System.out
					.println("data management error: not bound for condition api");
		}
	}

	public void makeMapApiCallAndLoadRecents() {
		CbApiCall api = buildMapAPICall(.5);

		CbApiCall currentApi = buildMapCurrentConditionsCall(1);
		askForCurrentConditionRecents(currentApi);
		makeCurrentConditionsAPICall(currentApi);

		askForUniqueRecents(api);
	}

	// Stop listening to the barometer when our app is paused.
	@Override
	protected void onPause() {
		super.onPause();
		stopDataStream();
		unBindCbService();

		stopSensorListeners();
	}

	// Register a broadcast listener
	@Override
	protected void onResume() {
		super.onResume();
		bindCbService();
		

		getStoredPreferences();

		addDataToMap(false);
		editLocation.setText("");
		
		startSensorListeners();
		startDataStream();
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
	
	public String displayPressureValue(double value) {
		DecimalFormat df = new DecimalFormat("####.00");
		PressureUnit unit = new PressureUnit(preferenceUnit);
		unit.setValue(value);
		unit.setAbbreviation(preferenceUnit);
		double pressureInPreferredUnit = unit
				.convertToPreferredUnit();
		return df.format(pressureInPreferredUnit) + " " + unit.fullToAbbrev();
	}

	public void updateVisibleReading() {
		preferenceUnit = getUnitPreference();

		if (recentPressureReading != 0.0) {
			String toPrint = displayPressureValue(recentPressureReading);
			buttonBarometer.setText(toPrint);
			ActionBar bar = getActionBar();
			bar.setTitle(toPrint);
			int actionBarTitleId = getResources().getSystem().getIdentifier("action_bar_title", "id", "android");
			
			TextView actionBarTextView = (TextView)findViewById(actionBarTitleId); 
			actionBarTextView.setTextColor(Color.WHITE);
			
		} else {
			buttonBarometer.setText("No barometer detected.");
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

	public void startSensorListeners() {
		try {
			sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			Sensor pressureSensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
			Sensor temperatureSensor = sm
					.getDefaultSensor(TYPE_AMBIENT_TEMPERATURE);
			Sensor humiditySensor = sm.getDefaultSensor(TYPE_RELATIVE_HUMIDITY);

			if (pressureSensor != null) {
				pressureReadingsActive = sm.registerListener(this,
						pressureSensor, SensorManager.SENSOR_DELAY_UI);
			}
			if (temperatureSensor != null) {
				temperatureReadingsActive = sm.registerListener(this,
						temperatureSensor, SensorManager.SENSOR_DELAY_UI);
			}
			if (humiditySensor != null) {
				humidityReadingsActive = sm.registerListener(this,
						humiditySensor, SensorManager.SENSOR_DELAY_UI);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stopSensorListeners() {
		sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sm.unregisterListener(this);
		pressureReadingsActive = false;
		temperatureReadingsActive = false;
		humidityReadingsActive = false;
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
			// System.out.println("new app pressure reading " +
			// event.values[0]);
			recentPressureReading = event.values[0];
			updateVisibleReading();
		}
	}
}
