package ca.cumulonimbus.wendy;

import java.text.DecimalFormat;

/*
 *  Distance Units
 */
public class DistanceUnit {
	double valueInMeters;
	String abbrev;
	
	public static double kmToM(double km) {
		return km / .001;
	}
	
	public static double ftToM(double ft) {
		return ft / 3.28084;
	}
	
	public static double miToM(double mi) {
		return mi / 0.000621371;
	}
	
	private void log(String message ){
		if(PressureNETConfiguration.DEBUG_MODE) {
			System.out.println(message);
		}
	}
	
	public String fullToAbbrev() {
		log("altitudetest: fullToAbbrev " + abbrev);
		if(abbrev.contains("(m)")) {
			log("altitudetest returning m");
			return "m";
		} else if(abbrev.contains("(km)")) {
			log("altitudetest returning km");
			return "km";
		} else if(abbrev.contains("(ft)")) {
			log("altitudetest returning ft");
			return "ft";
		} else if(abbrev.contains("(mi)")) {
			log("altitudetest returning mi");
			return "mi";
		} else {
			log("altitudetest returning default m");
			return "m";
		}
	}
	
	public double convertToPreferredUnit() {
		log("NOTIFTESTconvert to preferred unit abbrev" + abbrev);
		try {
			if(abbrev.contains("(m)")) {
				double retVal = valueInMeters;
				log("altitudetest returning " + retVal);
				return retVal;
			} else if(abbrev.contains("(km)")) {
				double retVal = valueInMeters * .001;
				log("altitudetest returning " + retVal);
				return retVal;
			} else if(abbrev.contains("(ft)")) {
				double retVal = valueInMeters * 3.28084;
				log("altitudetest returning " + retVal);
				return retVal;
			} else if(abbrev.contains("(mi)")) {
				double retVal = valueInMeters * 0.000621371;
				log("altitudetest returning " + retVal);
				return retVal;
			} else {
				double retVal = valueInMeters;
				log("altitudetest returning default " + retVal + " " + abbrev);
				return retVal;
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