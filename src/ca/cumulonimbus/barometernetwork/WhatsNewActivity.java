package ca.cumulonimbus.barometernetwork;

import java.net.URLEncoder;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

public class WhatsNewActivity extends Activity {

	TextView pressureNETVersion;
	Button done;
	CheckBox checkReceiveConditionNotifications;
	CheckBox checkEnableSocial;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.whats_new);
		String versionName = "";
		done = (Button) findViewById(R.id.buttonDone);
		checkReceiveConditionNotifications = (CheckBox) findViewById(R.id.checkReceiveConditionNotifications);
		checkEnableSocial = (CheckBox) findViewById(R.id.checkEnableSocial);

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		PackageManager pkManager = getPackageManager();
		try {
			boolean twitterInstalled = false;
			String tweetUrl = 
				    String.format("https://twitter.com/intent/tweet?text=%s&url=%s",
				        URLEncoder.encode(""), URLEncoder.encode("https://play.google.com/store/apps/details?id=ca.cumulonimbus.barometernetwork"));
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweetUrl));
			List<ResolveInfo> matches = getPackageManager().queryIntentActivities(intent, 0);
			for (ResolveInfo info : matches) {
			    if (info.activityInfo.packageName.toLowerCase().startsWith("com.twitter")) {
			        intent.setPackage(info.activityInfo.packageName);
			        twitterInstalled = true;
			    }
			}

		    if (twitterInstalled)   {
		    	System.out.println("twitter installed");
		    	checkEnableSocial.setChecked(true);
		    	SharedPreferences.Editor editor =  prefs.edit();
		    	editor.putBoolean("enable_social", true);
		    	editor.commit();
		    	
		    } else {
		    	System.out.println("twitter not installed");
		    	checkEnableSocial.setChecked(false);
		    	SharedPreferences.Editor editor =  prefs.edit();
		    	editor.putBoolean("enable_social", false);
		    	editor.commit();
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		try {
			versionName = this.getPackageManager().getPackageInfo(
					this.getPackageName(), 0).versionName;
		} catch (NameNotFoundException nnfe) {

		}
		setTitle(getString(R.string.pressureNet) + " "  + versionName);
		
		checkReceiveConditionNotifications.setChecked(prefs.getBoolean("send_condition_notifications", true));
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		SharedPreferences.Editor editor = settings.edit();
		if(checkReceiveConditionNotifications.isChecked()) {
			editor.putBoolean("send_condition_notifications",true); 
		} else {
			editor.putBoolean("send_condition_notifications",false);
		}
		editor.commit(); 
		
		
		checkReceiveConditionNotifications
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						SharedPreferences settings = PreferenceManager
								.getDefaultSharedPreferences(getApplicationContext());

						SharedPreferences.Editor editor = settings.edit();
						if(isChecked) {
							editor.putBoolean("send_condition_notifications",true); 
						} else {
							editor.putBoolean("send_condition_notifications",false);
						}
						editor.commit(); 
						long check = isChecked ? 1 : 0; 
						EasyTracker.getInstance(getApplicationContext()).send(MapBuilder.createEvent(
								BarometerNetworkActivity.GA_CATEGORY_MAIN_APP, 
								BarometerNetworkActivity.GA_ACTION_BUTTON, 
								"whats_new_conditions_notifications_check", 
								 check).build());
					}
				});
		
		checkEnableSocial.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext());

				SharedPreferences.Editor editor = settings.edit();
				if(isChecked) {
					editor.putBoolean("enable_social",true); 
				} else {
					editor.putBoolean("enable_social",false);
				}
				editor.commit(); 
				long check = isChecked ? 1 : 0; 
				EasyTracker.getInstance(getApplicationContext()).send(MapBuilder.createEvent(
						BarometerNetworkActivity.GA_CATEGORY_MAIN_APP, 
						BarometerNetworkActivity.GA_ACTION_BUTTON, 
						"enable_social", 
						 check).build());
			}
		});

		done.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});

	}

	@Override
	protected void onPause() {

		super.onPause();
	}

	@Override
	protected void onResume() {

		super.onResume();
	}

	@Override
	protected void onStart() {
		EasyTracker.getInstance(this).activityStart(this);
		super.onStart();
	}

	@Override
	protected void onStop() {
		EasyTracker.getInstance(this).activityStop(this);
		super.onStop();
	}

}
