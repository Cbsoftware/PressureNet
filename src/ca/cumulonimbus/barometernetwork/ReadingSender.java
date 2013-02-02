package ca.cumulonimbus.barometernetwork;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class ReadingSender extends AsyncTask<String, Integer, String> {
	
	Context appContext = null;
	private String serverURL = PressureNETConfiguration.SERVER_URL;
	private String responseText = "";
	private static final String PREFS_NAME = "pressureNETPrefs";
	
	public ReadingSender(Context context) {
		appContext = context;
	}
	
	@Override
	protected String doInBackground(String... params) {
		DefaultHttpClient client = new SecureHttpClient(appContext);
		HttpPost httppost = new HttpPost(serverURL);
		try {

			SharedPreferences settings = appContext.getSharedPreferences(PREFS_NAME, 0);
			String share = settings.getString("sharing_preference", "Us, Researchers and Forecasters");
			
			// No sharing? get out!
			if(share.equals("Nobody")) {
				return null;
			}
			
			// split the args into nvps assuming each string is csv
			ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
			for(String singleParam : params) {
				//System.out.println(singleParam);
				String key = singleParam.split(",")[0];
				String value = singleParam.split(",")[1];
				nvps.add(new BasicNameValuePair(key, value));
			} 
			httppost.setEntity(new UrlEncodedFormEntity(nvps));
			HttpResponse response = client.execute(httppost);
			
			HttpEntity responseEntity = response.getEntity();
			responseText = "";

			InputStreamReader sr;
			sr = new InputStreamReader(responseEntity.getContent());
			for(int bt = 0; (bt = sr.read()) != -1;) {
				responseText+=(char)bt;
			}
			sr.close();
		} catch(ClientProtocolException cpe) {

		} catch(IOException ioe) {

		} catch(ArrayIndexOutOfBoundsException aioobe) {
			
		}
		return responseText;
	}
	
}
