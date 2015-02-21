package ca.cumulonimbus.wendy;

import java.util.Locale;

import ca.cumulonimbus.barometernetwork.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class NewWelcomeIntroFragment extends Fragment implements OnClickListener, OnItemSelectedListener {

	Context context;
	boolean hasBarometer = true;
	Spinner spinnerWelcomeSharing;
	Button nextButton;
	
	public static final String PREFS_NAME = "ca.cumulonimbus.barometernetwork_preferences";
	
	ViewPager pager;
	
	public void moveNext() {
	    //it doesn't matter if you're already in the last item
	    pager.setCurrentItem(pager.getCurrentItem() + 1);
	}

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
		
		nextButton = (Button) v.findViewById(R.id.buttonNewWelcomeNext);
		nextButton.setOnClickListener(this);
		
		ArrayAdapter<CharSequence> adapterSharing = ArrayAdapter
				.createFromResource(context, R.array.privacy_settings,
						R.layout.welcome_spinner);
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
		
		spinnerWelcomeSharing.setOnItemSelectedListener(this);
	
		// set default units that are region aware/localized
		Locale current = getResources().getConfiguration().locale;
		if(current.getCountry().equals("US")) {
			// default to 'ft' and 'F'
		      SharedPreferences.Editor editor = settings.edit();
		      editor.putString("distance_units", "Feet (ft)");
		      editor.putString("temperature_units", "Fahrenheit (�F)");
		      editor.commit();
		}
		

		// updateView();
		return v;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.buttonNewWelcomeNext:
			pager = (ViewPager) getActivity().findViewById(R.id.pager);
			moveNext();	
			break;
		default:
			break;
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> v, View view,
			int position, long id) {
		if(v.getId()== R.id.spinnerNewWelcomeSharing) {
			String[] array = getResources().getStringArray(R.array.privacy_settings);
			saveSharingPrivacy(array[position]);
			
		} else {
			System.out.println("setting preference none");
		}
		
	}

	public void saveSharingPrivacy(String value) {
	      SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
	      SharedPreferences.Editor editor = settings.edit();
	      System.out.println("setting preference " + value);
	      editor.putString("sharing_preference", value);
	      editor.commit();
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		
		
	}
}
