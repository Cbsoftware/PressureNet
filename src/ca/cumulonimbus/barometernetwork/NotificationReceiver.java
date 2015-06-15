package ca.cumulonimbus.barometernetwork;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import ca.cumulonimbus.pressurenetsdk.CbCurrentCondition;
import ca.cumulonimbus.pressurenetsdk.CbForecastAlert;
import ca.cumulonimbus.pressurenetsdk.CbService;

public class NotificationReceiver extends BroadcastReceiver {

	private Context mContext;
	
    public void onReceive(Context context, Intent intent) {
        String message = "";
        mContext = context;
    	if(intent!=null){
            log("app notificationreceiver has received an intent");
            if(intent!=null){
                Bundle extras = intent.getExtras();
                if(extras!=null){
                    for(String key: extras.keySet()){
                        message+= key + "=" + extras.getString(key) + "\n";
                        log("key, value: " + key + " " +  extras.getString(key));
                        if(key.equals("notification")) {
                        	
                        	if(extras.getString(key).contains("alert")) {
                        		log("notificationreceiver creating forecast alert");
                        		String jsonAlert = extras.getString(key);
                        		try {
                        			JSONObject js = new JSONObject(jsonAlert);
                        			String alertString = js.getString("alert");
                        			CbForecastAlert alert = createForecastAlertFromJSONNotification(alertString);
                                	CbCurrentCondition condition = createSmallConditionForAlert(alertString);
                                	if(alert!=null) {
                                		sendAlert(alert, condition);
                                	} else {
                                		log("notificationreceiver not sending alert, alert is null");
                                	} 
                        		} catch(JSONException e) {
                        			log("json exception e" + e.getMessage());
                        		}
                        		
                            
                        	} else {
                        	
	                        	log("notificationreceiver creating condition notification");
	                        	CbCurrentCondition condition = createConditionFromJSONNotification(extras.getString(key));
	                        	if(condition!=null) {
	                        		sendNotification(condition);
	                        	}
                        	
                        	}
                        	
                        } else {
                        	log("notificationreceiver doing nothing; " + extras.getString(key));
                        }
                    }
                }
            }
            log("app notifcation messages " + message);
        }
    }
    
    /**
     * Weather forecast alerts
     */
    private void sendAlert(CbForecastAlert alert, CbCurrentCondition condition) {
		// potentially notify about nearby conditions
		Intent intent = new Intent();
		log("notification receiver sending weather alert intent");
		intent.setAction(CbService.WEATHER_FORECAST_ALERT);
		intent.putExtra("ca.cumulonimbus.pressurenetsdk.alertNotification", alert);
		intent.putExtra("ca.cumulonimbus.pressurenetsdk.alertNotificationCondition", condition);
		mContext.sendBroadcast(intent);
    }
    
    private CbCurrentCondition createSmallConditionForAlert(String json) {
    	try {
    		log("notification receiver creating small condition from " + json);
    		JSONObject object = new JSONObject(json);
        	CbCurrentCondition condition = new CbCurrentCondition();
    		Location loc = new Location("network");
    		try {
    			LocationManager lm = (LocationManager) mContext
    					.getSystemService(Context.LOCATION_SERVICE);
    			loc = lm
    					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    			
    		} catch (Exception e) {
    			log("notificationreceiver no location " + e.getMessage());
    		}
    		condition.setLocation(loc);
    		
    		// Weather conditions
    		if(object.has("event")) {
    			String event = object.getString("event").toLowerCase();
    			log("notification receiver event is " + event);
    			if(event.equals("rain")) {
    				condition.setGeneral_condition("Precipitation");
    				condition.setPrecipitation_type("Rain");
    			} else if(event.equals("light-rain")) {
    				condition.setGeneral_condition("Precipitation");
    				condition.setPrecipitation_type("Rain");
    				condition.setPrecipitation_amount(0);
    			} if(event.equals("moderate-rain")) {
    				condition.setGeneral_condition("Precipitation");
    				condition.setPrecipitation_type("Rain");
    				condition.setPrecipitation_amount(1);
    			} if(event.equals("heavy-rain")) {
    				condition.setGeneral_condition("Precipitation");
    				condition.setPrecipitation_type("Rain");
    				condition.setPrecipitation_amount(2);
    			} else if (event.equals("hail")) {
    				condition.setGeneral_condition("Precipitation");
    				condition.setPrecipitation_type("Hail");
    			} else if (event.equals("snow")) {
    				condition.setGeneral_condition("Precipitation");
    				condition.setPrecipitation_type("Snow");
    			} else if (event.equals("thunderstorm")) {
    				condition.setGeneral_condition("Thunderstorm");
    			} 
    		}
    		return condition;
    	} catch (Exception e) {
    		log("notificationreceiver exception" + e.getMessage());
    		return null;
    	}
		
    }
    
    private CbForecastAlert createForecastAlertFromJSONNotification(String json) {
    	log("creating forecast alert from JSON "  + json);
    	try {
    		JSONObject object = new JSONObject(json);
    		
    		CbForecastAlert alert = new CbForecastAlert();
    		
    		
    		if(object.has("temperature")) {
    			// TODO: fix this
    			try {
    				alert.setTemperature(Double.parseDouble(object.getString("temperature")));
    				log("notificationreceiver setting temperature " + alert.getTemperature());
    			} catch(Exception e) {
    				log("number errorl " + e.getMessage());
    			}	
    		}
    		if(object.has("temperatureUnit")) {
    			// TODO: fix this
    			try {
    				alert.setTemperatureUnit(object.getString("temperatureUnit"));
    				log("notificationreceiver setting temperature unit " + alert.getTemperatureUnit());
    			} catch(Exception e) {
    				log("number errorl " + e.getMessage());
    			}	
    		} 
    		
    		if(alert.getTemperatureUnit().toLowerCase().contains("f")) {
    			double temp = (alert.getTemperature() - 32) * (5/9);
    			alert.setTemperature(temp);
    			alert.setTemperatureUnit("c");
    		}
    		
    		
    		if(object.has("time")) {
    			// TODO: fix this
    			try {
    				alert.setAlertTime(Long.parseLong(object.getString("time")));
    			} catch(Exception e) {
    				log("number errorl " + e.getMessage());
    			}	
    		} 
    		
    		return alert;
    	} catch(JSONException jsone) {
    		log("JSON error : " + jsone.getMessage());
    	} catch (Exception e) {
    		log("other JSON error: " + e.getMessage());
    	}
    	return null;
    }
    
    
    /**
     * Current Condition Notifications
     * 
     */
    
    private void sendNotification(CbCurrentCondition condition) {
		// potentially notify about nearby conditions
		Intent intent = new Intent();
		intent.setAction(CbService.LOCAL_CONDITIONS_ALERT);
		intent.putExtra("ca.cumulonimbus.pressurenetsdk.conditionNotification", condition);
		mContext.sendBroadcast(intent);
    }
    
    private CbCurrentCondition createConditionFromJSONNotification(String json) {
    	log("creating condition from JSON "  + json);
    	try {
    		JSONObject object = new JSONObject(json);
    		// object = object.getJSONObject("notification");
    		
    		CbCurrentCondition condition = new CbCurrentCondition();
    		
    		// Weather conditions
    		if(object.has("general_condition")) {
    			condition.setGeneral_condition(object.getString("general_condition"));
    		}
    		if(object.has("cloud_type")){
    			condition.setCloud_type(object.getString("cloud_type"));
    		}
    		if(object.has("windy")) {
    			condition.setWindy(object.getString("windy"));
    		}
    		if(object.has("fog_thickness")) {
    			condition.setFog_thickness(object.getString("fog_thickness"));
    		}
    		if(object.has("precipitation_type")) {
    			condition.setPrecipitation_type(object.getString("precipitation_type"));
    		}
    		if(object.has("precipitation_amount")) {
    			double amount = 0.0;
    			try {
    				amount = Double.parseDouble(object.getString("precipitation_amount"));
    			} catch(Exception e) {
    				// 
    			}
        		condition.setPrecipitation_amount(amount);    			
    		}
    		if(object.has("thunderstorm_intensity")) {
    			condition.setThunderstorm_intensity(object.getString("thunderstorm_intensity"));
    		}
    		if(object.has("user_comment")) {
    			condition.setUser_comment(object.getString("user_comment"));
    		}
    		// Location
    		if(object.has("latitude")) {
	    		Location location = new Location("network");
	    		location.setLatitude(Double.parseDouble(object.getString("latitude")));
	    		location.setLongitude(Double.parseDouble(object.getString("longitude")));
	    		condition.setLocation(location);
	    		condition.setLat(Double.parseDouble(object.getString("latitude")));
	    		condition.setLon(Double.parseDouble(object.getString("longitude")));
    		}

    		// Time
    		if(object.has("daterecorded")) {
    			condition.setTime(Long.parseLong(object.getString("daterecorded")));
    		}
    		if(object.has("tzoffset")) {
    			condition.setTzoffset(Integer.parseInt(object.getString("tzoffset")));
    		}
    		
    		
    		return condition;
    	} catch(JSONException jsone) {
    		log("JSON error : " + jsone.getMessage());
    	} catch (Exception e) {
    		log("other JSON error: " + e.getMessage());
    	}
    	return null;
    }
    
    public void log(String message) {
		if(PressureNETConfiguration.DEBUG_MODE) {
			System.out.println(message);
		}
	}
}
	
