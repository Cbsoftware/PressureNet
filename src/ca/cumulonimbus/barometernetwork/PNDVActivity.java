package ca.cumulonimbus.barometernetwork;

import java.util.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class PNDVActivity extends Activity {
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	if(item.getItemId()==R.id.menu_inbrowser) {
    		Uri uri = Uri.parse(PressureNETConfiguration.PRESSURENET_URL);
    		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    		startActivity(intent);
    	}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.pndv_menu, menu);
	    return true;
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pndv_main);
		loadPNDV();
	}
	
	public void loadPNDV() {
		WebView pndvWebView = (WebView) findViewById(R.id.webViewPNDV);
		WebSettings webSettings = pndvWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setBuiltInZoomControls(true);
		Intent intent = getIntent();
		double latitude = intent.getDoubleExtra("latitude",0.0);
		double longitude = intent.getDoubleExtra("longitude", 0.0);
		Calendar cal = Calendar.getInstance();
		long now = cal.getTimeInMillis();
		long dayInMillis = 1000*60*60*24;
		long twoDaysAgo = cal.getTimeInMillis() - (2 * dayInMillis);
		// gotta use tomorrow for UTC hack
		long tomorrow = now + dayInMillis;
		if(latitude!=0){
			pndvWebView.loadUrl(PressureNETConfiguration.PRESSURENET_URL + "/?event=true&latitude=" + latitude + "&longitude=" + longitude + "&startTime=" + twoDaysAgo + "&endTime=" + tomorrow + "&zoomLevel=10");
		} else {
			pndvWebView.loadUrl(PressureNETConfiguration.PRESSURENET_URL);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}
}
