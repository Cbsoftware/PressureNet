package ca.cumulonimbus.barometernetwork;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import ca.cumulonimbus.barometernetwork.PressureNetApplication.TrackerName;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class PlayServicesLegalActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.legal);
		
		
		TextView legalText = (TextView) findViewById(R.id.textLegal);
		legalText.setText( GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(getApplicationContext()));
		
	}
	
	@Override
	protected void onStart() {// Get tracker.
		Tracker t = ((PressureNetApplication) getApplication()).getTracker(
			    TrackerName.APP_TRACKER);
	// Set screen name.
	t.setScreenName("Play Services Legal");

	// Send a screen view.
	t.send(new HitBuilders.ScreenViewBuilder().build());
		super.onStart();
	}

	@Override
	protected void onStop() { 
		super.onStop();
	}


	
}
