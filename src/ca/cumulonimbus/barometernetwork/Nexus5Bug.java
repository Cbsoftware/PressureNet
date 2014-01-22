package ca.cumulonimbus.barometernetwork;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Nexus5Bug extends Activity {

	private Button buttonCloseBug;
	private Button nexus5StarTheIssue;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.nexus5bug);
		
		buttonCloseBug = (Button) findViewById(R.id.buttonCloseBug);
		nexus5StarTheIssue = (Button) findViewById(R.id.nexus5StarTheIssue);
		
		nexus5StarTheIssue.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://code.google.com/p/android/issues/detail?id=62938"));
				startActivity(browserIntent);				
			}
		});
		
		buttonCloseBug.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		super.onCreate(savedInstanceState);
	}

	
}
