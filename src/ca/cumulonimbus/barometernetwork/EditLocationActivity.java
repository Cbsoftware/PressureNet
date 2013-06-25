package ca.cumulonimbus.barometernetwork;

import ca.cumulonimbus.pressurenetsdk.CbApiCall;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;

public class EditLocationActivity extends Activity {

	private GoogleMap mMap;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_location);
		setUpMap();
	}


	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the
		// map.
		if (mMap == null) {
			mMap = ((MapFragment) getFragmentManager().findFragmentById(
					R.id.mapLocation)).getMap();			
		
			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				// The Map is verified. It is now safe to manipulate the map.
			}
		}

	}

	// Zoom into the user's location, add pinch zoom controls
	public void setUpMap() {
		setUpMapIfNeeded();

		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapLocation))
				.getMap();

		// TODO: Set default coordinates (centered at the location)

	}

	public void moveMapTo(double latitude, double longitude) {
		setUpMapIfNeeded();

		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapLocation))
				.getMap();
		try {
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
					latitude, longitude), 13));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
