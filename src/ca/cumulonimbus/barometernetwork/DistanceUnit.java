package ca.cumulonimbus.barometernetwork;

import java.text.DecimalFormat;

/*
 *  Distance Units
 */
public class DistanceUnit {
	double valueInMeters;
	String abbrev;
	
	private String fullToAbbrev(String full) {
		if(abbrev.contains("Meters")) {
			return "m";
		} else if(abbrev.contains("Kilometers")) {
			return "km";
		} else if(abbrev.contains("Feet")) {
			return "ft";
		} else if(abbrev.contains("Miles")) {
			return "mi";
		} else {
			return "m";
		}
	}
	
	public double convertToPreferredUnit(String unit) {
		try {
			if(abbrev.equals("m")) {
				return valueInMeters;
			} else if(abbrev.equals("km")) {
				return valueInMeters * 1000;
			} else if(abbrev.contains("ft")) {
				return 0;
			} else if(abbrev.contains("mi")) {
				return 0;
			} else {
				return valueInMeters;
			}
		} catch(Exception e) {
			return -1;
		}
	}
	
	public String getDisplayText() {
		double accurateVal = convertToPreferredUnit(abbrev);
		DecimalFormat df = new DecimalFormat("##.0");
		String formatted = df.format(accurateVal);
		String abb = fullToAbbrev(abbrev);
		return formatted + " " + abb;
	}
	
	public DistanceUnit(String abbrev) {
		this.abbrev = abbrev;
	}
	public double getValueInMeters() {
		return valueInMeters;
	}
	public void setValue(double valueInMeters) {
		this.valueInMeters = valueInMeters;
	}
	public String getAbbreviation() {
		return abbrev;
	}
	public void setAbbreviation(String abbreviation) {
		this.abbrev = abbreviation;
	}
}