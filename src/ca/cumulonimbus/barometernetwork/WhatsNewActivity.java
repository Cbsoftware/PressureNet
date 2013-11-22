package ca.cumulonimbus.barometernetwork;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class WhatsNewActivity extends Activity {

	TextView pressureNETVersion;
	Button done;
	CheckBox enableConditionNotifications;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.whats_new);
		String versionName = "";
		pressureNETVersion = (TextView) findViewById(R.id.textWhatsNewTitle);
		done = (Button) findViewById(R.id.buttonDone);
		enableConditionNotifications = (CheckBox) findViewById(R.id.checkReceiveConditionNotifications);
		
		SharedPreferences prefs  = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean checkedNotifs = prefs.getBoolean("send_condition_notifications", false);
		enableConditionNotifications.setChecked(checkedNotifs);
		
		try {
			versionName = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
		} catch(NameNotFoundException nnfe) {
			
		}
		pressureNETVersion.setText("pressureNET " + versionName);
		
		done.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		enableConditionNotifications.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putBoolean("send_condition_notifications", isChecked);
				editor.commit();
			
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
	protected void onStop() {
		
		super.onStop();
	}

	
}
