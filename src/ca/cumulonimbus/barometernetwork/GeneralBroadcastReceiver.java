package ca.cumulonimbus.barometernetwork;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import ca.cumulonimbus.pressurenetsdk.CbService;

public class GeneralBroadcastReceiver extends BroadcastReceiver  {

    @Override
    public void onReceive(Context context, Intent intent) {
    	try {
    		Intent startServiceIntent = new Intent(context, CbService.class);
    		context.startService(startServiceIntent);
    	} catch (Exception e) {
    		// 
    	}
    }
}

