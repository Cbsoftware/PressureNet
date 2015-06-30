package ca.cumulonimbus.barometernetwork;

public class ForecastLocation {

	private String locationID;
	private double latitude;
	private double longitude;
	
	public ForecastLocation(String id, double lat, double lon) {
		locationID = id;
		latitude = lat;
		longitude = lon;
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
