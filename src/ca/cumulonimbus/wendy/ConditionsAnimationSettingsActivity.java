package ca.cumulonimbus.wendy;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TimePicker;

import ca.cumulonimbus.barometernetwork.R;

import com.google.analytics.tracking.android.EasyTracker;

public class ConditionsAnimationSettingsActivity extends Activity {

	private SeekBar seekBar;
	private TextView textRange;
	private Button startDate;
	private Button startTime;
	private Button cancel;
	private Button okay;
	
	private TextView textPreviewStartDate;
	private TextView textPreviewEndDate;
	private TextView textPreviewStartTime;
	private TextView textPreviewEndTime;

	Calendar calDate = Calendar.getInstance();
	private long rangeInMs = 0;

	private String[] timeSegments = { "3 hours", "6 hours", "12 hours",
			"24 hours", "2 days", "3 days" };

	private long hourInMs = 1000 * 60 * 60;
	private long dayInMs = 1000 * 60 * 60 * 24;

	private long[] timeSegmentsMs = { 3 * hourInMs, 6 * hourInMs,
			12 * hourInMs, 24 * hourInMs, 2 * dayInMs, 3 * dayInMs };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.conditions_animation_settings);
		
		setUpUI();
		
		Intent intent = getIntent();
		if (intent.hasExtra("animationDuration")) {
			rangeInMs = intent.getLongExtra("animationDuration",
					getRangeInMsFromText(0));
			if (rangeInMs == 0) {
				rangeInMs = getRangeInMsFromText(0);
			}
			int index = 0;
			for (long range : timeSegmentsMs) {
				if (range == rangeInMs) {
					break;
				}
				index++;
			}
			seekBar = (SeekBar) findViewById(R.id.seekBarAnimationRange);
			seekBar.setProgress(index);
			textRange = (TextView) findViewById(R.id.textViewRangeValue);
			textRange.setText(timeSegments[index]);
		}

		calDate.setTimeInMillis(intent.getLongExtra(
					"calAnimationStartDate", System.currentTimeMillis()));
				
		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
		startDate.setText(sdf.format(new Date(calDate.getTimeInMillis())));
		
		SimpleDateFormat sdfTime = new SimpleDateFormat("H:mm");
		
		if(!DateFormat.is24HourFormat(getApplicationContext())) {
			sdfTime = new SimpleDateFormat("h:mm a");
		} else {
			sdfTime = new SimpleDateFormat("HH:mm");
		}
		startTime.setText(sdfTime.format(new Date(calDate.getTimeInMillis())));
		
		updatePreview();
		
		super.onCreate(savedInstanceState);
	}

	public class StartDatePickerFragment extends DialogFragment implements
			DatePickerDialog.OnDateSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			System.out.println("conditions animations settings timezone"
					+ calDate.getTimeZone());
			int year = calDate.get(Calendar.YEAR);
			int month = calDate.get(Calendar.MONTH);
			int day = calDate.get(Calendar.DAY_OF_MONTH);

			return new DatePickerDialog(getActivity(), this, year, month, day);
		}

		public void onDateSet(DatePicker view, int year, int month, int day) {
			calDate.set(Calendar.YEAR, year);
			calDate.set(Calendar.MONTH, month);
			calDate.set(Calendar.DAY_OF_MONTH, day);

			SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
			startDate.setText(sdf.format(new Date(calDate.getTimeInMillis())));
			
			updatePreview();
		}
	}

	public class StartTimePickerFragment extends DialogFragment implements
			TimePickerDialog.OnTimeSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			int hour = calDate.get(Calendar.HOUR_OF_DAY);
			int minute = calDate.get(Calendar.MINUTE);

			return new TimePickerDialog(getActivity(), this, hour, minute,
					DateFormat.is24HourFormat(getActivity()));
		}

		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			calDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
			calDate.set(Calendar.MINUTE, minute);
			
			SimpleDateFormat sdf = new SimpleDateFormat("H:mm");
			
			if(!DateFormat.is24HourFormat(getActivity())) {
				sdf = new SimpleDateFormat("h:mm a");
			} else {
				sdf = new SimpleDateFormat("HH:mm");
			}
			
			startTime.setText(sdf.format(new Date(calDate.getTimeInMillis())));
			
			updatePreview();
		}
	}
	
	private void updatePreview() {
		Calendar endDate = (Calendar) calDate.clone();
		endDate.add(Calendar.MILLISECOND, (int)rangeInMs);
		
		String formattedStartDate = buildHumanStartDateFormat(calDate, endDate);
		String formattedEndDate = buildHumanEndDateFormat(calDate, endDate);
		textPreviewStartDate.setText(formattedStartDate);
		textPreviewStartTime.setText(getHumanStartTime(calDate, endDate));
		
		textPreviewEndDate.setText(formattedEndDate);
		textPreviewEndTime.setText(getHumanEndTime(endDate, endDate));
		
	}

	private String getHumanStartTime(Calendar start, Calendar end) {
		SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
		String formattedTime = "";
		
		if(!DateFormat.is24HourFormat(getApplicationContext())) {
			sdfTime = new SimpleDateFormat("h:mm a");
		} else {
			sdfTime = new SimpleDateFormat("HH:mm");
		}
		
		formattedTime = sdfTime.format(start.getTimeInMillis());
		/*
		if( (start.get(Calendar.HOUR_OF_DAY) == 0) && (end.get(Calendar.HOUR_OF_DAY) == 0)) {
			formattedTime = "";
		}
		*/
		
		return formattedTime;
	}
	
	private String getHumanEndTime(Calendar start, Calendar end) {
		SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
		String formattedTime = "";
		
		if(!DateFormat.is24HourFormat(getApplicationContext())) {
			sdfTime = new SimpleDateFormat("h:mm a");
		} else {
			sdfTime = new SimpleDateFormat("HH:mm");
		}
		
		formattedTime = sdfTime.format(end.getTimeInMillis());
		/*
		if ((start.get(Calendar.HOUR_OF_DAY) == 0) && (end.get(Calendar.HOUR_OF_DAY) == 0)) {
			formattedTime = "";
		}
		*/
		
		return formattedTime;
	}
	
	private String buildHumanStartDateFormat(Calendar start, Calendar end) {
		String yearFormat = "";
		String monthFormat = "MMM ";
		String dayFormat = "d";
		
		// if the years are both this year, don't show the value
		if( (start.get(Calendar.YEAR) == end.get(Calendar.YEAR)) && (start.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR) )) {
			yearFormat = "";
		} else {
			yearFormat = ", yyyy";
		}
		
		String format = monthFormat + dayFormat + yearFormat;
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		
		return sdf.format(start.getTimeInMillis());
	}
	
	private String buildHumanEndDateFormat(Calendar start, Calendar end) {
		String yearFormat = "";
		String monthFormat = "MMM ";
		String dayFormat = "d";
		
		// if the years are both this year, don't show the value
		if( (start.get(Calendar.YEAR) == end.get(Calendar.YEAR)) && (start.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR) )) {
			yearFormat = "";
		} else {
			yearFormat = ", yyyy";
		}
		
		String format = monthFormat + dayFormat + yearFormat;
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		
		return sdf.format(end.getTimeInMillis());
	}
	
	private long getRangeInMsFromText(int indexText) {
		return timeSegmentsMs[indexText];
	}

	private void setUpUI() {
		seekBar = (SeekBar) findViewById(R.id.seekBarAnimationRange);
		textRange = (TextView) findViewById(R.id.textViewRangeValue);
		startDate = (Button) findViewById(R.id.buttonAnimationStartDate);
		startTime = (Button) findViewById(R.id.buttonAnimationStartTime);
		cancel = (Button) findViewById(R.id.buttonAnimationCancel);
		okay = (Button) findViewById(R.id.buttonAnimationSet);

		textPreviewStartDate = (TextView) findViewById(R.id.textStartDateSummary);
		textPreviewStartTime = (TextView) findViewById(R.id.textStartTimeSummary);
		textPreviewEndDate = (TextView) findViewById(R.id.textEndDateSummary);
		textPreviewEndTime = (TextView) findViewById(R.id.textEndTimeSummary);
		
		okay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent resultIntent = new Intent();
				resultIntent.putExtra("startDate", calDate);
				resultIntent.putExtra("animationRange", rangeInMs);
				resultIntent.putExtra("requestCode",
						BarometerNetworkActivity.REQUEST_ANIMATION_PARAMS);
				setResult(Activity.RESULT_OK, resultIntent);

				finish();
			}
		});

		cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});

		startDate.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				DialogFragment newFragment = new StartDatePickerFragment();
				newFragment.show(getFragmentManager(), "datePicker");
			}
		});
		
		startTime.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				DialogFragment newFragment = new StartTimePickerFragment();
				newFragment.show(getFragmentManager(), "timePicker");
			}
		});

		textRange.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

			}
		});

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				textRange.setText(timeSegments[progress]);
				rangeInMs = getRangeInMsFromText(progress);
				updatePreview();
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	protected void onStop() {
		EasyTracker.getInstance(this).activityStop(this);
		super.onStop();
	}

}
