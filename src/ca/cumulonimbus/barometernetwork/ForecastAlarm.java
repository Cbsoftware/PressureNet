package ca.cumulonimbus.barometernetwork;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;

public class ForecastAlarm extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		log("forecast alarm on receive");
		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK, "");
		wl.acquire();

		Intent serviceIntent = new Intent(context, ForecastService.class);
		serviceIntent.setAction(ForecastService.ACTION_UPDATE_FORECAST);
		context.startService(serviceIntent);
		
		wl.release();
	}

	public void restartAlarm(Context context, long time) {
		cancelAlarm(context);
		setAlarm(context, time);
	}
	
	public void setAlarm(Context context, long time) {
		log("forecast alarm setting alarm");
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, ForecastAlarm.class);
		i.setAction(ForecastService.ACTION_UPDATE_FORECAST);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),
				time, pi); 
	}
	
	public void cancelAlarm(Context context) {
		log("forecast alarm cancelling alarm");
		Intent intent = new Intent(context, ForecastAlarm.class);
		intent.setAction(ForecastService.ACTION_UPDATE_FORECAST);
		PendingIntent sender = PendingIntent
				.getBroadcast(context, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
	}
	
	private void log(String text) {
		if(PressureNETConfiguration.DEBUG_MODE) {
			System.out.println(text);
		}
	}

	
}
