package ca.cumulonimbus.barometernetwork;

/**
 * User-searched location
 * @author jacob
 *
 */
public class SearchLocation {

	private String searchText;
	private double latitude;
	private double longitude;
	private long lastApiCall;
	
	public SearchLocation (String search, double lat, double lon) {
		searchText = search;
		latitude = lat;
		longitude = lon;
	}
	
	public String getSearchText() {
		return searchText;
	}
	public void setSearchText(String searchText) {
		this.searchText = searchText;
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
	public long getLastApiCall() {
		return lastApiCall;
	}
	public void setLastApiCall(long lastApiCall) {
		this.lastApiCall = lastApiCall;
	}
}
