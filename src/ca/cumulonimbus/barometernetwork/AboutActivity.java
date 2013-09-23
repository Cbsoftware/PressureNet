package ca.cumulonimbus.barometernetwork;

import ca.cumulonimbus.pressurenetsdk.CbConfiguration;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AboutActivity extends Activity {

	LinearLayout layoutPressureNET;
	LinearLayout layoutCumulonimbus;
	
	private void openWebBrowser(String url) {
		Uri uri = Uri.parse(url);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_legal_notices) {
			Intent intent = new Intent(getApplicationContext(), PlayServicesLegalActivity.class);
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.about, menu);
		return true;
	}

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		
		layoutPressureNET = (LinearLayout) findViewById(R.id.layoutAboutPressureNET);
		layoutCumulonimbus = (LinearLayout) findViewById(R.id.layoutAboutCumulonimbus);
		
		layoutPressureNET.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				openWebBrowser(CbConfiguration.SERVER_URL);
			}
		});
		

		layoutCumulonimbus.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				openWebBrowser(CbConfiguration.CB_WEBSITE);
			}
		});

		ActionBar bar = getActionBar();
		int actionBarTitleId = getResources().getSystem().getIdentifier("action_bar_title", "id", "android");
		
		TextView actionBarTextView = (TextView)findViewById(actionBarTitleId); 
		actionBarTextView.setTextColor(Color.WHITE);
		
		
	}

}
