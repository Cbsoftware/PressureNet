package ca.cumulonimbus.barometernetwork;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.TextView;
import ca.cumulonimbus.pressurenetsdk.CbApiCall;
import ca.cumulonimbus.pressurenetsdk.CbObservation;
import ca.cumulonimbus.pressurenetsdk.CbService;

public class LogViewerActivity extends Activity {

	TextView logText;

	boolean mBound;
	Messenger mService = null;

	private Messenger mMessenger = new Messenger(new IncomingHandler());

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			mBound = true;
			Message msg = Message.obtain(null, CbService.MSG_OKAY);

			getRecents();
		}

		public void onServiceDisconnected(ComponentName className) {
			mMessenger = null;
			mBound = false;
		}
	};

	public void getRecents() {
		if (mBound) {
			CbApiCall api = new CbApiCall();
			api.setMinLat(-90);
			api.setMaxLat(90);
			api.setMinLon(-180);
			api.setMaxLon(180);
			api.setStartTime(0);
			api.setEndTime(System.currentTimeMillis());
			
			Message msg = Message.obtain(null,
					CbService.MSG_GET_LOCAL_RECENTS, api);
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

	
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CbService.MSG_LOCAL_RECENTS:
				ArrayList<CbObservation> recents = (ArrayList<CbObservation>) msg.obj;
				try {
					
					String rawLog = "";
					for (CbObservation obs : recents) {
						Calendar c = Calendar.getInstance();
						c.setTimeInMillis(obs.getTime());
						String dateString = c.getTime().toLocaleString();
						DecimalFormat df = new DecimalFormat("####.00");
						String valueString = df.format(obs.getObservationValue());

						rawLog += dateString + ": " + valueString + "\n";
					}
					logText = (TextView) findViewById(R.id.editLog);
					logText.setText(rawLog);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
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
		setContentView(R.layout.logviewer);
		super.onCreate(savedInstanceState);
		
		
		bindCbService();
	}

	
	
	@Override
	protected void onPause() {
		unBindCbService();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		unBindCbService();
		super.onDestroy();
	}

}
