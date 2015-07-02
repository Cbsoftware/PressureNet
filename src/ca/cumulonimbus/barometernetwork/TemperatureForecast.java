package ca.cumulonimbus.barometernetwork;

import java.text.DecimalFormat;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class TemperatureForecast {

	private String locationID;
	private int scale;
	private double temperatureValue;
	private String startTime;
	private int forecastHour;
	
	private double latitude;
	private double longitude;
	
	private String displayTempValue;
	
	private Context context;
	
	public TemperatureForecast(String id, int tempScale, double value, String time, int hour) {
		this.locationID = id;
		this.scale = tempScale;
		this.temperatureValue = value;
		this.startTime = time;
		this.forecastHour = hour;
	}

	public void prepareDisplayValue() {
		if(this.scale == 2) {
			// Temperature arrived in degrees F
			double tempInC = ((this.temperatureValue - 32)*5)/9;
			this.displayTempValue = displayTemperatureValue(tempInC, "##");
		} else if (scale == 1) {
			// Temperature arrived in degrees C
			this.displayTempValue = displayTemperatureValue(this.temperatureValue, "##");
		} 
	}
	
	
	
	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	private String displayTemperatureValue(double value, String format) {
		String preferenceTemperatureUnit;
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		preferenceTemperatureUnit = sharedPreferences.getString(
				"temperature_units", "Celsius (°C)");
		
		DecimalFormat df = new DecimalFormat(format);
		TemperatureUnit unit = new TemperatureUnit(preferenceTemperatureUnit);
		unit.setValue(value);
		unit.setAbbreviation(preferenceTemperatureUnit);
		double temperatureInPreferredUnit = unit.convertToPreferredUnit();
		return df.format(temperatureInPreferredUnit) + " "
				+ unit.fullToAbbrev();
	}
	
	
	public String getDisplayTempValue() {
		return displayTempValue;
	}
	public void setDisplayTempValue(String displayTempValue) {
		this.displayTempValue = displayTempValue;
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
