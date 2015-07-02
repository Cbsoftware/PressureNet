package ca.cumulonimbus.barometernetwork;

import java.util.ArrayList;

public class ForecastLocation {

	private String locationID;
	private double latitude;
	private double longitude;
	
	private ArrayList<TemperatureForecast> temperatures;

	public ForecastLocation(String id, double lat, double lon) {
		locationID = id;
		latitude = lat;
		longitude = lon;
	}

	public ArrayList<TemperatureForecast> getTemperatures() {
		return temperatures;
	}
	public void setTemperatures(ArrayList<TemperatureForecast> temperatures) {
		this.temperatures = temperatures;
	}
	
	public String getLocationID() {
		return locationID;
	}
	public void setLocationID(String locationID) {
		this.locationID = locationID;
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
}
