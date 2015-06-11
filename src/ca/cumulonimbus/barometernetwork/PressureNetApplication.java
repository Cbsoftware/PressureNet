package ca.cumulonimbus.barometernetwork;

import java.util.HashMap;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import android.app.Application;

public class PressureNetApplication extends Application {
	
	/**
	 * Enum used to identify the tracker that needs to be used for tracking.
	 *
	 * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
	 * storing them all in Application object helps ensure that they are created only once per
	 * application instance.
	 */
	public enum TrackerName {
	  APP_TRACKER, // Tracker used only in this app.
	  GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.

	}

	HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();
	
	synchronized Tracker getTracker(TrackerName trackerId) {
		  if (!mTrackers.containsKey(trackerId)) {

		    GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
		    Tracker t = (trackerId == TrackerName.APP_TRACKER) ? analytics.newTracker(PressureNETConfiguration.GA_TOKEN)
		        : (trackerId == TrackerName.GLOBAL_TRACKER) ? analytics.newTracker(R.xml.global_tracker)
		            : analytics.newTracker(R.xml.global_tracker);
		    mTrackers.put(trackerId, t);

		  }
		  return mTrackers.get(trackerId);
		}
	
}
