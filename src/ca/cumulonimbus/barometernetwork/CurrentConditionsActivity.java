package ca.cumulonimbus.barometernetwork;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CurrentConditionsActivity extends Activity {

	Button buttonSunny;
	Button buttonFoggy;
	Button buttonCloudy;
	Button buttonPrecipitation;
	Button buttonThunderstorm;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.current_conditions);
		
		buttonSunny = (Button) findViewById(R.id.buttonSunny);
		buttonFoggy = (Button) findViewById(R.id.buttonFoggy);
		buttonCloudy = (Button) findViewById(R.id.buttonCloudy);
		buttonPrecipitation = (Button) findViewById(R.id.buttonPrecipitation);
		buttonThunderstorm = (Button) findViewById(R.id.buttonThunderstorm);
		
		buttonSunny.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// send out 'sunny' with 'current_condition' nvp
			}
		});
		
		buttonFoggy.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
			}
		});
		
		buttonCloudy.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
			}
		});
		
		buttonPrecipitation.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
			}
		});
		
		buttonThunderstorm.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
			}
		});
		
		
		
		
		
		
	}

	@Override
	protected void onPause() {

		super.onPause();
	}

	
	
}
