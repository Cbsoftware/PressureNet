package ca.cumulonimbus.barometernetwork;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import ca.cumulonimbus.barometernetwork.PressureNetApplication.TrackerName;
import ca.cumulonimbus.pressurenetsdk.CbConfiguration;
import ca.cumulonimbus.pressurenetsdk.CbCurrentCondition;
import ca.cumulonimbus.pressurenetsdk.CbService;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.SunLocation;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

public class CurrentConditionsActivity extends Activity {

	private ImageButton buttonSunny;
	private ImageButton buttonFoggy;
	private ImageButton buttonCloudy;
	private ImageButton buttonPrecipitation;
	private ImageButton buttonThunderstorm;
	private Button buttonSendCondition;
	private Button buttonCancelCondition;
	private ImageButton buttonIsWindy1;
	private ImageButton buttonIsWindy2;
	private ImageButton buttonIsWindy3;
	private ImageButton buttonIsCalm;
	private ImageButton buttonRain;
	private ImageButton buttonSnow;
	private ImageButton buttonHail;
	private ImageButton buttonInfrequentLightning;
	private ImageButton buttonFrequentLightning;
	private ImageButton buttonHeavyLightning;
	private ImageButton buttonLowPrecip;
	private ImageButton buttonModeratePrecip;
	private ImageButton buttonHeavyPrecip;
	private ImageButton buttonPartlyCloudy;
	private ImageButton buttonMostlyCloudy;
	private ImageButton buttonVeryCloudy;
	private ImageButton buttonLightFog;
	private ImageButton buttonModerateFog;
	private ImageButton buttonHeavyFog;
	private ImageButton buttonTwitter;

	private ImageButton buttonExtreme;
	private ImageButton buttonFlooding;
	private ImageButton buttonFire;
	private ImageButton buttonTornado;
	private ImageButton buttonTropicalStorm;
	private ImageButton buttonDuststorm;

	private TextView textGeneralDescription;
	private TextView textWindyDescription;
	private TextView textPrecipitationDescription;
	private TextView textPrecipitationAmountDescription;
	private TextView textLightningDescription;
	private TextView textCloudyDescription;
	private TextView textFoggyDescription;
	private TextView textExtremeDescription;

	private ImageView imageHrGeneral;
	private ImageView imageHrPrecipitation;
	private ImageView imageHrFoggy;
	private ImageView imageHrCloudy;
	private ImageView imageHrPrecipitationAmount;
	private ImageView imageHrLightning;
	private ImageView imageHrWindy;

	private HorizontalScrollView scrollGeneral;
	private ScrollView scrollWind;
	private ScrollView scrollPrecipitation;
	private ScrollView scrollPrecipitationAmount;
	private ScrollView scrollLightning;
	private ScrollView scrollClouds;
	private ScrollView scrollFoggy;
	private LinearLayout layoutExtreme;

	private ImageView hrExtreme;

	// private CheckBox addPhoto;

	private double mLatitude = 0.0;
	private double mLongitude = 0.0;
	private CbCurrentCondition condition;

	private String serverURL = CbConfiguration.SERVER_URL_PRESSURENET;

	public final String PREFS_NAME = "ca.cumulonimbus.barometernetwork_preferences";

	public String mAppDir = "";

	boolean mBound;
	Messenger mService = null;

	private long lastConditionsSubmit = 0;

	private boolean sending = false;

	static final int REQUEST_IMAGE_CAPTURE = 1;

	private boolean shareToTwitter = false;

	private boolean precipStateSelected = false;
	private boolean extremeStateSelected = false;
	private boolean lightningStateSelected = false;
	
	MixpanelAPI mixpanel;

	private boolean sharingEnabled() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		return sharedPreferences.getBoolean("enable_social", true);
	}

	private boolean socialAssumed() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		return sharedPreferences.getBoolean("assume_social", false);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

	}

	@Override
	protected void onStart() {
		// Get tracker.
		Tracker t = ((PressureNetApplication) getApplication())
				.getTracker(TrackerName.APP_TRACKER);
		// Set screen name.
		t.setScreenName("Current Conditions");

		// Send a screen view.
		t.send(new HitBuilders.ScreenViewBuilder().build());
		super.onStart();
	}

	@Override
	protected void onStop() {
		mixpanel.flush();
		super.onStop();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		log("currentconditions onconfig changed");
		super.onConfigurationChanged(newConfig);
	}

	public void unBindCbService() {
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}

	public void bindCbService() {
		bindService(new Intent(getApplicationContext(), CbService.class),
				mConnection, Context.BIND_AUTO_CREATE);
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			mBound = true;
			Message msg = Message.obtain(null, CbService.MSG_OKAY);
		}

		public void onServiceDisconnected(ComponentName className) {
			mBound = false;
		}
	};

	// Get the phone ID and hash it
	public String getID() {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");

			String actual_id = Secure.getString(getApplicationContext()
					.getContentResolver(), Secure.ANDROID_ID);
			byte[] bytes = actual_id.getBytes();
			byte[] digest = md.digest(bytes);
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < digest.length; i++) {
				hexString.append(Integer.toHexString(0xFF & digest[i]));
			}
			return hexString.toString();
		} catch (Exception e) {
			return "--";
		}
	}

	// Preparation for sending a condition through the network.
	// Take the object and NVP it.
	public List<NameValuePair> currentConditionToNVP(CbCurrentCondition cc) {
		List<NameValuePair> nvp = new ArrayList<NameValuePair>();
		nvp.add(new BasicNameValuePair("latitude", cc.getLocation()
				.getLatitude() + ""));
		nvp.add(new BasicNameValuePair("longitude", cc.getLocation()
				.getLongitude() + ""));
		nvp.add(new BasicNameValuePair("general_condition", cc
				.getGeneral_condition() + ""));
		nvp.add(new BasicNameValuePair("user_id", cc.getUser_id() + ""));
		nvp.add(new BasicNameValuePair("time", cc.getTime() + ""));
		nvp.add(new BasicNameValuePair("tzoffset", cc.getTzoffset() + ""));
		nvp.add(new BasicNameValuePair("windy", cc.getWindy() + ""));
		nvp.add(new BasicNameValuePair("precipitation_type", cc
				.getPrecipitation_type() + ""));
		nvp.add(new BasicNameValuePair("precipitation_amount", cc
				.getPrecipitation_amount() + ""));
		nvp.add(new BasicNameValuePair("thunderstorm_intensity", cc
				.getThunderstorm_intensity() + ""));
		nvp.add(new BasicNameValuePair("cloud_type", cc.getCloud_type() + ""));
		nvp.add(new BasicNameValuePair("foggy", cc.getFog_thickness() + ""));
		return nvp;
	}

	/**
	 * Moon phase info
	 */
	private int getMoonPhaseIndex() {
		MoonPhase mp = new MoonPhase(Calendar.getInstance());
		return mp.getPhaseIndex();
	}

	public void pickAndSetMoonIcon(boolean on) {

		int moonNumber = getMoonPhaseIndex() + 1;

		switch (moonNumber) {
		case 1:
			if (on) {
				buttonSunny.setImageResource(R.drawable.ic_wea_on_moon1);
			} else {
				buttonSunny.setImageResource(R.drawable.ic_wea_moon1);
			}
			break;
		case 2:
			if (on) {
				buttonSunny.setImageResource(R.drawable.ic_wea_on_moon2);
			} else {
				buttonSunny.setImageResource(R.drawable.ic_wea_moon2);
			}
			break;
		case 3:
			if (on) {
				buttonSunny.setImageResource(R.drawable.ic_wea_on_moon3);
			} else {
				buttonSunny.setImageResource(R.drawable.ic_wea_moon3);
			}
			break;
		case 4:
			if (on) {
				buttonSunny.setImageResource(R.drawable.ic_wea_on_moon4);
			} else {
				buttonSunny.setImageResource(R.drawable.ic_wea_moon4);
			}
			break;
		case 5:
			if (on) {
				buttonSunny.setImageResource(R.drawable.ic_wea_on_moon5);
			} else {
				buttonSunny.setImageResource(R.drawable.ic_wea_moon5);
			}
			break;
		case 6:
			if (on) {
				buttonSunny.setImageResource(R.drawable.ic_wea_on_moon6);
			} else {
				buttonSunny.setImageResource(R.drawable.ic_wea_moon6);
			}
			break;
		case 7:
			if (on) {
				buttonSunny.setImageResource(R.drawable.ic_wea_on_moon7);
			} else {
				buttonSunny.setImageResource(R.drawable.ic_wea_moon7);
			}
			break;
		case 8:
			if (on) {
				buttonSunny.setImageResource(R.drawable.ic_wea_on_moon8);
			} else {
				buttonSunny.setImageResource(R.drawable.ic_wea_moon8);
			}
			break;
		default:
			if (on) {
				buttonSunny.setImageResource(R.drawable.ic_wea_on_moon2);
			} else {
				buttonSunny.setImageResource(R.drawable.ic_wea_moon2);
			}
			break;
		}
	}

	/**
	 * Choose icon between sun and moon depending on daytimes and on/off status.
	 */
	public void setCorrectClearIcon(boolean on) {
		if (isDaytime(
				mLatitude,
				mLongitude,
				System.currentTimeMillis(),
				Calendar.getInstance().getTimeZone()
						.getOffset(System.currentTimeMillis()))) {
			// set to Sun icon
			if (on) {
				buttonSunny.setImageResource(R.drawable.ic_wea_on_sun);
			} else {
				buttonSunny.setImageResource(R.drawable.ic_wea_sun);
			}
		} else {
			// set to Moon icon
			pickAndSetMoonIcon(on);
		}

	}

	/**
	 * Change the buttons on the UI. General Conditions.
	 * 
	 * @param condition
	 */
	private void switchActiveGeneral(String condition) {
		// Turn everything off
		setCorrectClearIcon(false);
		buttonFoggy.setImageResource(R.drawable.ic_wea_fog3);
		buttonCloudy.setImageResource(R.drawable.ic_wea_cloud);
		buttonPrecipitation.setImageResource(R.drawable.ic_wea_precip);
		buttonThunderstorm.setImageResource(R.drawable.ic_wea_r_l0);
		buttonExtreme.setImageResource(R.drawable.ic_wea_severe);

		// Turn the new one on
		if (condition.equals(getString(R.string.sunny))) {
			setCorrectClearIcon(true);
			scrollPrecipitation.setVisibility(View.GONE);
			textPrecipitationDescription.setVisibility(View.GONE);
			imageHrPrecipitation.setVisibility(View.GONE);

			scrollPrecipitationAmount.setVisibility(View.GONE);
			textPrecipitationAmountDescription.setVisibility(View.GONE);
			imageHrPrecipitationAmount.setVisibility(View.GONE);
			textCloudyDescription.setVisibility(View.GONE);
			scrollClouds.setVisibility(View.GONE);
			scrollFoggy.setVisibility(View.GONE);
			textFoggyDescription.setVisibility(View.GONE);
			imageHrFoggy.setVisibility(View.GONE);
			imageHrCloudy.setVisibility(View.GONE);

			if (!lightningStateSelected) {
				scrollLightning.setVisibility(View.GONE);
				textLightningDescription.setVisibility(View.GONE);
				imageHrLightning.setVisibility(View.GONE);
			}

			hrExtreme.setVisibility(View.GONE);
			layoutExtreme.setVisibility(View.GONE);
			textExtremeDescription.setVisibility(View.GONE);
			// And enable the submit button
			buttonSendCondition.setEnabled(true);

			this.condition.setGeneral_condition(getString(R.string.sunny));
		} else if (condition.equals(getString(R.string.foggy))) {
			buttonFoggy.setImageResource(R.drawable.ic_wea_on_fog3);
			scrollPrecipitation.setVisibility(View.GONE);
			textPrecipitationDescription.setVisibility(View.GONE);
			imageHrPrecipitation.setVisibility(View.GONE);
			if (!lightningStateSelected) {
				scrollLightning.setVisibility(View.GONE);
				textLightningDescription.setVisibility(View.GONE);
				imageHrLightning.setVisibility(View.GONE);
			}
			scrollPrecipitationAmount.setVisibility(View.GONE);
			textPrecipitationAmountDescription.setVisibility(View.GONE);
			imageHrPrecipitationAmount.setVisibility(View.GONE);
			textCloudyDescription.setVisibility(View.GONE);
			scrollClouds.setVisibility(View.GONE);
			scrollFoggy.setVisibility(View.VISIBLE);
			textFoggyDescription.setVisibility(View.VISIBLE);
			imageHrFoggy.setVisibility(View.VISIBLE);
			imageHrCloudy.setVisibility(View.GONE);

			hrExtreme.setVisibility(View.GONE);
			layoutExtreme.setVisibility(View.GONE);
			textExtremeDescription.setVisibility(View.GONE);
			// And enable the submit button
			buttonSendCondition.setEnabled(true);

			this.condition.setGeneral_condition(getString(R.string.foggy));
			this.condition.setFog_thickness(getString(R.string.light_fog));
		} else if (condition.equals(getString(R.string.cloudy))) {
			buttonCloudy.setImageResource(R.drawable.ic_wea_on_cloud);
			scrollPrecipitation.setVisibility(View.GONE);
			textPrecipitationDescription.setVisibility(View.GONE);
			imageHrPrecipitation.setVisibility(View.GONE);
			imageHrPrecipitationAmount.setVisibility(View.GONE);
			scrollPrecipitationAmount.setVisibility(View.GONE);
			textPrecipitationAmountDescription.setVisibility(View.GONE);
			if (!lightningStateSelected) {
				scrollLightning.setVisibility(View.GONE);
				textLightningDescription.setVisibility(View.GONE);
				imageHrLightning.setVisibility(View.GONE);
			}
			imageHrLightning.setVisibility(View.GONE);
			textCloudyDescription.setVisibility(View.VISIBLE);
			imageHrCloudy.setVisibility(View.VISIBLE);
			scrollClouds.setVisibility(View.VISIBLE);
			scrollFoggy.setVisibility(View.GONE);
			textFoggyDescription.setVisibility(View.GONE);
			imageHrFoggy.setVisibility(View.GONE);

			hrExtreme.setVisibility(View.GONE);
			layoutExtreme.setVisibility(View.GONE);
			textExtremeDescription.setVisibility(View.GONE);

			// And enable the submit button
			buttonSendCondition.setEnabled(true);

			this.condition.setGeneral_condition(getString(R.string.cloudy));
			this.condition.setCloud_type(getString(R.string.mostly_cloudy));
		} else if (condition.equals(getString(R.string.precipitation))) {
			// Visibility of other rows
			scrollPrecipitation.setVisibility(View.VISIBLE);
			textPrecipitationDescription.setVisibility(View.VISIBLE);
			imageHrPrecipitation.setVisibility(View.VISIBLE);
			textCloudyDescription.setVisibility(View.GONE);
			imageHrCloudy.setVisibility(View.GONE);
			scrollClouds.setVisibility(View.GONE);
			buttonPrecipitation.setImageResource(R.drawable.ic_wea_on_precip);
			if (!lightningStateSelected) {
				scrollLightning.setVisibility(View.GONE);
				textLightningDescription.setVisibility(View.GONE);
				imageHrLightning.setVisibility(View.GONE);
			}
			imageHrFoggy.setVisibility(View.GONE);
			// Precipitation initialization
			// buttonRain.setImageResource(R.drawable.ic_on_rain3);
			// textPrecipitationDescription.setText(getString(R.string.rain));
			scrollFoggy.setVisibility(View.GONE);
			textFoggyDescription.setVisibility(View.GONE);

			// And disable the submit button
			if (!precipStateSelected) {
				buttonSendCondition.setEnabled(false);
			}

			// hrExtreme.setVisibility(View.GONE);
			// layoutExtreme.setVisibility(View.GONE);
			// textExtremeDescription.setVisibility(View.GONE);

			this.condition
					.setGeneral_condition(getString(R.string.precipitation));
			this.condition.setPrecipitation_type(getString(R.string.rain));
			this.condition.setPrecipitation_amount(0);
		} else if (condition.equals(getString(R.string.thunderstorm))) {
			scrollLightning.setVisibility(View.VISIBLE);
			textLightningDescription.setVisibility(View.VISIBLE);
			imageHrLightning.setVisibility(View.VISIBLE);
			buttonThunderstorm.setImageResource(R.drawable.ic_wea_on_r_l0);
			textCloudyDescription.setVisibility(View.GONE);
			imageHrCloudy.setVisibility(View.GONE);
			scrollClouds.setVisibility(View.GONE);
			scrollFoggy.setVisibility(View.GONE);
			textFoggyDescription.setVisibility(View.GONE);
			imageHrFoggy.setVisibility(View.GONE);
			layoutExtreme.setVisibility(View.GONE);

			hrExtreme.setVisibility(View.GONE);
			layoutExtreme.setVisibility(View.GONE);
			textExtremeDescription.setVisibility(View.GONE);

			// And enable the submit button
			buttonSendCondition.setEnabled(true);

			this.condition
					.setGeneral_condition(getString(R.string.thunderstorm));
			this.condition
					.setThunderstorm_intensity(getString(R.string.infrequentLightning));
		} else if (condition.equals(getString(R.string.extreme))) {
			if (!lightningStateSelected) {
				scrollLightning.setVisibility(View.GONE);
				textLightningDescription.setVisibility(View.GONE);
				imageHrLightning.setVisibility(View.GONE);
			}
			buttonExtreme.setImageResource(R.drawable.ic_wea_on_severe);
			textCloudyDescription.setVisibility(View.GONE);
			imageHrCloudy.setVisibility(View.GONE);
			scrollClouds.setVisibility(View.GONE);
			scrollFoggy.setVisibility(View.GONE);
			textFoggyDescription.setVisibility(View.GONE);
			imageHrFoggy.setVisibility(View.GONE);

			layoutExtreme.setVisibility(View.VISIBLE);
			textExtremeDescription.setVisibility(View.VISIBLE);
			hrExtreme.setVisibility(View.VISIBLE);

			if (!extremeStateSelected) {
				// And disable the submit button
				buttonSendCondition.setEnabled(false);
			}

			this.condition.setGeneral_condition(getString(R.string.extreme));
		}

		// Whichever one is chosen, show windy
		// scrollWind.setVisibility(View.VISIBLE);
		textWindyDescription.setVisibility(View.VISIBLE);
	}

	/**
	 * Change the buttons on the UI. Foggy
	 * 
	 * @param condition
	 */
	private void switchActiveFoggy(String foggy) {
		// Turn everything off
		buttonLightFog.setImageResource(R.drawable.ic_wea_fog1);
		buttonModerateFog.setImageResource(R.drawable.ic_wea_fog2);
		buttonHeavyFog.setImageResource(R.drawable.ic_wea_fog3);

		// Turn the new one on

		if (foggy.equals(getString(R.string.light_fog))) {
			buttonLightFog.setImageResource(R.drawable.ic_wea_on_fog1);
		} else if (foggy.equals(getString(R.string.moderate_fog))) {
			buttonModerateFog.setImageResource(R.drawable.ic_wea_on_fog2);
		} else if (foggy.equals(getString(R.string.heavy_fog))) {
			buttonHeavyFog.setImageResource(R.drawable.ic_wea_on_fog3);
		}
	}

	/**
	 * Change the buttons on the UI. Extreme
	 * 
	 * @param condition
	 */
	private void switchActiveExtreme(String condition) {
		// Turn everything off
		buttonTornado.setImageResource(R.drawable.ic_wea_tornado);
		buttonTropicalStorm.setImageResource(R.drawable.ic_wea_tropical_storm);
		buttonFire.setImageResource(R.drawable.ic_wea_fire);
		buttonFlooding.setImageResource(R.drawable.ic_wea_flooding);
		buttonDuststorm.setImageResource(R.drawable.ic_wea_dust);

		// And enable the submit button
		buttonSendCondition.setEnabled(true);
		extremeStateSelected = true;

		// Turn the new one on
		if (condition.equals(getString(R.string.flooding))) {
			buttonFlooding.setImageResource(R.drawable.ic_wea_on_flooding);
		} else if (condition.equals(getString(R.string.wildfire))) {
			buttonFire.setImageResource(R.drawable.ic_wea_on_fire);
		} else if (condition.equals(getString(R.string.tornado))) {
			buttonTornado.setImageResource(R.drawable.ic_wea_on_tornado);
		} else if (condition.equals(getString(R.string.duststorm))) {
			buttonDuststorm.setImageResource(R.drawable.ic_wea_on_dust);
		} else if (condition.equals(getString(R.string.tropicalstorm))) {
			buttonTropicalStorm
					.setImageResource(R.drawable.ic_wea_on_tropical_storm);
		}
	}

	/**
	 * Change the buttons on the UI. Windy
	 * 
	 * @param condition
	 */
	private void switchActiveWindy(String condition) {
		// Turn everything off
		buttonIsCalm.setImageResource(R.drawable.ic_wea_wind0);
		buttonIsWindy1.setImageResource(R.drawable.ic_wea_wind1);
		buttonIsWindy2.setImageResource(R.drawable.ic_wea_wind2);
		buttonIsWindy3.setImageResource(R.drawable.ic_wea_wind3);

		// Turn the new one on
		if (condition.equals(getString(R.string.calm))) {
			buttonIsCalm.setImageResource(R.drawable.ic_wea_on_wind0);
		} else if (condition.equals(getString(R.string.windyOne))) {
			buttonIsWindy1.setImageResource(R.drawable.ic_wea_on_wind1);
		} else if (condition.equals(getString(R.string.windyTwo))) {
			buttonIsWindy2.setImageResource(R.drawable.ic_wea_on_wind2);
		} else if (condition.equals(getString(R.string.windyThree))) {
			buttonIsWindy3.setImageResource(R.drawable.ic_wea_on_wind3);
		}
	}

	/**
	 * When the type changes, we show the new type icon for the heaviness of the
	 * precipitation type
	 * 
	 * @param condition
	 */
	private void switchVisiblePrecipitations(String precipCondition) {
		if (precipCondition.equals(getString(R.string.rain))) {
			buttonLowPrecip.setImageResource(R.drawable.ic_wea_on_rain1);
			buttonModeratePrecip.setImageResource(R.drawable.ic_wea_rain2);
			buttonHeavyPrecip.setImageResource(R.drawable.ic_wea_rain3);
		} else if (precipCondition.equals(getString(R.string.snow))) {
			buttonLowPrecip.setImageResource(R.drawable.ic_wea_on_snow1);
			buttonModeratePrecip.setImageResource(R.drawable.ic_wea_snow2);
			buttonHeavyPrecip.setImageResource(R.drawable.ic_wea_snow3);
		} else if (precipCondition.equals(getString(R.string.hail))) {
			buttonLowPrecip.setImageResource(R.drawable.ic_wea_on_hail1);
			buttonModeratePrecip.setImageResource(R.drawable.ic_wea_hail2);
			buttonHeavyPrecip.setImageResource(R.drawable.ic_wea_hail3);
		}

		precipStateSelected = true;

		double value = 0.0;
		String printValue = getString(R.string.minimal)
				+ condition.getPrecipitation_type();
		switchActivePrecipitationAmount("low");
		condition.setPrecipitation_amount(value);
		textPrecipitationAmountDescription.setText(printValue);
	}

	/**
	 * Change the buttons on the UI. Cloudy
	 * 
	 * @param condition
	 */
	private void switchActiveCloudy(String cloudyCondition) {
		// Turn everything off
		buttonPartlyCloudy.setImageResource(R.drawable.ic_wea_cloud1);
		buttonMostlyCloudy.setImageResource(R.drawable.ic_wea_cloud2);
		buttonVeryCloudy.setImageResource(R.drawable.ic_wea_cloud);

		// Turn the new one on
		if (cloudyCondition.equals(getString(R.string.partly_cloudy))) {
			switchVisiblePrecipitations(getString(R.string.partly_cloudy));
			buttonPartlyCloudy.setImageResource(R.drawable.ic_wea_on_cloud1);
		} else if (cloudyCondition.equals(getString(R.string.mostly_cloudy))) {
			switchVisiblePrecipitations(getString(R.string.mostly_cloudy));
			buttonMostlyCloudy.setImageResource(R.drawable.ic_wea_on_cloud2);
		} else if (cloudyCondition.equals(getString(R.string.very_cloudy))) {
			switchVisiblePrecipitations(getString(R.string.very_cloudy));
			buttonVeryCloudy.setImageResource(R.drawable.ic_wea_on_cloud);
		}

	}

	/**
	 * Change the buttons on the UI. Precipitation
	 * 
	 * @param condition
	 */
	private void switchActivePrecipitation(String precipCondition) {
		// Turn everything off
		buttonRain.setImageResource(R.drawable.ic_wea_rain3);
		buttonSnow.setImageResource(R.drawable.ic_wea_snow3);
		buttonHail.setImageResource(R.drawable.ic_wea_hail3);

		// And enable the submit button
		buttonSendCondition.setEnabled(true);

		// Turn the new one on
		if (precipCondition.equals(getString(R.string.rain))) {
			switchVisiblePrecipitations(getString(R.string.rain));
			buttonRain.setImageResource(R.drawable.ic_wea_on_rain3);
		} else if (precipCondition.equals(getString(R.string.snow))) {
			switchVisiblePrecipitations(getString(R.string.snow));
			buttonSnow.setImageResource(R.drawable.ic_wea_on_snow3);
		} else if (precipCondition.equals(getString(R.string.hail))) {
			switchVisiblePrecipitations(getString(R.string.hail));
			buttonHail.setImageResource(R.drawable.ic_wea_on_hail3);
		}

		scrollPrecipitationAmount.setVisibility(View.VISIBLE);
		textPrecipitationAmountDescription.setVisibility(View.VISIBLE);
		imageHrPrecipitationAmount.setVisibility(View.VISIBLE);
	}

	/**
	 * Change the buttons on the UI. Precipitation Amounts
	 * 
	 * @param condition
	 */
	private void switchActivePrecipitationAmount(String amount) {

		// Off and on, all in one go
		try {
			if (condition.getPrecipitation_type().equals(
					getString(R.string.rain))) {
				if (amount.equals("low")) {
					buttonLowPrecip
							.setImageResource(R.drawable.ic_wea_on_rain1);
					buttonModeratePrecip
							.setImageResource(R.drawable.ic_wea_rain2);
					buttonHeavyPrecip.setImageResource(R.drawable.ic_wea_rain3);
				} else if (amount.equals("moderate")) {
					buttonLowPrecip.setImageResource(R.drawable.ic_wea_rain1);
					buttonModeratePrecip
							.setImageResource(R.drawable.ic_wea_on_rain2);
					buttonHeavyPrecip.setImageResource(R.drawable.ic_wea_rain3);
				} else if (amount.equals("heavy")) {
					buttonLowPrecip.setImageResource(R.drawable.ic_wea_rain1);
					buttonModeratePrecip
							.setImageResource(R.drawable.ic_wea_rain2);
					buttonHeavyPrecip
							.setImageResource(R.drawable.ic_wea_on_rain3);
				}
			} else if (condition.getPrecipitation_type().equals(
					getString(R.string.snow))) {
				if (amount.equals("low")) {
					buttonLowPrecip
							.setImageResource(R.drawable.ic_wea_on_snow1);
					buttonModeratePrecip
							.setImageResource(R.drawable.ic_wea_snow2);
					buttonHeavyPrecip.setImageResource(R.drawable.ic_wea_snow3);
				} else if (amount.equals("moderate")) {
					buttonLowPrecip.setImageResource(R.drawable.ic_wea_snow1);
					buttonModeratePrecip
							.setImageResource(R.drawable.ic_wea_on_snow2);
					buttonHeavyPrecip.setImageResource(R.drawable.ic_wea_snow3);
				} else if (amount.equals("heavy")) {
					buttonLowPrecip.setImageResource(R.drawable.ic_wea_snow1);
					buttonModeratePrecip
							.setImageResource(R.drawable.ic_wea_snow2);
					buttonHeavyPrecip
							.setImageResource(R.drawable.ic_wea_on_snow3);
				}
			} else if (condition.getPrecipitation_type().equals(
					getString(R.string.hail))) {
				if (amount.equals("low")) {
					buttonLowPrecip
							.setImageResource(R.drawable.ic_wea_on_hail1);
					buttonModeratePrecip
							.setImageResource(R.drawable.ic_wea_hail2);
					buttonHeavyPrecip.setImageResource(R.drawable.ic_wea_hail3);
				} else if (amount.equals("moderate")) {
					buttonLowPrecip.setImageResource(R.drawable.ic_wea_hail1);
					buttonModeratePrecip
							.setImageResource(R.drawable.ic_wea_on_hail2);
					buttonHeavyPrecip.setImageResource(R.drawable.ic_wea_hail3);
				} else if (amount.equals("heavy")) {
					buttonLowPrecip.setImageResource(R.drawable.ic_wea_hail1);
					buttonModeratePrecip
							.setImageResource(R.drawable.ic_wea_hail2);
					buttonHeavyPrecip
							.setImageResource(R.drawable.ic_wea_on_hail3);
				}
			}
		} catch (NullPointerException npe) {
			// must have a precipitation type set.
		}
	}

	private void switchActiveLightning(String value) {
		// Turn everything off
		buttonInfrequentLightning
				.setImageResource(R.drawable.ic_wea_lightning1);
		buttonFrequentLightning.setImageResource(R.drawable.ic_wea_lightning2);
		buttonHeavyLightning.setImageResource(R.drawable.ic_wea_lightning3);

		lightningStateSelected = true;

		// Turn the new one on
		if (value.equals(getString(R.string.infrequentLightning))) {
			buttonInfrequentLightning
					.setImageResource(R.drawable.ic_wea_on_lightning1);
		} else if (value.equals(getString(R.string.frequentLightning))) {
			buttonFrequentLightning
					.setImageResource(R.drawable.ic_wea_on_lightning2);
		} else if (value.equals(getString(R.string.heavyLightning))) {
			;
			buttonHeavyLightning
					.setImageResource(R.drawable.ic_wea_on_lightning3);
		}
	}

	private void sendCondition() {
		if (mBound) {
			log("sending current condition");
			Message msg = Message.obtain(null,
					CbService.MSG_SEND_CURRENT_CONDITION, condition);
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				// e.printStackTrace();
			}
		} else {
			log("error: not bound");
		}
	}

	private void saveCondition() {
		if (mBound) {
			log("saving current condition");
			Message msg = Message.obtain(null,
					CbService.MSG_ADD_CURRENT_CONDITION, condition);
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				// e.printStackTrace();
			}
		} else {
			log("error: not bound");
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.current_conditions);
		log("currentconditions oncreate");
		bindCbService();
		try {
			String ns = Context.NOTIFICATION_SERVICE;
			NotificationManager nMgr = (NotificationManager) getSystemService(ns);
			nMgr.cancel(NotificationSender.CONDITION_NOTIFICATION_ID);
		} catch (Exception e) {

		}

		mixpanel = MixpanelAPI.getInstance(getApplicationContext(), PressureNetApplication.MIXPANEL_TOKEN);
		
		condition = new CbCurrentCondition();

		buttonSunny = (ImageButton) findViewById(R.id.buttonSunny);
		buttonFoggy = (ImageButton) findViewById(R.id.buttonFoggy);
		buttonCloudy = (ImageButton) findViewById(R.id.buttonCloudy);
		buttonPrecipitation = (ImageButton) findViewById(R.id.buttonPrecipitation);
		buttonThunderstorm = (ImageButton) findViewById(R.id.buttonThunderstorm);
		buttonSendCondition = (Button) findViewById(R.id.buttonSendCondition);
		buttonCancelCondition = (Button) findViewById(R.id.buttonCancelCondition);
		buttonIsWindy1 = (ImageButton) findViewById(R.id.buttonIsWindy1);
		buttonIsWindy2 = (ImageButton) findViewById(R.id.buttonIsWindy2);
		buttonIsWindy3 = (ImageButton) findViewById(R.id.buttonIsWindy3);
		buttonIsCalm = (ImageButton) findViewById(R.id.buttonIsCalm);
		buttonRain = (ImageButton) findViewById(R.id.buttonRain);
		buttonSnow = (ImageButton) findViewById(R.id.buttonSnow);
		buttonHail = (ImageButton) findViewById(R.id.buttonHail);
		buttonInfrequentLightning = (ImageButton) findViewById(R.id.buttonInfrequentLightning);
		buttonFrequentLightning = (ImageButton) findViewById(R.id.buttonFrequentLightning);
		buttonHeavyLightning = (ImageButton) findViewById(R.id.buttonHeavyLightning);
		buttonLowPrecip = (ImageButton) findViewById(R.id.buttonLowPrecip);
		buttonModeratePrecip = (ImageButton) findViewById(R.id.buttonModeratePrecip);
		buttonHeavyPrecip = (ImageButton) findViewById(R.id.buttonHeavyPrecip);
		buttonPartlyCloudy = (ImageButton) findViewById(R.id.buttonCloudy1);
		buttonMostlyCloudy = (ImageButton) findViewById(R.id.buttonCloudy2);
		buttonVeryCloudy = (ImageButton) findViewById(R.id.buttonCloudy3);
		buttonLightFog = (ImageButton) findViewById(R.id.buttonFoggy1);
		buttonModerateFog = (ImageButton) findViewById(R.id.buttonFoggy2);
		buttonHeavyFog = (ImageButton) findViewById(R.id.buttonFoggy3);
		buttonTwitter = (ImageButton) findViewById(R.id.buttonTwitter);

		buttonExtreme = (ImageButton) findViewById(R.id.buttonExtreme);
		buttonTornado = (ImageButton) findViewById(R.id.buttonTornado);
		buttonTropicalStorm = (ImageButton) findViewById(R.id.buttonTropicalStorm);
		buttonFire = (ImageButton) findViewById(R.id.buttonWildfire);
		buttonFlooding = (ImageButton) findViewById(R.id.buttonFlooding);
		buttonDuststorm = (ImageButton) findViewById(R.id.buttonDuststorm);

		layoutExtreme = (LinearLayout) findViewById(R.id.layoutExtreme);

		imageHrGeneral = (ImageView) findViewById(R.id.hrGeneral);
		imageHrPrecipitation = (ImageView) findViewById(R.id.hrPrecipitation);
		imageHrFoggy = (ImageView) findViewById(R.id.hrFoggy);
		imageHrCloudy = (ImageView) findViewById(R.id.hrCloudy);
		imageHrPrecipitationAmount = (ImageView) findViewById(R.id.hrPreciptitationAmount);
		imageHrLightning = (ImageView) findViewById(R.id.hrLightning);
		imageHrWindy = (ImageView) findViewById(R.id.hrWindy);

		textGeneralDescription = (TextView) findViewById(R.id.generalDescription);
		textWindyDescription = (TextView) findViewById(R.id.windyDescription);
		textLightningDescription = (TextView) findViewById(R.id.lightningDescription);
		textPrecipitationDescription = (TextView) findViewById(R.id.precipitationDescription);
		textPrecipitationAmountDescription = (TextView) findViewById(R.id.precipitationAmountDescription);
		textCloudyDescription = (TextView) findViewById(R.id.cloudyDescription);
		textFoggyDescription = (TextView) findViewById(R.id.foggyDescription);
		textExtremeDescription = (TextView) findViewById(R.id.extremeDescription);

		scrollGeneral = (HorizontalScrollView) findViewById(R.id.scrollGeneralCondition);
		// scrollWind = (ScrollView) findViewById(R.id.scrollWindy);
		scrollPrecipitation = (ScrollView) findViewById(R.id.scrollPrecip);
		scrollPrecipitationAmount = (ScrollView) findViewById(R.id.scrollPrecipAmount);
		scrollLightning = (ScrollView) findViewById(R.id.scrollLightning);
		scrollClouds = (ScrollView) findViewById(R.id.scrollClouds);
		scrollFoggy = (ScrollView) findViewById(R.id.scrollFog);

		hrExtreme = (ImageView) findViewById(R.id.hrExtreme);

		// addPhoto = (CheckBox) findViewById(R.id.checkAddPhoto);

		buttonDuststorm.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.duststorm);
				switchActiveExtreme(value);
				condition.setUser_comment(value);
				textExtremeDescription.setText(value);
			}
		});

		buttonFlooding.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.flooding);
				switchActiveExtreme(value);
				condition.setUser_comment(value);
				textExtremeDescription.setText(value);
			}
		});

		buttonFire.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.wildfire);
				switchActiveExtreme(value);
				condition.setUser_comment(value);
				textExtremeDescription.setText(value);
			}
		});

		buttonTropicalStorm.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.tropicalstorm);
				switchActiveExtreme(value);
				condition.setUser_comment(value);
				textExtremeDescription.setText(value);
			}
		});

		buttonTornado.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.tornado);
				switchActiveExtreme(value);
				condition.setUser_comment(value);
				textExtremeDescription.setText(value);
			}
		});

		buttonSendCondition.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sending = true;
				saveCondition();
				sendCondition();

				updateWidget();

				// save the time
				lastConditionsSubmit = System.currentTimeMillis();

				SharedPreferences sharedPreferences = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext());

				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putLong("lastConditionsSubmit", lastConditionsSubmit);
				editor.commit();

				PnDb pn = new PnDb(getApplicationContext());
				pn.open();
				pn.addDelivery(condition.getGeneral_condition(), condition
						.getLocation().getLatitude(), condition.getLocation()
						.getLongitude(), condition.getTime());
				pn.close();

				// Get tracker.
				Tracker t = ((PressureNetApplication) getApplication()).getTracker(
				    TrackerName.APP_TRACKER);
				// Build and send an Event.
				t.send(new HitBuilders.EventBuilder()
				    .setCategory(BarometerNetworkActivity.GA_CATEGORY_MAIN_APP)
				    .setAction("conditions_send_button")
				    .setLabel(condition.getGeneral_condition())
				    .build());

				JSONObject props = new JSONObject();
				try {
					props.put("Condition Type", condition.getGeneral_condition());
					mixpanel.track("Sent Condition", props);
				} catch (JSONException e) {
					log("setupmixpanel json exception " + e.getMessage());
					e.printStackTrace();
				}
				
				
				// take photo?
				/*
				 * if(addPhoto.isChecked()) { dispatchTakePictureIntent(); }
				 */

				// send to twitter?
				if (shareToTwitter) {
					sendTwitterIntent();

				} else {
					log("current conditions not sharing to twitter");
				}

				finish();

			}
		});

		buttonCancelCondition.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sending = false;
				condition.setGeneral_condition("");
				updateWidget();

				// Get tracker.
				Tracker t = ((PressureNetApplication) getApplication()).getTracker(
				    TrackerName.APP_TRACKER);
				// Build and send an Event.
				t.send(new HitBuilders.EventBuilder()
				    .setCategory(BarometerNetworkActivity.GA_CATEGORY_MAIN_APP)
				    .setAction("conditions_cancel_button")
				    .setLabel("Cancel")
				    .build());
				mixpanel.track("Cancel Condition", null);
				
				finish();
			}
		});

		/*
		 * General Conditions
		 */

		buttonSunny.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String value = getString(R.string.sunny);
				switchActiveGeneral(value);
				condition.setGeneral_condition(value);
				textGeneralDescription.setText(value);
			}
		});

		buttonFoggy.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String value = getString(R.string.foggy);
				switchActiveGeneral(value);
				condition.setGeneral_condition(value);
				textGeneralDescription.setText(value);
			}
		});

		buttonCloudy.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String value = getString(R.string.cloudy);
				switchActiveGeneral(value);
				condition.setGeneral_condition(value);
				textGeneralDescription.setText(value);
			}
		});

		buttonPrecipitation.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String value = getString(R.string.precipitation);
				switchActiveGeneral(value);
				condition.setGeneral_condition(value);
				textGeneralDescription.setText(value);
			}
		});

		buttonThunderstorm.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String value = getString(R.string.thunderstorm);
				switchActiveGeneral(value);
				condition.setGeneral_condition(value);
				textGeneralDescription.setText(value);
			}
		});

		buttonExtreme.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.extreme);
				switchActiveGeneral(value);
				condition.setGeneral_condition(value);
				textGeneralDescription.setText(value);
			}
		});

		/*
		 * Windy conditions
		 */

		buttonIsWindy1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.windyOne);
				switchActiveWindy(value);
				condition.setWindy(1 + "");
				textWindyDescription.setText(value);
			}
		});

		buttonIsWindy2.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.windyTwo);
				switchActiveWindy(value);
				condition.setWindy(2 + "");
				textWindyDescription.setText(value);
			}
		});

		buttonIsWindy3.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.windyThree);
				switchActiveWindy(value);
				condition.setWindy(3 + "");
				textWindyDescription.setText(value);
			}
		});

		buttonIsCalm.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.calm);
				switchActiveWindy(value);
				condition.setWindy(0 + "");
				textWindyDescription.setText(value);
			}
		});

		/*
		 * Precipitation Conditions
		 */

		buttonRain.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.rain);

				condition.setPrecipitation_type(value);
				textPrecipitationDescription.setText(value);
				switchActivePrecipitation(value);
			}
		});

		buttonSnow.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.snow);

				condition.setPrecipitation_type(value);
				textPrecipitationDescription.setText(value);
				switchActivePrecipitation(value);
			}
		});

		buttonHail.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.hail);

				condition.setPrecipitation_type(value);
				textPrecipitationDescription.setText(value);
				switchActivePrecipitation(value);
			}
		});

		/*
		 * Cloudy Conditions
		 */

		buttonPartlyCloudy.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.partly_cloudy);

				condition.setCloud_type(value);
				textCloudyDescription.setText(value);
				switchActiveCloudy(value);
			}
		});

		buttonMostlyCloudy.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.mostly_cloudy);

				condition.setCloud_type(value);
				textCloudyDescription.setText(value);
				switchActiveCloudy(value);
			}
		});

		buttonVeryCloudy.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.very_cloudy);

				condition.setCloud_type(value);
				textCloudyDescription.setText(value);
				switchActiveCloudy(value);
			}
		});

		/*
		 * Foggy Conditions
		 */

		buttonLightFog.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.light_fog);

				condition.setFog_thickness(value);
				textFoggyDescription.setText(value);
				switchActiveFoggy(value);
			}
		});

		buttonModerateFog.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.moderate_fog);

				condition.setFog_thickness(value);
				textFoggyDescription.setText(value);
				switchActiveFoggy(value);
			}
		});

		buttonHeavyFog.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.heavy_fog);

				condition.setCloud_type(value);
				textFoggyDescription.setText(value);
				switchActiveFoggy(value);
			}
		});

		/*
		 * Precipitation amount
		 */

		buttonLowPrecip.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				double value = 0.0;
				String printValue = getString(R.string.minimal)
						+ condition.getPrecipitation_type();
				condition.setPrecipitation_amount(value);
				switchActivePrecipitationAmount("low");
				textPrecipitationAmountDescription.setText(printValue);

			}
		});

		buttonModeratePrecip.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				double value = 1.0;
				String printValue = getString(R.string.moderate)
						+ condition.getPrecipitation_type();
				switchActivePrecipitationAmount("moderate");
				condition.setPrecipitation_amount(value);
				textPrecipitationAmountDescription.setText(printValue);

			}
		});

		buttonHeavyPrecip.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				double value = 2.0;
				String printValue = getString(R.string.heavy)
						+ condition.getPrecipitation_type();
				condition.setPrecipitation_amount(value);
				switchActivePrecipitationAmount("heavy");
				textPrecipitationAmountDescription.setText(printValue);
			}
		});

		/*
		 * Lightning
		 */

		buttonInfrequentLightning.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.infrequentLightning);
				switchActiveLightning(value);
				condition.setThunderstorm_intensity(value);
				textLightningDescription.setText(value);
			}
		});

		buttonFrequentLightning.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.frequentLightning);
				switchActiveLightning(value);
				condition.setThunderstorm_intensity(value);
				textLightningDescription.setText(value);
			}
		});

		buttonHeavyLightning.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String value = getString(R.string.heavyLightning);
				switchActiveLightning(value);
				condition.setThunderstorm_intensity(value);
				textLightningDescription.setText(value);
			}
		});

		buttonTwitter.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				shareToTwitter = !shareToTwitter;
				if (shareToTwitter) {
					turnSocialOn();
				} else {
					turnSocialOff();
				}

			}
		});

		// Start adding the data for our current condition
		Intent intent = getIntent();

		try {
			// mAppDir = bundle.getString("appdir");
			mLatitude = intent.getDoubleExtra("latitude", 0.0);
			mLongitude = intent.getDoubleExtra("longitude", -1.0);
			Location location = new Location("network");
			location.setLatitude(mLatitude);
			location.setLongitude(mLongitude);
			condition.setLocation(location);
			condition.setUser_id(getID());
			condition.setTime(Calendar.getInstance().getTimeInMillis());
			condition.setTzoffset(Calendar.getInstance().getTimeZone()
					.getOffset((long) condition.getTime()));

			if (mLatitude == 0.0) {
				Toast.makeText(getApplicationContext(),
						getString(R.string.noLocationAvailable),
						Toast.LENGTH_LONG).show();
				finish();

			}

			// cancel any notifications?
			if (intent.hasExtra("cancelNotification")) {
				if (intent.getBooleanExtra("cancelNotification", false)) {
					cancelNotification(BarometerNetworkActivity.NOTIFICATION_ID);
				}
			}
		} catch (Exception e) {
			log("conditions missing data, cannot submit");
		}

		// Check sunrise and sunset times to choose Sun vs. Moon
		if (isDaytime(
				mLatitude,
				mLongitude,
				System.currentTimeMillis(),
				Calendar.getInstance().getTimeZone()
						.getOffset(System.currentTimeMillis()))) {
			// set to Sun icon
			buttonSunny.setImageResource(R.drawable.ic_wea_sun);
		} else {
			// set to Moon icon
			pickAndSetMoonIcon(false);
		}

		if (getIntent().hasExtra("initial")) {
			String state = getIntent().getStringExtra("initial");
			if (state.equals("clear")) {
				buttonSunny.performClick();
			} else if (state.equals("fog")) {
				buttonFoggy.performClick();
			} else if (state.equals("cloud")) {
				buttonCloudy.performClick();
			} else if (state.equals("precip")) {
				buttonPrecipitation.performClick();
			} else if (state.equals("thunderstorm")) {
				buttonThunderstorm.performClick();
			} else if (state.equals("severe")) {
				buttonExtreme.performClick();
			}
			updateWidget();
			if (getIntent().hasExtra("from_widget")) {
				boolean fromWidget = getIntent().getBooleanExtra("from_widget",
						false);
				if (fromWidget) {

					// Get tracker.
					Tracker t = ((PressureNetApplication) getApplication()).getTracker(
					    TrackerName.APP_TRACKER);
					// Build and send an Event.
					t.send(new HitBuilders.EventBuilder()
					    .setCategory(BarometerNetworkActivity.GA_CATEGORY_MAIN_APP)
					    .setAction(BarometerNetworkActivity.GA_ACTION_BUTTON)
					    .setLabel("conditions_widget_opened_conditions_activity")
					    .build());
				}
			}
		}

		// Show or hide social icons
		if (sharingEnabled()) {
			showSocialIcons();
		} else {
			hideSocialIcons();
		}

		// Set the initial state: Sunny, no wind
		// Or guess from pressure data
		// condition.setGeneral_condition(getString(R.string.sunny));
		// buttonSunny.setImageResource(R.drawable.ic_on_sun);
		// textGeneralDescription.setText(getString(R.string.sunny));

		// buttonIsCalm.setImageResource(R.drawable.ic_on_wind0);
		// textWindyDescription.setText(getString(R.string.calm));
		// condition.setWindy(0 + "");
	}

	private void sendTwitterIntent() {
		log("current conditions sharing to twitter");

		String twitterCondition = "";
		String tweet = "";
		if (condition.getGeneral_condition().equals(
				getString(R.string.precipitation))) {
			if (condition.getPrecipitation_type().equals(
					getString(R.string.rain))) {
				twitterCondition = "raining";
			} else if (condition.getPrecipitation_type().equals(
					getString(R.string.snow))) {
				twitterCondition = "snowing";
			} else if (condition.getPrecipitation_type().equals(
					getString(R.string.hail))) {
				twitterCondition = "hailing";
			}
		} else if (condition.getGeneral_condition().equals(
				getString(R.string.cloudy))) {
			twitterCondition = "cloudy";
		} else if (condition.getGeneral_condition().equals(
				getString(R.string.foggy))) {
			twitterCondition = "foggy";
		} else if (condition.getGeneral_condition().equals(
				getString(R.string.thunderstorm))) {
			twitterCondition = "thunderstorming";
		} else if (condition.getGeneral_condition().equals(
				getString(R.string.sunny))) {
			twitterCondition = "clear";
		} else {
			twitterCondition = condition.getGeneral_condition();
		}

		if (condition.getGeneral_condition()
				.equals(getString(R.string.extreme))) {
			tweet = "#" + condition.getUser_comment() + " "
					+ getString(R.string.currentConditionsTweet);
		} else {
			tweet = "It's #" + twitterCondition + " "
					+ getString(R.string.currentConditionsTweet);
		}

		String tweetUrl = String
				.format("https://twitter.com/intent/tweet?text=%s&url=%s",
						URLEncoder.encode(tweet),
						URLEncoder
								.encode("https://play.google.com/store/apps/details?id=ca.cumulonimbus.barometernetwork"));
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweetUrl));

		// Narrow down to official Twitter app, if available:
		List<ResolveInfo> matches = getPackageManager().queryIntentActivities(
				intent, 0);
		for (ResolveInfo info : matches) {
			if (info.activityInfo.packageName.toLowerCase().startsWith(
					"com.twitter")) {
				intent.setPackage(info.activityInfo.packageName);
			}
		}

		startActivity(intent);
	}

	private void showSocialIcons() {
		buttonTwitter.setVisibility(View.VISIBLE);

		if (socialAssumed()) {
			turnSocialOn();
		} else {
			turnSocialOff();
		}
	}

	private void hideSocialIcons() {
		buttonTwitter.setVisibility(View.GONE);
	}

	private void turnSocialOn() {
		shareToTwitter = true;
		buttonTwitter.setImageResource(R.drawable.ic_wea_on_twitter);
	}

	private void turnSocialOff() {
		shareToTwitter = false;
		buttonTwitter.setImageResource(R.drawable.ic_wea_twitter);
	}

	@Override
	protected void onDestroy() {
		unBindCbService();
		super.onDestroy();
	}

	private void cancelNotification(int notifyId) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nMgr = (NotificationManager) getSystemService(ns);
		nMgr.cancel(notifyId);
	}

	public static boolean isDaytime(double latitude, double longitude,
			long time, long timeZoneOffset) {
		SunLocation sunLocation = new SunLocation(latitude, longitude);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		long tzHoursOffset = timeZoneOffset / (1000 * 60 * 60);
		String gmtString = "GMT";
		if (tzHoursOffset > 0) {
			gmtString += "+" + tzHoursOffset;
		} else if (tzHoursOffset < 0) {
			gmtString += tzHoursOffset;
		}
		SunriseSunsetCalculator sunCalculator = new SunriseSunsetCalculator(
				sunLocation, gmtString);
		calendar.setTimeZone(TimeZone.getTimeZone(gmtString));
		Calendar officialSunrise = sunCalculator
				.getOfficialSunriseCalendarForDate(calendar);
		Calendar officialSunset = sunCalculator
				.getOfficialSunsetCalendarForDate(calendar);

		// Make a reasonable guess about sunset/sunrise in case
		// the actual data isn't available for some reason
		int sunriseHour = 7;
		int sunsetHour = 20;
		try {
			sunriseHour = officialSunrise.get(Calendar.HOUR_OF_DAY);
			sunsetHour = officialSunset.get(Calendar.HOUR_OF_DAY);
		} catch (NullPointerException npe) {
			// TODO: investigate how this could be null
		}
		int nowHour = calendar.get(Calendar.HOUR_OF_DAY);

		return (nowHour >= sunriseHour) && (nowHour <= sunsetHour);
	}

	private void updateWidget() {
		Intent intent = new Intent(getApplicationContext(),
				ConditionsWidgetProvider.class);
		intent.setAction(ConditionsWidgetProvider.ACTION_UPDATEUI); // "android.appwidget.action.APPWIDGET_UPDATE"
		intent.putExtra("general_condition", condition.getGeneral_condition());
		int ids[] = AppWidgetManager.getInstance(getApplication())
				.getAppWidgetIds(
						new ComponentName(getApplication(),
								ConditionsWidgetProvider.class));
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
		sendBroadcast(intent);
	}

	/**
	 * Log data to SD card for debug purposes. To enable logging, ensure the
	 * Manifest allows writing to SD card.
	 * 
	 * @param text
	 */
	public void logToFile(String text) {
		try {
			OutputStream output = new FileOutputStream(mAppDir + "/log.txt",
					true);
			String logString = (new Date()).toString() + ": " + text + "\n";
			output.write(logString.getBytes());
			output.close();
		} catch (FileNotFoundException e) {

		} catch (IOException ioe) {

		}
	}

	public void log(String text) {
		if (PressureNETConfiguration.DEBUG_MODE) {
			// logToFile(text);
			System.out.println(text);
		}
	}

	@Override
	protected void onPause() {
		if (!sending) {
			// condition.setGeneral_condition("");
		}
		updateWidget();
		unBindCbService();
		super.onPause();
	}
}
