package ca.cumulonimbus.extensions.alarmpad;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import ca.cumulonimbus.barometernetwork.R;
import ca.cumulonimbus.pressurenetsdk.CbApiCall;
import ca.cumulonimbus.pressurenetsdk.CbCurrentCondition;
import ca.cumulonimbus.pressurenetsdk.CbService;

import com.mindmeapp.extensions.ExtensionData;
import com.mindmeapp.extensions.MindMeExtension;

public class AlarmPadExtension extends MindMeExtension {

	public static final String TAG = "AlarmPadExtension";

	boolean mBound;
	private Messenger mMessenger = new Messenger(new IncomingHandler());
	Messenger mService = null;

	private CbCurrentCondition alarmCondition = null;
	
	/**
	 * Communicate with CbService
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.v(TAG, "client says : service connected");
			mService = new Messenger(service);
			mBound = true;
			Message msg = Message.obtain(null, CbService.MSG_OKAY);
			
			CbApiCall conditionApi = buildMapCurrentConditionsCall(2);
			makeCurrentConditionsAPICall(conditionApi);
			
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.v(TAG, "client: service disconnected");
			mMessenger = null;
			mBound = false;
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		bindService(new Intent(getApplicationContext(), CbService.class),
				mConnection, Context.BIND_AUTO_CREATE);
	}
	
	

	@Override
	public void onDestroy() {
		unbindService(mConnection);
		super.onDestroy();
	}



	@Override
	protected void onUpdateData(int reason) {
		Log.v(TAG, "onUpdateData() - reason: " + reason);

		if (reason == MindMeExtension.UPDATE_REASON_MANUAL
				|| reason == MindMeExtension.UPDATE_REASON_SETTINGS_CHANGED) {
			// Manual updates are called every time AlarmPad needs to update its PressureNet cache.
			// In this case the extension should call publishUpdate() to let AlarmPad know about
			// new data available before showing it on an alarm screen.
			
			updateAlarmPadCache();
			
			//We do not stop the service here because we need to wait for the results of API calls
		} else if (reason == MindMeExtension.UPDATE_REASON_PERIODIC) {
			// Periodic updates are called every time an alarm with this extension selected rings
			// Calling publishUpdate() here has no effect on the current alarm ringing as it will
			// use previously cached data. Use this if you want to run any background process when
			// an alarm rings

			// TODO
			
			
			stopSelf();
		}

	}

	private void updateAlarmPadCache() {

		// Update RemoteViews layout that will be displayed on AlarmPad
//		RemoteViews rv = new RemoteViews(this.getPackageName(),
//				R.layout.horoscope_info);
//
//		rv.setTextViewText(R.id.textViewTitle, horoscopeTitle);
//		rv.setTextViewText(R.id.textViewDescription, horoscopeDescription);

		
		if(!mBound) {
			Log.v(TAG, "updating alarmpadcache, not bound so binding");
			
			bindService(new Intent(getApplicationContext(), CbService.class),
					mConnection, Context.BIND_AUTO_CREATE);
		} else {
			
			// askForCurrentConditionRecents();
			// Sets up the data to publish
			CbApiCall conditionApi = buildMapCurrentConditionsCall(2);
			makeCurrentConditionsAPICall(conditionApi);
			
			if(alarmCondition!=null) {
				Log.v(TAG, "update cache with condition " + alarmCondition.getGeneral_condition());
				ExtensionData data = new ExtensionData()
						.visible(true)
						.icon(R.drawable.ic_notification)
						.statusToDisplay(getString(R.string.extension_description));
	
				// Publish the extension data update
				publishUpdate(data);
				stopSelf();
			} else {
				Log.v(TAG, "condition is null");
			}
		}
		
	

	}
	
	private CbApiCall buildMapCurrentConditionsCall(double hoursAgo) {
		Log.v(TAG, "alarmpad building map conditions call for hours: " + hoursAgo);
		long startTime = System.currentTimeMillis()
				- (int) ((hoursAgo * 60 * 60 * 1000));
		long endTime = System.currentTimeMillis();
		CbApiCall api = new CbApiCall();

		LocationManager lm = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		Location loc = lm
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		
		
		double minLat = loc.getLatitude() - 0.1;
		double maxLat = loc.getLatitude() + 0.1;
		double minLon = loc.getLongitude() - 0.1;
		double maxLon = loc.getLongitude() + 0.1;

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
	
	private void makeCurrentConditionsAPICall(CbApiCall apiCall) {
		Log.v(TAG, "alarmpad making current conditions api call");
		if (mBound) {
			Message msg = Message.obtain(null,
					CbService.MSG_MAKE_CURRENT_CONDITIONS_API_CALL, apiCall);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
				
			} catch (RemoteException e) {
				// e.printStackTrace();
			}
		} else {
			System.out
					.println("alarmpad data management error: not bound for condition api");
		}
	}
	
	/**
	 * Query the database for locally stored current conditions
	 * 
	 * @param api
	 */
	private void askForCurrentConditionRecents() {

		if (mBound) {
			Log.v(TAG, "alarmpad asking for current conditions");
			CbApiCall api = buildMapCurrentConditionsCall(2);
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
	 * Handle communication with CbService. Listen for messages and act when they're received, sometimes responding with answers.
	 * 
	 * @author jacob
	 * 
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CbService.MSG_CURRENT_CONDITIONS:
				ArrayList<CbCurrentCondition> receivedList = (ArrayList<CbCurrentCondition>) msg.obj;
				if (receivedList != null) {
					if (receivedList.size() > 0) {
						Log.i(TAG, "alarmpad conditions received");
						for (CbCurrentCondition c : receivedList) {
							alarmCondition = c;
							Log.v(TAG, "app received " + c.getGeneral_condition());
						}
						
						/*
						// Sets up the data to publish
						ExtensionData data = new ExtensionData()
								.visible(true)
								.icon(R.drawable.ic_notification)
								.statusToDisplay(getString(R.string.extension_description));
						*/
						// Sets up the data to publish
						ExtensionData data = new ExtensionData()
								.visible(true)
								.icon(R.drawable.ic_launcher)
								.statusToDisplay(receivedList.get(0).getGeneral_condition());

						// Publish the extension data update
						publishUpdate(data);
						stopSelf();
					} else {
						Log.w(TAG, "app received conditions size 0");
					}

				} else {
					Log.e(TAG, "app received null conditions");
					break;
				}

				break;
			case CbService.MSG_API_RESULT_COUNT:
				int count = msg.arg1;
				Log.v(TAG, "api res count, gonna ask for recents , count " + count);
				askForCurrentConditionRecents();
				break;
			default:
				Log.v(TAG, "received default message");
				super.handleMessage(msg);
			}
		}
	}
}
