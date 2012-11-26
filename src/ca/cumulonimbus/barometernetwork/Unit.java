package ca.cumulonimbus.barometernetwork;

import java.text.DecimalFormat;

/*
 *  Units
 *  Millibars (mbar)
    Hectopascals (hPa)
    Standard Atmosphere (atm)
    Millimeteres of Mercury (mmHg)
 */
public class Unit {
	double valueInMb;
	String abbrev;
	
	String actualAbbrev;
	
	private String fullToAbbrev(String full) {
		if(abbrev.contains("mbar")) {
			// No change. reading comes to us in mbar.
			return "mbar";
		} else if(abbrev.contains("hPa")) {
			return "hPa";
		} else if(abbrev.contains("atm")) {
			return "atm";
		} else if(abbrev.contains("mmHg")) {
			return "mmHg";
		} else if(abbrev.contains("inHg")) {
			return "inHg"; 
		} else {
			return "mbar";
		}
	}
	
	// Conversion factors from http://www.csgnetwork.com/meteorologyconvtbl.html
	public double convertToPreferredUnit(String unit) {
		try {
			if(abbrev.contains("mbar")) {
				// No change. reading comes to us in mbar.
				return valueInMb;
			} else if(abbrev.contains("hPa")) {
				// mbar = hpa.
				return valueInMb;
			} else if(abbrev.contains("atm")) {
				return valueInMb * 0.000986923;
			} else if(abbrev.contains("mmHg")) {
				return valueInMb * 0.75006;
			} else if(abbrev.contains("inHg")) {
				return valueInMb * 0.02961; 
			} else {
				return valueInMb;
			}
		} catch(Exception e) {
			return valueInMb;
		}
	}
	
	public String getDisplayText() {
		double accurateVal = convertToPreferredUnit(abbrev);
		DecimalFormat df = new DecimalFormat("####.##");
		String formatted = df.format(accurateVal);
		String abb = fullToAbbrev(abbrev);
		return formatted + " " + abb;
	}
	
	public Unit(String abbrev) {
		this.abbrev = abbrev;
	}
	public double getValueInMb() {
		return valueInMb;
	}
	public void setValue(double valueInMb) {
		this.valueInMb = valueInMb;
	}
	public String getAbbreviation() {
		return abbrev;
	}
	public void setAbbreviation(String abbreviation) {
		this.abbrev = abbreviation;
	}
}