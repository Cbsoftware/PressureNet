package ca.cumulonimbus.barometernetwork;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import ca.cumulonimbus.pressurenetsdk.CbApiCall;
import ca.cumulonimbus.pressurenetsdk.CbConfiguration;
import ca.cumulonimbus.pressurenetsdk.CbObservation;
import ca.cumulonimbus.pressurenetsdk.CbService;

public class DataManagementActivity extends Activity {

	Button buttonExportMyData;
	Button buttonClearMyData;
	Button buttonAdvancedAccess;
	Button buttonClearCache;

	TextView textMyData;
	TextView textDataCache;

	boolean mBound;
	Messenger mService = null;

	private Messenger mMessenger = new Messenger(new IncomingHandler());
	
	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;
	
	private String mAppDir = "";
	
	private void clearLocalCache() {
		if (mBound) {
			Message msg = Message.obtain(null, CbService.MSG_CLEAR_LOCAL_CACHE,
					0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				//e.printStackTrace();
			}
		} else {
			log("dm error: not bound");
		}
	}

	public void getAllLocalObservations() {
		if (mBound) {
			CbApiCall api = new CbApiCall();
			api.setMinLat(-90);
			api.setMaxLat(90);
			api.setMinLon(-180);
			api.setMaxLon(180);
			api.setStartTime(0);
			api.setEndTime(System.currentTimeMillis());

			Message msg = Message.obtain(null, CbService.MSG_GET_LOCAL_RECENTS,
					api);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				//e.printStackTrace();
			}
		} else {
			log("dm getlocalobs error: not bound");
		}
	}
	
	public void log(String message) { 
		if(!PressureNETConfiguration.DEBUG_MODE) {
			System.out.println(message);
			logToFile(message);
		}
	}

	public void logToFile(String message ) {
		try {
			OutputStream output = new FileOutputStream(mAppDir + "/log.txt",
					true);
			String logString = (new Date()).toString() + ": " + message + "\n";
			output.write(logString.getBytes());
			output.close();
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
		} catch (IOException ioe) {
			//ioe.printStackTrace();
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
			//e.printStackTrace();
		}
	}
	
	private void askForCacheCounts() {
		if (mBound) {
			Message msg = Message.obtain(null, CbService.MSG_COUNT_API_CACHE,
					0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				//e.printStackTrace();
			}
		} else {
			//log("error: not bound");
		}
	}

	private void askForUserCounts() {
		if (mBound) {
			Message msg = Message.obtain(null, CbService.MSG_COUNT_LOCAL_OBS,
					0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				//e.printStackTrace();
			}
		} else {
			//log("error: not bound");
		}
	}

	private void clearAPICache() {
		if (mBound) {
			Message msg = Message.obtain(null, CbService.MSG_CLEAR_API_CACHE,
					0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				//e.printStackTrace();
			}
		} else {
			//log("error: not bound");
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			mBound = true;
			Message msg = Message.obtain(null, CbService.MSG_OKAY);
			//log("dm bound");
			askForUserCounts();
			askForCacheCounts();
		}

		public void onServiceDisconnected(ComponentName className) {
			mMessenger = null;
			mBound = false;
		}
	};

	@Override
	protected void onResume() {
		if (mBound) {
			askForUserCounts();
			askForCacheCounts();
		} else {
			bindCbService();
		}
		super.onResume();
	}

	public void unBindCbService() {
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}

	public void bindCbService() {
		bindService(new Intent(getApplicationContext(), CbService.class),
				mConnection, Context.BIND_AUTO_CREATE);

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.data_management);
		checkStorage();
		buttonExportMyData = (Button) findViewById(R.id.buttonExportMyData);
		buttonClearMyData = (Button) findViewById(R.id.buttonClearMyData);
		buttonAdvancedAccess = (Button) findViewById(R.id.buttonDataAccess);
		buttonClearCache = (Button) findViewById(R.id.buttonClearCache);

		textDataCache = (TextView) findViewById(R.id.textDataCacheDescription);
		textMyData = (TextView) findViewById(R.id.textMyDataDescription);

		ActionBar bar = getActionBar();
		int actionBarTitleId = getResources().getSystem().getIdentifier(
				"action_bar_title", "id", "android");

		TextView actionBarTextView = (TextView) findViewById(actionBarTitleId);
		actionBarTextView.setTextColor(Color.WHITE);

		buttonExportMyData.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getAllLocalObservations();
			}
		});

		buttonClearMyData.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clearLocalCache();
			}
		});

		buttonAdvancedAccess.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Open Live API sign up
				Uri uri = Uri
						.parse(CbConfiguration.API_SIGNUP_URL);
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
			}
		});

		buttonClearCache.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clearAPICache();
				Intent data = new Intent();
				if (getParent() == null) {
				    setResult(Activity.RESULT_OK, data);
				} else {
				    getParent().setResult(Activity.RESULT_OK, data);
				}
			}
		});

		bindCbService();
	
		setUpFiles();
	}

	@Override
	protected void onPause() {
		unBindCbService();
		super.onPause();
	}

	/**
	 * Check the available storage options.
	 * Used for logging to SD card.
	 */
	public void checkStorage() {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
	}
	
	/**
	 * Handle messages and communication with CbService
	 * 
	 * @author jacob
	 *
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			String measurement = "measurements.";
			switch (msg.what) {
			case CbService.MSG_COUNT_LOCAL_OBS_TOTALS:
				int count = msg.arg1;
				if(count==1) {
					measurement = "measurement.";
				}
				textMyData.setText("You have recorded and stored " + count
						+ " " + measurement);
				break;
			case CbService.MSG_COUNT_API_CACHE_TOTALS:
				int countCache = msg.arg1;
				measurement = "measurements";
				if(countCache==1) {
					measurement = "measurement";
				}
				textDataCache.setText("You have cached " + countCache
						+ " " + measurement + " from our servers.");
				break;
			case CbService.MSG_LOCAL_RECENTS:
				ArrayList<CbObservation> recents = (ArrayList<CbObservation>) msg.obj;

				log("dma receiving local recents size " + recents.size());
				
				
				if(mExternalStorageWriteable) {
					File export = new File(Environment.getExternalStorageDirectory(), "pressureNET-export.csv");
					try {
				        BufferedWriter out = new BufferedWriter(new FileWriter(export.getAbsolutePath(), false));
				        out.write("Time,Pressure,Latitude,Longitude\n");
				        for (CbObservation obs : recents) {
							String data = obs.getTime() + ","
									+ obs.getObservationValue() + ","
									+ obs.getLocation().getLatitude() + ","
									+ obs.getLocation().getLongitude() + "\n";
							out.write(data);
						}
				        out.close();

				    } catch (Exception e) {
				    	Toast.makeText(
								getApplicationContext(),
								"Error saving data."
										+ recents.size(), Toast.LENGTH_LONG).show();	
				    }
					
					Toast.makeText(
							getApplicationContext(),
							"Saved " 
									+ recents.size() + " data points to " + export.getAbsolutePath(), Toast.LENGTH_LONG).show();					
					
				} else {
					Toast.makeText(
							getApplicationContext(),
							"Error: Storage not available."
									+ recents.size(), Toast.LENGTH_LONG).show();					
				}

				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	/**
	 * Checks if external storage is available for read and write
	 * @return
	 */
	public boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}

}
