package ca.cumulonimbus.barometernetwork;

import java.io.Serializable;

/**
 * Hold data on a single barometer reading.
 * @author jacob
 *
 */
public class BarometerReading implements Serializable {
	private static final long serialVersionUID = 4085660664718400896L;
	double latitude;
	double longitude;
	double time;
	double reading;
	int timeZoneOffset;
	String androidId;
	
	public String toString() {
		String ret = "Reading: " + reading + "\n" +
					 "Latitude: " + latitude + "\n" + 
				     "Longitude: " + longitude + "\n" +
				     "ID: " + androidId + "\n" +
				     "Time: " + time + "\n" +
				     "TZOffset: " + timeZoneOffset + "\n"; 
		
		return ret;
	}

	
	
	public String getAndroidId() {
		return androidId;
	}
	public void setAndroidId(String androidId) {
		this.androidId = androidId;
	}
	public int getTimeZoneOffset() {
		return timeZoneOffset;
	}
	public void setTimeZoneOffset(int timeZoneOffset) {
		this.timeZoneOffset = timeZoneOffset;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public double getReading() {
		return reading;
	}
	public double getTime() {
		return time;
	}
	public void setTime(double time) {
		this.time = time;
	}
	public void setReading(double reading) {
		this.reading = reading;
	}	
}
