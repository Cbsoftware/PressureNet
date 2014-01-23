package ca.cumulonimbus.barometernetwork;

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
import ca.cumulonimbus.pressurenetsdk.CbStats;

public class StatsChart {

	Context context;
	private String mAppDir = "";

	private int textSize = 20;
	private float pointSize = 1f;

	public StatsChart(Context ctx) {
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
		    	 pointSize = 1f;
		         break;
		     case DisplayMetrics.DENSITY_MEDIUM:
		    	 textSize = 16;
		    	 pointSize = 1f;
		    	 break;
		     case DisplayMetrics.DENSITY_HIGH:
		    	 textSize = 18;
		    	 pointSize = 1f;
		    	 break;
		     case DisplayMetrics.DENSITY_XHIGH:
		    	 textSize = 20;
		    	 pointSize = 1f;
                 break;
		     case DisplayMetrics.DENSITY_XXHIGH:
		    	 textSize = 26;
		    	 pointSize = 1f;
                 break;
		     default:
		    	 break;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	protected void setRenderer(XYMultipleSeriesRenderer renderer, int[] colors,
			PointStyle[] styles, ArrayList<CbStats> statList) {

		setTextSize();
		renderer.setAxisTitleTextSize(textSize);
		renderer.setChartTitleTextSize(textSize);
		renderer.setLabelsTextSize(textSize);
		renderer.setLegendTextSize(textSize);
		renderer.setPointSize(pointSize);
		renderer.setMargins(new int[] { 20, 50, 15, 20 });

		XYSeriesRenderer r = new XYSeriesRenderer();
		r.setColor(colors[0]);
		r.setPointStyle(PointStyle.CIRCLE);
		r.setLineWidth(4);
		r.setPointStrokeWidth(2);
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
			PointStyle[] styles, ArrayList<CbStats> statList) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		setRenderer(renderer, colors, styles, statList);
		return renderer;
	}

	public int random128() {
		return 1 + (int) (Math.random() * ((128 - 1) + 1));
	}

	public View drawChart(ArrayList<CbStats> statsList) {
		// System.out.println("drawing chart " + obsList.size() +
		// " data points");

		if (statsList.size() <= 2) {
			Toast.makeText(context, "There's no data to plot",
					Toast.LENGTH_SHORT).show();
		}

		String[] titles = new String[] { "" };
		List<Date[]> x = new ArrayList<Date[]>();
		List<double[]> values = new ArrayList<double[]>();
		int length = titles.length;
		int count = statsList.size();

		Date[] xValues = new Date[count];
		double[] yValues = new double[count];

		// TODO: Expand to multiple observations
		// currently only pressure
		double minObservation = 1200;
		double maxObservation = 0;
		long minTime = System.currentTimeMillis();
		long maxTime = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 7);
		
		int i = 0;
		for(CbStats stat : statsList) {
			if (i>0) {
				double previous = yValues[i-1];
				if (Math.abs(stat.getMean() - previous) > 30) {
					stat.setMean(previous);
				}
			}
			
			xValues[i] = new Date(stat.getTimeStamp());
			yValues[i] = stat.getMean();
		
			//log(xValues[i] + " " + yValues[i]);
			
			if (stat.getMean() < minObservation) {
				minObservation = stat.getMean();
			}
			if (stat.getMean() > maxObservation) {
				maxObservation = stat.getMean();
			}
			if (stat.getTimeStamp() < minTime) {
				minTime = stat.getTimeStamp();
			}
			if (stat.getTimeStamp() > maxTime) {
				maxTime = stat.getTimeStamp();
			}
			i++;

		}
		
		x.add(xValues);
		values.add(yValues);

		int[] colors = new int[count];
		for (i = 0; i < count; i++) {
			colors[i] = Color.rgb(51, 181, 229);
		}

		PointStyle[] styles = new PointStyle[] { PointStyle.CIRCLE };
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles,
				statsList);
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
		XYMultipleSeriesDataset dataset = buildDataset(titles, statsList);
		int total = dataset.getSeriesCount();

		return ChartFactory.getLineChartView(context, dataset, renderer);

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
			ArrayList<CbStats> statsList) {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		// add each component to the dataset
		List<Date[]> xValues = new ArrayList<Date[]>();
		List<double[]> yValues = new ArrayList<double[]>();
		int count = 0;
		for (CbStats obs : statsList) {
			Date[] dates = new Date[1];
			double[] values = new double[1];
			dates[0] = new Date(obs.getTimeStamp());

			values[0] = obs.getMean();

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

