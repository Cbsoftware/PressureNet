package ca.cumulonimbus.barometernetwork;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		switch(v.getId()) {
		case R.id.newWelcomeNotificationsNoButton:
			editor.putBoolean("send_condition_notifications", false);
			editor.commit();
			getActivity().finish();
			break;
		case R.id.newWelcomeNotificationsYesButton:
			editor.putBoolean("send_condition_notifications", true);
			editor.commit();
			getActivity().finish();
			break;
			
		default:
		}
	}
}
