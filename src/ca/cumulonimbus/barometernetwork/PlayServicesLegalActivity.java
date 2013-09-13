package ca.cumulonimbus.barometernetwork;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesUtil;

public class PlayServicesLegalActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.legal);
		
		
		TextView legalText = (TextView) findViewById(R.id.textLegal);
		legalText.setText( GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(getApplicationContext()));
		
	}


	
}
