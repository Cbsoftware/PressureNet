package ca.cumulonimbus.wendy;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import ca.cumulonimbus.barometernetwork.R;

import com.google.analytics.tracking.android.EasyTracker;

public class WelcomeActivity extends Activity {
	
	public static final String PREFS_NAME = "ca.cumulonimbus.barometernetwork_preferences";
	
	Spinner spinnerWelcomeSharing;
	Button closeButton;
	TextView textWelcome;
	boolean hasBarometer = true;
	
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
	
	/**
	 * Check if we have a barometer. Use info to disable menu items, choose to
	 * run the service or not, etc.
	 */
	private boolean checkBarometer() {
		PackageManager packageManager = this.getPackageManager();
		hasBarometer = packageManager
				.hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER);
		return hasBarometer;
	}
	
	private void log(String message) {
		if(PressureNETConfiguration.DEBUG_MODE) {
			System.out.println(message);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome);
		
		spinnerWelcomeSharing = (Spinner) findViewById(R.id.spinnerWelcomeSharing);
		closeButton = (Button) findViewById(R.id.buttonCloseWelcome);
		textWelcome = (TextView) findViewById(R.id.textAppAndSharingDescription);
		
		checkBarometer();
		if(!hasBarometer) {
			textWelcome.setText(getString(R.string.appDescritptionNoBarometer));
		}

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	
		ArrayAdapter<CharSequence> adapterSharing = ArrayAdapter
				.createFromResource(this, R.array.privacy_settings,
						android.R.layout.simple_spinner_item);
		adapterSharing
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerWelcomeSharing.setAdapter(adapterSharing);

		final Spinner spinnerSharing = (Spinner) findViewById(R.id.spinnerWelcomeSharing);
		String[] sharingArray = getResources().getStringArray(
				R.array.privacy_settings);
		String share = settings.getString("sharing_preference",
				"Us, Researchers and Forecasters");
		int positionShare = 0;
		for (int i = 0; i < sharingArray.length; i++) {
			if (sharingArray[i].equals(share)) {
				positionShare = i;
			}
		}
		spinnerWelcomeSharing.setSelection(positionShare);
	
		// set default units that are region aware/localized
		Locale current = getResources().getConfiguration().locale;
		if(current.getCountry().equals("US")) {
			// default to 'ft' and 'F'
		      SharedPreferences.Editor editor = settings.edit();
		      editor.putString("distance_units", "Feet (ft)");
		      editor.putString("temperature_units", "Fahrenheit (�F)");
		      editor.commit();
		}
		
		closeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(), WhatsNewActivity.class);
				startActivity(intent);
				finish();
			}
		});
		
		spinnerWelcomeSharing.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View view,
					int position, long id) {
				String[] array = getResources().getStringArray(R.array.privacy_settings);
				saveSharingPrivacy(array[position]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
			
		});
	}
	

	
	public void saveSharingPrivacy(String value) {
	      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	      SharedPreferences.Editor editor = settings.edit();
	      editor.putString("sharing_preference", value);
	      editor.commit();
	}
}
