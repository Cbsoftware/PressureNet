package ca.cumulonimbus.barometernetwork;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class SkyPhotosActivity extends Activity {

	private double latitude = 0;
	private double longitude = 0;
	
	private ArrayList<SkyPhoto> photosList = new ArrayList<SkyPhoto>();
	
	private void loadSkyPhotos() {
		PnDb db = new PnDb(getApplicationContext());
		db.open();
		Cursor photosCursor = db.fetchLocalRecentSkyPhotos(
				latitude - .1,
				latitude + .1, 
				longitude - .1, 
				longitude + .1, 
				System.currentTimeMillis() - (1000 * 60 * 60 * 2));
		
		Toast.makeText(getApplicationContext(), "Loading " + photosCursor.getCount() + " photos", Toast.LENGTH_SHORT).show();
		while(photosCursor.moveToNext()) {
			SkyPhoto photo = new SkyPhoto();
			photo.setFileName(photosCursor.getString(1));
			photo.setLatitude(photosCursor.getDouble(2));
			photo.setLongitude(photosCursor.getDouble(3));
			photo.setTimeTaken(photosCursor.getInt(4));
			photo.setThumbnail(photosCursor.getBlob(5));
			photosList.add(photo);
		}
		
		db.close();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		int actionBarTitleId = getResources().getSystem().getIdentifier(
				"action_bar_title", "id", "android");
		TextView actionBarTextView = (TextView) findViewById(actionBarTitleId);
		actionBarTextView.setTextColor(Color.WHITE);
		
		Intent intent = getIntent();
		if (intent.hasExtra("latitude")) {
			latitude = intent.getDoubleExtra("latitude", 0);
			longitude = intent.getDoubleExtra("longitude", 0);
		}
		
		if(latitude == 0) {
			Toast.makeText(getApplicationContext(), "Location unknown, sky photos might not be nearby you", Toast.LENGTH_LONG).show();
		}
		
		loadSkyPhotos();
		
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onStart() {
		
		super.onStart();
	}

	@Override
	protected void onStop() {
		
		super.onStop();
	}

	
	
}
