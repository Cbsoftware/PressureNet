package ca.cumulonimbus.barometernetwork;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import com.google.analytics.tracking.android.EasyTracker;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import ca.cumulonimbus.pressurenetsdk.CbApiCall;
import ca.cumulonimbus.pressurenetsdk.CbObservation;
import ca.cumulonimbus.pressurenetsdk.CbScience;
import ca.cumulonimbus.pressurenetsdk.CbService;

public class LogViewerActivity extends Activity {
	

	TextView logText;

	boolean mBound;
	Messenger mService = null;

	private Messenger mMessenger = new Messenger(new IncomingHandler());

	Button oneHour;
	Button sixHours;
	Button oneDay;
	Button oneWeek;
	long hoursAgo;

	private String preferenceUnit;
	
	private int hoursSelected = 6;
	
	ProgressDialog pd;

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CbService.MSG_LOCAL_RECENTS:

				ArrayList<CbObservation> recents = (ArrayList<CbObservation>) msg.obj;
				Collections.sort(recents,
						new CbScience.TimeComparator());
				pd.dismiss();
				try {

					String rawLog = "";
					for (CbObservation obs : recents) {
						Calendar c = Calendar.getInstance();
						c.setTimeInMillis(obs.getTime());
						String dateString = c.getTime().toLocaleString();
						DecimalFormat df = new DecimalFormat("####.00000");
						String valueString = df.format(convertedPressureValue(obs.getObservationValue()));
						rawLog += dateString + ": " + valueString + "\n";
					}
					
					// display in text form
					try {
						logText = (TextView) findViewById(R.id.editLog);
						logText.setText(rawLog);
					} catch (NullPointerException npe) {
						// TODO; fix hack. 
						//log("not loading text");
					}
					
					// display in chart form
					try {
						Chart chart = new Chart(getApplicationContext());
						
						// set units according to preference
						ArrayList<CbObservation> displayRecents = new ArrayList<CbObservation>();
						for(CbObservation ob : recents) {
							double rawValue = ob.getObservationValue();
							
							PressureUnit unit = new PressureUnit(preferenceUnit);
							unit.setValue(rawValue);
							unit.setAbbreviation(preferenceUnit);
							double pressureInPreferredUnit = unit
									.convertToPreferredUnit();
							
							ob.setObservationUnit(preferenceUnit);
							ob.setObservationValue(pressureInPreferredUnit);
							displayRecents.add(ob);
						}
						
						View chartView = chart.drawChart(displayRecents);
						showChart(chartView);
					} catch (NullPointerException npe ) {
						// TODO: fix hack.
						//log("not drawing chart");
					}
					
				} catch (Exception e) {
					//e.printStackTrace();
				} finally {
					
				}
				
				// enable buttons
				oneHour.setEnabled(true);
				oneHour.setTypeface(null, Typeface.NORMAL);
				sixHours.setEnabled(true);
				sixHours.setTypeface(null, Typeface.NORMAL);
				oneDay.setEnabled(true);
				oneDay.setTypeface(null, Typeface.NORMAL);
				oneWeek.setEnabled(true);
				oneWeek.setTypeface(null, Typeface.NORMAL);
				
				oneHour.setTextColor(Color.BLACK);
				sixHours.setTextColor(Color.BLACK);
				oneDay.setTextColor(Color.BLACK);
				oneWeek.setTextColor(Color.BLACK);
				
				switch(hoursSelected) {
				case 1:
					oneHour.setTypeface(null, Typeface.BOLD);
					break;
				case 6:
					sixHours.setTypeface(null, Typeface.BOLD);
					break;
				case 24:
					oneDay.setTypeface(null, Typeface.BOLD);
					break;
				case 24*7:
					oneWeek.setTypeface(null, Typeface.BOLD);
					break;
				}
				
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	public void unBindCbService() {
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}

	public void bindCbService() {
		bindService(new Intent(getApplicationContext(), CbService.class),
				mConnection, Context.BIND_AUTO_CREATE);

	}
	public double convertedPressureValue(double value) {
		PressureUnit unit = new PressureUnit(preferenceUnit);
		unit.setValue(value);
		unit.setAbbreviation(preferenceUnit);
		return unit.convertToPreferredUnit();
	}

	 	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			mBound = true;
			Message msg = Message.obtain(null, CbService.MSG_OKAY);

			getRecents(6);
			sixHours.setTextColor(Color.BLACK);
			sixHours.setTypeface(null, Typeface.BOLD);
		}

		public void onServiceDisconnected(ComponentName className) {
			mMessenger = null;
			mBound = false;
		}
	};

	public void getRecents(long numHoursAgo) {
		this.hoursAgo = numHoursAgo;
		// disable buttons
		oneHour = (Button) findViewById(R.id.buttonOneHour);
		sixHours = (Button) findViewById(R.id.buttonSixHours);
		oneDay = (Button) findViewById(R.id.buttonOneDay);
		oneWeek = (Button) findViewById(R.id.buttonOneWeek);
		
		oneHour.setEnabled(false);
		oneHour.setTextColor(Color.GRAY);
		sixHours.setEnabled(false);
		sixHours.setTextColor(Color.GRAY);
		oneDay.setEnabled(false);
		oneDay.setTextColor(Color.GRAY);
		oneWeek.setEnabled(false);
		oneWeek.setTextColor(Color.GRAY);
		
		if (mBound) {
			if((hoursAgo > 1) && (hoursAgo<100)) {
				pd = ProgressDialog.show(LogViewerActivity.this,"Loading", hoursAgo + " hours of data",true,true,null);
			} else if(hoursAgo>100) {
				// TODO: fix hack '1 week'
				pd = ProgressDialog.show(LogViewerActivity.this,"Loading", "1 week of data",true,true,null);
			}
			MessageSender message = new MessageSender();
			message.execute("");
		} else {
			//log("error: not bound");
		}
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
	
	private class MessageSender extends AsyncTask<String, Integer, Integer> {

		@Override
		protected Integer doInBackground(String... params) {
			CbApiCall api = new CbApiCall();
			api.setMinLat(-90);
			api.setMaxLat(90);
			api.setMinLon(-180);
			api.setMaxLon(180);
			api.setStartTime(System.currentTimeMillis()
					- (hoursAgo * 60 * 60 * 1000));
			api.setEndTime(System.currentTimeMillis());

			Message msg = Message.obtain(null, CbService.MSG_GET_LOCAL_RECENTS,
					api);
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				//e.printStackTrace();
			}
			return null;
		}
		
		
		
	}
	
	class DataTabsListener implements ActionBar.TabListener {
		public Fragment fragment;

		public DataTabsListener(Fragment fragment) {
			this.fragment = fragment;
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			ft.replace(R.id.fragment_container, fragment);
			getRecents(hoursSelected);
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			ft.remove(fragment);
		}

	}

	@Override
	public void onDestroy() {
		unBindCbService();
		super.onDestroy();
	}


	public void showChart(View v) {
		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.layout_chart_fragment);

		try {
			View testChartView = findViewById(100); // TODO: ...
			mainLayout.removeView(testChartView);
		} catch (Exception e) {
			//e.printStackTrace();
		}

		v.setId(100); // TODO: what's safe?

		// add to layout
		LayoutParams lparams = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);

		v.setBackgroundColor(Color.rgb(238, 238, 238));
		v.setLayoutParams(lparams);
		if (mainLayout == null) {
			//log("ERROR layout null, chart");
			return;
		}
		mainLayout.addView(v);
	}
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.logviewer);
		bindCbService();
		
		Fragment allLogFrags = new LogViewerFragment();
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();
		fragmentTransaction.add(R.id.layoutLogFragContainer, allLogFrags);
		fragmentTransaction.commit();

	

		// ActionBar gets initiated
		ActionBar actionbar = getActionBar();
		// Tell the ActionBar we want to use Tabs.
		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		// initiating both tabs and set text to it.
		ActionBar.Tab chartTab = actionbar.newTab().setText("Chart");
		ActionBar.Tab logTab = actionbar.newTab().setText("Log");

		// create the two fragments we want to use for display content
		Fragment chartFragment = new ChartFragment();
		Fragment logFragment = new LogFragment();

		// set the Tab listener. Now we can listen for clicks.
		chartTab.setTabListener(new DataTabsListener(chartFragment));
		logTab.setTabListener(new DataTabsListener(logFragment));

		// add the two tabs to the actionbar
		actionbar.addTab(chartTab);
		actionbar.addTab(logTab);
		int actionBarTitleId = getResources().getSystem().getIdentifier(
				"action_bar_title", "id", "android");

		TextView actionBarTextView = (TextView) findViewById(
				actionBarTitleId);
		actionBarTextView.setTextColor(Color.WHITE);
	
		preferenceUnit = getUnitPreference();
	
		
		oneHour = (Button) findViewById(R.id.buttonOneHour);
		sixHours = (Button) findViewById(R.id.buttonSixHours);
		oneDay = (Button) findViewById(R.id.buttonOneDay);
		oneWeek = (Button) findViewById(R.id.buttonOneWeek);


		
		
		
		oneHour.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				oneHour.setTypeface(null, Typeface.BOLD);
				sixHours.setTypeface(null, Typeface.NORMAL);
				oneDay.setTypeface(null, Typeface.NORMAL);
				oneWeek.setTypeface(null, Typeface.NORMAL);
				hoursSelected = 1;
				getRecents(1);
			}
		});

		sixHours.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				oneHour.setTypeface(null, Typeface.NORMAL);
				sixHours.setTypeface(null, Typeface.BOLD);
				oneDay.setTypeface(null, Typeface.NORMAL);
				oneWeek.setTypeface(null, Typeface.NORMAL);
				hoursSelected = 6;
				getRecents(6);
			}
		});
		oneDay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				oneHour.setTypeface(null, Typeface.NORMAL);
				sixHours.setTypeface(null, Typeface.NORMAL);
				oneDay.setTypeface(null, Typeface.BOLD);
				oneWeek.setTypeface(null, Typeface.NORMAL);
				hoursSelected = 24;
				getRecents(24);
			}
		});
		oneWeek.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				oneHour.setTypeface(null, Typeface.NORMAL);
				sixHours.setTypeface(null, Typeface.NORMAL);
				oneDay.setTypeface(null, Typeface.NORMAL);
				oneWeek.setTypeface(null, Typeface.BOLD);
				hoursSelected = 24 * 7;
				getRecents(24 * 7);
			}
		});

		
	}

	/**
	 * Check the Android SharedPreferences for important values. Save relevant
	 * ones to CbSettings for easy access in submitting readings
	 */
	public String getUnitPreference() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		return sharedPreferences.getString("units", "millibars");
	}


}
