package ca.cumulonimbus.barometernetwork;

import java.util.Calendar;

import com.google.analytics.tracking.android.EasyTracker;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class ConditionsAnimationSettingsActivity extends Activity {

	private SeekBar seekBar;
	private TextView textRange;
	private DatePicker startDate;
	private Button cancel;
	private Button okay;
	
	Calendar calDate = Calendar.getInstance();
	private long rangeInMs = 0;
	
	private String[] timeSegments = { "3 hours", "6 hours", "12 hours",
			"24 hours", "2 days", "3 days"};
	
	private long hourInMs = 1000 * 60 * 60;
	private long dayInMs = 1000 * 60 * 60 * 24;
	
	private long[] timeSegmentsMs = {
			3 * hourInMs,
			6 * hourInMs,
			12 * hourInMs,
			24 * hourInMs,
			2 * dayInMs,
			3 * dayInMs
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.conditions_animation_settings);

		Intent intent = getIntent();
		if (intent.hasExtra("animationDuration")) {
			rangeInMs = intent.getLongExtra("animationDuration", getRangeInMsFromText(0));
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
			textRange.setText("Range: " + timeSegments[index]);
		}
		
		if (intent.hasExtra("calAnimationStartDate")) {
			calDate.setTimeInMillis(intent.getLongExtra("calAnimationStartDate", System.currentTimeMillis()));
		}
		
		setUpUI();
		super.onCreate(savedInstanceState);
	}

	
	public class StartDatePickerFragment extends DialogFragment implements
			DatePickerDialog.OnDateSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			System.out.println("conditions animations settings timezone" + calDate.getTimeZone());
			int year = calDate.get(Calendar.YEAR);
			int month = calDate.get(Calendar.MONTH);
			int day = calDate.get(Calendar.DAY_OF_MONTH);

			return new DatePickerDialog(getActivity(), this, year, month, day);
		}

		public void onDateSet(DatePicker view, int year, int month, int day) {
			calDate.set(Calendar.YEAR, year);
			calDate.set(Calendar.MONTH, month);
			calDate.set(Calendar.DAY_OF_MONTH, day);
			
		}
	}

	private long getRangeInMsFromText(int indexText) {
		return timeSegmentsMs[indexText];
	}
	
	private void setUpUI() {
		seekBar = (SeekBar) findViewById(R.id.seekBarAnimationRange);
		textRange = (TextView) findViewById(R.id.textViewRangeValue);
		startDate = (DatePicker) findViewById(R.id.dateAnimationStart);
		cancel = (Button) findViewById(R.id.buttonAnimationCancel);
		okay = (Button) findViewById(R.id.buttonAnimationSet);

		calDate.set(Calendar.HOUR_OF_DAY, 0);
		calDate.set(Calendar.MINUTE, 0);
		calDate.set(Calendar.SECOND, 0);
		
		startDate.init(calDate.get(Calendar.YEAR), calDate.get(Calendar.MONTH), calDate.get(Calendar.DAY_OF_MONTH), new OnDateChangedListener() {
			
			@Override
			public void onDateChanged(DatePicker arg0, int year, int month, int day) {
				calDate.set(Calendar.YEAR, year);
				calDate.set(Calendar.MONTH, month);
				calDate.set(Calendar.DAY_OF_MONTH, day);
			}
		});
		
		okay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent resultIntent = new Intent();
				resultIntent.putExtra("startDate", calDate);
				resultIntent.putExtra("animationRange", rangeInMs);
				resultIntent.putExtra("requestCode", BarometerNetworkActivity.REQUEST_ANIMATION_PARAMS);
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
				textRange.setText("Range: " + timeSegments[progress]);
				rangeInMs = getRangeInMsFromText(progress);
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
