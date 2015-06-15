package ca.cumulonimbus.barometernetwork;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import ca.cumulonimbus.barometernetwork.PressureNetApplication.TrackerName;
import ca.cumulonimbus.pressurenetsdk.CbCurrentCondition;
import ca.cumulonimbus.pressurenetsdk.CbForecastAlert;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Show the user the forecast details after tapping on a forecast notification
 * 
 * @author jacob
 *
 */
public class ForecastDetailsActivity extends Activity {
	
	
	TextView textAlertTime;
	
	TextView forecastTextView;
	ImageView imageConditionAlert;
	
	TextView textAlertTemperature;
	
	Button dismiss;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.forecast_details);

		dismiss = (Button) findViewById(R.id.buttonDismissForecastDetails);
		forecastTextView = (TextView) findViewById(R.id.textForecastDescription);
		imageConditionAlert = (ImageView) findViewById(R.id.imageConditionAlert);

		textAlertTime = (TextView) findViewById(R.id.textAlertTime);
		textAlertTemperature = (TextView) findViewById(R.id.textAlertTemperature);
		
		String timingText = "in 1 hour";
		
		dismiss.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				finish();
			}
		});
		
		try {
			log("cancelling existing notification");
			cancelNotification(NotificationSender.ALERT_NOTIFICATION_ID);
		} catch (Exception e) {
			log("conditions missing data, cannot submit");
		}
		
		CbForecastAlert alert = new CbForecastAlert();
		CbCurrentCondition condition = new CbCurrentCondition();
		
		Intent intent = getIntent();
		if(intent!=null ) {
			
			if(intent.hasExtra("weatherEvent")) {
				
			
				alert = (CbForecastAlert) intent.getSerializableExtra("weatherEvent");
				condition = (CbCurrentCondition) intent.getSerializableExtra("condition");
				
				if(alert.getCondition() != null) {
					String type = alert.getCondition().getGeneral_condition();
					Double temperature = alert.getTemperature();
		
					
					textAlertTemperature.setText(displayTemperatureValue(temperature));
					
					String displayTime = "";
					
					long now = System.currentTimeMillis() / 1000;
					long timeDiff = alert.getAlertTime() - now;
					int minutesFuture = (int) (timeDiff / 60);
					timingText = "in " + minutesFuture + " minutes";
					
					
					ConditionsDrawables draws = new ConditionsDrawables(getApplicationContext());
					
					
					LocationManager lm = (LocationManager) this
							.getSystemService(Context.LOCATION_SERVICE);
					Location loc = lm
							.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					condition.setLocation(loc);
					
					long localAlertTime = alert.getAlertTime() * 1000; // + condition.getTzoffset();
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(localAlertTime);
					SimpleDateFormat df = new SimpleDateFormat("HH:mm");
					displayTime = df.format(cal.getTimeInMillis()).toString();
					
					textAlertTime.setText(displayTime);
					
					
					if(type.equals("Precipitation")) {
						if(condition.getPrecipitation_type().equals("Rain")) {
							condition.setPrecipitation_type("Rain");
							setTitle("Rain " + timingText);	
						} else if(condition.getPrecipitation_type().equals("Snow")) {
							condition.setPrecipitation_type("Snow");
							setTitle("Snow "+ timingText);	
						} if(condition.getPrecipitation_type().equals("Hail")) {
							condition.setPrecipitation_type("Hail");
							setTitle("Hail " + timingText);	
						} 
					} else {
						condition.setGeneral_condition("Thunderstorm");
						setTitle("Thunderstorm " + timingText);
					}
					
					LayerDrawable drLayer = draws.getCurrentConditionDrawable(condition,
							null);
					if (drLayer == null) {
						log("drlayer null, next!");
						
					}
					
					Drawable draw = draws.getSingleDrawable(drLayer);
					
					Bitmap image = ((BitmapDrawable) draw).getBitmap();
					
					forecastTextView.setText(alert.getTagLine());
					imageConditionAlert.setImageBitmap(image);
					
					
					
				}
			}
		} else {
			forecastTextView.setText("No active alerts");
		}

		
	}

	private String displayTemperatureValue(double value) {
		DecimalFormat df = new DecimalFormat("##.0");
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		String preferenceTemperatureUnit =  sharedPreferences
				.getString("temperature_units", "Celsius (°C)");
		
		TemperatureUnit unit = new TemperatureUnit(preferenceTemperatureUnit);
		unit.setValue(value);
		unit.setAbbreviation(preferenceTemperatureUnit);
		double temperatureInPreferredUnit = unit.convertToPreferredUnit();
		return df.format(temperatureInPreferredUnit) + " "
				+ unit.fullToAbbrev();
	}
	
	private void cancelNotification(int notifyId) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nMgr = (NotificationManager) getSystemService(ns);
		nMgr.cancel(notifyId);
	}
	
	private void log(String text) {
		if (PressureNETConfiguration.DEBUG_MODE) {
			// logToFile(text);
			System.out.println(text);
		}
	}
	
	@Override
	protected void onStart() {
		// Get tracker.
		Tracker t = ((PressureNetApplication) getApplication())
				.getTracker(TrackerName.APP_TRACKER);
		// Set screen name.
		t.setScreenName("Forecast Details");

		// Send a screen view.
		t.send(new HitBuilders.ScreenViewBuilder().build());
		super.onStart();
	}
	
	

	@Override
	protected void onStop() {

		super.onStop();
	}

}
