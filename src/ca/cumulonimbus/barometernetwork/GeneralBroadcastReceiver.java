package ca.cumulonimbus.barometernetwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import ca.cumulonimbus.pressurenetsdk.CbService;

public class GeneralBroadcastReceiver extends BroadcastReceiver  {

	private Context mContext;
	private String mAppDir = "";
	
	private String ACTION_BOOT = "android.intent.action.BOOT_COMPLETED";
	private String QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON";
	
	
    @Override
    public void onReceive(Context context, Intent intent) {
    	mContext = context;
    	
    	if(intent.getAction().equals(ACTION_BOOT) || intent.getAction().equals(QUICKBOOT_POWERON)) {
    		try {
        		setUpFiles();
        	} catch (Exception e) {
        		log(e.getStackTrace().toString());
        	}
        	
        	try {
        		startCbService();
        	} catch (Exception e) {
        		log(e.getStackTrace().toString());
        	}
    	}
    	
    	
    }
    
    
	/**
	 * Initiate the CbService
	 */
	private void startCbService() {
		try {
			if(hasBarometer()) {
				log("broadcast receiver start cbservice");
				Intent serviceIntent = new Intent(mContext, CbService.class);
				mContext.startService(serviceIntent);
				log("generalbroadcastreceiver started service");
			} else {
				log("generalbroadcastreceiver detects no barometer, not starting service");
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
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
		if(PressureNETConfiguration.DEBUG_MODE) {
			System.out.println(message);
			//logToFile(message);
		}
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
			File homeDirectory = mContext.getExternalFilesDir(null);
			if (homeDirectory != null) {
				mAppDir = homeDirectory.getAbsolutePath();

			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}
}

