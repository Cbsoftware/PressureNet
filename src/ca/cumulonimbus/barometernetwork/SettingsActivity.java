package ca.cumulonimbus.barometernetwork;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.Toast;

public class SettingsActivity extends Activity {
	
	public static final String PREFS_NAME = "pressureNETPrefs";
	
	private String serverURL = "";
	
	SensorManager sm;
	
	boolean hasBarometer = true;
	
	Spinner spinnerFrequency;
	Spinner spinnerUnits;
	Spinner spinnerSharing;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Intent i = getIntent();
		hasBarometer = i.getBooleanExtra("hasBarometer", true);
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		
	    spinnerFrequency = (Spinner) findViewById(R.id.spinnerFrequency);
	    ArrayAdapter<CharSequence> adapterFrequency = ArrayAdapter.createFromResource(
	            this, R.array.refresh_frequency, android.R.layout.simple_spinner_item);
	    adapterFrequency.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinnerFrequency.setAdapter(adapterFrequency);
	    
	    spinnerUnits = (Spinner) findViewById(R.id.spinnerUnits);
	    ArrayAdapter<CharSequence> adapterUnits = ArrayAdapter.createFromResource(
	            this, R.array.default_units, android.R.layout.simple_spinner_item);
	    adapterUnits.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinnerUnits.setAdapter(adapterUnits);
	    
	    spinnerSharing = (Spinner) findViewById(R.id.spinnerSharing);
	    ArrayAdapter<CharSequence> adapterSharing = ArrayAdapter.createFromResource(
	    		this, R.array.privacy_settings, android.R.layout.simple_spinner_item);
	    adapterSharing.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinnerSharing.setAdapter(adapterSharing);
	    
	    
	    restoreSettings();
	    setUpListeners();
	    
	    setUI(); 
	    
	}
	
	
	public void setUI() {
		if(hasBarometer) {

		} else {
			spinnerFrequency.setVisibility(View.GONE);
			CheckBox checkBoxRefresh = (CheckBox) findViewById(R.id.checkboxRefresh);
			checkBoxRefresh.setVisibility(View.GONE);
			Button delete = (Button) findViewById(R.id.buttonDelete);
			delete.setVisibility(View.GONE);			
		}
	}

	public void saveRefresh(boolean value) {
	      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	      SharedPreferences.Editor editor = settings.edit();
	      editor.putBoolean("autoupdate", value);
	      editor.commit();
	}
	
	public void saveFrequency(String value) {
	      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	      SharedPreferences.Editor editor = settings.edit();
	      editor.putString("autofrequency", value);
	      editor.commit();
	}

	public void saveUnit(String value) {
	      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	      SharedPreferences.Editor editor = settings.edit();
	      editor.putString("units", value);
	      editor.commit();
	}
	
	public void saveSharingPrivacy(String value) {
	      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	      SharedPreferences.Editor editor = settings.edit();
	      editor.putString("sharing_preference", value);
	      editor.commit();
	}
	
	private void restoreSettings() {
		CheckBox refreshCheck = (CheckBox) findViewById(R.id.checkboxRefresh);
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	    refreshCheck.setChecked(settings.getBoolean("autoupdate", true));

	    
	    // TODO: This is definitely not the right way to do this.
	    
	    final Spinner spinnerUnits = (Spinner) findViewById(R.id.spinnerUnits);
	    String[] unitsArray = getResources().getStringArray(R.array.default_units);
	    String unitFound = settings.getString("units", "Millibars (mbar)");
	    int positionArray = 0;
	    for(int i = 0; i < unitsArray.length; i++) {
	    	if(unitsArray[i].equals(unitFound)) {
	    		positionArray=i;
	    	}
	    }
	    spinnerUnits.setSelection(positionArray);
	    
	    
	    final Spinner refreshFrequency = (Spinner) findViewById(R.id.spinnerFrequency);
	    String[] refreshArray = getResources().getStringArray(R.array.refresh_frequency);
	    String freq = settings.getString("autofrequency", "10 minutes");
	    int position = 0;
	    for(int i = 0; i < refreshArray.length; i++) {
	    	if(refreshArray[i].equals(freq)) {
	    		position=i;
	    	}
	    }
	    refreshFrequency.setSelection(position);
	    
	    final Spinner spinnerSharing = (Spinner) findViewById(R.id.spinnerSharing);
	    String[] sharingArray = getResources().getStringArray(R.array.privacy_settings);
	    String share = settings.getString("sharing_preference", "Cumulonimbus and Academic Researchers");
	    int positionShare = 0;
	    for(int i = 0; i < sharingArray.length; i++) {
	    	if(sharingArray[i].equals(share)) {
	    		positionShare=i;
	    	}
	    }
	    spinnerSharing.setSelection(positionShare);
	    
	    // Disable spinner if auto submit is unchecked
	    if(!refreshCheck.isChecked()) {
	    	refreshFrequency.setEnabled(false);
	    }
		
	}
	
	public String getID() {
    	try {
    		MessageDigest md = MessageDigest.getInstance("MD5");
    		
    		String actual_id = Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
    		byte[] bytes = actual_id.getBytes();
    		byte[] digest = md.digest(bytes);
    		StringBuffer hexString = new StringBuffer();
    		for(int i = 0; i< digest.length; i++) {
    			hexString.append(Integer.toHexString(0xFF & digest[i]));
    		}
    		return hexString.toString();
    	} catch(Exception e) {
    		return "--";
    	}
	}
	
    private class SendDeleteRequest extends AsyncTask<String, String, String> {
    	String responseText= "";
    	
    	@Override
		protected String doInBackground(String... arg0) {
    		DefaultHttpClient client = new SecureHttpClient(getApplicationContext());
        	HttpPost httppost = new HttpPost(serverURL);
        	String id = getID();
        	try {
        		List<NameValuePair> nvp = new ArrayList<NameValuePair>();
        		nvp.add(new BasicNameValuePair("download","full_delete_request"));
        		nvp.add(new BasicNameValuePair("userid",id));
        
        		httppost.setEntity(new UrlEncodedFormEntity(nvp));
        		HttpResponse response = client.execute(httppost);
        		HttpEntity responseEntity = response.getEntity();
        		
        		BufferedReader r = new BufferedReader(new InputStreamReader(responseEntity.getContent()));
        		
        		StringBuilder total = new StringBuilder();
        		
        		String line;
        		if(r!=null) {
    	    		while((line = r.readLine()) != null) {
    	    			total.append(line);
    	    		}
    	    		responseText = total.toString();
        		}
        		return responseText;
        		
        	}catch(Exception e) {
        		//Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        	}
	    	return responseText;
		}
		protected void onPostExecute(String result) {
			//Toast.makeText(getApplicationContext(), "Delete data result: " + responseText, Toast.LENGTH_SHORT).show();
			Toast.makeText(getApplicationContext(), "Data deleted.", Toast.LENGTH_SHORT).show();
		}
    }
	
	public void deleteUserData() {
		// show a dialog, listen for its response.
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getResources().getString(R.string.deleteWarning)).setPositiveButton("Continue", dialogDeleteClickListener)
		    .setNegativeButton("Cancel", dialogDeleteClickListener).show();
	}
	
	DialogInterface.OnClickListener dialogDeleteClickListener = new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	        switch (which){
	        case DialogInterface.BUTTON_POSITIVE:
	        	new SendDeleteRequest().execute("");
	        	break;

	        case DialogInterface.BUTTON_NEGATIVE:
	            break;
	        }
	    }
	};
	
	private void setUpListeners() {
		CheckBox refreshCheck = (CheckBox) findViewById(R.id.checkboxRefresh);
		final Spinner spinnerFrequency = (Spinner) findViewById(R.id.spinnerFrequency);
		final Spinner spinnerUnitPrefs = (Spinner) findViewById(R.id.spinnerUnits);
		final Spinner spinnerSharing = (Spinner) findViewById(R.id.spinnerSharing);
		final Button close = (Button) findViewById(R.id.buttonClose);
		final Button delete = (Button) findViewById(R.id.buttonDelete);
		
		close.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				finish();
				
			}
		});
		
		delete.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				deleteUserData();
				
			}
		});
		
		refreshCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean value) {
				if(value) {
					spinnerFrequency.setEnabled(true);
					saveRefresh(true);
				} else {
					spinnerFrequency.setEnabled(false);
					saveRefresh(false);
				}
			}
		});
		
		spinnerFrequency.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View view,
					int position, long id) {
				String[] array = getResources().getStringArray(R.array.refresh_frequency);
				saveFrequency(array[position]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
			
		});
		
		spinnerUnitPrefs.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View view,
					int position, long id) {
				String[] array = getResources().getStringArray(R.array.default_units);
				saveUnit(array[position]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
			
		});
		
		spinnerSharing.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View view,
					int position, long id) {
				String[] array = getResources().getStringArray(R.array.privacy_settings);
				saveSharingPrivacy(array[position]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
			
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
}
