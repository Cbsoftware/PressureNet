package ca.cumulonimbus.barometernetwork;

import java.util.Random;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import ca.cumulonimbus.barometernetwork.PressureNetApplication.TrackerName;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.forecast_details);

		dismiss = (Button) findViewById(R.id.buttonDismissForecastDetails);
		dismiss.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		forecastTextView = (TextView) findViewById(R.id.textForecastDescription);
		
		String[] alertTypes = {
				"Rain",
				"Snow",
				"Hail",
				"Thunderstorm"
		};
		String type = alertTypes[new Random().nextInt(alertTypes.length)];
		ForecastDetails details = new ForecastDetails();
		forecastTextView.setText(details.composeNotificationText(type));
		
		
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
