package ca.cumulonimbus.barometernetwork;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import ca.cumulonimbus.pressurenetsdk.CbCurrentCondition;
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
                        if(key.equals("notification")) {
                        	CbCurrentCondition condition = createConditionFromJSONNotification(extras.getString(key));
                        	if(condition!=null) {
                        		sendNotification(condition);
                        	}
                        }
                    }
                }
            }
            log("app notifcation messages " + message);
        }
    }
    
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
	
