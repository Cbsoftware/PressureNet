package ca.cumulonimbus.barometernetwork;

import java.util.Calendar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import ca.cumulonimbus.pressurenetsdk.CbCurrentCondition;

public class ConditionsDrawables {

	Context mContext;
	
	/**
	 * Moon phase info
	 */
	private int getMoonPhaseIndex() {
		MoonPhase mp = new MoonPhase(Calendar.getInstance());
		return mp.getPhaseIndex();
	}


	
	public ConditionsDrawables (Context ctx) {
		mContext = ctx;
	}
	
	// The gesture threshold expressed in dp
		// http://developer.android.com/guide/practices/screens_support.html#density-independence
		private static final float GESTURE_THRESHOLD_DP = 16.0f;

	
	/**
	 * Resize drawables on demand. High-res bitmaps on Android? Be careful of
	 * memory issues
	 * 
	 * @param image
	 * @return
	 */
	private Drawable resizeDrawable(Drawable image) {
		Bitmap d = ((BitmapDrawable) image).getBitmap();
		final float scale = mContext.getResources().getDisplayMetrics().density;
		int p = (int) (GESTURE_THRESHOLD_DP * scale + 0.5f);
		Bitmap bitmapOrig = Bitmap.createScaledBitmap(d, p * 4, p * 4, false);
		return new BitmapDrawable(bitmapOrig);
	}
	
	
	/**
	 * Create neat drawables for weather conditions depending on the type of
	 * weather, the time, etc.
	 * 
	 * @param condition
	 * @param drawable
	 * @return
	 */
	public LayerDrawable getCurrentConditionDrawable(
			CbCurrentCondition condition, Drawable drawable) {

		Drawable weatherBackgroundDrawable = resizeDrawable(mContext
				.getResources().getDrawable(R.drawable.bg_wea_square));

		if (CurrentConditionsActivity.isDaytime(condition.getLocation()
				.getLatitude(), condition.getLocation().getLongitude(),
				condition.getTime(), condition.getTzoffset())) {
			weatherBackgroundDrawable = resizeDrawable(mContext
					.getResources().getDrawable(R.drawable.bg_wea_day));
		} else {
			weatherBackgroundDrawable = resizeDrawable(mContext
					.getResources().getDrawable(R.drawable.bg_wea_night));
		}

		int moonNumber = getMoonPhaseIndex() + 1;

		if (condition.getGeneral_condition().equals(mContext.getString(R.string.sunny))) {
			Drawable sunDrawable = mContext.getResources().getDrawable(
					R.drawable.ic_wea_col_sun);
			if (!CurrentConditionsActivity.isDaytime(condition.getLocation()
					.getLatitude(), condition.getLocation().getLongitude(),
					condition.getTime(), condition.getTzoffset())) {
				switch (moonNumber) {
				case 1:
					sunDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_moon1);
					break;
				case 2:
					sunDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_moon2);
					break;
				case 3:
					sunDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_moon3);
					break;
				case 4:
					sunDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_moon4);
					break;
				case 5:
					sunDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_moon5);
					break;
				case 6:
					sunDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_moon6);
					break;
				case 7:
					sunDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_moon7);
					break;
				case 8:
					sunDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_moon8);
					break;
				default:
					sunDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_moon2);
					break;
				}

			}
			Drawable[] layers = { weatherBackgroundDrawable,
					resizeDrawable(sunDrawable) };
			LayerDrawable layerDrawable = new LayerDrawable(layers);
			return layerDrawable;
		} else if (condition.getGeneral_condition().equals(
				mContext.getString(R.string.precipitation))) {
			if (condition.getPrecipitation_type().equals(
					mContext.getString(R.string.rain))) {
				if (condition.getPrecipitation_amount() == 0.0) {
					Drawable rainDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_rain1);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(rainDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				} else if (condition.getPrecipitation_amount() == 1.0) {
					Drawable rainDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_rain2);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(rainDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				} else if (condition.getPrecipitation_amount() == 2.0) {
					Drawable rainDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_rain3);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(rainDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				}
			} else if (condition.getPrecipitation_type().equals(
					mContext.getString(R.string.snow))) {
				if (condition.getPrecipitation_amount() == 0.0) {
					Drawable snowDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_snow1);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(snowDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				} else if (condition.getPrecipitation_amount() == 1.0) {
					Drawable snowDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_snow2);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(snowDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				} else if (condition.getPrecipitation_amount() == 2.0) {
					Drawable snowDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_snow3);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(snowDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				}
			} else if (condition.getPrecipitation_type().equals(
					mContext.getString(R.string.hail))) {
				if (condition.getPrecipitation_amount() == 0.0) {
					Drawable hailDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_hail1);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(hailDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				} else if (condition.getPrecipitation_amount() == 1.0) {
					Drawable hailDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_hail2);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(hailDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				} else if (condition.getPrecipitation_amount() == 2.0) {
					Drawable hailDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_hail3);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(hailDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				}
			} else {
				// TODO: this is a copypaste of rain
				if (condition.getPrecipitation_amount() == 0.0) {
					Drawable rainDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_rain1);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(rainDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				} else if (condition.getPrecipitation_amount() == 1.0) {
					Drawable rainDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_rain2);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(rainDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				} else if (condition.getPrecipitation_amount() == 2.0) {
					Drawable rainDrawable = mContext.getResources().getDrawable(
							R.drawable.ic_wea_col_rain3);
					Drawable[] layers = { weatherBackgroundDrawable,
							resizeDrawable(rainDrawable) };
					LayerDrawable layerDrawable = new LayerDrawable(layers);
					return layerDrawable;
				}
			}
		} else if (condition.getGeneral_condition().equals(
				mContext.getString(R.string.cloudy))) {
			if (condition.getCloud_type().equals(
					mContext.getString(R.string.partly_cloudy))) {
				Drawable cloudDrawable = mContext.getResources().getDrawable(
						R.drawable.ic_wea_col_cloud1);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(cloudDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else if (condition.getCloud_type().equals(
					mContext.getString(R.string.mostly_cloudy))) {
				Drawable cloudDrawable = mContext.getResources().getDrawable(
						R.drawable.ic_wea_col_cloud2);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(cloudDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else if (condition.getCloud_type().equals(
					mContext.getString(R.string.very_cloudy))) {
				Drawable cloudDrawable = mContext.getResources().getDrawable(
						R.drawable.ic_wea_col_cloud);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(cloudDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else {
				Drawable cloudDrawable = mContext.getResources().getDrawable(
						R.drawable.ic_wea_col_cloud);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(cloudDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			}
		} else if (condition.getGeneral_condition().equals(
				mContext.getString(R.string.foggy))) {
			if (condition.getFog_thickness().equals(
					mContext.getString(R.string.light_fog))) {
				Drawable fogDrawable = mContext.getResources().getDrawable(
						R.drawable.ic_wea_col_fog1);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(fogDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else if (condition.getFog_thickness().equals(
					mContext.getString(R.string.moderate_fog))) {
				Drawable fogDrawable = mContext.getResources().getDrawable(
						R.drawable.ic_wea_col_fog2);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(fogDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else if (condition.getFog_thickness().equals(
					mContext.getString(R.string.heavy_fog))) {
				Drawable fogDrawable = mContext.getResources().getDrawable(
						R.drawable.ic_wea_col_fog3);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(fogDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else {
				Drawable fogDrawable = mContext.getResources().getDrawable(
						R.drawable.ic_wea_col_fog2);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(fogDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			}
		} else if (condition.getGeneral_condition().equals(
				mContext.getString(R.string.thunderstorm))) {
			try {
				double d = Double.parseDouble(condition
						.getThunderstorm_intensity());
			} catch (Exception e) {
				condition.setThunderstorm_intensity("0");
			}
			if (Double.parseDouble(condition.getThunderstorm_intensity()) == 0.0) {
				Drawable thunderstormDrawable = mContext
						.getResources().getDrawable(R.drawable.ic_wea_col_r_l1);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(thunderstormDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else if (Double
					.parseDouble(condition.getThunderstorm_intensity()) == 1.0) {
				Drawable thunderstormDrawable = mContext
						.getResources().getDrawable(R.drawable.ic_wea_col_r_l2);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(thunderstormDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else if (Double
					.parseDouble(condition.getThunderstorm_intensity()) == 2.0) {
				Drawable thunderstormDrawable = mContext
						.getResources().getDrawable(R.drawable.ic_wea_col_r_l3);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(thunderstormDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			}
		} else if (condition.getGeneral_condition().equals(
				mContext.getString(R.string.extreme))) {
			if (condition.getUser_comment().equals(mContext.getString(R.string.flooding))) {
				Drawable floodingDrawable = mContext
						.getResources().getDrawable(R.drawable.ic_wea_col_flooding);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(floodingDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else if (condition.getUser_comment().equals(mContext.getString(R.string.wildfire))) {
				Drawable fireDrawable = mContext
						.getResources().getDrawable(R.drawable.ic_wea_col_fire);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(fireDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else if (condition.getUser_comment().equals(mContext.getString(R.string.tornado))) {
				Drawable tornadoDrawable = mContext
						.getResources().getDrawable(R.drawable.ic_wea_col_tornado);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(tornadoDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else if (condition.getUser_comment().equals(mContext.getString(R.string.duststorm))) {
				Drawable dustDrawable = mContext
						.getResources().getDrawable(R.drawable.ic_wea_col_dust);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(dustDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			} else if (condition.getUser_comment().equals(mContext.getString(R.string.tropicalstorm))) {
				Drawable tropicalDrawable = mContext
						.getResources().getDrawable(R.drawable.ic_wea_col_tropical_storm);
				Drawable[] layers = { weatherBackgroundDrawable,
						resizeDrawable(tropicalDrawable) };
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				return layerDrawable;
			}
		}

		return null;
	}

	public Drawable getSingleDrawable(LayerDrawable layerDrawable) {

		int resourceBitmapHeight = layerDrawable.getMinimumHeight(), resourceBitmapWidth = layerDrawable
				.getMinimumWidth();

		float widthInInches = 0.2f;

		int widthInPixels = (int) (widthInInches * mContext.getResources()
				.getDisplayMetrics().densityDpi);
		int heightInPixels = (int) (widthInPixels * resourceBitmapHeight / resourceBitmapWidth);

		int insetLeft = 10, insetTop = 10, insetRight = 10, insetBottom = 10;

		layerDrawable.setLayerInset(1, insetLeft, insetTop, insetRight,
				insetBottom);

		Bitmap bitmap = Bitmap.createBitmap(widthInPixels, heightInPixels,
				Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmap);
		layerDrawable.setBounds(0, 0, widthInPixels, heightInPixels);
		layerDrawable.draw(canvas);

		BitmapDrawable bitmapDrawable = new BitmapDrawable(mContext.getResources(),
				bitmap);
		bitmapDrawable.setBounds(0, 0, widthInPixels, heightInPixels);

		return bitmapDrawable;
	}

	
}

