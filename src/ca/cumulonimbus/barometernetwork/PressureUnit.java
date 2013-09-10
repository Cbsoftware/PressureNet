package ca.cumulonimbus.barometernetwork;

import java.text.DecimalFormat;

/*
 *  Atmospheric Pressure Units
 *  There ought to be a parent Unit class
 */
public class PressureUnit {
	double valueInMb;
	String abbrev;
	
	public String fullToAbbrev() {
		if(abbrev.contains("mbar")) {
			// No change. reading comes to us in mbar.
			return "mbar";
		} else if(abbrev.contains("hPa")) {
			return "hPa";
		} else if(abbrev.contains("kPa")) {
			return "kPa";
		} else if(abbrev.contains("atm")) {
			return "atm";
		} else if(abbrev.contains("mmHg")) {
			return "mmHg";
		} else if(abbrev.contains("inHg")) {
			return "inHg"; 
		} else if(abbrev.contains("psi")) {
			return "psi"; 
		} else {
			return "mbar";
		}
	}
	
	// Conversion factors from http://www.csgnetwork.com/meteorologyconvtbl.html
	public double convertToPreferredUnit() {
		try {
			if(abbrev.contains("mbar")) {
				// No change. reading comes to us in mbar.
				return valueInMb;
			} else if(abbrev.contains("hPa")) {
				// mbar = hpa.
				return valueInMb;
			} else if(abbrev.contains("kPa")) {
				return valueInMb * 0.1;
			} else if(abbrev.contains("atm")) {
				return valueInMb * 0.000986923;
			} else if(abbrev.contains("mmHg")) {
				return valueInMb * 0.75006;
			} else if(abbrev.contains("inHg")) {
				return valueInMb * 0.02961; 
			} else if(abbrev.contains("psi")) {
				return valueInMb * 0.01450377; 
			} else {
				return valueInMb;
			}
		} catch(Exception e) {
			return valueInMb;
		}
	}
	
	public String getDisplayText() {
		double accurateVal = convertToPreferredUnit();
		DecimalFormat df = new DecimalFormat("####.00");
		String formatted = df.format(accurateVal);
		String abb = fullToAbbrev();
		return formatted + " " + abb;
	}
	
	public PressureUnit(String abbrev) {
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