package ca.cumulonimbus.barometernetwork;

import java.util.Locale;

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
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class NewWelcomeIntroFragment extends Fragment implements OnClickListener {

	Context context;
	boolean hasBarometer = true;
	Button nextButton;
	TextView welcomeNewDescription; 
	
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
		
		checkBarometer();
		
		welcomeNewDescription = (TextView) v.findViewById(R.id.welcome_new_description);
		
		if(!hasBarometer) {
			welcomeNewDescription.setText(getString(R.string.newWelcomeNoBarometerIntro));
		} else {
			welcomeNewDescription.setText(getString(R.string.newWelcomeBarometerIntro));
		}
		
		
		// set default units that are region aware/localized
		Locale current = getResources().getConfiguration().locale;
		if(current.getCountry().equals("US")) {
			// default to 'ft' and 'F'
		      SharedPreferences.Editor editor = settings.edit();
		      editor.putString("distance_units", "Feet (ft)");
		      editor.putString("temperature_units", "Fahrenheit (°F)");
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

}
