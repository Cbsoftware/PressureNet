package ca.cumulonimbus.wendy;

import ca.cumulonimbus.barometernetwork.R;

import com.google.analytics.tracking.android.EasyTracker;

import android.app.Activity;
import android.os.Bundle;

public class SingleSkyPhotoActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.sky_photos_single);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onStart() {
		EasyTracker.getInstance(this).activityStart(this); 
		super.onStart();
	}

	@Override
	protected void onStop() {
		EasyTracker.getInstance(this).activityStop(this); 
		super.onStop();
	}

	
}
