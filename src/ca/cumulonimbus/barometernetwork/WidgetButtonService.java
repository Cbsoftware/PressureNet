package ca.cumulonimbus.barometernetwork;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;
import ca.cumulonimbus.pressurenetsdk.CbObservation;

public class WidgetButtonService extends Service implements SensorEventListener {
	
	private double mReading = 0.0;
	SensorManager sm;
	
	boolean running = false;
	
	boolean mIsBound = false;
	
	public static final String PREFS_NAME = "ca.cumulonimbus.barometernetwork_preferences";
	PressureUnit mUnit = new PressureUnit("mbar");
	
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
	
				// This is messy. Fix it.
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	    		String abbrev = settings.getString("units", "mbar"); 
	    		mUnit = new PressureUnit(abbrev);
	    		double val = Double.valueOf(msg);
	    		mUnit.setValue(val);
	    		String toPrint = mUnit.getDisplayText();
	    		toPrint = toPrint.replace(" ", "\n");
				
				//Toast.makeText(getApplicationContext(), "Submitting Barometer Reading", Toast.LENGTH_SHORT).show();
				remoteView.setTextViewText(R.id.widgetSmallText, toPrint);
				
				try {
					ArrayList<CbObservation> recents = new ArrayList<CbObservation>();
					// String tendency = ScienceHandler.findTendency(recents);
					
					String tendency = ""; //science.findApproximateTendency(recents);
					
					log("widget getting tendency, updating and sending: " + tendency);
					
					if(tendency.contains("Rising")) {
						remoteView.setInt(R.id.widget_tendency_image_up, "setVisibility", View.VISIBLE);
						remoteView.setInt(R.id.widget_tendency_image_down, "setVisibility", View.GONE);
						//remoteView.setInt(R.id.widget_tendency_image, "setGravity", Gravity.TOP);
						//remoteView.setTextViewText(R.id.widgetSmallText, toPrint + "\n" + "rising");
					} else if(tendency.contains("Falling")) {
						remoteView.setInt(R.id.widget_tendency_image_up, "setVisibility", View.GONE);
						remoteView.setInt(R.id.widget_tendency_image_down, "setVisibility", View.VISIBLE);
						//remoteView.setInt(R.id.widget_tendency_image, "setGravity", Gravity.BOTTOM);
						//remoteView.setTextViewText(R.id.widgetSmallText, toPrint + "\n" + "falling");
					} else if(tendency.contains("Steady")) {
						remoteView.setInt(R.id.widget_tendency_image_up, "setVisibility", View.GONE);
						remoteView.setInt(R.id.widget_tendency_image_down, "setVisibility", View.GONE);
						//remoteView.setTextViewText(R.id.widgetSmallText, toPrint + "\n" + "steady");
					} else {
						//remoteView.setInt(R.id.widgetSmallSubmitButton, "setImageResource", R.drawable.widget_button_drawable);
						//remoteView.setFloat(R.id.widgetSmallSubmitButton, "setImageResource", R.drawable.widget_button_drawable);
						//remoteView.setTextViewText(R.id.widgetSmallText, toPrint + "\n" + "--");
					}
	
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
	
	@Override
	public void onDestroy() {
	    super.onDestroy();
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
    	//System.out.println(text);
    	//logToFile(text);
    }
	
}
