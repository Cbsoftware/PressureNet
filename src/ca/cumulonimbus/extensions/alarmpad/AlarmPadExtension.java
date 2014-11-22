package ca.cumulonimbus.extensions.alarmpad;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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

	/**
	 * Communicate with CbService
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.v(TAG, "client says : service connected");
			mService = new Messenger(service);
			mBound = true;
			Message msg = Message.obtain(null, CbService.MSG_OKAY);

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

		askForCurrentConditionRecents();
	}
	
	/**
	 * Query the database for locally stored current conditions
	 * 
	 * @param api
	 */
	private void askForCurrentConditionRecents() {

		if (mBound) {
			Log.v(TAG, "asking for current conditions");
			CbApiCall api = new CbApiCall();
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
						Log.i(TAG, "conditions received");
						for (CbCurrentCondition c : receivedList) {
							
						}
						
						// Sets up the data to publish
						ExtensionData data = new ExtensionData()
								.visible(true)
								.icon(R.drawable.ic_notification)
								.statusToDisplay(getString(R.string.extension_description));

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
			default:
				Log.v(TAG, "received default message");
				super.handleMessage(msg);
			}
		}
	}
}
