package ca.cumulonimbus.barometernetwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import android.app.Activity;

public class ScienceHandler {
	
	private String mAppDir;
    
	private static class TimeComparator implements Comparator<BarometerReading> {
        @Override
        public int compare(BarometerReading o1, BarometerReading o2) {
            if(o1.getTime() < o2.getTime()) {
            	return -1;
            } else {
            	return 1;
            }
        }
    }
    
    
    private int slopeOfBestFit(ArrayList<BarometerReading> recents) {
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
    
    // 2013's improvement to yesterday's tendency algorithm
    public String findApproximateTendency(ArrayList<BarometerReading> recents) {
    	if(recents == null) {
    		return "Unknown";
    	}
    	if(recents.size() < 5) {
    		return "Unknown";
    	}
    	
    	int slope = slopeOfBestFit(recents);
    	
    	if (slope == 1) {
    		return "Rising";
    	} else if(slope == -1) {
    		return "Falling";
    	} else if (slope == 0) {
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
    
    // Given a list of recent readings, find the tendency and return it
    public static String findTendency(ArrayList<BarometerReading> recents) {
    	// a nice, rough-guide approach is to look for change more than
    	// 3-4 millibars over about 5 hours.
    	if(recents==null) {
    		return "no recents";
    	}
    	// bail if there aren't many results
    	if(recents.size() < 4) {
    		return "unknown";
    	}
    	// sort recents by time.
    	Collections.sort(recents, new TimeComparator());
    	// compare the first and last, 
    	// and maybe the ones in the middle
    	BarometerReading first = recents.get(0);
    	BarometerReading second = recents.get(1);
    	BarometerReading last = recents.get(recents.size() - 1);
    	BarometerReading secondlast = recents.get(recents.size() - 2);
    	double widerSlope = slopeOfReadings(first, last);
    	double innerSlope = slopeOfReadings(second, secondlast);
    	if(widerSlope > 0) {
    		double diff = second.getReading() - first.getReading();
    		diff = Math.abs(diff);
    		if(diff > 2) {
	    		if(innerSlope > 0) {
	    			return "rising";
	    		} else {
	    			return "probably rising";
	    		}
    		} else {
    			return "maybe rising";
    		}
    	} else if(widerSlope < 0) {
    		double diff = second.getReading() - first.getReading();
    		diff = Math.abs(diff);
    		if(diff > 2) {
	    		if(innerSlope < 0) {
	    			return "falling";
	    		} else {
	    			return "probably falling";
	    		} 
    		} else {
    			return "maybe falling";
    		}
    	} else {
    		return "steady";
    	}
    }
    
	// Not really very useful.
    public static double slopeOfReadings(BarometerReading first, BarometerReading second) {
    	return ((second.getReading() - first.getReading()) / (second.getTime() -  first.getTime())); 
    }

}
