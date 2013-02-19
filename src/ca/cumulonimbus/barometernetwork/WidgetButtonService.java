package ca.cumulonimbus.barometernetwork;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Visibility;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;
import ca.cumulonimbus.barometernetwork.Unit;

public class WidgetButtonService extends Service implements SensorEventListener {
	
	private double mReading = 0.0;
	SensorManager sm;
	
	boolean running = false;
	
	boolean mIsBound = false;
	
	public static final String PREFS_NAME = "ca.cumulonimbus.barometernetwork_preferences";
	Unit mUnit = new Unit("mbar");
	
	private String localHistoryFile = "recent.txt";
	
	private String mAppDir = "";
	
	
	
	public void startListening() {
		try {
	    	sm = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
	    	Sensor bar = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
	    	log("widget start listening pressure");
	    	if(bar!=null) {
	        	running = sm.registerListener(this, bar, SensorManager.SENSOR_DELAY_NORMAL);
	        	//Toast.makeText(getApplicationContext(), "starting listener", Toast.LENGTH_SHORT).show();
	    	}
		} catch(Exception e) {
			Toast.makeText(getApplicationContext(), "error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
	
	public void update(Intent intent, double reading) {
		DecimalFormat df = new DecimalFormat("####.00");
		String msg = "0.00";
		if(reading>1) {
			msg = df.format(reading);
		} else {
			 try {
				 msg = intent.getStringExtra("msg");
				 //Toast.makeText(getApplicationContext(), "msg: " + msg, Toast.LENGTH_SHORT).show();
			 } catch(NullPointerException e) {
				 //
			 }
		}
		
		try {
			RemoteViews remoteView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.small_widget_layout);
			
			if(msg.contains(",")) {
				msg = msg.replace(",", ".");
			}
			
			if(Double.parseDouble(msg) != 0) {
				// send the reading
				try {
					doBindService();
				} catch (Exception e) {
				}
				
				// This is messy. Fix it.
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	    		String abbrev = settings.getString("units", "mbar"); 
	    		mUnit = new Unit(abbrev);
	    		double val = Double.valueOf(msg);
	    		mUnit.setValue(val);
	    		String toPrint = mUnit.getDisplayText();
	    		toPrint = toPrint.replace(" ", "\n");
				
				//Toast.makeText(getApplicationContext(), "Submitting Barometer Reading", Toast.LENGTH_SHORT).show();
				remoteView.setTextViewText(R.id.widgetSmallText, toPrint);
				
				// Tendency
				/*
				String tendencyType = getTendencyFromLocal();
				remoteView.setTextViewText(R.id.widgetTendencyText, tendencyType); 
				*/
	
				
				DBAdapter dbAdapter;
				try {
					dbAdapter = new DBAdapter(getApplicationContext());
					dbAdapter.open();
					ArrayList<BarometerReading> recents = new ArrayList<BarometerReading>();
					recents = dbAdapter.fetchRecentReadings(5); // the last few hours
					// String tendency = ScienceHandler.findTendency(recents);
					ScienceHandler science = new ScienceHandler(mAppDir);
					String tendency = science.findApproximateTendency(recents);
					
					log("widget getting tendency, updating and sending");
					
					if(tendency.contains("Rising")) {
						remoteView.setInt(R.id.widget_tendency_image_up, "setVisibility", View.VISIBLE);
						remoteView.setInt(R.id.widget_tendency_image_down, "setVisibility", View.INVISIBLE);
						//remoteView.setInt(R.id.widget_tendency_image, "setGravity", Gravity.TOP);
						//remoteView.setTextViewText(R.id.widgetSmallText, toPrint + "\n" + "rising");
					} else if(tendency.contains("Falling")) {
						remoteView.setInt(R.id.widget_tendency_image_up, "setVisibility", View.INVISIBLE);
						remoteView.setInt(R.id.widget_tendency_image_down, "setVisibility", View.VISIBLE);
						//remoteView.setInt(R.id.widget_tendency_image, "setGravity", Gravity.BOTTOM);
						//remoteView.setTextViewText(R.id.widgetSmallText, toPrint + "\n" + "falling");
					} else if(tendency.contains("Steady")) {
						remoteView.setInt(R.id.widget_tendency_image_up, "setVisibility", View.INVISIBLE);
						remoteView.setInt(R.id.widget_tendency_image_down, "setVisibility", View.INVISIBLE);
						//remoteView.setTextViewText(R.id.widgetSmallText, toPrint + "\n" + "steady");
					} else {
						//remoteView.setInt(R.id.widgetSmallSubmitButton, "setImageResource", R.drawable.widget_button_drawable);
						//remoteView.setFloat(R.id.widgetSmallSubmitButton, "setImageResource", R.drawable.widget_button_drawable);
						//remoteView.setTextViewText(R.id.widgetSmallText, toPrint + "\n" + "--");
					}
	
					dbAdapter.close();
				} catch(Exception e) {
					System.out.println("oy! " + e.getMessage());
				}
				
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
				ComponentName component = new ComponentName(getApplicationContext().getPackageName(), WidgetProvider.class.getName());    
				appWidgetManager.updateAppWidget(component, remoteView);
			}
		
		} catch(Exception e) {
			// :(
		}
			
	}
	
	public ArrayList<BarometerReading> getBRsFromFileContents(String content) {
		ArrayList<BarometerReading> list = new ArrayList<BarometerReading>();
		try {
			String[] arr = content.split("\n");
			for(String a : arr) {
				BarometerReading br = new BarometerReading();
				String[] parts = a.split(",");
				String reading = parts[0];
				String latitude = parts[1];
				String longitude = parts[2];
				String time = parts[3];
				br.setReading(Double.parseDouble(reading));
				br.setLatitude(Double.parseDouble(latitude));
				br.setLongitude(Double.parseDouble(longitude));
				br.setTime(Double.parseDouble(time));
				list.add(br);
			}
		} catch(Exception e) { 
			
		}
		return list;
	}
	
	public ArrayList<BarometerReading> readLocalHistory() {
		ArrayList<BarometerReading> historyList = new ArrayList<BarometerReading>();
		String fileHistoryContents = "";
		try {
			BufferedInputStream input = new BufferedInputStream(new FileInputStream(mAppDir + "/" + localHistoryFile));
			DataInputStream dis = new DataInputStream(input);
			while(input.available() != 0) {	
				fileHistoryContents += dis.readLine();
			}
		} catch (FileNotFoundException fnfe) {
			
		} catch (IOException ioe) {
		
		}
		historyList = getBRsFromFileContents(fileHistoryContents);
		
		
		return historyList;
	}
	
	// Get tendency from information in local file
	/*
	private String getTendencyFromLocal() {
		ArrayList<BarometerReading> recents = readLocalHistory();
		// sort, pull ,
		return "";
	}
	*/
	
	// From Google. Connect to the SubmitReadingService and
	// send off a reading.
	private SubmitReadingService mBoundService;
	
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to a explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
	    	try {
	    		mBoundService = ((SubmitReadingService.LocalBinder)service).getService();
	    		mBoundService.sendBarometerReading();
	    		
	    	} catch(Exception e) {
	    		
	    	}
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
	        mBoundService = null;

	    }
	};

	void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because we want a specific service implementation that
	    // we know will be running in our own process (and thus won't be
	    // supporting component replacement by other applications).
		try {
		    bindService(new Intent(getApplicationContext(), SubmitReadingService.class), mConnection, Context.BIND_AUTO_CREATE);
		    mIsBound = true;
		} catch(Exception e) {
			
		}
	}

	void doUnbindService() {
	    if (mIsBound) {
	        // Detach our existing connection.
	        unbindService(mConnection);
	        mIsBound = false;
	    }
	}

	@Override
	public void onDestroy() {
	    super.onDestroy();
	    doUnbindService();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		try {
			mAppDir = intent.getStringExtra("appdir"); 
			
		} catch(Exception e) {
			
		}
		
		startListening();
		update(intent,0.1);
		super.onStart(intent, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
	

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_PRESSURE: 
			mReading = event.values[0];
			log("widget sensor changed " + mReading + " and unregistering");
			update(new Intent(), mReading);
			sm.unregisterListener(this);
			mReading = 0.0;
		    break;
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
	
}
