package ca.cumulonimbus.barometernetwork;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

public class SearchLocations extends ListActivity {

	PnDb pn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_locations);
		
		pn = new PnDb(getApplicationContext());
		pn.open();
		Cursor cursor = pn.fetchAllLocations();
		
		
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
