package ca.cumulonimbus.barometernetwork;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

public class NewWelcomeNotificationsFragment extends Fragment implements OnClickListener {

	Context context;
	
	Button noThanks;
	Button yesPlease;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.new_welcome_notifications, null);
		context = v.getContext();
		
		noThanks = (Button) v.findViewById(R.id.newWelcomeNotificationsNoButton);
		yesPlease = (Button) v.findViewById(R.id.newWelcomeNotificationsYesButton);

		noThanks.setOnClickListener(this);
		yesPlease.setOnClickListener(this);
		
		return v;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.newWelcomeNotificationsNoButton:
			
			getActivity().finish();
			break;
		case R.id.newWelcomeNotificationsYesButton:
			
			getActivity().finish();
			break;
			
		default:
		}
	}
}
