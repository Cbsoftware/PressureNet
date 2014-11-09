package ca.cumulonimbus.barometernetwork;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

public class PnDb {

	// Tables
	public static final String SEARCH_LOCATIONS_TABLE = "pn_searchlocations";
	public static final String CONDITIONS_DELIVERED = "conditions_delivered";

	// Search Locations Fields
	public static final String KEY_ROW_ID = "_id";
	public static final String KEY_SEARCH_TEXT = "search_text";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_LAST_CALL = "last_api_call";
	
	// Conditions fields
	public static final String KEY_TIME = "time";
	public static final String KEY_CONDITION = "condition";
	
	// Sky photos fields
	public static final String KEY_IMAGE_FILENAME = "image_filename";
	// KEY_TIME
	// KEY_LATITUDE
	// KEY_LONGITUDE
	public static final String KEY_THUMBNAIL = "image_thumbnail";
	

	private Context mContext;

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDB;
	private static final String SEARCH_LOCATIONS_TABLE_CREATE = "create table "
			+ SEARCH_LOCATIONS_TABLE
			+ " (_id integer primary key autoincrement, " + KEY_SEARCH_TEXT
			+ " text not null, " + KEY_LATITUDE + " real not null, "
			+ KEY_LONGITUDE + " text not null, " + KEY_LAST_CALL + " real, UNIQUE (" + KEY_SEARCH_TEXT
			+ ") ON CONFLICT REPLACE)";
	
	private static final String CONDITIONS_DELIVERED_TABLE_CREATE = "create table "
			+ CONDITIONS_DELIVERED
			+ " (_id integer primary key autoincrement, " + KEY_CONDITION
			+ " text not null, " + KEY_LATITUDE + " real not null, "
			+ KEY_LONGITUDE + " real not null, " + KEY_TIME + " real)";
	

	private static final String DATABASE_NAME = "PnDb";
	private static final int DATABASE_VERSION = 20; 
	// TODO: fix this nonsense
	// db = 2 at pN <=4.0.11. 5=4.1.6, 6=4.1.7, 7=4.2.5, 8=4.2.6
	// 9 = 4.2.7
	// 10-12 = 4.3.0
	// 13 = 4.4.2-4
	// 14 = 4.4.6
	// 15 = 4.4.7
	// 16 = 4.4.8
	// 17-20 = 4.5.x
	
	public PnDb open() throws SQLException {
		mDbHelper = new DatabaseHelper(mContext);
		mDB = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	/**
	 * Add new condition delivery

	 * @return
	 */
	public long addDelivery(String condition, double latitude, double longitude, long time) {
		log("pndb adding delivery " + condition);
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_CONDITION, condition);
		initialValues.put(KEY_LATITUDE, latitude);
		initialValues.put(KEY_LONGITUDE, longitude);
		initialValues.put(KEY_TIME, time);

		return mDB.insert(CONDITIONS_DELIVERED, null, initialValues);
	}

	/**
	 * Delete old condition deliveries to keep the table small
	 */
	public void deleteOldDeliveries() {
		long ancientConditionMsAgo = 1000 * 60 * 60 * 10;
		mDB.execSQL("delete from " + CONDITIONS_DELIVERED + " where " + 
		KEY_TIME + "<" + ancientConditionMsAgo);
	}
	
	/**
	 * Fetch every condition delivery
	 * 
	 * @return
	 */
	public Cursor fetchAllDeliveries() {
		return mDB.query(CONDITIONS_DELIVERED, new String[] { KEY_ROW_ID,
				KEY_CONDITION, KEY_LATITUDE, KEY_LONGITUDE, KEY_TIME },
				null, null, null, null, null);
	}



	/**
	 * Fetch recent condition delivery
	 * 
	 * @return
	 */
	public Cursor fetchRecentDeliveries() {
		return mDB.query(CONDITIONS_DELIVERED, new String[] { KEY_ROW_ID,
				KEY_CONDITION, KEY_LATITUDE, KEY_LONGITUDE, KEY_TIME },
				KEY_TIME + " > " + (System.currentTimeMillis() - (1000 * 60 * 60 * 2)), null, null, null, null);
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
	public long updateLocation(long rowId, String searchText, double latitude,
			double longitude) {

		ContentValues newValues = new ContentValues();
		newValues.put(KEY_SEARCH_TEXT, searchText);
		newValues.put(KEY_LATITUDE, latitude);
		newValues.put(KEY_LONGITUDE, longitude);

		return mDB.update(SEARCH_LOCATIONS_TABLE, newValues, KEY_ROW_ID
				+ "='" + rowId + "'", null);
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
	
	private void showWelcome() {
		Intent intent = new Intent(mContext, NewWelcomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(intent);
	}
	
	private void showWhatsNew() {
		Intent intent = new Intent(mContext, WhatsNewActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(intent);
	}

	private class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SEARCH_LOCATIONS_TABLE_CREATE);
			db.execSQL(CONDITIONS_DELIVERED_TABLE_CREATE);
			
			showWelcome();
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Build upgrade mechanism
			
			// If upgrading from 4.2.5 to 4.2.6,
			// users get a fix for https://github.com/Cbsoftware/pressureNET/issues/119
			// Database table creation to support fix.
			if ((oldVersion <= 7) && (newVersion >= 8)) {
				db.execSQL(CONDITIONS_DELIVERED_TABLE_CREATE);
			}
			
			/*
			// add a table to store info about sky photos
			if ((oldVersion <= 9) && (newVersion>=10)) {
				db.execSQL("DROP TABLE " + SKY_PHOTOS);
				db.execSQL(SKY_PHOTOS_TABLE_CREATE);
			}
			*/
			
			
			showWhatsNew();
		}
	}
	
	private void log(String message) {
		if (PressureNETConfiguration.DEBUG_MODE) {
			System.out.println(message);
		}
	}
	
	public PnDb(Context ctx) {
		this.mContext = ctx;
	}

}
