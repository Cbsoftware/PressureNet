package ca.cumulonimbus.barometernetwork;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

public class SkyPhotosActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		int actionBarTitleId = getResources().getSystem().getIdentifier(
				"action_bar_title", "id", "android");
		TextView actionBarTextView = (TextView) findViewById(actionBarTitleId);
		actionBarTextView.setTextColor(Color.WHITE);
		
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onStart() {
		
		super.onStart();
	}

	@Override
	protected void onStop() {
		
		super.onStop();
	}

	
	
}
