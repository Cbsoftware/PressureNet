package ca.cumulonimbus.barometernetwork;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.widget.RemoteViews;

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

		// TODO: This Intent system is absurd. Surely there is a better way.

		Intent conditionsClearIntent = new Intent(mContext, CurrentConditionsActivity.class);
		conditionsClearIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Intent conditionsFogIntent = new Intent(mContext, CurrentConditionsActivity.class);
		conditionsFogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Intent conditionsCloudIntent = new Intent(mContext, CurrentConditionsActivity.class);
		conditionsCloudIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Intent conditionsPrecipIntent = new Intent(mContext, CurrentConditionsActivity.class);
		conditionsPrecipIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Intent conditionsThunderstormIntent = new Intent(mContext, CurrentConditionsActivity.class);
		conditionsThunderstormIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
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
		conditionsClearIntent.putExtra("latitude", notificationLatitude);
		conditionsClearIntent.putExtra("longitude", notificationLongitude);
		
		conditionsFogIntent.putExtra("latitude", notificationLatitude);
		conditionsFogIntent.putExtra("longitude", notificationLongitude);
		
		conditionsCloudIntent.putExtra("latitude", notificationLatitude);
		conditionsCloudIntent.putExtra("longitude", notificationLongitude);
		
		conditionsPrecipIntent.putExtra("latitude", notificationLatitude);
		conditionsPrecipIntent.putExtra("longitude", notificationLongitude);
		
		conditionsThunderstormIntent.putExtra("latitude", notificationLatitude);
		conditionsThunderstormIntent.putExtra("longitude", notificationLongitude);
		
		conditionsClearIntent.putExtra("initial", "clear");
		conditionsClearIntent.setData(Uri.parse(conditionsClearIntent.toUri(Intent.URI_INTENT_SCHEME)));
		PendingIntent clearPI = PendingIntent.getActivity(context, 0, conditionsClearIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		conditionsFogIntent.putExtra("initial", "fog");
		conditionsFogIntent.setData(Uri.parse(conditionsFogIntent.toUri(Intent.URI_INTENT_SCHEME)));
		PendingIntent fogPI = PendingIntent.getActivity(context, 0, conditionsFogIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		conditionsCloudIntent.putExtra("initial", "cloud");
		conditionsCloudIntent.setData(Uri.parse(conditionsCloudIntent.toUri(Intent.URI_INTENT_SCHEME)));
		PendingIntent cloudPI = PendingIntent.getActivity(context, 0, conditionsCloudIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		conditionsPrecipIntent.putExtra("initial", "precip");
		conditionsPrecipIntent.setData(Uri.parse(conditionsPrecipIntent.toUri(Intent.URI_INTENT_SCHEME)));
		PendingIntent precipPI = PendingIntent.getActivity(context, 0, conditionsPrecipIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		conditionsThunderstormIntent.putExtra("initial", "thunderstorm");
		conditionsThunderstormIntent.setData(Uri.parse(conditionsThunderstormIntent.toUri(Intent.URI_INTENT_SCHEME)));
		PendingIntent thunderstormPI = PendingIntent.getActivity(context, 0, conditionsThunderstormIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		remoteViews.setOnClickPendingIntent(R.id.condition_clear, clearPI);
		remoteViews.setOnClickPendingIntent(R.id.condition_fog, fogPI);
		remoteViews.setOnClickPendingIntent(R.id.condition_cloud, cloudPI);
		remoteViews.setOnClickPendingIntent(R.id.condition_precip, precipPI);
		remoteViews.setOnClickPendingIntent(R.id.condition_thunderstorm, thunderstormPI);
	
		
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
