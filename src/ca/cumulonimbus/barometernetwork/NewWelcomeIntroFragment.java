package ca.cumulonimbus.barometernetwork;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class NewWelcomeIntroFragment extends Fragment {

	Context context;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.new_welcome_intro, null);
		context = v.getContext();
		// summary = (TextView) v.findViewById(R.id.home_summary);

		// updateView();
		return v;
	}
}
