package ca.cumulonimbus.barometernetwork;

import com.google.analytics.tracking.android.EasyTracker;

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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.whats_new);
		String versionName = "";
		pressureNETVersion = (TextView) findViewById(R.id.textWhatsNewTitle);
		done = (Button) findViewById(R.id.buttonDone);
		
		SharedPreferences prefs  = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
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
