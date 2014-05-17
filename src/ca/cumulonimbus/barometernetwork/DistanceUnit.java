package ca.cumulonimbus.barometernetwork;

import java.text.DecimalFormat;

/*
 *  Distance Units
 */
public class DistanceUnit {
	double valueInMeters;
	String abbrev;
	
	public static double kmToM(double km) {
		return km * 1000;
	}
	
	public static double ftToM(double ft) {
		return ft / 3.28084;
	}
	
	public static double miToM(double mi) {
		return mi / 0.000621371;
	}
	
	public String fullToAbbrev() {
		if(abbrev.contains("(m)")) {
			return "m";
		} else if(abbrev.contains("(km)")) {
			return "km";
		} else if(abbrev.contains("(ft)")) {
			return "ft";
		} else if(abbrev.contains("(mi)")) {
			return "mi";
		} else {
			return "m";
		}
	}
	
	public double convertToPreferredUnit() {
		try {
			if(abbrev.equals("m")) {
				return valueInMeters;
			} else if(abbrev.equals("km")) {
				return valueInMeters * .001;
			} else if(abbrev.contains("ft")) {
				return valueInMeters * 3.28084;
			} else if(abbrev.contains("mi")) {
				return valueInMeters * 0.000621371;
			} else {
				return valueInMeters;
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