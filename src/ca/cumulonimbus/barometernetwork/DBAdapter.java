package ca.cumulonimbus.barometernetwork;

import java.util.ArrayList;
import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapter {

    public static final String KEY_READING = "reading";
    public static final String KEY_TIME = "time";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_SHARING = "sharingprivacy";
    public static final String KEY_ROWID = "_id";

    private static final String TAG = "pressureNETDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
        "create table barometer_readings (_id integer primary key autoincrement, "
      + "reading real not null, "
      + "latitude real not null, "
      + "longitude real not null, "
      + "time integer not null, "
      + "sharingprivacy text not null);";

    private static final String DATABASE_NAME = "pressurenet_data";
    private static final String DATABASE_TABLE = "barometer_readings";
    private static final int DATABASE_VERSION = 6;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        	// TODO: actually upgrade
            db.execSQL("DROP TABLE IF EXISTS barometer_readings");
            onCreate(db);
        }
    }

    
    public ArrayList<BarometerReading> fetchRecentReadings(int numHours) {
    	ArrayList<BarometerReading> recent = new ArrayList<BarometerReading>();
    	String[] cols = {KEY_LATITUDE, KEY_LONGITUDE, KEY_READING, KEY_TIME, KEY_SHARING};
    	long hoursInMillis = numHours * 60 * 60 * 1000;
    	long now = Calendar.getInstance().getTimeInMillis();
    	long millisAgo = now - hoursInMillis;
    	String[] timelimit = {millisAgo + ""};
    	
    	Cursor c = mDb.query(DATABASE_TABLE, cols, "time > ?", timelimit, null, null, KEY_TIME, "1000");
    	c.moveToFirst();
    	while(c.isAfterLast() == false) {
    		BarometerReading br = new BarometerReading();
    		br.setLatitude(c.getDouble(0));
    		br.setLongitude(c.getDouble(1));
    		br.setReading(c.getDouble(2));
    		br.setTime(c.getDouble(3));
    		br.setSharingPrivacy(c.getString(4));
    		recent.add(br);
    		c.moveToNext();
    	}
    	c.close();
    	return recent;
    }
    
    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public DBAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    public long addReading(double reading, double latitude, double longitude, double time, String sharingPrivacy) {
    	
        ContentValues initialValues = new ContentValues();
        // System.out.println(reading + " " + latitude + " " + longitude + " " + time);
        initialValues.put(KEY_READING, reading);
        initialValues.put(KEY_LATITUDE, latitude);
        initialValues.put(KEY_LONGITUDE, longitude);
        initialValues.put(KEY_TIME, time);
        initialValues.put(KEY_SHARING, time);
        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }
    
    /**
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public DBAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    public boolean deleteReading(long rowId) {
        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    public Cursor fetchAllReadings() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_READING, KEY_LATITUDE, KEY_LONGITUDE,
                KEY_TIME, KEY_SHARING}, null, null, null, null, null);
    }
    
    

    public Cursor fetchReading(long rowId) throws SQLException {

        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_READING, KEY_LATITUDE, KEY_LONGITUDE, KEY_TIME, KEY_SHARING}, KEY_ROWID + "=" + rowId, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

}
