package ca.cumulonimbus.barometernetwork;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.widget.RemoteViews;
import android.widget.Toast;

public class ConditionsWidgetProvider extends AppWidgetProvider {

	Context mContext;
	SensorManager sm;
	public static String ACTION_UPDATEUI = "UpdateUIConditions";

	@Override
	public void onEnabled(Context context) {
		mContext = context;
		super.onEnabled(context);
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		mContext = context;
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
				R.layout.conditions_widget_layout);
		ComponentName conditionsWidget;

		conditionsWidget = new ComponentName(context,
				ConditionsWidgetProvider.class);


		Intent conditionsIntent = new Intent(mContext, CurrentConditionsActivity.class);
		conditionsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		//conditionsIntent.setAction(ACTION_UPDATEUI);
		// Current Conditions activity likes to know the location in the Intent
		double notificationLatitude = 0.0;
		double notificationLongitude = 0.0;
		try {
			LocationManager lm = (LocationManager) mContext
					.getSystemService(Context.LOCATION_SERVICE);
			Location loc = lm
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (loc.getLatitude() != 0) {
				notificationLatitude = loc.getLatitude();
				notificationLongitude = loc.getLongitude();
			}
		} catch (Exception e) {

		}
		conditionsIntent.putExtra("latitude", notificationLatitude);
		conditionsIntent.putExtra("longitude", notificationLongitude);
		
		
		PendingIntent clickPI = PendingIntent.getActivity(context, 0, conditionsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.condition_clear, clickPI);
		remoteViews.setOnClickPendingIntent(R.id.condition_fog, clickPI);
		remoteViews.setOnClickPendingIntent(R.id.condition_cloud, clickPI);
		remoteViews.setOnClickPendingIntent(R.id.condition_precip, clickPI);
		remoteViews.setOnClickPendingIntent(R.id.condition_thunderstorm, clickPI);
		
		
		try {
			clickPI.send();
		} catch(CanceledException ce) {
			//log(ce.getMessage());
		}
		
		appWidgetManager.updateAppWidget(conditionsWidget, remoteViews);
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		if (ACTION_UPDATEUI.equals(intent.getAction())) {
		
		}
		super.onReceive(context, intent);
	}

}
