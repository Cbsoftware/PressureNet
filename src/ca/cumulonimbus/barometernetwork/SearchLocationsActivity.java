package ca.cumulonimbus.barometernetwork;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class SearchLocationsActivity extends ListActivity {

	PnDb pn;
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		System.out.println("clicked " +  position  + " " + id );
		Intent resultIntent = new Intent();
		resultIntent.putExtra("location_id", id);
		setResult(Activity.RESULT_OK, resultIntent);
		finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_locations);
		
		pn = new PnDb(getApplicationContext());
		pn.open();
		Cursor cursor = pn.fetchAllLocations();
		
		if(cursor.getCount()==0) {
			Toast.makeText(getApplicationContext(), "No saved locations", Toast.LENGTH_SHORT).show();
			finish();
		}
		
		startManagingCursor(cursor);
		

		ListAdapter adapter = new SimpleCursorAdapter(this, 
				R.layout.location_list_item, 
				cursor, 
				new String[] {PnDb.KEY_SEARCH_TEXT},
				new int[] {R.id.textLocationName}
				);
		setListAdapter(adapter);
		
		
	}

	@Override
	protected void onStop() {
		if(pn!=null ) {
			pn.close();
		}
		super.onStop();
	}

	
	
}
