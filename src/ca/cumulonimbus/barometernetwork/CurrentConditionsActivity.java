package ca.cumulonimbus.barometernetwork;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class CurrentConditionsActivity extends Activity {

	private Button buttonSunny;
	private Button buttonFoggy;
	private Button buttonCloudy;
	private Button buttonPrecipitation;
	private Button buttonThunderstorm;
	private Button buttonSendCondition;
	
	private double mLatitude = 0.0;
	private double mLongitude = 0.0;
	private CurrentCondition condition;
	
    private String serverURL = PressureNETConfiguration.SERVER_URL;

	public final String PREFS_NAME = "ca.cumulonimbus.barometernetwork_preferences";
	
	public String mAppDir = "";
	
    // Send data to the server in the background.
    private class ConditionSender extends AsyncTask<String, Integer, Long> {
		@Override
		protected Long doInBackground(String... arg0) {
			
			if((mLatitude == 0.0) || (mLongitude == 0.0)) {
				//don't submit
				return null;
			}
			
			// get sharing preference
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			String share = settings.getString("sharing_preference", "Us and Researchers");
		    
			// if sharing is None, don't send anything anywhere.
			if (share.equals("Nobody")) {
				return null;
			}
			
			log("app sending " + condition.getGeneral_condition());
			
			
	    	DefaultHttpClient client = new SecureHttpClient(getApplicationContext());
	    	HttpPost httppost = new HttpPost(serverURL);
	    	// TODO: keep a history of readings on the user's device
	    	// addToLocalDatabase(cc);
	    	
	    	try {
	    		List<NameValuePair> nvps = currentConditionToNVP(condition);
	    		nvps.add(new BasicNameValuePair("current_condition", "add"));
	    		httppost.setEntity(new UrlEncodedFormEntity(nvps));
	    		HttpResponse response = client.execute(httppost);
	    	} catch(ClientProtocolException cpe) {
	    		log(cpe.getMessage());
	    		// TODO: alert of failed submit
	    	} catch(IOException ioe) {
	    		log(ioe.getMessage());
	    		// TODO: alert of failed submit
	    	}
			return null;
		}
    	
		protected void onPostExecute(Long result) {	
			Toast.makeText(getApplicationContext(), "Sent: " + condition.getGeneral_condition(), Toast.LENGTH_SHORT).show();
//			finish();
		}
    }
    
    // Get the phone ID and hash it
	public String getID() {
    	try {
    		MessageDigest md = MessageDigest.getInstance("MD5");
    		
    		String actual_id = Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
    		byte[] bytes = actual_id.getBytes();
    		byte[] digest = md.digest(bytes);
    		StringBuffer hexString = new StringBuffer();
    		for(int i = 0; i< digest.length; i++) {
    			hexString.append(Integer.toHexString(0xFF & digest[i]));
    		}
    		return hexString.toString();
    	} catch(Exception e) {
    		return "--";
    	}
	}

	
    
    // Preparation for sending a condition through the network. 
    // Take the object and NVP it.
    public List<NameValuePair> currentConditionToNVP(CurrentCondition cc) {
    	List<NameValuePair> nvp = new ArrayList<NameValuePair>();
    	nvp.add(new BasicNameValuePair("latitude", cc.getLatitude() + ""));
    	nvp.add(new BasicNameValuePair("longitude", cc.getLongitude() + ""));
    	nvp.add(new BasicNameValuePair("general_condition", cc.getGeneral_condition() + ""));
    	nvp.add(new BasicNameValuePair("user_id", cc.getUser_id() + ""));
    	nvp.add(new BasicNameValuePair("time", cc.getTime() + ""));
    	nvp.add(new BasicNameValuePair("tzoffset", cc.getTzoffset() + ""));
    	return nvp;
    }
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.current_conditions);
		
		condition = new CurrentCondition();
		
		buttonSunny = (Button) findViewById(R.id.buttonSunny);
		buttonFoggy = (Button) findViewById(R.id.buttonFoggy);
		buttonCloudy = (Button) findViewById(R.id.buttonCloudy);
		buttonPrecipitation = (Button) findViewById(R.id.buttonPrecipitation);
		buttonThunderstorm = (Button) findViewById(R.id.buttonThunderstorm);
		buttonSendCondition = (Button) findViewById(R.id.buttonSendCondition);
		
		buttonSendCondition.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new ConditionSender().execute("");
			}
		});
		
		buttonSunny.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				condition.setGeneral_condition("Sunny");
			}
		});
		
		buttonFoggy.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				condition.setGeneral_condition("Foggy");
			}
		});
		
		buttonCloudy.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				condition.setGeneral_condition("Cloudy");
			}
		});
		
		buttonPrecipitation.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				condition.setGeneral_condition("Precipitation");
			}
		});
		
		buttonThunderstorm.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				condition.setGeneral_condition("Thunderstorm!");
			}
		});
		
		// Start adding the data for our current condition
		Bundle bundle = getIntent().getExtras();
		try {
			mAppDir = bundle.getString("appdir");
			mLatitude = bundle.getDouble("latitude");
			mLongitude = bundle.getDouble("longitude");
			condition.setLatitude(mLatitude);
			condition.setLongitude(mLongitude);
			condition.setUser_id(getID());
			condition.setTime(Calendar.getInstance().getTimeInMillis());
	    	condition.setTzoffset(Calendar.getInstance().getTimeZone().getOffset((long)condition.getTime()));
	    	
			//Toast.makeText(this, userSelf + " " + mAppDir, Toast.LENGTH_SHORT).show();
		} catch(Exception e) {
			log("conditions missing data, cannot submit");
		}
		
	}
	
	// Log data to SD card for debug purposes.
	// To enable logging, ensure the Manifest allows writing to SD card.
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
    	logToFile(text);
    	System.out.println(text);
    }
	
	@Override
	protected void onPause() {

		super.onPause();
	}

	
	
}
