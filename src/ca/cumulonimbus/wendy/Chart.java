package ca.cumulonimbus.wendy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;
import ca.cumulonimbus.barometernetwork.R;
import ca.cumulonimbus.pressurenetsdk.CbObservation;

public class Chart {

	Context context;
	private String mAppDir = "";

	private int textSize = 20;
	private float pointSize = 5f;

	public Chart(Context ctx) {
		context = ctx;
		setUpFiles();
	}

	/**
	 * Set the chart text size based on screen metrics
	 */
	private void setTextSize() {
		try {
			DisplayMetrics displayMetrics = new DisplayMetrics(); 
			displayMetrics = context.getResources().getDisplayMetrics();
			log("setting text size, density is " + displayMetrics.densityDpi);
			switch(displayMetrics.densityDpi){
		     case DisplayMetrics.DENSITY_LOW:
		    	 textSize = 12;
		    	 pointSize = 2f;
		         break;
		     case DisplayMetrics.DENSITY_MEDIUM:
		    	 textSize = 16;
		    	 pointSize = 3f;
		    	 break;
		     case DisplayMetrics.DENSITY_HIGH:
		    	 textSize = 18;
		    	 pointSize = 4f;
		    	 break;
		     case DisplayMetrics.DENSITY_XHIGH:
		    	 textSize = 20;
		    	 pointSize = 5f;
                 break;
		     case DisplayMetrics.DENSITY_XXHIGH:
		    	 textSize = 26;
		    	 pointSize = 7f;
                 break;
		     default:
		    	 break;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	protected void setRenderer(XYMultipleSeriesRenderer renderer, int[] colors,
			PointStyle[] styles, ArrayList<CbObservation> obsList) {

		setTextSize();
		renderer.setAxisTitleTextSize(textSize);
		renderer.setChartTitleTextSize(textSize);
		renderer.setLabelsTextSize(textSize);
		renderer.setLegendTextSize(textSize);
		renderer.setPointSize(pointSize);
		renderer.setMargins(new int[] { 20, 50, 15, 20 });

		XYSeriesRenderer r = new XYSeriesRenderer();
		r.setColor(colors[0]);
		r.setPointStyle(styles[0]);
		renderer.addSeriesRenderer(r);

	}

	/**
	 * Builds an XY multiple series renderer.
	 * 
	 * @param colors
	 *            the series rendering colors
	 * @param styles
	 *            the series point styles
	 * @return the XY multiple series renderers
	 */
	protected XYMultipleSeriesRenderer buildRenderer(int[] colors,
			PointStyle[] styles, ArrayList<CbObservation> obsList) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		setRenderer(renderer, colors, styles, obsList);
		return renderer;
	}

	public int random128() {
		return 1 + (int) (Math.random() * ((128 - 1) + 1));
	}

	public View drawChart(ArrayList<CbObservation> obsList) {
		// System.out.println("drawing chart " + obsList.size() +
		// " data points");

		if (obsList.size() < 2) {
			Toast.makeText(context, context.getString(R.string.noDataToPlot),
					Toast.LENGTH_SHORT).show();
		}

		String[] titles = new String[] { "" };
		List<Date[]> x = new ArrayList<Date[]>();
		List<double[]> values = new ArrayList<double[]>();
		int length = titles.length;
		int count = obsList.size();

		Date[] xValues = new Date[count];
		double[] yValues = new double[count];

		// TODO: Expand to multiple observations
		// currently only pressure
		double minObservation = 1200;
		double maxObservation = 0;
		long minTime = System.currentTimeMillis();

		long maxTime = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 7);

		int i = 0;
		double yMean = 0;
		double ySum = 0;
		
		// TODO: There are too many loops through the data
		// Use a faster, simpler way to remove outliers

		// calculate simple statistics to improve chart display
		// find the mean
		for (CbObservation obs : obsList) {
			ySum += obs.getObservationValue();
			i++;
		}
		yMean = ySum / i;

		// find standard deviation.
		double yDSqSum = 0;
		i = 0;
		for(CbObservation obs : obsList) {
			double distance = obs.getObservationValue() - yMean;
			double dSquared = distance * distance;
			yDSqSum += dSquared;
			i++;
		}
		
		double meanOfSquaredDist = yDSqSum / i;
		double standardDeviation = Math.sqrt(meanOfSquaredDist);
		log("sd is " + standardDeviation);
		// remove outliers from the list
		// (2 std away from the mean) 
		double min = yMean - (standardDeviation*3);
		double max = yMean + (standardDeviation*3);
		ArrayList<CbObservation> toRemove = new ArrayList<CbObservation>();
		for(CbObservation  obs : obsList) {
			double v = obs.getObservationValue();
			if( (v < min) || (v > max)) {
				//log("removing " + v + " min " + min + " max " + max);
				toRemove.add(obs);
			}
		}
		
		// filter values
		obsList.removeAll(toRemove);
		
		i = 0;
		for(CbObservation obs : obsList) {
			xValues[i] = new Date(obs.getTime());
			yValues[i] = obs.getObservationValue();
			if (obs.getObservationValue() < minObservation) {
				minObservation = obs.getObservationValue();
			}
			if (obs.getObservationValue() > maxObservation) {
				maxObservation = obs.getObservationValue();
			}
			if (obs.getTime() < minTime) {
				minTime = obs.getTime();
			}
			if (obs.getTime() > maxTime) {
				maxTime = obs.getTime();
			}
			i++;

		}
		
		x.add(xValues);
		values.add(yValues);

		// TODO: Implement smarter axis min/max. range around 1 sd from the
		// mean?
		// current hack is min/max observation or hardcoded default range
		int[] colors = new int[count];
		for (i = 0; i < count; i++) {
			colors[i] = Color.rgb(51, 181, 229);
		}

		PointStyle[] styles = new PointStyle[] { PointStyle.CIRCLE };
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles,
				obsList);
		int axesColor = Color.rgb(0, 0, 0);
		int labelColor = Color.rgb(0, 0, 0);
		setChartSettings(renderer, "Pressure", "Time", "Pressure", minTime,
				maxTime, minObservation, maxObservation, axesColor, labelColor);
		renderer.setXLabels(0);
		renderer.setYLabels(5);
		length = renderer.getSeriesRendererCount();
		for (i = 0; i < length; i++) {
			((XYSeriesRenderer) renderer.getSeriesRendererAt(i))
					.setFillPoints(true);
		}
		XYMultipleSeriesDataset dataset = buildDataset(titles, obsList);
		int total = dataset.getSeriesCount();

		return ChartFactory.getScatterChartView(context, dataset, renderer);

	}

	/**
	 * Builds an XY multiple dataset using the provided values.
	 * 
	 * @param titles
	 *            the series titles
	 * @param xValues
	 *            the values for the X axis
	 * @param yValues
	 *            the values for the Y axis
	 * @return the XY multiple dataset
	 */
	protected XYMultipleSeriesDataset buildDataset(String[] titles,
			ArrayList<CbObservation> obsList) {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		// add each component to the dataset
		List<Date[]> xValues = new ArrayList<Date[]>();
		List<double[]> yValues = new ArrayList<double[]>();
		int count = 0;
		for (CbObservation obs : obsList) {
			Date[] dates = new Date[1];
			double[] values = new double[1];
			dates[0] = new Date(obs.getTime());

			values[0] = obs.getObservationValue();

			xValues.add(dates);
			yValues.add(values);

		}

		dataset = addXYSeries(dataset, titles, xValues, yValues, 0);
		return dataset;
	}

	public XYMultipleSeriesDataset addXYSeries(XYMultipleSeriesDataset dataset,
			String[] titles, List<Date[]> xValues, List<double[]> yValues,
			int scale) {
		TimeSeries series = new TimeSeries(titles[0]);
		for (int i = 0; i < xValues.size(); i++) {
			Date[] xV = xValues.get(i);
			double[] yV = yValues.get(i);
			if (yV[0] < 0.0) {
				continue;
			}
			series.add(xV[0], yV[0]);

		}
		dataset.addSeries(series);
		return dataset;
	}

	/**
	 * Add appropriate x-axis date/time labels
	 * 
	 * @param renderer
	 * @param xMin
	 * @param xMax
	 */
	protected void addXLabels(XYMultipleSeriesRenderer renderer, long xMin,
			long xMax) {
		long timeSpanHours = (xMax - xMin) / (1000 * 60 * 60);

		Date minDate = new Date(xMin);
		Calendar minCal = Calendar.getInstance();
		minCal.setTime(minDate);

		Date maxDate = new Date(xMax);
		Calendar maxCal = Calendar.getInstance();
		maxCal.setTime(maxDate);

		Calendar markerCal = Calendar.getInstance();

		SimpleDateFormat df = new SimpleDateFormat("kk:mm");
		if (timeSpanHours <= 6) {
			// markers at 0 and every couple hours
			df = new SimpleDateFormat("kk:mm");
			markerCal.setTime(minDate);

			// renderer.addXTextLabel(xMin, df.format(minDate).toString());

			markerCal.set(Calendar.MINUTE, 0);
			int step = 1;
			for (int i = 1; i <= 6; i += step) {
				markerCal.add(Calendar.HOUR_OF_DAY, step);
				long markerInMs = markerCal.getTimeInMillis();
				renderer.addXTextLabel(markerInMs,
						df.format(markerCal.getTime()).toString());
			}
		} else if ((timeSpanHours > 6) && (timeSpanHours < 24)) {
			// marker at 0, then one marker per day
			df = new SimpleDateFormat("L/dd kk:mm");
			markerCal.setTime(minDate);
			// renderer.addXTextLabel(xMin, df.format(minDate).toString());
			markerCal.set(Calendar.MINUTE, 0);
			int step = 3;
			if (timeSpanHours > 12) {
				step = 4;
			}
			for (int i = 1; i < 19; i += step) {
				markerCal.add(Calendar.HOUR_OF_DAY, step);
				long markerInMs = markerCal.getTimeInMillis();
				renderer.addXTextLabel(markerInMs,
						df.format(markerCal.getTime()).toString());
			}
		} else if (timeSpanHours >= 24) {
			// marker at 0, then one marker per day
			df = new SimpleDateFormat("LLL dd");
			markerCal.setTime(minDate);
			// renderer.addXTextLabel(xMin, df.format(minDate).toString());
			int step = 1;
			for (int i = 1; i <= 7; i += step) {
				markerCal.add(Calendar.DAY_OF_MONTH, step);
				long markerInMs = markerCal.getTimeInMillis();
				renderer.addXTextLabel(markerInMs,
						df.format(markerCal.getTime()).toString());
			}
		}

	}

	/**
	 * Sets a few of the series renderer settings.
	 * 
	 * @param renderer
	 *            the renderer to set the properties to
	 * @param title
	 *            the chart title
	 * @param xTitle
	 *            the title for the X axis
	 * @param yTitle
	 *            the title for the Y axis
	 * @param xMin
	 *            the minimum value on the X axis
	 * @param xMax
	 *            the maximum value on the X axis
	 * @param yMin
	 *            the minimum value on the Y axis
	 * @param yMax
	 *            the maximum value on the Y axis
	 * @param axesColor
	 *            the axes color
	 * @param labelsColor
	 *            the labels color
	 */
	protected void setChartSettings(XYMultipleSeriesRenderer renderer,
			String title, String xTitle, String yTitle, long xMin, long xMax,
			double yMin, double yMax, int axesColor, int labelsColor) {

		renderer.setXTitle("");
		renderer.setYTitle("");
		renderer.setShowLegend(false);
		renderer.setXLabelsPadding(10);
		renderer.setYLabelsPadding(0);
		renderer.setYAxisMin(yMin);
		renderer.setYAxisMax(yMax);
		renderer.setYLabelsColor(0, Color.rgb(51, 51, 51));
		renderer.setXLabelsColor(Color.rgb(51, 51, 51));
		renderer.setYLabelsAlign(Align.RIGHT, 0);
		renderer.setAxesColor(axesColor);
		renderer.setLabelsColor(labelsColor);
		renderer.setMarginsColor(Color.rgb(238, 238, 238));
		renderer.setXLabels(0);
		addXLabels(renderer, xMin, xMax);
		renderer.setMargins(new int[] { 10, 60, 15, 20 });

	}

	/**
	 * 
	 * @param message
	 */
	public void logToFile(String message) {
		try {
			OutputStream output = new FileOutputStream(mAppDir + "/log.txt",
					true);
			String logString = (new Date()).toString() + ": " + message + "\n";
			output.write(logString.getBytes());
			output.close();
		} catch (FileNotFoundException e) {
			// e.printStackTrace();
		} catch (IOException ioe) {
			// ioe.printStackTrace();
		}
	}

	/**
	 * Log
	 * 
	 * @param message
	 */
	public void log(String message) {
		if (PressureNETConfiguration.DEBUG_MODE) {
			System.out.println(message);
			//logToFile(message);
		}
	}

	/**
	 * Prepare to write a log to SD card. Not used unless logging enabled.
	 */
	private void setUpFiles() {
		try {
			File homeDirectory = context.getExternalFilesDir(null);
			if (homeDirectory != null) {
				mAppDir = homeDirectory.getAbsolutePath();

			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

}
