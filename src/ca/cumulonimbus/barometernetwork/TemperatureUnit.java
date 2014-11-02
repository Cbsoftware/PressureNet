package ca.cumulonimbus.barometernetwork;

import java.text.DecimalFormat;

/*
 *  Temperature Units
 */
public class TemperatureUnit {
	double valueInC;
	String abbrev;
	
	public String fullToAbbrev() {
		if(abbrev.contains("C")) {
			return "°C";
		} else if(abbrev.contains("F")) {
			return "°F";
		} else if(abbrev.contains("K")) {
			return "K ";
		} else {
			return "°C";
		}
	}
	
	public double convertToPreferredUnit() {
		try {
			if(abbrev.contains("C")) {
				return valueInC;
			} else if(abbrev.contains("F")) {
				return ((valueInC * 9) /5) + 32;
			} else if(abbrev.contains("K")) {
				return valueInC + 273.15;
			} else {
				return valueInC;
			}
		} catch(Exception e) {
			return -1;
		}
	}
	
	public String getDisplayText() {
		double accurateVal = convertToPreferredUnit();
		DecimalFormat df = new DecimalFormat("##.0");
		String formatted = df.format(accurateVal);
		String abb = fullToAbbrev();
		return formatted + " " + abb;
	}
	
	public TemperatureUnit(String abbrev) {
		this.abbrev = abbrev;
	}
	public double getValueInMeters() {
		return valueInC;
	}
	public void setValue(double valueInC) {
		this.valueInC = valueInC;
	}
	public String getAbbreviation() {
		return abbrev;
	}
	public void setAbbreviation(String abbreviation) {
		this.abbrev = abbreviation;
	}
}