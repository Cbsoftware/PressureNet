package ca.cumulonimbus.barometernetwork;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ScienceHandler extends Service {
	
	private String mAppDir;
	
	public String findTendency(DBAdapter dbAdapter, int half) {
		String tendency = "";
		try {
			dbAdapter = new DBAdapter(getApplicationContext());
			dbAdapter.open();
			ArrayList<BarometerReading> recents = new ArrayList<BarometerReading>();
			recents = dbAdapter.fetchRecentReadings(1); // the last little while (in hours)
			
			List<BarometerReading> theHalf;
			// split in half
			Collections.sort(recents, new ScienceHandler.TimeComparator());
			if (half==1) {
				theHalf = recents.subList(0,recents.size() / 2);
			} else {
				theHalf = recents.subList(recents.size() / 2, recents.size() -1);
			}
			
			findApproximateTendency(theHalf);
			dbAdapter.close();
		} catch(Exception e) {
			System.out.println("tendency error " + e.getMessage());
		}
		return tendency;
	}

	public void checkForTrends(DBAdapter dbAdapter) {
		// TODO: Check the Preferences to see if we're allowed
		if (true) {
			String firstHalf = findTendency(dbAdapter, 1);
			String secondHalf = findTendency(dbAdapter, 2);
			String notificationString = "";
			
			if (firstHalf.equals("Rising") && secondHalf.equals("Falling")) {
				// Pressure just dropped. 
				notificationString = "The pressure is dropping";
			} else if (firstHalf.equals("Steady") && secondHalf.equals("Falling")) {
				// Pressure just dropped. 
				notificationString = "The pressure is dropping";
			} else if (firstHalf.equals("Falling") && secondHalf.equals("Rising")) {
				// Pressure is rising. 
				notificationString = "The pressure is starting to rise";
			} 
			
			if (notificationString.equals("")) {
				return;
			}
			
			log("checking for trends: " + notificationString);
			NotificationManager notificationManager = (NotificationManager) 
					  getSystemService(NOTIFICATION_SERVICE); 
			// Prepare intent which is triggered if the
			// notification is selected

			Intent intent = new Intent(getApplicationContext(), CurrentConditionsActivity.class);
			PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
			
			// Build notification
			// Actions are just fake
			Notification noti = new Notification.Builder(getApplicationContext())
			        .setContentTitle("pressureNET Alert")
			        .setContentText(notificationString).setSmallIcon(R.drawable.ic_launcher)
			        .setContentIntent(pIntent).getNotification();
			        
			    
			  
			// Hide the notification after its selected
			noti.flags |= Notification.FLAG_AUTO_CANCEL;

			notificationManager.notify(0, noti); 
		}
	}
	
	public static class TimeComparator implements Comparator<BarometerReading> {
        @Override
        public int compare(BarometerReading o1, BarometerReading o2) {
            if(o1.getTime() < o2.getTime()) {
            	return -1;
            } else {
            	return 1;
            }
        }
    }

	public static class PressureComparator implements Comparator<BarometerReading> {
        @Override
        public int compare(BarometerReading o1, BarometerReading o2) {
            if(o1.getReading() < o2.getReading()) {
            	return -1;
            } else {
            	return 1;
            }
        }
    }
    
    private int slopeOfBestFit(List<BarometerReading> recents) {
    	double time[] = new double[recents.size()];
    	double pressure[] = new double[recents.size()];
    	int x = 0;
    	long sumTime = 0L;
    	long sumPressure = 0L;
    	for(BarometerReading br : recents) {
    		time[x] = br.getTime();
    		sumTime += time[x];
    		sumTime += time[x] * time[x];
    		sumPressure += pressure[x];
    		pressure[x] = br.getReading();
    		x++;
    	}
		double timeBar = sumTime / x;
		double pressureBar = sumPressure / x;
		double ttBar = 0.0;
		double tpBar = 0.0;
		for (int y = 0; y < x; y++) {
			ttBar += (time[y] - timeBar) * (time[y] - timeBar);
			tpBar += (time[y] - timeBar) * (pressure[y] - pressureBar);
		}
		double beta1 = tpBar / ttBar;
		log("tendency best fit slope " + beta1);
		if (beta1 < -0.1) {
			return -1;
		} else if (beta1 > 0.1) {
			return 1;
		} else if (beta1 >= -0.1 && beta1 <= 0.1) {
			return 0;
		} else {
		   	return 0;
		}
    }
    
    // Take a good guess about the recent meteorological trends
    // (TODO: There's too much sorting going on here. Should use min and max) 
    private int guessedButGoodDecision(List<BarometerReading> recents) {
    	// Sort by pressure
    	Collections.sort(recents, new PressureComparator());
    	double minPressure = recents.get(0).getReading();
    	double maxPressure = recents.get(recents.size()-1).getReading();
    	
    	// Sort by time
    	Collections.sort(recents, new TimeComparator());
    	double minTime = recents.get(0).getTime();
    	double maxTime = recents.get(recents.size()-1).getTime();
    	// Start time at 0
    	for(BarometerReading br : recents) {
    		// we'd like to compare delta pressure and delta time
    		// preferably in millibars and hours.
    		br.setTime((br.getTime() - minTime) / (1000 * 3600));
    		br.setReading(br.getReading() - minPressure);
    	}
    	int slope = slopeOfBestFit(recents);
    	
    	return slope;
    }
    
    // 2013's improvement to yesterday's tendency algorithm
    public String findApproximateTendency(List<BarometerReading> recents) {
    	if(recents == null) {
    		log("tendency: recents is null");
    		return "Unknown";
    	}
    	if(recents.size() < 5) {
    		log("tendency: fewer than 5");
    		return "Unknown";
    	}
    	
    	int decision = guessedButGoodDecision(recents);
    	
    	log("tendency decision: " + decision + " from size " + recents.size());
    	
    	if (decision == 1) {
    		return "Rising";
    	} else if(decision == -1) {
    		return "Falling";
    	} else if (decision == 0) {
    		return "Steady";
    	} else {
    		return "Unknown";
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
	
	public ScienceHandler(String appDir) {
		mAppDir = appDir;
	}
	
    public void log(String text) {
    	logToFile(text);
    	System.out.println(text);
    }


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
    

}
