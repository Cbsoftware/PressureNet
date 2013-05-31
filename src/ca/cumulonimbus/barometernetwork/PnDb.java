package ca.cumulonimbus.barometernetwork;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PnDb {

	// Tables
	public static final String SEARCH_LOCATIONS_TABLE = "pn_searchlocations";

	// Search Locations Fields
	public static final String KEY_ROW_ID = "_id";
	public static final String KEY_SEARCH_TEXT = "search_text";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_LAST_CALL = "last_api_call";

	private Context mContext;

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDB;
	private static final String SEARCH_LOCATIONS_TABLE_CREATE = "create table "
			+ SEARCH_LOCATIONS_TABLE
			+ " (_id integer primary key autoincrement, " + KEY_SEARCH_TEXT
			+ " text not null, " + KEY_LATITUDE + " real not null, "
			+ KEY_LONGITUDE + " text not null, " + KEY_LAST_CALL + " real)";

	private static final String DATABASE_NAME = "PnDb";
	private static final int DATABASE_VERSION = 1;

	
	public PnDb open() throws SQLException {
		mDbHelper = new DatabaseHelper(mContext);
		mDB = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}
	
	
	/**
	 * Fetch every location
	 * 
	 * @return
	 */
	public Cursor fetchAllLocations() {
		return mDB.query(SEARCH_LOCATIONS_TABLE, new String[] { KEY_ROW_ID,
				KEY_SEARCH_TEXT, KEY_LATITUDE, KEY_LONGITUDE, KEY_LAST_CALL },
				null, null, null, null, null);
	}

	/**
	 * Update location
	 * 
	 * @return
	 */
	public long updateLocation(String searchText, double latitude,
			double longitude, long lastCall) {

		ContentValues newValues = new ContentValues();
		newValues.put(KEY_SEARCH_TEXT, searchText);
		newValues.put(KEY_LATITUDE, latitude);
		newValues.put(KEY_LONGITUDE, longitude);
		newValues.put(KEY_LAST_CALL, lastCall);

		return mDB.update(SEARCH_LOCATIONS_TABLE, newValues, KEY_SEARCH_TEXT
				+ "='" + searchText + "'", null);
	}

	public void deleteLocation(long rowId) {

		mDB.execSQL("delete from " + SEARCH_LOCATIONS_TABLE + " where " + 
		KEY_ROW_ID + "=" + rowId);
	
	}
	

	/**
	 * Add new location

	 * @return
	 */

	public long addLocation(String searchText, double latitude, double longitude, long lastCall) {

		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_SEARCH_TEXT, searchText);
		initialValues.put(KEY_LATITUDE, latitude);
		initialValues.put(KEY_LONGITUDE, longitude);
		initialValues.put(KEY_LAST_CALL, lastCall);

		return mDB.insert(SEARCH_LOCATIONS_TABLE, null, initialValues);
	}

	/**
	 * Get a single location
	 * 
	 * @param rowId
	 * @return
	 * @throws SQLException
	 * 
	 */
	public Cursor fetchLocation(long rowId) throws SQLException {
		Cursor mCursor =

		mDB.query(true, SEARCH_LOCATIONS_TABLE, new String[] { KEY_ROW_ID,
				KEY_SEARCH_TEXT, KEY_LATITUDE, KEY_LONGITUDE, KEY_LAST_CALL },
				KEY_ROW_ID + "=" + rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SEARCH_LOCATIONS_TABLE_CREATE);

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Build upgrade mechanism
			db.execSQL("DROP TABLE IF EXISTS " + SEARCH_LOCATIONS_TABLE);
			onCreate(db);
		}
	}
	
	public PnDb(Context ctx) {
		this.mContext = ctx;
	}

}
