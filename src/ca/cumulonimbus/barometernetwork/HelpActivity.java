package ca.cumulonimbus.barometernetwork;

import com.google.analytics.tracking.android.EasyTracker;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

public class HelpActivity extends Activity {
	
	WebView webHelp;

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
		setContentView(R.layout.help);
		
		ActionBar bar = getActionBar();
		int actionBarTitleId = getResources().getSystem().getIdentifier("action_bar_title", "id", "android");
		
		TextView actionBarTextView = (TextView)findViewById(actionBarTitleId); 
		actionBarTextView.setTextColor(Color.WHITE);
		
		webHelp = (WebView) findViewById(R.id.webViewHelp);
		WebView pndvWebView = (WebView) findViewById(R.id.webViewHelp);
		WebSettings webSettings = pndvWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setBuiltInZoomControls(true);
		webHelp.loadUrl("http://www.cumulonimbus.ca/pressurenet-help/");
		 
	}
}
