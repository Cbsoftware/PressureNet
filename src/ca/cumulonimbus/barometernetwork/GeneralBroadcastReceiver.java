package ca.cumulonimbus.barometernetwork;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GeneralBroadcastReceiver extends BroadcastReceiver  {

    @Override
    public void onReceive(Context context, Intent intent) {
    	Intent startServiceIntent = new Intent(context, SubmitReadingService.class);
        context.startService(startServiceIntent);
    }
}

