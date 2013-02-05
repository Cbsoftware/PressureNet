package ca.cumulonimbus.barometernetwork;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class SingleUserChartActivity extends Activity {

	private String userId = "";
	private String htmlPage = "";
	private String mAppDir = "";
	private String mUnits = "mbar";
	private String userSelf = "";
	public static final String PREFS_NAME = "ca.cumulonimbus.barometernetwork_preferences";
	
	public class ChartFetcher extends ReadingSender  {
	
		public ChartFetcher(SingleUserChartActivity context) {
			super(context);
			this.appContext = context;
		}

		protected void onPostExecute(String result) {
			log("chart fetcher post execute: " + result);
			//Process the result.
			//System.out.println(result);
			WebView chartWebPage = (WebView) findViewById(R.id.webViewChart);
			htmlPage = result;
	    	WebSettings webSettings = chartWebPage.getSettings();
	    	webSettings.setJavaScriptEnabled(true);
	    	webSettings.setBlockNetworkImage(false);
	    	webSettings.setBlockNetworkLoads(false);
	    	webSettings.setDomStorageEnabled(true);
	    	webSettings.setLoadsImagesAutomatically(true);
	    	webSettings.setPluginsEnabled(true);
	    
	    	chartWebPage.loadData(htmlPage, "text/html", null);
		}
	}
	
	// Get the chart HTML from the server.
	// Assume a time of one week. Add option for user
	// to change later.
	private void getChartFromUserId(String userId, String self) {
		log("getting chart...");
		AsyncTask<String, Integer, String> readingSender = new ChartFetcher(this);
		long week = 1000 * 60 * 60 * 24 * 7;
		double dateCutoff = Calendar.getInstance().getTimeInMillis() - week;
		readingSender.execute("getcharthtml,true", "statistics,by_user", "user_id,"+userId,"sinceWhen,"+dateCutoff, "units,"+mUnits, "selfstats,"+self);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// We're being created to show the graph of a userId, but
		// we don't yet have the userId due to the nature of the MapView's
		// overlay objects in BarometerNetworkActivity. We have enough information
		// that should help us find the userId
		Bundle bundle = getIntent().getExtras();
		setContentView(R.layout.singleuserchart);
		
		WebView chartWebPage = (WebView) findViewById(R.id.webViewChart);
		chartWebPage.loadData("<html><body bgcolor='#fff'><h2 style='color:#000'>Loading...</body></html>", "text/html", null);
		
		userId = bundle.getString("userid");
		try {
			mAppDir = bundle.getString("appdir");
			userSelf = bundle.getString("selfstats");
			//Toast.makeText(this, userSelf + " " + mAppDir, Toast.LENGTH_SHORT).show();
		} catch(Exception e) {
			
		}
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		mUnits = settings.getString("units", "mbar");
		setTitle("" + mUnits + " over days");
		getChartFromUserId(userId, userSelf);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
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
		super.onStart();
	}

	public void logToFile(String text) {
		try {
			OutputStream output = new FileOutputStream(mAppDir + "/log.txt", true);
			String logString = (new Date()).toString() + ": " + text + "\n";
			output.write(logString.getBytes());
			output.close();
		} catch(FileNotFoundException e) {
			
		} catch(IOException ioe) {
			
		}
	}
	
    public void log(String text) {
    	//logToFile(text);
    	//System.out.println(text);
    }
	
}
