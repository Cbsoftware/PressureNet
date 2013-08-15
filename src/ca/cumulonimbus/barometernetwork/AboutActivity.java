package ca.cumulonimbus.barometernetwork;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
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
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		
		layoutPressureNET = (LinearLayout) findViewById(R.id.layoutAboutPressureNET);
		layoutCumulonimbus = (LinearLayout) findViewById(R.id.layoutAboutCumulonimbus);
		
		layoutPressureNET.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				openWebBrowser("http://pressurenet.cumulonimbus.ca");
			}
		});
		

		layoutCumulonimbus.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				openWebBrowser("http://cumulonimbus.ca");
			}
		});

		ActionBar bar = getActionBar();
		int actionBarTitleId = getResources().getSystem().getIdentifier("action_bar_title", "id", "android");
		
		TextView actionBarTextView = (TextView)findViewById(actionBarTitleId); 
		actionBarTextView.setTextColor(Color.WHITE);
		
		
	}

}
