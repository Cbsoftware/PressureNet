package ca.cumulonimbus.barometernetwork;

import java.util.ArrayList;

import ca.cumulonimbus.pressurenetsdk.CbCurrentCondition;
import ca.cumulonimbus.pressurenetsdk.CbWeather;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

public class PnDb {

	// Tables
	public static final String SEARCH_LOCATIONS_TABLE = "pn_searchlocations";
	public static final String CONDITIONS_DELIVERED = "conditions_delivered";
	public static final String FORECAST_ALERTS = "forecast_alerts";
	
	public static final String FORECAST_LOCATIONS = "forecast_locations";
	public static final String TEMPERATURES = "temperatures";

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
	
	// Forecast Alert fields
	private static final String KEY_ALERT_TIME = "alert_time";
	private static final String KEY_ALERT_CONDITION = "alert_condition";
	private static final String KEY_ALERT_PRECIP = "alert_precip";
	private static final String KEY_ALERT_POLITE_STRING = "alert_polite_string";
	private static final String KEY_ALERT_TEMP = "alert_temperature";
	private static final String KEY_ALERT_ISSUE_TIME = "issue_time";
	
	// Forecast Locations
	private static final String KEY_FORECAST_LOCATION_ID = "forecast_id";
	private static final String KEY_FORECAST_LATITUDE = "latitude";
	private static final String KEY_FORECAST_LONGITUDE = "longitude";
	
	// Temperature Forecasts
	private static final String KEY_TEMP_FORECAST_START_TIME = "temperature_forecast_start";
	private static final String KEY_TEMP_FORECAST_HOUR = "temperature_forecast_hour";
	private static final String KEY_TEMP_FORECAST_SCALE = "temperature_forecast_scale";
	private static final String KEY_TEMP_FORECAST_VALUE = "temperature_forecast_value";
	
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
	
	private static final String FORECAST_ALERTS_TABLE_CREATE = "create table "
			+ FORECAST_ALERTS
			+ " (_id integer primary key autoincrement, " + KEY_ALERT_CONDITION
			+ " text, "  + KEY_ALERT_PRECIP
			+ " text not null, " + KEY_ALERT_TIME + " real not null, "
			+ KEY_ALERT_TEMP + " real not null, " + KEY_ALERT_POLITE_STRING + " text, "
			+ KEY_ALERT_ISSUE_TIME + " real not null)";
	
	private static final String FORECAST_LOCATIONS_TABLE_CREATE = "create table "
			+ FORECAST_LOCATIONS 
			+ " (_id integer primary key autoincrement, "
			+ KEY_FORECAST_LOCATION_ID + " text not null, "
			+ KEY_FORECAST_LATITUDE + " real not null, " 
			+ KEY_FORECAST_LONGITUDE + " real not null)";
	
	private static final String TEMPERATURES_TABLE_CREATE = "create table "
			+ TEMPERATURES
			+ " (_id integer primary key autoincrement, " 
			+ KEY_FORECAST_LOCATION_ID + " text not null, "
			+ KEY_TEMP_FORECAST_START_TIME + " text not null, "
			+ KEY_TEMP_FORECAST_HOUR + " real not null, " 
			+ KEY_TEMP_FORECAST_SCALE + " real not null,"
			+ KEY_TEMP_FORECAST_VALUE + " real not null)";
	

	private static final String DATABASE_NAME = "PnDb";
	private static final int DATABASE_VERSION = 27; 
	// TODO: fix this nonsense
	// db = 2 at pN <=4.0.11. 5=4.1.6, 6=4.1.7, 7=4.2.5, 8=4.2.6
	// 9 = 4.2.7
	// 10-12 = 4.3.0
	// 13 = 4.4.2-4
	// 14 = 4.4.6
	// 15 = 4.4.7
	// 16 = 4.4.8
	// 17-20 = 4.5.x
	// 21-22 = 4.5.8
	// 23 = 4.6.0RC
	// 24+ = 4.7.x
	// 27 = 5.0
	
	public PnDb open() throws SQLException {
		mDbHelper = new DatabaseHelper(mContext);
		mDB = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}
	
	private static final String FORECAST_LOCATION_INDEX = "forecast_location_idx";
	private static final String FORECAST_HOUR_INDEX = "forecast_hour_idx";
	
	private void createIndex(SQLiteDatabase db) {
		String forecastLocationIndex = "Create Index IF NOT EXISTS " + FORECAST_LOCATION_INDEX + " ON " + FORECAST_LOCATIONS + "(" + KEY_FORECAST_LOCATION_ID + ", " + KEY_FORECAST_LATITUDE + ", " + KEY_FORECAST_LONGITUDE + ")";
		String forecastHourIndex = "Create Index IF NOT EXISTS " + FORECAST_HOUR_INDEX + " ON " + TEMPERATURES + "(" + KEY_FORECAST_LOCATION_ID + ", " + KEY_TEMP_FORECAST_HOUR + ")";
		

		db.execSQL(forecastLocationIndex);
		db.execSQL(forecastHourIndex);
	}
	
	public Cursor getRegionalForecastLocations(double minLat, double minLon, double maxLat, double maxLon) {
		Cursor cursor = mDB.query(false, FORECAST_LOCATIONS, new String[] {
				KEY_ROW_ID, KEY_FORECAST_LOCATION_ID, KEY_FORECAST_LATITUDE, KEY_FORECAST_LONGITUDE },
				KEY_LATITUDE + " > ? and " + KEY_LATITUDE + " < ? and " + KEY_LONGITUDE
				+ " > ? and " + KEY_LONGITUDE + " < ?", new String[] {
				minLat + "", maxLat + "", minLon + "", maxLon + ""}, null, null, KEY_TIME, null);
		
		return cursor;
	}
	
	public Cursor getMapTemperatures(double minLat, double minLon, double maxLat, double maxLon) {
		String query = "SELECT " + KEY_FORECAST_LATITUDE + ", " + KEY_FORECAST_LONGITUDE + ", " + KEY_TEMP_FORECAST_VALUE 
				+ " FROM " + FORECAST_LOCATIONS + " locations INNER JOIN " + TEMPERATURES + " temperatures "
				+ "ON locations." + KEY_FORECAST_LOCATION_ID + "=temperatures." + KEY_FORECAST_LOCATION_ID + " WHERE "
				+ "locations." + KEY_FORECAST_LATITUDE + " > ? and locations." + KEY_FORECAST_LATITUDE + " < ? and "
				+ "locations." + KEY_FORECAST_LONGITUDE + " > ? and locations." + KEY_FORECAST_LONGITUDE + " < ? and temperatures." 
				+ KEY_TEMP_FORECAST_HOUR + "=0 LIMIT 10";
		log("map temperature query " + query);
		String[] locationParams = new String[] {minLat + "", maxLat + "", minLon + "", maxLon + ""};
		Cursor cursor = mDB.rawQuery(query, locationParams);
				
		return cursor;
	}
	
	/**
	 * Add new forecast location
	 * @return
	 */
	public long addForecastLocation(String locationID, double latitude, double longitude) {
		// log("pndb adding forecast location");
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_FORECAST_LOCATION_ID, locationID);
		initialValues.put(KEY_FORECAST_LATITUDE, latitude);
		initialValues.put(KEY_FORECAST_LONGITUDE, longitude);

		return mDB.insert(FORECAST_LOCATIONS, null, initialValues);
	}
	
	public boolean addTemperatureForecastArrayList(ArrayList<TemperatureForecast> tempForecasts) {
		
		try {
			mDB.beginTransaction();
		} catch(SQLiteDatabaseLockedException sqldble) {
			// This try/catch block is a bad hack. Refactor db usaage to use only one lock
			// regardless of the thread
			
		}
		
		String insertSQL = "INSERT INTO "
				+ TEMPERATURES
				+ " ("
				+ KEY_FORECAST_LOCATION_ID
				+ ", "
				+ KEY_TEMP_FORECAST_START_TIME
				+ ", "
				+ KEY_TEMP_FORECAST_HOUR
				+ ", "
				+ KEY_TEMP_FORECAST_SCALE
				+ ", "
				+ KEY_TEMP_FORECAST_VALUE
				+ ") values (?, ?, ?, ?, ?)";

		try {
			SQLiteStatement insert = mDB.compileStatement(insertSQL);
			for (TemperatureForecast temp : tempForecasts) {
				insert.bindString(1, temp.getLocationID());
				insert.bindString(2, temp.getStartTime());
				insert.bindLong(3, temp.getForecastHour());
				insert.bindLong(4, temp.getScale());
				insert.bindDouble(5, temp.getTemperatureValue());
				insert.executeInsert();
			}

			mDB.setTransactionSuccessful();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} finally {
			mDB.endTransaction();
		}

		return true;
	}
	
	public boolean addForecastLocationArrayList(ArrayList<ForecastLocation> locationList) {
		
		try {
			mDB.beginTransaction();
		} catch(SQLiteDatabaseLockedException sqldble) {
			// This try/catch block is a bad hack. Refactor db usaage to use only one lock
			// regardless of the thread
			
		}
		
		String insertSQL = "INSERT INTO "
				+ FORECAST_LOCATIONS
				+ " ("
				+ KEY_FORECAST_LOCATION_ID
				+ ", "
				+ KEY_FORECAST_LATITUDE
				+ ", "
				+ KEY_FORECAST_LONGITUDE
				+ ") values (?, ?, ?)";

		try {
			SQLiteStatement insert = mDB.compileStatement(insertSQL);
			for (ForecastLocation location : locationList) {
				insert.bindString(1, location.getLocationID());
				insert.bindDouble(2, location.getLatitude());
				insert.bindDouble(3, location.getLongitude());
				insert.executeInsert();
			}

			mDB.setTransactionSuccessful();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} finally {
			mDB.endTransaction();
		}

		return true;
	}

	

	/**
	 * Add new forecast alert
	 * @return
	 */
	public long addForecastAlert(String condition, long time, double temp, String politeText, String precip) {
		log("pndb adding forecast alert " + condition);
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_ALERT_CONDITION, condition);
		initialValues.put(KEY_ALERT_TIME, time);
		initialValues.put(KEY_ALERT_TEMP, temp);
		initialValues.put(KEY_ALERT_POLITE_STRING, politeText);
		initialValues.put(KEY_ALERT_PRECIP, precip);
		initialValues.put(KEY_ALERT_ISSUE_TIME, System.currentTimeMillis());

		return mDB.insert(FORECAST_ALERTS, null, initialValues);
	}
	

	/**
	 * Delete old forecast alerts to keep the table current
	 */
	public void deleteOldForecastAlerts() {
		
		long now = System.currentTimeMillis();
		long timeAgo = now - (1000 * 60 * 60 * 1);
		mDB.execSQL("delete from " + FORECAST_ALERTS + " where " + 
		KEY_ALERT_ISSUE_TIME + "<" + timeAgo);
		log("pndb deleted old forecast alerts");
	}
	
	
	/**
	 * Delete old forecast alerts to keep the table current
	 */
	public void deleteSingleForecastAlert(int id) {
		mDB.execSQL("delete from " + FORECAST_ALERTS + " WHERE " + KEY_ROW_ID + "=" + id);
		log("pndb single forecast alert");
	}
	

	/**
	 * Fetch recent forecast alerts
	 * 
	 * @return
	 */
	public Cursor fetchRecentForecastAlerts() {
		return mDB.query(FORECAST_ALERTS, new String[] { KEY_ROW_ID,
				KEY_ALERT_CONDITION, KEY_ALERT_PRECIP, KEY_ALERT_TIME, 
				KEY_ALERT_TEMP, KEY_ALERT_POLITE_STRING, KEY_ALERT_ISSUE_TIME},
				KEY_ALERT_TIME + " > 0" , null, null, null, null);
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
		long timeAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 10);
		mDB.execSQL("delete from " + CONDITIONS_DELIVERED + " where " + 
		KEY_TIME + "<" + timeAgo);
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
			db.execSQL(FORECAST_ALERTS_TABLE_CREATE);
			db.execSQL(FORECAST_LOCATIONS_TABLE_CREATE);
			db.execSQL(TEMPERATURES_TABLE_CREATE);
			createIndex(db);
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
			if( (oldVersion < 24)) {
				db.execSQL(FORECAST_ALERTS_TABLE_CREATE);
			}
			if( (oldVersion <= 27)) {
				db.execSQL(FORECAST_LOCATIONS_TABLE_CREATE);
				db.execSQL(TEMPERATURES_TABLE_CREATE);
			}
			
			createIndex(db);
			
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
