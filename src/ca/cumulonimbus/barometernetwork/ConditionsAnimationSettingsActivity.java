package ca.cumulonimbus.barometernetwork;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class ConditionsAnimationSettingsActivity extends Activity {

	private SeekBar seekBar;
	private TextView textRange;
	private Button startDate;
	private Button cancel;
	private Button okay;
	
	Calendar calDate = Calendar.getInstance();
	private long rangeInMs = 0;
	
	private String[] timeSegments = { "3 hours", "6 hours", "12 hours",
			"24 hours", "2 days", "3 days", "4 days", "5 days", "6 days",
			"7 days", };
	
	private long hourInMs = 1000 * 60 * 60;
	private long dayInMs = 1000 * 60 * 60 * 24;
	
	private long[] timeSegmentsMs = {
			3 * hourInMs,
			6 * hourInMs,
			12 * hourInMs,
			24 * hourInMs,
			2 * dayInMs,
			3 * dayInMs,
			4 * dayInMs,
			5 * dayInMs,
			6 * dayInMs,
			7 * dayInMs
	};

	public class StartDatePickerFragment extends DialogFragment implements
			DatePickerDialog.OnDateSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);

			return new DatePickerDialog(getActivity(), this, year, month, day);
		}

		public void onDateSet(DatePicker view, int year, int month, int day) {
			calDate.set(Calendar.YEAR, year);
			calDate.set(Calendar.MONTH, month);
			calDate.set(Calendar.DAY_OF_MONTH, day);
			
			SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy");
			startDate.setText(sdf.format(new Date(calDate.getTimeInMillis())));
		}
	}

	private long getRangeInMsFromText(int indexText) {
		return timeSegmentsMs[indexText];
	}
	
	private void setUpUI() {
		seekBar = (SeekBar) findViewById(R.id.seekBarAnimationRange);
		textRange = (TextView) findViewById(R.id.textViewRangeValue);
		startDate = (Button) findViewById(R.id.buttonAnimationStartDate);
		cancel = (Button) findViewById(R.id.buttonAnimationCancel);
		okay = (Button) findViewById(R.id.buttonAnimationSet);

		okay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent resultIntent = new Intent();
				resultIntent.putExtra("startDate", calDate);
				resultIntent.putExtra("animationRange", rangeInMs);
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
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.conditions_animation_settings);
		setUpUI();
		super.onCreate(savedInstanceState);
	}

}
