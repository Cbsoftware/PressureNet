package ca.cumulonimbus.barometernetwork;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import ca.cumulonimbus.barometernetwork.PressureNetApplication.TrackerName;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class EditLocationActivity extends Activity {

	private EditText editLocationName;
	private EditText editLatitude;
	private EditText editLongitude;
	private Button buttonSave;
	private Button buttonCancel;
	private Button buttonDelete;

	private String initialName = "";
	private double initialLatitude = 0;
	private double initialLongitude = 0;
	private long initialRowId = -1;

	@Override
	protected void onStart() {
		// Get tracker.
		Tracker t = ((PressureNetApplication) getApplication())
				.getTracker(TrackerName.APP_TRACKER);
		// Set screen name.
		t.setScreenName("Edit Location");

		// Send a screen view.
		t.send(new HitBuilders.ScreenViewBuilder().build());
		super.onStart();
	}

	public void populateFields() {
		// get the row id from the intent and load
		// the correct data
		Intent intent = getIntent();
		if (intent != null) {
			try {
				long rowId = intent.getLongExtra("rowId", -1);
				if (rowId == -1) {
					return;
				}

				PnDb pn = new PnDb(getApplicationContext());
				pn.open();
				Cursor c = pn.fetchLocation(rowId);
				pn.close();

				initialName = c.getString(1);
				initialLatitude = c.getDouble(2);
				initialLongitude = c.getDouble(3);
				initialRowId = rowId;

				editLocationName.setText(initialName);
				editLatitude.setText(initialLatitude + "");
				editLongitude.setText(initialLongitude + "");

			} catch (Exception e) {
				// meh
			}
		}

	}

	public void setUpUI() {
		editLocationName = (EditText) findViewById(R.id.editLocationName);
		editLatitude = (EditText) findViewById(R.id.editLocationLatitude);
		editLongitude = (EditText) findViewById(R.id.editLocationLongitude);

		buttonCancel = (Button) findViewById(R.id.buttonCancelLocation);
		buttonDelete = (Button) findViewById(R.id.buttonDeleteLocation);
		buttonSave = (Button) findViewById(R.id.buttonSaveLocation);

		buttonCancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});

		buttonDelete.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				PnDb pn = new PnDb(getApplicationContext());
				pn.open();
				pn.deleteLocation(initialRowId);
				pn.close();
				Toast.makeText(getApplicationContext(),
						getString(R.string.deleted) + initialName,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		});

		buttonSave.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					PnDb pn = new PnDb(getApplicationContext());
					pn.open();
					pn.updateLocation(initialRowId, editLocationName.getText()
							.toString(), Double.parseDouble(editLatitude
							.getText().toString()), Double
							.parseDouble(editLongitude.getText().toString()));
					pn.close();
					Toast.makeText(
							getApplicationContext(),
							getString(R.string.saved)
									+ editLocationName.getText().toString(),
							Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					// meh
				}
				finish();
			}
		});

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_location);
		setUpUI();
		populateFields();
	}

}
