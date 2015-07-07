package ca.cumulonimbus.barometernetwork;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

public class ForecastService extends Service {

	public static String ACTION_UPDATE_FORECAST = "ca.cumulonimbus.barometernetwork.ACTION_UPDATE_FORECAST";
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		// Run a forecast update
		log("app received action_update_forecast, running temperature forecast updates");
		
		downloadTemperatures();
		
		
	
		return super.onStartCommand(intent, flags, startId);
	}
	
	
	private void downloadTemperatures() {
		log("forecast service downloading temperature data");
		TemperatureDownloader temps = new TemperatureDownloader();
		temps.execute("");
	}
	
	@SuppressWarnings("deprecation")
	public class TemperatureDownloader extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {
			String responseText = "";
			try {
				DefaultHttpClient client = new DefaultHttpClient();

				String serverURL = PressureNETConfiguration.TEMPERATURE_FORECASTS;

				log("app downloading temperature forecasts");
				HttpGet get = new HttpGet(serverURL);
				// Execute the GET call and obtain the response
				HttpResponse getResponse = client.execute(get);
				HttpEntity responseEntity = getResponse.getEntity()	;
				log("temperature response " + responseEntity.getContentLength());
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(responseEntity.getContent(), "UTF-8"));
				responseText = reader.readLine();

			} catch (Exception e) {
				log("app temperature exception " + e.getMessage());
			}
			return responseText;
		}

		protected void onPostExecute(String result) {
			processJSONTemperatures(result);	
		}
		
		private void log(String text) {
			if(PressureNETConfiguration.DEBUG_MODE) {
				System.out.println(text);
			}
		}

	}

	private void processJSONTemperatures(String json) {
		try {
			log("forecast service finished downloading, now processing json temperatures");
			JSONObject object = new JSONObject(json);
			JSONArray forecastArray = object.getJSONArray("data");
			
			String forecastID;
			double latitude;
			double longitude;
			ArrayList<ForecastLocation> forecastLocations = new ArrayList<ForecastLocation>();
			ArrayList<TemperatureForecast> temperatureForecasts = new ArrayList<TemperatureForecast>();
			
			PnDb db = new PnDb(getApplicationContext());
			db.open();
			
			db.deleteAllTemperatureData();
			
			for(int i = 0; i< forecastArray.length(); i++ ){
				
				// Location
				
				JSONObject row = forecastArray.getJSONObject(i);
				forecastID = row.getString("id");
				JSONObject location = row.getJSONObject("location");
				latitude = location.getDouble("latitude");
				longitude = location.getDouble("longitude");
				forecastLocations.add(new ForecastLocation(forecastID, latitude, longitude));
				
				// Temperature forecasts
				JSONObject forecastObject = row.getJSONObject("temperatureForecast");
				String forecastTime = forecastObject.getString("date");
				JSONArray forecastTemps = forecastObject.getJSONArray("forecast");
				
				for(int j = 0; j < forecastTemps.length(); j++) {
					JSONObject forecastRow = forecastTemps.getJSONObject(j);
					int scale = forecastRow.getInt("scale");
					double degrees = forecastRow.getDouble("degrees");
					
					temperatureForecasts.add(new TemperatureForecast(forecastID, scale, degrees, forecastTime, j));
				}

			}
			log("forecast service created both arraylists, size " + forecastLocations.size() + ", " + temperatureForecasts.size() ); 
			
			db.addTemperatureForecastArrayList(temperatureForecasts);
			log("forecast service added temp forecast array list");
			db.addForecastLocationArrayList(forecastLocations);
			db.close();
			log("forecast service done; added locations to db");
		} catch(JSONException jsone) {
			log("app failed to parse temperature json: " + jsone.getMessage());
		}
		
		
	}
	
	
	@Override
	public IBinder onBind(Intent intent) {
		
		return null;
	}

	
	private void log(String text) {
		if(PressureNETConfiguration.DEBUG_MODE) {
			System.out.println(text);
		}
	}
	
}
