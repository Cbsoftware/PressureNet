package ca.cumulonimbus.barometernetwork;

public class TemperatureForecast {

	private String locationID;
	private int scale;
	private double temperatureValue;
	private String startTime;
	private int forecastHour;
	
	public TemperatureForecast(String id, int tempScale, double value, String time, int hour) {
		this.locationID = id;
		this.scale = tempScale;
		this.temperatureValue = value;
		this.startTime = time;
		this.forecastHour = hour;
	}

	public String getLocationID() {
		return locationID;
	}

	public void setLocationID(String locationID) {
		this.locationID = locationID;
	}

	public int getScale() {
		return scale;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}

	public double getTemperatureValue() {
		return temperatureValue;
	}

	public void setTemperatureValue(double temperatureValue) {
		this.temperatureValue = temperatureValue;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public int getForecastHour() {
		return forecastHour;
	}

	public void setForecastHour(int forecastHour) {
		this.forecastHour = forecastHour;
	}
	
}
