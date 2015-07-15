package ca.cumulonimbus.barometernetwork;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.CursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import ca.cumulonimbus.barometernetwork.PressureNetApplication.TrackerName;
import ca.cumulonimbus.pressurenetsdk.CbCurrentCondition;
import ca.cumulonimbus.pressurenetsdk.CbForecastAlert;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Show the user the forecast details after tapping on a forecast notification
 * 
 * @author jacob
 *
 */
public class ForecastDetailsActivity extends Activity {
	
	PnDb db;
	
	TextView textNoForecastAlerts;
	
	Button dismiss;
	
	Cursor adapterCursor;
	
	
	 public class CustomForecastAdapter extends CursorAdapter {
	        // Cursor cursor;
	        Context context;

	        public CustomForecastAdapter(Context context, Cursor cursor) {
	            super(context, cursor);
	            // this.cursor = cursor;
	            this.context = context;
	        }
	        
	        

	        @Override
	        public void bindView(View view, Context context, Cursor cursor) {

	        	String generalCondition = cursor.getString(1);
	        	String alertPrecip = cursor.getString(2);
	        	long alertTime = cursor.getLong(3);
	        	double alertTemp = cursor.getDouble(4);
	        	String politeText = cursor.getString(5);
	        	long issueTime = cursor.getLong(6);
	        	
	        	CbCurrentCondition condition = new CbCurrentCondition();
	        	CbForecastAlert alert = new CbForecastAlert();
	        	adapterCursor = cursor;
	        	
	        	condition.setGeneral_condition(generalCondition);
	        	if(generalCondition.equals("Precipitation")) {

					if(alertPrecip.equals("Rain")) {
						condition.setPrecipitation_type("Rain");
					} else if(alertPrecip.equals("Snow")) {
						condition.setPrecipitation_type("Snow");
					} if(alertPrecip.equals("Hail")) {
						condition.setPrecipitation_type("Hail");
					} 
	        	} else {
					condition.setGeneral_condition("Thunderstorm");
				}

	        	Location location = new Location("network");
	        	try {
	    			LocationManager lm = (LocationManager) mContext
	    					.getSystemService(Context.LOCATION_SERVICE);
	    			location = lm
	    					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	    			
	    			condition.setLocation(location);
		        	
		    		condition.setTime(alertTime);
		        	Calendar cal = Calendar.getInstance();
		        	int offset = cal.getTimeZone().getRawOffset();
		        	condition.setTzoffset(offset);
					
		        	ConditionsDrawables draws = new ConditionsDrawables(getApplicationContext());
					LayerDrawable drLayer = draws.getCurrentConditionDrawable(condition,
							null);
					if (drLayer == null) {
						log("drlayer null, next!");
						
					}
					
					Drawable draw = draws.getSingleDrawable(drLayer);
					
					Bitmap image = ((BitmapDrawable) draw).getBitmap();
					
					
		            ImageView imageAlert = (ImageView) view.findViewById(R.id.imageConditionAlert);
		            imageAlert.setImageBitmap(image);
	    		} catch (Exception e) {
	    			log("notificationreceiver no location " + e.getMessage());
	    		}
	    		

	            
	            TextView textForecastDescription = (TextView) view.findViewById(R.id.textForecastDescription);
	            textForecastDescription.setText(politeText);
	            
	            TextView textAlertTime = (TextView) view.findViewById(R.id.textAlertTime);
	            String displayTime = "";
				
				long localAlertTime = issueTime;
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(localAlertTime);
				SimpleDateFormat df = new SimpleDateFormat("HH:mm");
				displayTime = df.format(new Date(localAlertTime));
				
				textAlertTime.setText(displayTime);
	            
	            TextView textAlertTemperature = (TextView) view.findViewById(R.id.textAlertTemperature);
	            textAlertTemperature.setText(displayTemperatureValue(alertTemp));
	            
	            if(cursor.getCount() == 0) {
					textNoForecastAlerts.setVisibility(View.VISIBLE); 
				}
	        }

	        
	        
	        
	        @Override
	        public View newView(Context context, Cursor cursor, ViewGroup parent) {
	            LayoutInflater inflater = LayoutInflater.from(context);
	            View v = inflater.inflate(R.layout.forecast_list_item, parent, false);
	            return v;
	        }
	}

	 
	 
	@Override
	protected void onResume() {
		log("onresume");
		super.onResume();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.forecast_details);

		dismiss = (Button) findViewById(R.id.buttonDismissForecastDetails);
		
		dismiss.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		try {
			db = new PnDb(getApplicationContext());
			// Query for items from the database and get a cursor back
			db.open();
			db.deleteOldForecastAlerts();
			
			Cursor cursor = db.fetchRecentForecastAlerts();
			
			log("forecast details cursor count " + cursor.getCount());
			if(cursor.getCount() == 0 ) {
				textNoForecastAlerts = (TextView) findViewById(R.id.textNoForecastAlerts);
				textNoForecastAlerts.setVisibility(View.VISIBLE);
			}
			
			// Find ListView to populate
			ListView lvItems = (ListView) findViewById(R.id.listForecastAlerts);
			// Setup cursor adapter using cursor from last step
			CustomForecastAdapter forecastAdapter = new CustomForecastAdapter(getApplicationContext(), cursor);
			// Attach cursor adapter to the ListView 
			lvItems.setAdapter(forecastAdapter);
			
			
			registerForContextMenu(lvItems);
			db.close();
		} catch(Exception e) {
			log("app SQL exception " + e.getMessage());
		}
		
		
		try {
			log("cancelling existing notification");
			cancelNotification(NotificationSender.ALERT_NOTIFICATION_ID);
		} catch (Exception e) {
			log("conditions missing data, cannot submit");
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	    ContextMenuInfo menuInfo) {
	  if (v.getId()==R.id.listForecastAlerts) {
	    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
	    menu.setHeaderTitle("Alert Options");
	    String[] menuItems = {"Delete", "Close"};
	    for (int i = 0; i<menuItems.length; i++) {
	      menu.add(Menu.NONE, i, i, menuItems[i]);
	    }
	  }
	}
	
	
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	  AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
	  int menuItemIndex = item.getItemId();
	  String[] menuItems = {"Delete", "Close"};
	  
	  if(menuItems[menuItemIndex].equals("Delete")) {
		  log("item selected for deletion " + info.position );
		// Find ListView to populate
			ListView lvItems = (ListView) findViewById(R.id.listForecastAlerts);
			log("delete row id " + lvItems.getAdapter().getItemId(info.position));
			try {
				PnDb db = new PnDb(getApplicationContext());
				db.open();
				db.deleteSingleForecastAlert((int)lvItems.getAdapter().getItemId(info.position));
				db.close();
				adapterCursor.requery();
				if(adapterCursor.getCount() == 0) {
					textNoForecastAlerts = (TextView) findViewById(R.id.textNoForecastAlerts);
					textNoForecastAlerts.setVisibility(View.VISIBLE); 
				}
			} catch(Exception e) {
				log("db delete error " + e.getMessage());
			}
		  
	  } else {
		  log("closing menu without deleting");
	  }
	  
	  return true;
	}

	private String displayTemperatureValue(double value) {
		DecimalFormat df = new DecimalFormat("##.0");
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		String preferenceTemperatureUnit =  sharedPreferences
				.getString("temperature_units", "Celsius (°C)");
		
		TemperatureUnit unit = new TemperatureUnit(preferenceTemperatureUnit);
		unit.setValue(value);
		unit.setAbbreviation(preferenceTemperatureUnit);
		double temperatureInPreferredUnit = unit.convertToPreferredUnit();
		return df.format(temperatureInPreferredUnit) + " "
				+ unit.fullToAbbrev();
	}
	
	private void cancelNotification(int notifyId) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nMgr = (NotificationManager) getSystemService(ns);
		nMgr.cancel(notifyId);
	}
	
	private void log(String text) {
		if (PressureNETConfiguration.DEBUG_MODE) {
			// logToFile(text);
			System.out.println(text);
		}
	}
	
	@Override
	protected void onStart() {
		// Get tracker.
		Tracker t = ((PressureNetApplication) getApplication())
				.getTracker(TrackerName.APP_TRACKER);
		// Set screen name.
		t.setScreenName("Forecast Details");

		// Send a screen view.
		t.send(new HitBuilders.ScreenViewBuilder().build());
		super.onStart();
	}
	
	

	@Override
	protected void onStop() {
		db.close();
		super.onStop();
	}

}
