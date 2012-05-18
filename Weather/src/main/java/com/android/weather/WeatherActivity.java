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
import com.weather.acquisition.LRUFtpFiles;
import com.weather.acquisition.MapOverlayImageInfo;
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
	private ProgressDialog progressDialog;

	private final AtomicBoolean processing = new AtomicBoolean(false);
	private int progress = 0;
	private String message = "";
	private Bitmap imBitmap;
	private String downloadTarFileName;
	private long downloadTarFileSize;
	private Populator<Class3X01Y192> imageGeneralData;

	private MapView mapView;
	private ExpandableRadarSelectionAdapter mapAdapter;

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

			final ExpandableListView radarComboList = (ExpandableListView)findViewById(R.id.radarComboList);
			radarComboList.setItemsCanFocus(false);
			radarComboList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

			final ExpandableRadarSelectionListener listener = new ExpandableRadarSelectionListener() {
				@Override
				public void itemSelected(final RadarCenter selectedRadar) {
					setSelectedRadar(selectedRadar);
					setMapRadarCenter();
				}
			};
			mapAdapter = new ExpandableRadarSelectionAdapter(radarComboList, listener);
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

			progressDialog = new ProgressDialog(WeatherActivity.this.applyButton.getContext());
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setCancelable(false);
			progressDialog.setMessage("Comprobando datos.");

			processing.set(false);
			progress = 0;

			setMapRadarCenter();

			//Carga de lru
			LRUFtpFiles.getInstance();
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
		mapAdapter.setSelected(this.selectedRadar, false);
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
			applyButton.setEnabled(false);
			applyButton.setVisibility(View.INVISIBLE);
			progress = 0;

			progressDialog.setMax(100);
			progressDialog.show();
		}

	};

	private final Runnable progressThread = new Runnable() {

		@Override
		public synchronized void run() {
			progressDialog.setMessage(message);
			progressDialog.setProgress(progress);
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

		if(progressDialog != null) {
			progressDialog.hide();
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
		if (!processing.getAndSet(true)) {
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

		((ExceptionActionRunnable) processResetAction).setException((Exception) e);
		progressThreadHandler.post(processResetAction);
	}

	@Override
	public void processGetFtpEnded() {
		progressThreadHandler.post(processResetAction);
	}

	@Override
	public void setPercentageMessage(final String message) {
		this.message = message;
		lastProgressUpdate = -1;
		progressThreadHandler.post(progressThread);
	}

	@Override
	public void setPercentageProgression(final int partial, final int maxTotal) {
		if (maxTotal > 0) {
			try {
				progress = (int) (((double) partial / (double) maxTotal) * 99);
			} catch (Exception e) {
				Log.e("set progress", "error asignando valor", e);
			}

			if ( (progress - lastProgressUpdate > 5)
					|| (lastProgressUpdate > 0 && progress < lastProgressUpdate) ) {
				lastProgressUpdate = progress;
				progressThreadHandler.post(progressThread);
			}
		} else {
			progress = 0;
		}
	}

	@Override
	public void setDownloadTarFile(final String downloadTarFileName, final long downloadTarFileSize) {
		this.downloadTarFileName = downloadTarFileName;
		this.downloadTarFileSize = downloadTarFileSize;
	}

	@Override
	public void setProcessedImage(final Bitmap tempBitmap,
			final Populator<Class3X01Y192> imageGeneralData) {

		imBitmap = tempBitmap;
		imBitmap.prepareToDraw();

		this.imageGeneralData = imageGeneralData;

		//mapear en las ultimas y persistir
		final MapOverlayImageInfo lastImage = new MapOverlayImageInfo(tempBitmap, downloadTarFileSize, imageGeneralData);
		LRUFtpFiles.getInstance().put(downloadTarFileName, lastImage);
		LRUFtpFiles.getInstance().serialize();

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
