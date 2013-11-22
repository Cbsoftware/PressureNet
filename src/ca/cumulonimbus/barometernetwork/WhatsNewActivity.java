package ca.cumulonimbus.barometernetwork;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
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
	protected void onStop() {
		
		super.onStop();
	}

	
}
