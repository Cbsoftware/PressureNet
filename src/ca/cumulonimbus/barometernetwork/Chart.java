package ca.cumulonimbus.barometernetwork;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import ca.cumulonimbus.pressurenetsdk.CbObservation;

public class Chart {

	Context context;

	public Chart(Context ctx) {
		context = ctx;
	}

	protected void setRenderer(XYMultipleSeriesRenderer renderer, int[] colors,
			PointStyle[] styles,
			ArrayList<CbObservation> obsList) {
		renderer.setAxisTitleTextSize(20);
		renderer.setChartTitleTextSize(20);
		renderer.setLabelsTextSize(20);
		renderer.setLegendTextSize(20);
		renderer.setPointSize(5f);
		renderer.setMargins(new int[] { 20, 50, 15, 20 });

		for (int i = 0; i < 1; i++) { // TODO: fix 1 hack
			// TODO: Colors and Style
			XYSeriesRenderer r = new XYSeriesRenderer();
			r.setColor(colors[i]);
			// System.out.println("setting renderer color " + colors[i] );
			r.setPointStyle(styles[0]);
			renderer.addSeriesRenderer(r);
		}
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
			PointStyle[] styles,
			ArrayList<CbObservation> obsList) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		setRenderer(renderer, colors, styles, obsList);
		return renderer;
	}

	public int random128() {
		return 1 + (int) (Math.random() * ((128 - 1) + 1));
	}

	public View drawChart(ArrayList<CbObservation> obsList) {
		System.out.println("drawing chart " + obsList.size()
				+ " data points");
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
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());

		Date minTime = c.getTime();

		c.setTimeInMillis(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 7)); 
		Date maxTime = c.getTime();

		int i = 0;
		double yMean = 1000;
		double ySum = 0;
		for (CbObservation obs : obsList) {
			if(obs.getObservationValue() < 0) {
				i++;
				continue; // TODO: fix hack
			}
			if (obs.getObservationValue() < minObservation) {
				minObservation = obs.getObservationValue();
			}
			if (obs.getObservationValue() > maxObservation) {
				maxObservation = obs.getObservationValue();
			}
			if (obs.getTime() < minTime.getTime()) {
				minTime = new Date(obs.getTime());
			}
			if (obs.getTime() > maxTime.getTime()) {
				maxTime = new Date(obs.getTime());
			}
			xValues[i] = new Date(obs.getTime());
			yValues[i] = obs.getObservationValue();
			
			ySum += yValues[i];
			i++;
		}

		yMean = ySum / i;
		
		x.add(xValues);
		values.add(yValues);

		int[] colors = new int[count];
		int colorVal = 16;
		for (i = 0; i < count; i++) {
			int blue = 128 + random128();
			int green = blue - (3 * colorVal);
			int red = blue - (11 * colorVal);
			if (red < 0) {
				red = 0;
			}
			
			// colors[i] = Color.rgb(red, green, blue);
			 colors[i] = Color.rgb(20, 20, 255);
		}
		
		// TODO: Implement smarter axis min/max. range around 1 sd from the mean?
		// current hack is min/max observation or hardcoded default range

		PointStyle[] styles = new PointStyle[] { PointStyle.CIRCLE };
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles,
				obsList);
		setChartSettings(renderer, "Pressure", "Time", "Pressure (mb)",
				minTime, maxTime, minObservation, maxObservation, Color.GRAY,
				Color.LTGRAY);
		renderer.setXLabels(5);
		renderer.setYLabels(5);
		length = renderer.getSeriesRendererCount();
		for (i = 0; i < length; i++) {
			((XYSeriesRenderer) renderer.getSeriesRendererAt(i))
					.setFillPoints(true);
		}
		XYMultipleSeriesDataset dataset = buildDataset(titles, obsList);
		System.out.println("FINAL CALL " + dataset.getSeriesCount() + ", "
				+ renderer.getSeriesRendererCount());
		int total = dataset.getSeriesCount();
		for (i = 0; i < total; i++) {
			// System.out.println(i + " min ys " +
			// dataset.getSeriesAt(i).getMinY());
		}

		return ChartFactory.getTimeChartView(context, dataset, renderer,
				"yyyy/MM/dd");

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
		System.out.println("build dataset");
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		// add each component to the dataset
		List<Date[]> xValues = new ArrayList<Date[]>();
		List<double[]> yValues = new ArrayList<double[]>();
		int count = 0;
		for(CbObservation obs : obsList) {
			Date[] dates = new Date[1];
			double[] values = new double[1];	
			dates[0] = new Date(obs.getTime());
			
			values[0] = obs.getObservationValue();

			xValues.add(dates);
			yValues.add(values);
			
			// bail after a while
			count++;
			if(count>5000) {
				System.out.println("CHART 5000 BAILING");
				break;
			}
		
		}
		
		System.out.println("dataset sizes split to  " + titles.length + " titles " + xValues.size() + " xvalues " + yValues.size() + " yValues");
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
			if(yV[0] < 0.0) {
				continue;
			}
			series.add(xV[0], yV[0]);
			
		}
		dataset.addSeries(series);
		return dataset;
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
			String title, String xTitle, String yTitle, Date xMin, Date xMax,
			double yMin, double yMax, int axesColor, int labelsColor) {
		// renderer.setChartTitle(title);
		renderer.setXTitle(xTitle);
		renderer.setYTitle(yTitle);
		renderer.setShowLegend(false);
		// renderer.setXAxisMin(xMin);
		// renderer.setXAxisMax(xMax);
		renderer.setYAxisMin(yMin);
		renderer.setYAxisMax(yMax);
		renderer.setAxesColor(axesColor);
		renderer.setLabelsColor(labelsColor);
	}

}
