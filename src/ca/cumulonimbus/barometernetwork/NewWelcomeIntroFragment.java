package ca.cumulonimbus.barometernetwork;

import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class NewWelcomeIntroFragment extends Fragment {

	Context context;
	boolean hasBarometer = true;
	Spinner spinnerWelcomeSharing;
	
	public static final String PREFS_NAME = "ca.cumulonimbus.barometernetwork_preferences";
	
	/**
	 * Check if we have a barometer. Use info to disable menu items, choose to
	 * run the service or not, etc.
	 */
	private boolean checkBarometer() {
		PackageManager packageManager = context.getPackageManager();
		hasBarometer = packageManager
				.hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER);
		return hasBarometer;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.new_welcome_intro, null);
		context = v.getContext();
		
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		
		ArrayAdapter<CharSequence> adapterSharing = ArrayAdapter
				.createFromResource(context, R.array.privacy_settings,
						android.R.layout.simple_spinner_item);
		adapterSharing
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerWelcomeSharing = (Spinner) v.findViewById(R.id.spinnerNewWelcomeSharing);
		spinnerWelcomeSharing.setAdapter(adapterSharing);

		String[] sharingArray = getResources().getStringArray(
				R.array.privacy_settings);
		String share = settings.getString("sharing_preference",
				"Us, Researchers and Forecasters");
		int positionShare = 0;
		for (int i = 0; i < sharingArray.length; i++) {
			if (sharingArray[i].equals(share)) {
				positionShare = i;
			}
		}
		spinnerWelcomeSharing.setSelection(positionShare);
	
		// set default units that are region aware/localized
		Locale current = getResources().getConfiguration().locale;
		if(current.getCountry().equals("US")) {
			// default to 'ft' and 'F'
		      SharedPreferences.Editor editor = settings.edit();
		      editor.putString("distance_units", "Feet (ft)");
		      editor.putString("temperature_units", "Fahrenheit (¡F)");
		      editor.commit();
		}
		

		// updateView();
		return v;
	}
}
