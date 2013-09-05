package ca.cumulonimbus.barometernetwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import ca.cumulonimbus.barometernetwork.BarometerNetworkActivity.IncomingHandler;
import ca.cumulonimbus.pressurenetsdk.CbApiCall;
import ca.cumulonimbus.pressurenetsdk.CbCurrentCondition;
import ca.cumulonimbus.pressurenetsdk.CbObservation;
import ca.cumulonimbus.pressurenetsdk.CbService;
import ca.cumulonimbus.pressurenetsdk.CbSettingsHandler;

public class GeneralBroadcastReceiver extends BroadcastReceiver  {

	private Context mContext;
	private String mAppDir = "";
	
	// pressureNET 4.0
	// SDK communication
	boolean mBound;
	private Messenger mMessenger = new Messenger(new IncomingHandler());
	Messenger mService = null;
	
	/**
	 * Handle incoming communication from CbService.
	 * 
	 * @author jacob
	 *
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			
		}
	}
	
    @Override
    public void onReceive(Context context, Intent intent) {
    	mContext = context;
    	
    	try {
    		setUpFiles();
    		startCbService();
    	} catch (Exception e) {
    		log(e.getStackTrace().toString());
    	}
    }
    
	/**
	 * Initiate the CbService
	 */
	private void startCbService() {
		try {
			if(hasBarometer()) {
				log("start cbservice");
				Intent serviceIntent = new Intent(mContext, CbService.class);
				mContext.startService(serviceIntent);
				bindCbService();
				log("generalbroadcastreceiver started service");
			} else {
				log("generalbroadcastreceiver detects no barometer, not starting service");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			log("general broadcast receiver says : service connected");
			mService = new Messenger(service);
			mBound = true;
			Message msg = Message.obtain(null, CbService.MSG_OKAY);
			log("general broadcast receiver received " + msg.arg1 + " " + msg.arg2);
			sendChangeNotification();
		}
		
		/**
		 * Send a message with a good replyTo for the Service to send notifications through.
		 */
		private void sendChangeNotification() {
			if (mBound) {
				log("send change notif request");

				Message msg = Message.obtain(null, CbService.MSG_CHANGE_NOTIFICATION,
						0,0);
				try {
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			} else {
				//log("error: not bound");
			}			
		}

		public void onServiceDisconnected(ComponentName className) {
			log("client: service disconnected");
			mMessenger = null;
			mBound = false;
		}
	};
	
	public void unBindCbService() {
		if (mBound) {
			mContext.unbindService(mConnection);
			mBound = false;
		}
	}

	public void bindCbService() {
		log("bind cbservice");
		mContext.bindService(new Intent(mContext, CbService.class),
				mConnection, Context.BIND_AUTO_CREATE);

	}

	/**
	 * Check if we have a barometer. Use info to disable menu items,
	 * choose to run the service or not, etc.
	 */
	private boolean hasBarometer() {
		PackageManager packageManager = mContext.getPackageManager();
		return packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER);
	}
	
	private void log(String message) {
		System.out.println(message);
		logToFile(message);
	}
	
	/** 
	 * Log data to SD card for debug purposes.
	 * To enable logging, ensure the Manifest allows writing to SD card.
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
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Prepare to write a log to SD card. Not used unless logging enabled.
	 */
	private void setUpFiles() {
		try {
			File homeDirectory = mContext.getExternalFilesDir(null);
			if (homeDirectory != null) {
				mAppDir = homeDirectory.getAbsolutePath();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

