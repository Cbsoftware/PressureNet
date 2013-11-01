package ca.cumulonimbus.barometernetwork;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
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
		RemoteViews remoteViews;
		ComponentName conditionsWidget;
		
		remoteViews = new RemoteViews(context.getPackageName(), R.layout.conditions_widget_layout);
		conditionsWidget = new ComponentName(context, ConditionsWidgetProvider.class);
		
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
