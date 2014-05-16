package ca.cumulonimbus.barometernetwork;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.google.analytics.tracking.android.EasyTracker;

public class AltitudeActivity extends Activity {
	
	Spinner spinUnit;
	Button cancelAltitude;
	Button okayAltitude;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.altitude);
		
		spinUnit = (Spinner) findViewById(R.id.spinDistanceUnit);
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		
		ArrayAdapter<CharSequence> adapterSharing = ArrayAdapter
				.createFromResource(this, R.array.distance_units,
						android.R.layout.simple_spinner_item);
		adapterSharing
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinUnit.setAdapter(adapterSharing);

		String[] unitsArray = getResources().getStringArray(
				R.array.privacy_settings);
		String distance = settings.getString("distance_units", "Meters (m)");
		int positionDistance = 0;
		for (int i = 0; i < unitsArray.length; i++) {
			if (unitsArray[i].equals(distance)) {
				positionDistance = i;
			}
		}
		spinUnit.setSelection(positionDistance);
		
		
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
