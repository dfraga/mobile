package com.android.weather;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.ListView;

import com.android.utils.ExpandableRadarSelectionAdapter;
import com.android.utils.ExpandableRadarSelectionListener;
import com.android.utils.MapOverlay;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.weather.populate.Class3X01Y192;
import com.weather.populate.Populator;

public class WeatherActivity extends MapActivity implements WeatherProcessListener {

	{
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(final Thread thread, final Throwable ex) {
				processException(ex);
			}
		});
	}

	private Button applyButton;
	private static ProgressDialog progressDialog;

	private final AtomicBoolean processing = new AtomicBoolean(false);
	private static int progress = 0;
	private static String message = "";
	private Bitmap imBitmap;
	private String pngPath;
	private Populator<Class3X01Y192> imageGeneralData;

	private ExpandableListView radarComboList;
	private MapView mapView;

	private final Runnable processResetAction = new ExceptionActionRunnable() {
		@Override
		public void run() {
			resetLayout();
		}
	};

	final Handler progressThreadHandler = new Handler();
	private long lastProgressUpdate = -1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		try {

			applyButton = (Button) findViewById(R.id.execute);
			applyButton.setEnabled(true);
			applyButton.setVisibility(View.VISIBLE);
			applyButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					applyFilter();
				}
			});


			radarComboList = (ExpandableListView)findViewById(R.id.radarComboList);
			radarComboList.setItemsCanFocus(false);
			radarComboList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);


			final ExpandableRadarSelectionListener listener = new ExpandableRadarSelectionListener() {
				@Override
				public void itemSelected(final RadarCenter selectedRadar) {
					setSelectedRadar(selectedRadar);
					setMapRadarCenter();
				}
			};
			new ExpandableRadarSelectionAdapter(radarComboList, listener);
			radarComboList.setOnGroupExpandListener(new OnGroupExpandListener() {

				@Override
				public void onGroupExpand(final int groupPosition) {
					applyButton.setVisibility(View.INVISIBLE);
				}
			});
			radarComboList.setOnGroupCollapseListener(new OnGroupCollapseListener() {

				@Override
				public void onGroupCollapse(final int groupPosition) {
					applyButton.setVisibility(View.VISIBLE);
				}
			});


			mapView = (MapView) findViewById(R.id.mapView);
			mapView.setBuiltInZoomControls(true);
			mapView.setStreetView(false);
			mapView.setTraffic(false);
			mapView.setSatellite(true);

			WeatherActivity.progressDialog = new ProgressDialog(
					WeatherActivity.this.applyButton.getContext());
			WeatherActivity.progressDialog
			.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			WeatherActivity.progressDialog.setCancelable(false);
			WeatherActivity.progressDialog.setMessage("Comprobando datos.");

			processing.set(false);
			WeatherActivity.progress = 0;


			setMapRadarCenter();
		} catch (final Exception e) {
			// No compatible file manager.
			processException(e);
		}

	}

	public void setMapRadarCenter() {
		//Obtenemos ultima posicion conocida para centrar el mapa
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		String locationProvider = LocationManager.NETWORK_PROVIDER;
		Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);

		// obetener centro por defecto del seleccionado
		final GeoPoint center = (lastKnownLocation == null ?
				getGeoPoint(this.selectedRadar.getLatitude(), this.selectedRadar.getLongitude()):
					getGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));

		centerMap(center);
		RadarCenter nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for(RadarCenter radar:RadarCenter.values()) {
			GeoPoint currentGeo = getGeoPoint(radar.getLatitude(), radar.getLongitude());
			double distance = getFakeDistance(currentGeo, center);
			if(distance < nearestDistance) {
				nearest = radar;
				nearestDistance = distance;
			}
		}
		this.selectedRadar = nearest;
		mapView.invalidate();
	}

	private double getFakeDistance(final GeoPoint currentGeo, final GeoPoint center) {
		if(currentGeo == null || center == null) {
			return Double.MAX_VALUE;
		}
		return Math.pow((currentGeo.getLatitudeE6() - center.getLatitudeE6()), 2)
				+ Math.pow((currentGeo.getLongitudeE6() - center.getLongitudeE6()),2);
	}

	private void centerMap(final GeoPoint center) {
		final MapController mapController = mapView.getController();
		mapController.setCenter(center);
		mapController.animateTo(center);
		mapController.setZoom(8);
	}

	private final Runnable processBeginAction = new Runnable() {

		@Override
		public void run() {
			// Toast.makeText(PhotoColorSwitcherActivity.this,
			// "processBeginAction", Toast.LENGTH_SHORT).show();

			applyButton.setEnabled(false);
			applyButton.setVisibility(View.INVISIBLE);
			WeatherActivity.progress = 0;

			WeatherActivity.progressDialog.setMax(100);
			//XXX
			WeatherActivity.progressDialog.show();
		}

	};

	private final static Runnable progressThread = new Runnable() {

		@Override
		public synchronized void run() {
			WeatherActivity.progressDialog.setMessage(WeatherActivity.message);
			WeatherActivity.progressDialog
			.setProgress(WeatherActivity.progress);
		}

	};

	private final Runnable processEndAction = new Runnable() {

		@Override
		public void run() {
			resetLayout();

			if(imageGeneralData != null) {
				final GeoPoint center = getGeoPoint(Class3X01Y192.RADAR_LATITUDE, Class3X01Y192.RADAR_LONGITUDE);
				centerMap(center);


				List<Overlay> overlays = mapView.getOverlays();
				overlays.clear();

				MapOverlay mapOverlay = new MapOverlay(center, imBitmap,
						getGeoPoint(Class3X01Y192.LATITUDE_NW, Class3X01Y192.LONGITUDE_NW),
						getGeoPoint(Class3X01Y192.LATITUDE_SE, Class3X01Y192.LONGITUDE_SE));
				overlays.add(mapOverlay);
			}
			mapView.invalidate();

		}

	};

	private GeoPoint getGeoPoint(final Class3X01Y192 latitude, final Class3X01Y192 longitude) {
		return new GeoPoint((int)(1e6 * imageGeneralData.getData(latitude).getData().doubleValue()),
				(int)(1e6 * imageGeneralData.getData(longitude).getData().doubleValue()));
	}

	private GeoPoint getGeoPoint(final double latitude, final double longitude) {
		return new GeoPoint((int)(1e6 * latitude), (int)(1e6 * longitude));
	}

	private void resetLayout() {
		processing.set(false);
		lastProgressUpdate = -1;

		if(applyButton != null) {
			applyButton.setVisibility(View.VISIBLE);
			applyButton.setEnabled(true);
		}

		if(WeatherActivity.progressDialog != null) {
			WeatherActivity.progressDialog.hide();
		}
	}

	private class ExceptionActionRunnable implements Runnable {
		private Exception e;

		public void setException(final Exception e) {
			this.e = e;
		}

		@Override
		public void run() {
			processException(e);
		}
	};

	private RadarCenter selectedRadar = RadarCenter.MADRID;
	public void setSelectedRadar(final RadarCenter selectedRadar) {
		this.selectedRadar = selectedRadar == null ? this.selectedRadar : selectedRadar;
	}

	private void applyFilter() {
		// Toast.makeText(WeatherActivity.this, "applyFilter 1",
		// Toast.LENGTH_SHORT).show();
		if (!processing.getAndSet(true)) {
			// Toast.makeText(WeatherActivity.this, "applyFilter 2",
			// Toast.LENGTH_SHORT).show();
			try {
				// processing = true;
				progressThreadHandler.post(processBeginAction);
				final Thread processT = new MainProcess(
						this.selectedRadar,
						this,
						progressThreadHandler);
				processT.start();

			} catch (final Exception e) {
				processException(e);
			} finally {
				//progressThreadHandler.post(processEndAction);
			}
		}
	}

	public static String getStackTrace(final String prefix,
			final Throwable aThrowable) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return prefix + "\n" + result.toString();
	}

	@Override
	public void processException(final Throwable e) {
		final String error = WeatherActivity.getStackTrace(("Error: " + e
				+ " - " + e.getMessage()), e);
		Log.e("error", error);
		//Toast.makeText(WeatherActivity.this, error, Toast.LENGTH_SHORT).show();

		((ExceptionActionRunnable) processResetAction).setException((Exception) e);
		progressThreadHandler.post(processResetAction);
	}

	@Override
	public void processGetFtpEnded() {
		progressThreadHandler.post(processResetAction);
	}

	@Override
	public void setPercentageMessage(final String message) {
		WeatherActivity.message = message;
		lastProgressUpdate = -1;
		progressThreadHandler.post(WeatherActivity.progressThread);
	}

	@Override
	public void setPercentageProgression(final int partial, final int maxTotal) {
		if (maxTotal > 0) {
			try {
				WeatherActivity.progress = (int) (((double) partial / (double) maxTotal) * 99);
			} catch (Exception e) {
				Log.e("set progress", "error asignando valor", e);
			}

			if ( (WeatherActivity.progress - lastProgressUpdate > 5)
					|| (lastProgressUpdate > 0 && WeatherActivity.progress < lastProgressUpdate) ) {
				lastProgressUpdate = WeatherActivity.progress;
				progressThreadHandler.post(WeatherActivity.progressThread);
			}
		} else {
			WeatherActivity.progress = 0;
		}
	}

	@Override
	public void setProcessedImage(final Bitmap tempBitmap, final String pngPath,
			final Populator<Class3X01Y192> imageGeneralData) {

		imBitmap = tempBitmap;
		imBitmap.prepareToDraw();

		this.pngPath = pngPath;
		this.imageGeneralData = imageGeneralData;

		progressThreadHandler.post(processEndAction);

	}

	@Override
	public Context getContext() {
		return WeatherActivity.this;
	}

	@Override
	protected boolean isRouteDisplayed() {
		// Definir si se usa informacion de rutas
		return false;
	}

}
