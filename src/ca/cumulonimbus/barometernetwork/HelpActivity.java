package ca.cumulonimbus.barometernetwork;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import ca.cumulonimbus.barometernetwork.PressureNetApplication.TrackerName;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class HelpActivity extends Activity {

	WebView webHelp;

	@Override
	protected void onStart() {
		// Get tracker.
		Tracker t = ((PressureNetApplication) getApplication())
				.getTracker(TrackerName.APP_TRACKER);
		// Set screen name.
		t.setScreenName("Help");

		// Send a screen view.
		t.send(new HitBuilders.ScreenViewBuilder().build());
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);

		ActionBar bar = getActionBar();
		int actionBarTitleId = getResources().getSystem().getIdentifier(
				"action_bar_title", "id", "android");

		TextView actionBarTextView = (TextView) findViewById(actionBarTitleId);
		actionBarTextView.setTextColor(Color.WHITE);

		ImageView view = (ImageView) findViewById(android.R.id.home);
		view.setPadding(8, 0, 0, 0);

		webHelp = (WebView) findViewById(R.id.webViewHelp);
		WebView pndvWebView = (WebView) findViewById(R.id.webViewHelp);
		WebSettings webSettings = pndvWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setBuiltInZoomControls(true);
		webHelp.loadUrl("http://www.cumulonimbus.ca/pressurenet-help/");

	}
}
