package ca.cumulonimbus.barometernetwork;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.RemoteViews;
import android.widget.Toast;

public class WidgetProvider extends AppWidgetProvider implements SensorEventListener {

	Context mContext;
	SensorManager sm;
	public static String ACTION_SUBMIT_AND_UPDATE = "SubmitAndUpdate";
	public static String ACTION_UPDATEUI = "UpdateUI";
	public static String ACTION_SUBMIT_SINGLE = "SubmitSingle";
	
	
	double mReading = 0.0;
	
	@Override
	public void onEnabled(Context context) {
		mContext = context;
		// TODO Auto-generated method stub
		super.onEnabled(context);
	}

	@Override
	public void onDisabled(Context context) {
		// TODO Auto-generated method stub
		super.onDisabled(context);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		mContext = context;
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.small_widget_layout);
		Intent intent = new Intent(context, WidgetButtonService.class);
		intent.putExtra("msg", mReading + "");
		intent.putExtra("widget", "yes");
		intent.setAction(ACTION_UPDATEUI);
		
		Intent clickIntent = new Intent(context, BarometerNetworkActivity.class);
		PendingIntent clickPI = PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widgetSmallText, clickPI);
		remoteViews.setOnClickPendingIntent(R.id.widget_tendency_image_down, clickPI);
		remoteViews.setOnClickPendingIntent(R.id.widget_tendency_image_steady, clickPI);
		remoteViews.setOnClickPendingIntent(R.id.widget_tendency_image_up, clickPI);
		
		PendingIntent actionPendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT); 
		
		remoteViews.setOnClickPendingIntent(R.id.widgetSmallSubmitButton, actionPendingIntent);
		
		try {
			actionPendingIntent.send();
		} catch(CanceledException ce) {
			//log(ce.getMessage());
		}
		
		appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
		
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
	// Text tap
	public void onClick() {
		
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String msg = "null";
		mContext = context;
		try {
			msg = intent.getStringExtra("msg");
		} catch(NullPointerException e) {
			// eh
		}
		if(ACTION_SUBMIT_AND_UPDATE.equals(intent.getAction())) {
			// Start by starting up the barometer
			boolean running = false;
			try {
		    	sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		    	Sensor bar = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
		    	
		    	if(bar!=null) {
		        	running = sm.registerListener(this, bar, SensorManager.SENSOR_DELAY_NORMAL);
		    	}
	    	} catch(Exception e) {
	    		Toast.makeText(context, "error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
	    	}
		} 
		super.onReceive(context, intent);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_PRESSURE: 
			mReading = event.values[0];
			
			Intent updateUI = new Intent(mContext, WidgetProvider.class);
			updateUI.setAction(ACTION_UPDATEUI);
			updateUI.putExtra("reading", mReading + "");
			mContext.sendBroadcast(updateUI);
			
			sm.unregisterListener(this);

		    break;
	    }
		
	}
	
}
