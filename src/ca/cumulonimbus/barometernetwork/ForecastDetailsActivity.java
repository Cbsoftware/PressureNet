package ca.cumulonimbus.barometernetwork;

import java.util.Calendar;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import ca.cumulonimbus.barometernetwork.PressureNetApplication.TrackerName;
import ca.cumulonimbus.pressurenetsdk.CbCurrentCondition;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Show the user the forecast details after tapping on a forecast notification
 * 
 * @author jacob
 *
 */
public class ForecastDetailsActivity extends Activity {
	
	Button dismiss;
	TextView forecastTextView;
	ImageView imageConditionAlert;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.forecast_details);

		dismiss = (Button) findViewById(R.id.buttonDismissForecastDetails);
		forecastTextView = (TextView) findViewById(R.id.textForecastDescription);
		imageConditionAlert = (ImageView) findViewById(R.id.imageConditionAlert);
		
		dismiss.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		String[] alertTypes = {
				"Rain",
				"Snow",
				"Hail",
				"Thunderstorm"
		};
		
		String type = alertTypes[new Random().nextInt(alertTypes.length)];
		
		ConditionsDrawables draws = new ConditionsDrawables(getApplicationContext());
		
		CbCurrentCondition condition = new CbCurrentCondition();
		LocationManager lm = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		Location loc = lm
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		condition.setLocation(loc);
		condition.setTime(System.currentTimeMillis() + (1800 * 1000)); // now + 30 minutes
		Calendar cal = Calendar.getInstance();
		condition.setTzoffset(cal.getTimeZone().getRawOffset());
		
		if(type.equals("Rain")) {
			condition.setGeneral_condition("Precipitation");
			condition.setPrecipitation_type("Rain");
		} else if(type.equals("Snow")) { 
			condition.setGeneral_condition("Precipitation");
			condition.setPrecipitation_type("Snow");
		} else if(type.equals("Hail")) {
			condition.setGeneral_condition("Precipitation");
			condition.setPrecipitation_type("Hail");
		} else {
			condition.setGeneral_condition("Thunderstorm");
		}
		
		LayerDrawable drLayer = draws.getCurrentConditionDrawable(condition,
				null);
		if (drLayer == null) {
			log("drlayer null, next!");
			
		}
		
		Drawable draw = draws.getSingleDrawable(drLayer);
		
		Bitmap image = ((BitmapDrawable) draw).getBitmap();
		
		ForecastDetails details = new ForecastDetails();
		forecastTextView.setText(details.composeNotificationText(type));
		imageConditionAlert.setImageBitmap(image);
		
		
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
