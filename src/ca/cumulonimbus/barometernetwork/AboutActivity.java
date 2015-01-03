package ca.cumulonimbus.barometernetwork;

import com.google.analytics.tracking.android.EasyTracker;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import ca.cumulonimbus.pressurenetsdk.CbConfiguration;
import ca.cumulonimbus.pressurenetsdk.CbService;

public class AboutActivity extends Activity {

	LinearLayout layoutPressureNET;
	LinearLayout layoutCumulonimbus;
	TextView textViewVersion;
	TextView textViewSDKVersion;

	String versionName = "";
	String sdkVersionName = "";

	private void openWebBrowser(String url) {
		Uri uri = Uri.parse(url);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_legal_notices) {
			Intent intent = new Intent(getApplicationContext(),
					PlayServicesLegalActivity.class);
			startActivity(intent);
		} else if (item.getItemId() == R.id.menu_whats_new) {
			showWhatsNew();
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void showWhatsNew() {
		Intent intent = new Intent(this, WhatsNewActivity.class);
		startActivity(intent);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.about, menu);
		return true;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);

		layoutPressureNET = (LinearLayout) findViewById(R.id.layoutAboutPressureNET);
		layoutCumulonimbus = (LinearLayout) findViewById(R.id.layoutAboutCumulonimbus);
		textViewVersion = (TextView) findViewById(R.id.textVersion);
		textViewSDKVersion = (TextView) findViewById(R.id.textVersionSDK);

		try {
			versionName = this.getPackageManager().getPackageInfo(
					this.getPackageName(), 0).versionName;

			textViewVersion.setText(getString(R.string.version) + versionName);

		} catch (NameNotFoundException e) {
			//System.out.println("name not found " + e.getMessage());
		}

		try {
			sdkVersionName = CbConfiguration.SDK_VERSION;
			textViewSDKVersion.setText(getString(R.string.SDK) + sdkVersionName);
		} catch (Exception e) {
			//System.out.println("error " + e.getMessage());
		}

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
		int actionBarTitleId = getResources().getSystem().getIdentifier(
				"action_bar_title", "id", "android");

		TextView actionBarTextView = (TextView) findViewById(actionBarTitleId);
		actionBarTextView.setTextColor(Color.WHITE);
		
		ImageView view = (ImageView)findViewById(android.R.id.home);
	    view.setPadding(8, 0, 0, 0);

	}

}
