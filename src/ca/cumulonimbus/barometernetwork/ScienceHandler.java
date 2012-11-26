package ca.cumulonimbus.barometernetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ScienceHandler {
    
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
    
    public static double slopeOfReadings(BarometerReading first, BarometerReading second) {
    	return ((second.getReading() - first.getReading()) / (second.getTime() -  first.getTime())); 
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
}
