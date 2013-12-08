package ca.cumulonimbus.barometernetwork;

import com.google.analytics.tracking.android.EasyTracker;

import android.app.ActionBar;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.TextView;

public class PreferencesActivity extends PreferenceActivity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		ActionBar bar = getActionBar();
		int actionBarTitleId = getResources().getSystem().getIdentifier("action_bar_title", "id", "android");
		
		TextView actionBarTextView = (TextView)findViewById(actionBarTitleId); 
		actionBarTextView.setTextColor(Color.WHITE);
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
