package ca.cumulonimbus.barometernetwork;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.analytics.tracking.android.EasyTracker;

public class AltitudeActivity extends Activity {
	
	Spinner spinUnit;
	Button cancelAltitude;
	Button okayAltitude;
	EditText editAltitude;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.altitude);
		
		Intent intent = getIntent();
		double startingAltitude = 0;
		
		if(intent.hasExtra("altitude")) {
			startingAltitude = intent.getDoubleExtra("altitude", 0);
			log("altitudeactivity has altitude " + startingAltitude);
		} else {
			log("altitudeactivity doesn't have starting altitude");
		}
		
		DecimalFormat df = new DecimalFormat("##");
		String altitudeToPrint = df.format(startingAltitude);
		
		okayAltitude = (Button) findViewById(R.id.okayAltitude);
		cancelAltitude = (Button) findViewById(R.id.cancelAltitude);
		spinUnit = (Spinner) findViewById(R.id.spinDistanceUnit);
		editAltitude = (EditText) findViewById(R.id.editAltitude);
		
		editAltitude.setText(altitudeToPrint + "");
		
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
		
		cancelAltitude.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		okayAltitude.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent resultIntent = new Intent();
				double newAltitude = 0.0;
				try {
					newAltitude = Double.parseDouble(editAltitude.getText().toString());
				} catch(Exception e) {
					// 
				}
				resultIntent.putExtra("altitude", newAltitude);
				resultIntent.putExtra("requestCode",
						BarometerNetworkActivity.REQUEST_ANIMATION_PARAMS);
				setResult(Activity.RESULT_OK, resultIntent);

				finish();
			}
		});
		
		editAltitude.requestFocus();
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	}

	private void log(String message) {
		if(PressureNETConfiguration.DEBUG_MODE) {
			System.out.println(message);
		}
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
