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
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;
import ca.cumulonimbus.barometernetwork.BarometerNetworkActivity.IncomingHandler;
import ca.cumulonimbus.pressurenetsdk.CbApiCall;
import ca.cumulonimbus.pressurenetsdk.CbCurrentCondition;
import ca.cumulonimbus.pressurenetsdk.CbObservation;
import ca.cumulonimbus.pressurenetsdk.CbScience;
import ca.cumulonimbus.pressurenetsdk.CbService;
import ca.cumulonimbus.pressurenetsdk.CbSettingsHandler;

public class WidgetButtonService extends Service implements SensorEventListener {
	
	private double mReading = 0.0;
	SensorManager sm;
	
	boolean running = false;
	
	boolean mIsBound = false;
	
	public static final String PREFS_NAME = "ca.cumulonimbus.barometernetwork_preferences";
	PressureUnit mUnit = new PressureUnit("mbar");
	
	private String localHistoryFile = "recent.txt";
	
	private String mAppDir = "";

	CbSettingsHandler activeSettings;
	ArrayList<CbObservation> listRecents = new ArrayList<CbObservation>();
	
	private Intent mIntent;
	
	// pressureNET 4.0
	// SDK communication
	boolean mBound;
	private Messenger mMessenger = new Messenger(new IncomingHandler());
	Messenger mService = null;
	
	public void unBindCbService() {
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}
	
	/**
	 * Get the settings from the Cb database
	 */
	private void askForSettings() {
		if (mBound) {
			log("asking for settings");

			Message msg = Message.obtain(null, CbService.MSG_GET_SETTINGS,
					0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				//e.printStackTrace();
			}
		} else {
			// log("error: not bound");
		}
	}
	
	private void sendSingleObservation() {
		if (mBound) {
			Message msg = Message.obtain(null, CbService.MSG_SEND_OBSERVATION, 0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				//e.printStackTrace();
			}
		} else {
			log("widget failed to send single obs; data management error: not bound");
		}
	}

	public void bindCbService() {
		log("widget bind cbservice");
		bindService(new Intent(getApplicationContext(), CbService.class),
				mConnection, Context.BIND_AUTO_CREATE);

	}
	
	/**
	 * Handle communication with CbService. Listen for messages
	 * and act when they're received, sometimes responding with answers.
	 * 
	 * @author jacob
	 *
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CbService.MSG_SETTINGS:
				activeSettings = (CbSettingsHandler) msg.obj;
				if (activeSettings != null) {
					log("widgetbuttonservice got settings, share level " + activeSettings.getShareLevel());
					log("Client Received from service "
							+ activeSettings.getServerURL());
				} else {
					log("settings null");
				}
				break;
			case CbService.MSG_LOCAL_RECENTS:
				ArrayList<CbObservation> recents = (ArrayList<CbObservation>) msg.obj;
				log("widget msg_local_recents received " + recents.size() + " mreading " + mReading);
				DecimalFormat df = new DecimalFormat("####.00");
				String message = "0.00";
				if(mReading>1) {
					message = df.format(mReading);
				} else {
					 try {
						 message = mIntent.getStringExtra("msg");
						 //Toast.makeText(getApplicationContext(), "msg: " + msg, Toast.LENGTH_SHORT).show();
					 } catch(NullPointerException e) {
						 log("widget tried mintent getstringextra, failed");
					 }
				}
				
				try {
					RemoteViews remoteView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.small_widget_layout);
					// TODO: fix ugly localization hack
					if(message.contains(",")) {
						message = message.replace(",", ".");
					}
					
					if(Double.parseDouble(message) != 0) {
						// send the reading
			
						// This is messy. Fix it.
						SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			    		String abbrev = settings.getString("units", "mbar"); 
			    		mUnit = new PressureUnit(abbrev);
			    		double val = Double.valueOf(message);
			    		mUnit.setValue(val);
			    		String toPrint = mUnit.getDisplayText();
			    		toPrint = toPrint.replace(" ", "\n");
						
			    		sendSingleObservation();
			    		//Toast.makeText(getApplicationContext(), "Submitting Barometer Reading", Toast.LENGTH_SHORT).show();
						
			    		remoteView.setTextViewText(R.id.widgetSmallText, toPrint);
						
						try {
							String tendency = CbScience.findApproximateTendency(recents);
							
							log("widget getting tendency, updating and sending: " + tendency + " from " + recents.size() + " recents");
							
							if(tendency.contains("Rising")) {
								remoteView.setInt(R.id.widget_tendency_image_up, "setVisibility", View.VISIBLE);
								remoteView.setInt(R.id.widget_tendency_image_down, "setVisibility", View.GONE);
								remoteView.setInt(R.id.widget_tendency_image_steady, "setVisibility", View.GONE);
							} else if(tendency.contains("Falling")) {
								remoteView.setInt(R.id.widget_tendency_image_up, "setVisibility", View.GONE);
								remoteView.setInt(R.id.widget_tendency_image_down, "setVisibility", View.VISIBLE);
								remoteView.setInt(R.id.widget_tendency_image_steady, "setVisibility", View.GONE);
							} else if(tendency.contains("Steady")) {
								remoteView.setInt(R.id.widget_tendency_image_up, "setVisibility", View.GONE);
								remoteView.setInt(R.id.widget_tendency_image_down, "setVisibility", View.GONE);
								remoteView.setInt(R.id.widget_tendency_image_steady, "setVisibility", View.VISIBLE);
							} else {
								remoteView.setInt(R.id.widget_tendency_image_up, "setVisibility", View.INVISIBLE);
								remoteView.setInt(R.id.widget_tendency_image_down, "setVisibility", View.GONE);
								remoteView.setInt(R.id.widget_tendency_image_steady, "setVisibility", View.GONE);
								//remoteView.setInt(R.id.widgetSmallSubmitButton, "setImageResource", R.drawable.widget_button_drawable);
								//remoteView.setFloat(R.id.widgetSmallSubmitButton, "setImageResource", R.drawable.widget_button_drawable);
								//remoteView.setTextViewText(R.id.widgetSmallText, toPrint + "\n" + "--");
							}
			
						} catch(Exception e) {
							//log("oy! " + e.getMessage());
						}
						
						AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
						ComponentName component = new ComponentName(getApplicationContext().getPackageName(), WidgetProvider.class.getName());    
						appWidgetManager.updateAppWidget(component, remoteView);
					} else {
						//log("widget value is 0.0, didn't update");
					}
				
				} catch(Exception e) {
					// :(
				}
				break;
			case CbService.MSG_CHANGE_NOTIFICATION:
				String change = (String) msg.obj;
				// TODO: handle change notification
			default:
				break;
			}
		}
	}
	
	/**
	 * Communicate with CbService
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			log("widget client says : service connected");
			mService = new Messenger(service);
			mBound = true;
			Message msg = Message.obtain(null, CbService.MSG_OKAY);
			log("widget client received " + msg.arg1 + " " + msg.arg2);
			askForLocalRecents(3);
			
			askForSettings();
		}

		
		
		public void onServiceDisconnected(ComponentName className) {
			log("widget client: service disconnected");
			mMessenger = null;
			mBound = false;
		}
	};

	private void startListening() {
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
	
	private void askForLocalRecents(int hoursAgo) {
		CbApiCall api = new CbApiCall();
		api.setMinLat(-90);
		api.setMaxLat(90);
		api.setMinLon(-180);
		api.setMaxLon(180);
		api.setStartTime(System.currentTimeMillis()
				- (hoursAgo * 60 * 60 * 1000));
		api.setEndTime(System.currentTimeMillis());

		Message msg = Message.obtain(null, CbService.MSG_GET_LOCAL_RECENTS,
				api);
		try {
			msg.replyTo = mMessenger;
			mService.send(msg);
		} catch (RemoteException e) {
			//e.printStackTrace();
		}

	}
	
	public void update(Intent intent, double reading) {
		log("widget binding to service (update call, reading " + reading + ")");
		mIntent = intent;
		bindCbService(); 
		// TODO: clean this up, (sometimes runs twice?)
		try {
			askForLocalRecents(3);
		} catch(Exception e) {
			log("no recents, exception");
			//e.printStackTrace();
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
