package ca.cumulonimbus.wendy;

import ca.cumulonimbus.barometernetwork.R;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class LogViewerFragment extends Fragment {

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);

		
		return inflater.inflate(R.layout.fragment_all_log, container, false);
	}

	@Override
	public void onPause() {
		//unBindCbService();
		super.onPause();
	}

}
