package ca.cumulonimbus.barometernetwork;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class WelcomeActivity extends Activity {
	
	public static final String PREFS_NAME = "ca.cumulonimbus.barometernetwork_preferences";
	
	Spinner spinnerWelcomeSharing;
	Button closeButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome);

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

		
		
		spinnerWelcomeSharing = (Spinner) findViewById(R.id.spinnerWelcomeSharing);
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
		
		closeButton = (Button) findViewById(R.id.buttonCloseWelcome);
		closeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
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
