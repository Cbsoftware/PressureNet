package ca.cumulonimbus.barometernetwork;

import java.util.Calendar;

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
import android.widget.Toast;

public class ConditionsWidgetProvider extends AppWidgetProvider {

	
	Context mContext;
	SensorManager sm;
	double mLatitude = 0;
	double mLongitude = 0;
	
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

	private void turnEverythingOff() {
		log("conditions widget turning everything off");
		RemoteViews remoteView = new RemoteViews(mContext.getPackageName(), R.layout.conditions_widget_layout);
		setCorrectClearIcon(false);
		remoteView.setImageViewResource(R.id.condition_fog, R.drawable.ic_wea_fog3);
		remoteView.setImageViewResource(R.id.condition_cloud, R.drawable.ic_wea_cloud);
		remoteView.setImageViewResource(R.id.condition_precip, R.drawable.ic_wea_precip);
		remoteView.setImageViewResource(R.id.condition_thunderstorm, R.drawable.ic_wea_r_l0);
		sendUpdate(remoteView);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.conditions_widget_layout);
		if (ACTION_UPDATEUI.equals(intent.getAction())) {
			if(intent.hasExtra("general_condition")) {
				String general = intent.getStringExtra("general_condition");
				if(general.equals(context.getResources().getString(R.string.sunny))) {
					// remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_on_sun);
					setCorrectClearIcon(true);
				} else if(general.equals(context.getResources().getString(R.string.foggy))) {
					remoteView.setImageViewResource(R.id.condition_fog, R.drawable.ic_wea_on_fog3);
					sendUpdate(remoteView);
				} else if(general.equals(context.getResources().getString(R.string.cloudy))) {
					remoteView.setImageViewResource(R.id.condition_cloud, R.drawable.ic_wea_on_cloud);
					sendUpdate(remoteView);
				} else if(general.equals(context.getResources().getString(R.string.precipitation))) {
					remoteView.setImageViewResource(R.id.condition_precip, R.drawable.ic_wea_on_precip);
					sendUpdate(remoteView);
				} else if(general.equals(context.getResources().getString(R.string.thunderstorm))) {
					remoteView.setImageViewResource(R.id.condition_thunderstorm, R.drawable.ic_wea_on_r_l0);
					sendUpdate(remoteView);
				} else if (general.equals("")) {
					turnEverythingOff();
				}
			}  
		} else {
			turnEverythingOff();
		}
		
		super.onReceive(context, intent);
	}
	
	private void sendUpdate(RemoteViews remoteView) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
		ComponentName component = new ComponentName(mContext.getPackageName(), ConditionsWidgetProvider.class.getName());    
		appWidgetManager.updateAppWidget(component, remoteView);
	}
	
	/**
	 * Moon phase info
	 */
	private int getMoonPhaseIndex() {
		MoonPhase mp = new MoonPhase(Calendar.getInstance());
		return mp.getPhaseIndex();
	}
    
	/**
	 * Choose which icon to show based on moon phase and state
	 * 
	 * TODO: This code is duplicated here from CurrentConditionsActivity. 
	 * Make the code general and keep it only in one place
	 * 
	 * @param on
	 */
	public void pickAndSetMoonIcon(boolean on) {
		RemoteViews remoteView = new RemoteViews(mContext.getPackageName(), R.layout.conditions_widget_layout);
		
		int moonNumber = getMoonPhaseIndex() + 1;
		
		switch(moonNumber) {
		case 1:
			if(on) {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_on_moon1);
			} else {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_moon1);
			}
			break;
		case 2:
			if(on) {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_on_moon2);				
			} else {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_moon2);
			}
			break;
		case 3:
			if(on) {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_on_moon3);
			} else {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_moon3);
			}
			break;
		case 4:
			if(on) {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_on_moon4);
			} else {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_moon4);
			}
			break;
		case 5:
			if(on) {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_on_moon5);		
			} else {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_moon5);	
			}
			break;
		case 6:
			if(on) {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_on_moon6);
			} else {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_moon6);
			}
			break;
		case 7:
			if(on) {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_on_moon7);
			} else {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_moon7);
			}
			break;
		case 8:
			if(on) {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_on_moon8);
			} else {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_moon8);
			}
			break;
		default:
			if(on) {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_on_moon2);
			} else {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_moon2);
			}
			break;
		}
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
		ComponentName component = new ComponentName(mContext.getPackageName(), ConditionsWidgetProvider.class.getName());    
		appWidgetManager.updateAppWidget(component, remoteView);
	}
	
    /** 
     * Choose icon between sun and moon depending on daytimes
     * and on/off status. 
     */
    public void setCorrectClearIcon(boolean on) {
    	setLastKnownLocation();
    	log("conditions widget location " + mLatitude + ", "  + mLongitude);
		if(CurrentConditionsActivity.isDaytime(mLatitude, mLongitude)) {
			// set to Sun icon
			log("daytime, sunny");
			RemoteViews remoteView = new RemoteViews(mContext.getPackageName(), R.layout.conditions_widget_layout);
			if(on) {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_on_sun);
				sendUpdate(remoteView);
			} else {
				remoteView.setImageViewResource(R.id.condition_clear, R.drawable.ic_wea_sun);
				sendUpdate(remoteView);
			}
		} else {
			// set to Moon icon
			log("nighttime, moony");
			pickAndSetMoonIcon(on);
		}
    }
    

	/**
	 * Update local location data with the last known location.
	 */
	private void setLastKnownLocation() {
		try {
			LocationManager lm = (LocationManager) mContext
					.getSystemService(Context.LOCATION_SERVICE);
			Location loc = lm
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		
			double latitude = loc.getLatitude();
			double longitude = loc.getLongitude();
			mLatitude = latitude;
			mLongitude = longitude;
		} catch (Exception e) {
			// everything stays as previous, likely 0
			//Toast.makeText(mContext, "Location not found", Toast.LENGTH_SHORT).show();
		}

	}
	
	private void log(String message) {
		System.out.println(message);
	}

}
