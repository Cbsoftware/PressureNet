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

public class ScienceHandler {
	
	private String mAppDir;
    
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
    

}
