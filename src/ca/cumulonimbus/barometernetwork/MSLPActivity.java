package ca.cumulonimbus.barometernetwork;

import android.app.Activity;
import android.os.Bundle;
import android.test.PerformanceTestCase;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import ca.cumulonimbus.pressurenetsdk.CbScience;

import com.google.analytics.tracking.android.EasyTracker;

public class MSLPActivity extends Activity {

	private EditText editAltitude;
	private Button buttonDistanceUnit;
	private EditText editTemperature;
	private Button buttonTemperatureUnit;
	private TextView textShowMSLP;
	private Button buttonGoMSLP;
	
	private double temperature = 288.15; // 15 C in K

	private double altitude = 0; // elevation + height
	private double elevation = 0; // sea-level to ground-level
	private double height = 0; // distance above ground level
	private double pressure = 990;
	
	public double pressureCalculation(double altitude, double temperature) {
		return CbScience.estimateMSLP(pressure, altitude, temperature);
	}

	private void setUpUI() {
		editAltitude = (EditText) findViewById(R.id.editAltitude);
		buttonDistanceUnit = (Button) findViewById(R.id.buttonDistanceUnit);
		editTemperature = (EditText) findViewById(R.id.editTemperature);
		buttonTemperatureUnit = (Button) findViewById(R.id.buttonTemperatureUnit);
		textShowMSLP = (TextView) findViewById(R.id.textShowMSLP);
		buttonGoMSLP = (Button) findViewById(R.id.buttonGoMSLP);

		buttonDistanceUnit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

			}
		});

		buttonTemperatureUnit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

			}
		});

		buttonGoMSLP.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				textShowMSLP.setText(pressureCalculation(Double.valueOf(editAltitude.getText().toString()), 
						273.15 + Double.valueOf(editTemperature.getText().toString())) + "");
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.mslp_activity);
		setUpUI();
		super.onCreate(savedInstanceState);
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
