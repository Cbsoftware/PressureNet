package ca.cumulonimbus.barometernetwork;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;

public class SkyPhotosActivity extends Activity {

	private double latitude = 0;
	private double longitude = 0;
	
	private ArrayList<SkyPhoto> photosList = new ArrayList<SkyPhoto>();
	
	private void loadSkyPhotos() {
		try {
			PnDb db = new PnDb(getApplicationContext());
			db.open();
			Cursor photosCursor = db.fetchLocalRecentSkyPhotos(
					latitude - .1,
					latitude + .1, 
					longitude - .1, 
					longitude + .1, 
					System.currentTimeMillis() - (1000 * 60 * 60 * 2));
			
			while(photosCursor.moveToNext()) {
				SkyPhoto photo = new SkyPhoto();
				photo.setFileName(photosCursor.getString(1));
				photo.setLatitude(photosCursor.getDouble(2));
				photo.setLongitude(photosCursor.getDouble(3));
				photo.setTimeTaken(photosCursor.getLong(4));
				photo.setThumbnail(photosCursor.getBlob(5));
				photosList.add(photo);
			}
			db.close();
		} catch (Exception e) {
			log("error loading sky photos");
		}
		
		for(SkyPhoto photo : photosList) {
			TextView photoCaption = new TextView(getApplicationContext());
			Calendar cal = Calendar.getInstance();	
			cal.setTimeInMillis(photo.getTimeTaken());
			SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
			photoCaption.setText(sdf.format(cal.getTimeInMillis()));
			
			ImageView imageView = new ImageView(getApplicationContext());
			Bitmap image = BitmapFactory.decodeByteArray(photo.getThumbnail(), 0, photo.getThumbnail().length);
			imageView.setImageBitmap(image);
			
			LinearLayout linear = (LinearLayout) findViewById(R.id.skyPhotosLayout);
	        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	       
	        imageView.setLayoutParams(params);
	        photoCaption.setLayoutParams(params);
	        
	        photoCaption.setPadding(8, 8, 8, 8);
	        photoCaption.setTextColor(Color.BLACK);
	        imageView.setPadding(8, 8, 8, 8);
	        
	        linear.addView(photoCaption, params);
	        linear.addView(imageView, params);
		}
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.sky_photos);
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
		EasyTracker.getInstance(this).activityStart(this); 
		super.onStart();
	}

	@Override
	protected void onStop() {
		EasyTracker.getInstance(this).activityStop(this); 
		super.onStop();
	}

	private void log(String message) {
		if (PressureNETConfiguration.DEBUG_MODE) {
			System.out.println(message);
		}
	}
	
}
