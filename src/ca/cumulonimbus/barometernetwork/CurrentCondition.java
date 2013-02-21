package ca.cumulonimbus.barometernetwork;


/**
 * A description of the current weather 
 * at a specific location.
 * 
 * @author jacob
 *
 */
public class CurrentCondition {
	private double time;
	private int tzoffset;
	private String location_type;
	private double latitude;
	private double longitude;
	private double location_accuracy;
	private String general_condition;
	private String windy;
	private String fog_thickness;
	private String cloud_type;
	private String precipitation_type;
	private double precipitation_amount;
	private String precipitation_unit;
	private String thunderstorm_intensity;
	private String user_id;
	private String sharing_policy;
	private String user_comment;
	
	public String getUser_comment() {
		return user_comment;
	}
	public void setUser_comment(String user_comment) {
		this.user_comment = user_comment;
	}
	public String getUser_id() {
		return user_id;
	}
	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}
	public String getSharing_policy() {
		return sharing_policy;
	}
	public void setSharing_policy(String sharing_policy) {
		this.sharing_policy = sharing_policy;
	}
	public double getTime() {
		return time;
	}
	public int getTzoffset() {
		return tzoffset;
	}
	public void setTzoffset(int tzoffset) {
		this.tzoffset = tzoffset;
	}
	public void setTime(double time) {
		this.time = time;
	}
	public String getLocation_type() {
		return location_type;
	}
	public void setLocation_type(String location_type) {
		this.location_type = location_type;
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
	public double getLocation_accuracy() {
		return location_accuracy;
	}
	public void setLocation_accuracy(double location_accuracy) {
		this.location_accuracy = location_accuracy;
	}
	public String getGeneral_condition() {
		return general_condition;
	}
	public void setGeneral_condition(String general_condition) {
		this.general_condition = general_condition;
	}
	public String getWindy() {
		return windy;
	}
	public void setWindy(String windy) {
		this.windy = windy;
	}
	public String getFog_thickness() {
		return fog_thickness;
	}
	public void setFog_thickness(String fog_thickness) {
		this.fog_thickness = fog_thickness;
	}
	public String getCloud_type() {
		return cloud_type;
	}
	public void setCloud_type(String cloud_type) {
		this.cloud_type = cloud_type;
	}
	public String getPrecipitation_type() {
		return precipitation_type;
	}
	public void setPrecipitation_type(String precipitation_type) {
		this.precipitation_type = precipitation_type;
	}
	public double getPrecipitation_amount() {
		return precipitation_amount;
	}
	public void setPrecipitation_amount(double precipitation_amount) {
		this.precipitation_amount = precipitation_amount;
	}
	public String getPrecipitation_unit() {
		return precipitation_unit;
	}
	public void setPrecipitation_unit(String precipitation_unit) {
		this.precipitation_unit = precipitation_unit;
	}
	public String getThunderstorm_intensity() {
		return thunderstorm_intensity;
	}
	public void setThunderstorm_intensity(String thunderstorm_intensity) {
		this.thunderstorm_intensity = thunderstorm_intensity;
	}	
}