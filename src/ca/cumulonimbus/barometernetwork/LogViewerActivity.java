package ca.cumulonimbus.barometernetwork;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

public class LogViewerActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.logviewer);
		try {
			Intent intent = getIntent();
			String logString = intent.getStringExtra("log");
			EditText log = (EditText) findViewById(R.id.editLog);
			log.setText(logString);
			log.setSelection(0, logString.length());
			log.setFocusable(false);
			
		} catch(Exception e) {
			
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		
		super.onDestroy();
	}
	
}
