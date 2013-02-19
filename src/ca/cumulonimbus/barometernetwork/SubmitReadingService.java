package ca.cumulonimbus.barometernetwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings.Secure;
import android.widget.Toast;

public final class SubmitReadingService extends Service implements SensorEventListener {

	private static final String PREFS_NAME = "ca.cumulonimbus.barometernetwork_preferences";
	public static String ACTION_SUBMIT_SINGLE = "SubmitSingle";
	private Handler mHandler = new Handler();
	private long mSeconds; // Frequency
	private boolean mUpdateServerAutomatically;
    private String mUpdateServerFrequency;
	private SensorManager sm;
    private double mReading;
	private double mLatitude;
	private double mLongitude;
	
	private DBAdapter dbAdapter;
	
	private SubmitReadingService that = this;
	
	private String mAppDir = "";
	private String android_id;
	private boolean showToast = false;
	private String serverURL = PressureNETConfiguration.SERVER_URL;
	private static ArrayList<BarometerReading> submitList = new ArrayList<BarometerReading>();
	private boolean barometerReadingsActive = false;
	private long lastSubmitTime;
	private boolean waitingForReading = false;
	private long lastLocationSuccess = 0; 
	private long lastPressureSuccess = 0;
	

    // Used to write a log to SD card. Not used unless logging enabled.
    public void setUpFiles() {
    	try {
	    	File homeDirectory = getExternalFilesDir(null);
	    	if(homeDirectory!=null) {
	    		mAppDir = homeDirectory.getAbsolutePath();
	    	}
	    	log("setting up logging");
    	} catch (Exception e) {
    		//log(e.getMessage());
    	}
    }
    
	
	private boolean networkOnline() {
		try {
			// Test connectivity before attempting to send readings
			//log("testing network conn man");
			final ConnectivityManager connManager =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			//log("testing network active net");
			final NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
			//log("testing state");
			if (activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED) {
				//log("network on");
				return true;
			} else {
				//log("network off");
			    return false;
			}
		} catch(Exception e) {
			log(e.getMessage());
			return false;
		}
	}
	
	LocationManager locationManager;
	LocationListener locationListener;
    // Get the user's location from the location service, preferably GPS.
    public void getLocationInformation() {
    	log("service get location information");
    	// get the location
    	// Acquire a reference to the system Location Manager
    	locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

    	// Check when we last got a location successfully. Too recently? Don't bother checking. 
    	long now = System.currentTimeMillis();
    	long difference = now - lastLocationSuccess;
    	log("service location time check difference " + difference);
    	
    	// Five Seconds Maximum
    	if(difference > 1000*5) {
	    	// Define a listener that responds to location updates
	    	locationListener = new LocationListener() {
	    	    public void onLocationChanged(Location location) {
	    	        // Called when a new location is found by the network location provider.
	    	    	try {
		    	    	double latitude = location.getLatitude();
		    	    	double longitude = location.getLongitude();
		    	    	
		    	    	// no changes. stop for now.
		    	    	if(mLatitude == latitude) {
		    	    		if (mLongitude == longitude) {
		    	    			log("service pausing location lookup");
		    	    			locationManager.removeUpdates(locationListener);
		    	    		}
		    	    	}
		    	    	
		    	    	mLatitude = latitude;
		    	    	mLongitude = longitude;
		    	    	log("new location " + latitude + " " + longitude);
		    	    	lastLocationSuccess = System.currentTimeMillis();
		    	    	// now stop. we'll start again next time.
		    	    	//locationManager.removeUpdates(locationListener);
		    	    	locationManager.removeUpdates(this);
		    	    	
	    	    	} catch(Exception e) {
	    	    		log("service location exception " + e.getMessage());
	    	    	}
	    	    }
	
	    	    public void onStatusChanged(String provider, int status, Bundle extras) {}
	
	    	    public void onProviderEnabled(String provider) {}
	
	    	    public void onProviderDisabled(String provider) {}
	    	  };
	
	    	// Register the listener with the Location Manager to receive location updates
	    	try {
	    		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
	    	} catch(Exception e) {
	    		
	    	}
	    	
    	}
    	
    }
    
    // Start getting barometer readings.
    public void setUpBarometer() {
    	try {
    		log("setting up barometer");
	    	sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	    	Sensor bar = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
	    	
	    	if(bar!=null) {
	    		barometerReadingsActive = sm.registerListener(this, bar, SensorManager.SENSOR_DELAY_UI);

	    	} else {
	    		log("bar is null");
	    	}
    	} catch(Exception e) {
    		log("setupbarometer exception: " + e.getMessage());
    	}
    }
    
    public void setId() {
    	try {
    		MessageDigest md = MessageDigest.getInstance("MD5");
    		
    		String actual_id = Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
    		byte[] bytes = actual_id.getBytes();
    		byte[] digest = md.digest(bytes);
    		StringBuffer hexString = new StringBuffer();
    		for(int i = 0; i< digest.length; i++) {
    			hexString.append(Integer.toHexString(0xFF & digest[i]));
    		}
    		android_id = hexString.toString();
    	} catch(Exception e) {
    		log(e.getMessage());
    	}
    }
	
    public List<NameValuePair> barometerReadingToNVP(BarometerReading br) {
    	List<NameValuePair> nvp = new ArrayList<NameValuePair>();
    	nvp.add(new BasicNameValuePair("latitude",br.getLatitude() + ""));
    	nvp.add(new BasicNameValuePair("longitude",br.getLongitude() + ""));
    	nvp.add(new BasicNameValuePair("reading",br.getReading() + ""));
    	nvp.add(new BasicNameValuePair("time",br.getTime() + ""));
    	nvp.add(new BasicNameValuePair("tzoffset",br.getTimeZoneOffset() + ""));
    	nvp.add(new BasicNameValuePair("text",br.getAndroidId() + ""));
    	nvp.add(new BasicNameValuePair("share", br.getSharingPrivacy() + ""));
    	nvp.add(new BasicNameValuePair("client_key", getApplicationContext().getPackageName()));
    	return nvp;
    }
    
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        SubmitReadingService getService() {
            return SubmitReadingService.this;
        }
    }

    public void setUpDatabase() {
    	dbAdapter = new DBAdapter(this);
    	dbAdapter.open();
    }
    
	public void addToLocalDatabase(BarometerReading br) {
		try {
			dbAdapter = new DBAdapter(this);
	    	dbAdapter.open();
	    	dbAdapter.addReading(br.getReading(), br.getLatitude(), br.getLongitude(), br.getTime(), br.getSharingPrivacy());
	    	dbAdapter.close();
		} catch(Exception e) {
			
		}
	}
    
    private class ReadingSender extends AsyncTask<String, Integer, Long> {
		@Override
		protected Long doInBackground(String... arg0) {
			log("service readingsender do in bg");
			if((mLatitude == 0.0) || (mLongitude == 0.0) || (mReading == 0.0)) {
				return null;
			}
			
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			String share = settings.getString("sharing_preference", "Us, Researchers and Forecasters");
			
			// No sharing? get out!
			if(share.equals("Nobody")) {
				return null;
			}
			
			// don't submit too frequently (once per minute)
			long limit = 1000 * 59; 
			if(System.currentTimeMillis() - lastSubmitTime < limit) {
				log("service submit too frequent; cancelling this submit");
				return null;
			}
			
			lastSubmitTime = System.currentTimeMillis();
			
	    	BarometerReading br = new BarometerReading();
	    	br.setLatitude(mLatitude);
	    	br.setLongitude(mLongitude);
	    	br.setTime(Calendar.getInstance().getTimeInMillis());
	    	br.setTimeZoneOffset(Calendar.getInstance().getTimeZone().getOffset((long)br.getTime()));
	    	br.setReading(mReading);
	    	br.setAndroidId(android_id);
	    	br.setSharingPrivacy(share);
	    	
	    	// check for active network. if connected, send everything in the queue as 
	    	// well as the current reading. if not connected, add to the queue
	    	if(networkOnline()) {
	    		log("network is online");
		    	DefaultHttpClient client = new SecureHttpClient(getApplicationContext());
		    	HttpPost httppost = new HttpPost(serverURL);
		    	try {
		    		// all in the queue
		    		while(!submitList.isEmpty()) {
		    			log("reading sender emptying queue " + submitList.size());
			    		BarometerReading reading =  submitList.get(0);
			    		submitList.remove(0);
			    		List<NameValuePair> nvps = barometerReadingToNVP(reading);
			    		httppost.setEntity(new UrlEncodedFormEntity(nvps));
			    		
			    		HttpResponse response = client.execute(httppost);
		    		}
		    		
		    		log("service sender posting current " + mReading);
		    		// current reading
		    		List<NameValuePair> nvps = barometerReadingToNVP(br);
		    		httppost.setEntity(new UrlEncodedFormEntity(nvps));
		    		HttpResponse response = client.execute(httppost);
		    		// Until we implement tendencies, don't write to the local file
		    		addToLocalDatabase(br);
		    		
		    		try {
		    			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		    			locationManager.removeUpdates(locationListener);
		    		} catch(Exception e) {
		    			log(e.getMessage());
		    		}
		    		
		    	} catch(ClientProtocolException cpe) {
		    		try {
		    			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		    			locationManager.removeUpdates(locationListener);
		    		} catch(Exception e) {
		    			log(e.getMessage());
		    		}
		    		log(cpe.getMessage());
		    	} catch(IOException ioe) {
		    		try {
		    			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		    			locationManager.removeUpdates(locationListener);
		    		} catch(Exception e) {
		    			log(e.getMessage());
		    		}
		    		log(ioe.getMessage());
		    	}
	    	} else {
	    		// Offline. Add to queue
	    		try {
	    			log("service offline. adding to queue " + mReading + " of size " + submitList.size());
	    			submitList.add(br);
	    			try {
		    			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		    			locationManager.removeUpdates(locationListener);
		    		} catch(Exception e) {
		    			log(e.getMessage());
		    		}
	    		} catch(Exception e) {
	    			log(e.getMessage());
	    		}
	    	}
	    	return null;
		}
    	
		protected void onPostExecute(Long result) {
			if(showToast) {
				Toast.makeText(getApplicationContext(), "Sent!", Toast.LENGTH_SHORT).show();
				showToast = false;
			}
		}
    }
    
    private Runnable mWaitForBarometer = new Runnable() {
    	public void run() {
    		new ReadingSender().execute("");
    	}
    };
    
    public void sendBarometerReading() {
    	log("send barometer reading");
    	try {
	    	getLocationInformation();
	    	setUpBarometer();
	    	setId();
	    	
	    	if(mReading == 0.0) {
	    		//log("active barometer: " + barometerReadingsActive);
	    	} else {
				log("rs attempt: " + mLatitude + " " + mLongitude + ": " + mReading);
				new ReadingSender().execute("");
				
				//sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
				//sm.unregisterListener(that);
		    }
    	} catch(Exception e) {
    		log("service send barometer reading exception " + e.getMessage());
    	}
    }
    
	private Runnable mSubmitReading = new Runnable() {
		
		public void run() {
			long base = SystemClock.uptimeMillis();
			waitingForReading = true;
			sendBarometerReading();
			
    		
			mHandler.postAtTime(this, base + (mSeconds * 1000));
		}
	};
	
	// Send data every x seconds, depending on user preference.
	private void sendDataPeriodically() {
		log("send periodically");
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	mUpdateServerAutomatically = settings.getBoolean("autoupdate", true);
        mUpdateServerFrequency = settings.getString("autofrequency", "10 minutes");    
       
	    if(mUpdateServerAutomatically) {
	    	mSeconds = convertSettingsTextToSeconds(mUpdateServerFrequency);
	    	mHandler.removeCallbacks(mSubmitReading);
	    	mHandler.postDelayed(mSubmitReading, 0);
	    }
	}
	
	// Log data to SD card for debug purposes.
	// To enable logging, ensure the Manifest allows writing to SD card.
	public void logToFile(String text) {
		try {
			OutputStream output = new FileOutputStream(mAppDir + "/log.txt", true);
			String logString = (new Date()).toString() + ": " + text + "\n";
			output.write(logString.getBytes());
			output.close();
			
		} catch(FileNotFoundException e) {
			
		} catch(IOException ioe) {
			
		}
	}
	
    public void log(String text) {
    	System.out.println(text);
    	logToFile(text);
    }
	
	private long convertSettingsTextToSeconds(String text) {
		int quantity = Integer.parseInt(text.split(" ")[0]);
		String unit = text.split(" ")[1];
		int multiplier = 60*60;
		long seconds;
		if(unit.contains("min")) {
			multiplier = 60;
		} else if(unit.contains("hour")) {
			multiplier = 60*60;
		} else if(unit.contains("day")) {
			multiplier = 60*60*24;
		} else if(unit.contains("week")) {
			multiplier = 60*60*24*7;
		}
		
		seconds = quantity * multiplier;
		return seconds;
	}
	
	@Override
	public void onCreate() {
		log("service oncreate");
		try {
			setUpDatabase();
		} catch(Exception e) {
			
		}
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		log("service ondestroy");
		// stop listening for pressure readings
		// and location readings
		try {
			sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			sm.unregisterListener(that);
			locationManager.removeUpdates(locationListener);
		} catch(Exception e) {
			
		}
		
		try {
			dbAdapter.close();
		} catch (Exception e) {
			
		}
		super.onDestroy();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		log("service unbinding, closing db");
		try {
			dbAdapter.close();
		} catch(Exception e) {
			
		}
		return super.onUnbind(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("service on start command");
		try {
			setUpFiles(); // start logs
			setUpDatabase();
		} catch(Exception e) {
			
		}
		sendDataPeriodically();
		super.onStartCommand(intent, flags, startId);
		return START_STICKY; //super.onStartCommand(intent, flags, startId);
	}

	private final IBinder mBinder = new LocalBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		log("service on bind");

		return mBinder;
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_PRESSURE: 
			// Log
			long now = System.currentTimeMillis();
			long difference = now - lastPressureSuccess;
			log("service pressure change difference " + difference);
			if(difference > 1000) {
			
				mReading = event.values[0];
				
				log("new service sensor reading: " + mReading);
				lastPressureSuccess = System.currentTimeMillis();
				
				sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
				sm.unregisterListener(that);
				waitingForReading = false;
			}
		    break;
	    }		
	}
}
