package ca.cumulonimbus.barometernetwork;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import ca.cumulonimbus.barometernetwork.BarometerNetworkActivity.IncomingHandler;
import ca.cumulonimbus.pressurenetsdk.CbApiCall;
import ca.cumulonimbus.pressurenetsdk.CbObservation;
import ca.cumulonimbus.pressurenetsdk.CbService;
import ca.cumulonimbus.pressurenetsdk.CbSettingsHandler;

public class DataManagementActivity extends Activity {

	Button buttonClearCache;
	
	boolean mBound;
	Messenger mService = null;

	private Messenger mMessenger = new Messenger(new IncomingHandler());
	
	private void clearLocalCache() {
		if (mBound) {
			Message msg = Message.obtain(null, CbService.MSG_CLEAR_LOCAL_CACHE,
					0, 0);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("error: not bound");
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
				e.printStackTrace();
			}
		} else {
			System.out.println("error: not bound");
		}
	}

	private void makeCurrentConditionsAPICall(CbApiCall apiCall) {
		apiCall.setCallType("Conditions");
		Toast.makeText(getApplicationContext(), "Starting Conditions API call",
				Toast.LENGTH_SHORT).show();
		if (mBound) {
			Message msg = Message.obtain(null, CbService.MSG_MAKE_CURRENT_CONDITIONS_API_CALL,
					apiCall);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("data management error: not bound for condition api");
		}
	}
	
	
	private void makeAPICall(CbApiCall apiCall) {
		apiCall.setCallType("Readings");
		Toast.makeText(getApplicationContext(), "Starting API call",
				Toast.LENGTH_SHORT).show();
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
			System.out.println("data management error: not bound for API");
		}
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			mBound = true;
			Message msg = Message.obtain(null, CbService.MSG_OKAY);
			System.out.println("dm bound");
		}

		public void onServiceDisconnected(ComponentName className) {
			mMessenger = null;
			mBound = false;
		}
	};


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

		buttonClearCache = (Button) findViewById(R.id.buttonClearCache);
		buttonClearCache.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				clearAPICache();
				clearLocalCache();

			}
		});

			
		bindCbService();
	}
	
	

	@Override
	protected void onPause() {
		unBindCbService();
		super.onPause();
	}



	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CbService.MSG_API_RESULT_COUNT: 
				int count = msg.arg1;
				Toast.makeText(getApplicationContext(), count + " API results cached", Toast.LENGTH_SHORT).show();
				
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

}
