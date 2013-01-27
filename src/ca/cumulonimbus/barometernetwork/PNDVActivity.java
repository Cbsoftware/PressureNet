package ca.cumulonimbus.barometernetwork;

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
    		Uri uri = Uri.parse("http://pressurenet.cumulonimbus.ca");
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
		pndvWebView.loadUrl("http://pndv.cumulonimbus.ca");
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	
	
}
