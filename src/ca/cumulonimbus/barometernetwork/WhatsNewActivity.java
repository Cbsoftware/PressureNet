package ca.cumulonimbus.barometernetwork;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

public class WhatsNewActivity extends Activity {

	TextView pressureNETVersion;
	Button done;
	Spinner freq;
	CheckBox checkReceiveConditionNotifications;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.whats_new);
		String versionName = "";
		pressureNETVersion = (TextView) findViewById(R.id.textWhatsNewTitle);
		done = (Button) findViewById(R.id.buttonDone);
		freq = (Spinner) findViewById(R.id.spinnerNotificationFrequency);
		checkReceiveConditionNotifications = (CheckBox) findViewById(R.id.checkReceiveConditionNotifications);

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		ArrayAdapter<CharSequence> adapterSharing = ArrayAdapter
				.createFromResource(this, R.array.condition_refresh_frequency,
						android.R.layout.simple_spinner_item);
		adapterSharing
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		freq.setAdapter(adapterSharing);
		String[] timeArray = getResources().getStringArray(
				R.array.condition_refresh_frequency_values);
		String time = prefs.getString("condition_refresh_frequency", "1 hour");
		int positionTime = 0;
		for (int i = 0; i < timeArray.length; i++) {
			if (timeArray[i].equals(time)) {
				positionTime = i;
			}
		}
		freq.setSelection(positionTime);

		try {
			versionName = this.getPackageManager().getPackageInfo(
					this.getPackageName(), 0).versionName;
		} catch (NameNotFoundException nnfe) {

		}
		pressureNETVersion.setText("pressureNET " + versionName);

		checkReceiveConditionNotifications.setChecked(prefs.getBoolean("send_condition_notifications", false));
		if(checkReceiveConditionNotifications.isChecked()) {
			freq.setEnabled(true);
		} else {
			freq.setEnabled(false);
		}
		
		checkReceiveConditionNotifications
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						
						if(isChecked) {
							freq.setEnabled(true);
						} else {
							freq.setEnabled(false);
						}
						long check = isChecked ? 1 : 0; 
						EasyTracker.getInstance(getApplicationContext()).send(MapBuilder.createEvent(
								BarometerNetworkActivity.GA_CATEGORY_MAIN_APP, 
								BarometerNetworkActivity.GA_ACTION_BUTTON, 
								"whats_new_conditions_notifications_check", 
								 check).build());
					}
				});
		
		freq.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long id) {
				String text = freq.getSelectedItem().toString();
				
				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext());

				SharedPreferences.Editor editor = settings.edit();
				editor.putString("condition_refresh_frequency",text);
				editor.commit(); 
				EasyTracker.getInstance(getApplicationContext()).send(MapBuilder.createEvent(
						BarometerNetworkActivity.GA_CATEGORY_MAIN_APP, 
						"whats_new_conditions_notifications_freq", 
						text, null).build());
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
				
			}
		
		});

		done.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});

	}

	@Override
	protected void onPause() {

		super.onPause();
	}

	@Override
	protected void onResume() {

		super.onResume();
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
