package ca.cumulonimbus.barometernetwork;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

public class EditLocationActivity extends Activity {

	private EditText editLocationName;
	private EditText editLatitude;
	private EditText editLongitude;
	private Button buttonSave;
	private Button buttonCancel;
	private Button buttonDelete;
	
	public void populateFields() {
		editLocationName = (EditText) findViewById(R.id.editLocationName);
		editLatitude = (EditText) findViewById(R.id.editLocationLatitude);
		editLongitude = (EditText) findViewById(R.id.editLocationLongitude);

		buttonCancel = (Button) findViewById(R.id.buttonCancelLocation);
		buttonSave = (Button) findViewById(R.id.buttonSaveLocation);
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_location);
		
		populateFields();
	}
	

}
