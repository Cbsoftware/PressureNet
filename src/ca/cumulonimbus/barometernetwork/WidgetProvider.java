package ca.cumulonimbus.barometernetwork;

import java.security.MessageDigest;

import org.json.JSONException;
import org.json.JSONObject;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.provider.Settings.Secure;
import android.widget.RemoteViews;

public class WidgetProvider extends AppWidgetProvider {

	Context mContext;
	SensorManager sm;
	public static String ACTION_SUBMIT_AND_UPDATE = "ca.cumulonimbus.barometernetwork.SubmitAndUpdate";
	public static String ACTION_UPDATEUI = "ca.cumulonimbus.barometernetwork.UpdateUI";
	public static String ACTION_SUBMIT_SINGLE = "ca.cumulonimbus.barometernetwork.SubmitSingle";
	
	
	double mReading = 0.0;
	
	MixpanelAPI mixpanel;
	
	@Override
	public void onEnabled(Context context) {
		mContext = context;
		mixpanel = MixpanelAPI.getInstance(context, PressureNETConfiguration.MIXPANEL_TOKEN);
		mixpanel.identify(getID());
		
		mixpanel.getPeople().identify(getID());
		mixpanel.getPeople().set("UserID", getID());
		
		JSONObject props = new JSONObject();

		JSONObject hashedUserIdProps = new JSONObject();
		try {
			hashedUserIdProps.put("user_id", getID());
			mixpanel.registerSuperProperties(hashedUserIdProps);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		mixpanel.track("Small Widget enabled", null);
		mixpanel.flush();
		super.onEnabled(context);
	}
	
	
	/**
	 * Get a unique ID by fetching the phone ID and hashing it
	 * 
	 * @return
	 */
	private String getID() {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");

			String actual_id = Secure.getString(mContext
					.getContentResolver(), Secure.ANDROID_ID);
			byte[] bytes = actual_id.getBytes();
			byte[] digest = md.digest(bytes);
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < digest.length; i++) {
				hexString.append(Integer.toHexString(0xFF & digest[i]));
			}
			return hexString.toString();
		} catch (Exception e) {
			return "--";
		}
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
	
	// Text tap
	public void onClick() {
		
	}

	
}
