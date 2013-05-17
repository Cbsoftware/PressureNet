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
import android.graphics.Color;
import android.view.View;
import ca.cumulonimbus.pressurenetsdk.CbObservation;

public class Chart {
	
	Context context;
	
	public Chart(Context ctx) {
		context = ctx;
	}

	
	
	protected void setRenderer(XYMultipleSeriesRenderer renderer, int[] colors,
			PointStyle[] styles, HashMap<String, ArrayList<CbObservation>> userMap) {
		renderer.setAxisTitleTextSize(20);
		renderer.setChartTitleTextSize(20);
		renderer.setLabelsTextSize(20);
		renderer.setLegendTextSize(20);
		renderer.setPointSize(5f);
		renderer.setMargins(new int[] { 20, 50, 15, 20 });
		
		int uniq = userMap.size();
	//	System.out.println("renderer adding " + uniq);
		

		
		for ( int i = 0; i< uniq; i++) {
			// TODO: Colors and Style
			XYSeriesRenderer r = new XYSeriesRenderer();
			r.setColor(colors[i]);
	//		System.out.println("setting renderer color " + colors[i] );
			r.setPointStyle(styles[0]);
			renderer.addSeriesRenderer(r);
		}
	}
	
/**
 * Take a raw list of arbitrary observations and organize them by user id
 * @param rawListWeather
 * @return
 */
	public HashMap<String, ArrayList<CbObservation>> mixedDataToUserMap(ArrayList<CbObservation> rawList) {
		HashMap<String, ArrayList<CbObservation>> userMap = new HashMap<String, ArrayList<CbObservation>>();

		System.out.println("making user map raw size " + rawList.size());
		for (CbObservation currentWeather : rawList) {
			CbObservation current = (CbObservation) currentWeather;
			if (userMap.containsKey(current.getUser_id())) {
				userMap.get(current.getUser_id()).add(current);
			} else {
				ArrayList<CbObservation> newList = new ArrayList<CbObservation>();
				newList.add(current);
				userMap.put(current.getUser_id(), newList);
			}
		}
		System.out.println("returning user map size" + userMap.size());
		return userMap;
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
			PointStyle[] styles, HashMap<String, ArrayList<CbObservation>> userMap) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		setRenderer(renderer, colors, styles, userMap);
		return renderer;
	}

	public View drawChart(ArrayList<CbObservation> obsList) {
		HashMap<String, ArrayList<CbObservation>> userMap =  mixedDataToUserMap(obsList);
		System.out.println("drawing chart " + obsList.size() + " data points from " + userMap.size() + " users");
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
		
		c.setTimeInMillis(System.currentTimeMillis() - (1000 * 60 * 60 * 24)); // TODO: fix max 1-day-ago support
		Date maxTime = c.getTime();
		
		int i = 0;
		for(CbObservation obs : obsList) {
			if(obs.getObservationValue() < minObservation) {
				minObservation = obs.getObservationValue();
			}
			if(obs.getObservationValue() > maxObservation) {
				maxObservation = obs.getObservationValue();
			}
			if(obs.getTime() < minTime.getTime()) {
				minTime = new Date(obs.getTime());
			}
			if(obs.getTime() > maxTime.getTime()) {
				maxTime = new Date(obs.getTime());
			}	
			xValues[i] = new Date(obs.getTime());
			yValues[i] = obs.getObservationValue();
			i++;
			
		}
	
		x.add(xValues);
		values.add(yValues);
	
		
		int[] colors = new int[count];
		for(i = 0; i<count; i++ ) {
//			System.out.println("color " + i);
			colors[i] = Color.rgb(0, 192-(i*2), 255-(i*2));
		}
		
		
		PointStyle[] styles = new PointStyle[] { PointStyle.CIRCLE };
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles, userMap);
		setChartSettings(renderer, "Pressure", "Time", "Pressure (mb)", minTime, maxTime, minObservation, maxObservation,
				Color.GRAY, Color.LTGRAY);
		renderer.setXLabels(5);
		renderer.setYLabels(5);
		length = renderer.getSeriesRendererCount();
		for (i = 0; i < length; i++) {
			((XYSeriesRenderer) renderer.getSeriesRendererAt(i))
					.setFillPoints(true);
		}
		XYMultipleSeriesDataset dataset = buildDataset(titles, userMap);
		System.out.println("FINAL CALL " + dataset.getSeriesCount() + ", " + renderer.getSeriesRendererCount());
		int total = dataset.getSeriesCount();
		for(i = 0; i<total;i++) {
			//System.out.println(i + " min ys " + dataset.getSeriesAt(i).getMinY());
		}
		
		return ChartFactory.getTimeChartView(context, dataset, renderer, "yyyy/MM/dd");
		
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
			HashMap<String, ArrayList<CbObservation>> userMap) {
		System.out.println("build dataset");
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		// add each component to the dataset
		Iterator it = userMap.entrySet().iterator();
		while (it.hasNext()) {
			List<Date[]> xValues = new ArrayList<Date[]> ();
			List<double[]> yValues = new ArrayList<double[]>();
			Map.Entry<String,ArrayList<CbObservation>> pairs = (Map.Entry<String,ArrayList<CbObservation>>)it.next();
			ArrayList<CbObservation> singleUserObsList = pairs.getValue();
			Date[] dates = new Date[singleUserObsList.size()];
			double[] values = new double[singleUserObsList.size()];
			int x = 0;
			for(CbObservation obs : singleUserObsList ) {
				dates[x] = new Date(obs.getTime());
				values[x]= obs.getObservationValue();
				x++;
			}
			xValues.add(dates);
			yValues.add(values);
			dataset = addXYSeries(dataset, titles, xValues, yValues, 0);
			///it.remove();
		}
		return dataset;
	}

	public XYMultipleSeriesDataset addXYSeries(XYMultipleSeriesDataset dataset, String[] titles,
			List<Date[]> xValues, List<double[]> yValues, int scale) {
		int length = titles.length;
		for (int i = 0; i < length; i++) {
			TimeSeries series = new TimeSeries(titles[i]);
			Date[] xV = xValues.get(i);
			double[] yV = yValues.get(i);
			int seriesLength = xV.length;
			for (int k = 0; k < seriesLength; k++) {
				series.add(xV[k], yV[k]);
			}
			dataset.addSeries(series);
		}
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
			String title, String xTitle, String yTitle, Date xMin,
			Date xMax, double yMin, double yMax, int axesColor,
			int labelsColor) {
		//renderer.setChartTitle(title);
		renderer.setXTitle(xTitle);
		renderer.setYTitle(yTitle);
		renderer.setShowLegend(false);
		//renderer.setXAxisMin(xMin);
		//renderer.setXAxisMax(xMax);
		renderer.setYAxisMin(yMin);
		renderer.setYAxisMax(yMax);
		renderer.setAxesColor(axesColor);
		renderer.setLabelsColor(labelsColor);
	}

}
