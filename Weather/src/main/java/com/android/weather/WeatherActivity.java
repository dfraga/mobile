package com.android.weather;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.android.utils.GroundOverlay;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.weather.populate.Class3X01Y192;
import com.weather.populate.Populator;

public class WeatherActivity extends MapActivity implements WeatherProcessListener {

	private Button applyButton;
	private static ProgressDialog progressDialog;

	private final AtomicBoolean processing = new AtomicBoolean(false);
	private static int progress = 0;
	private static String message = "";
	private Bitmap imBitmap;
	private String pngPath;
	private Populator<Class3X01Y192> imageGeneralData;

	//	private ImageView imageView;
	private MapView mapView;

	private final Runnable processErrorAction = new ExceptionActionRunnable() {
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
			//imageView = (ImageView) findViewById(R.id.imageView);
			mapView = (MapView) findViewById(R.id.mapView);
			mapView.setBuiltInZoomControls(true);
			mapView.setStreetView(false);
			mapView.setTraffic(false);
			mapView.setSatellite(true);

			applyButton = (Button) findViewById(R.id.execute);
			applyButton.setEnabled(true);
			applyButton.setVisibility(View.VISIBLE);
			applyButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					applyFilter();
				}
			});

			WeatherActivity.progressDialog = new ProgressDialog(
					WeatherActivity.this.applyButton.getContext());
			WeatherActivity.progressDialog
			.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			WeatherActivity.progressDialog.setCancelable(false);
			WeatherActivity.progressDialog.setMessage("Comprobando datos.");

			processing.set(false);
			WeatherActivity.progress = 0;
		} catch (final Exception e) {
			// No compatible file manager.
			processException(e);
		}

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
			//imageView.setImageBitmap(imBitmap);

			Toast.makeText(WeatherActivity.this, "Photo processing finished.",
					Toast.LENGTH_LONG).show();

			resetLayout();

			/* TODO registrar android:apiKey para mapa de google (main.xml) */

			final MapController map = mapView.getController();
			map.setCenter(getGeoPoint(Class3X01Y192.RADAR_LATITUDE, Class3X01Y192.RADAR_LONGITUDE));
			map.setZoom(8);
			//map.setTypeId("terrain");


			List<Overlay> overlays = mapView.getOverlays();
			overlays.clear();

			GroundOverlay overlayImage = new GroundOverlay(mapView,
					getGeoPoint(Class3X01Y192.LATITUDE_NW, Class3X01Y192.LONGITUDE_NW),
					getGeoPoint(Class3X01Y192.LATITUDE_SE, Class3X01Y192.LONGITUDE_SE),
					imBitmap,
					"title","snippet");

			overlayImage.setMarker(overlayImage.drawable);

			//FIXME no se muestra la imagen

			//overlays.add(0, overlayRadar);
			mapView.invalidate();

			//TODO grafico de escala fijo. en imageView por ejemplo

		}

	};

	private GeoPoint getGeoPoint(final Class3X01Y192 latitude, final Class3X01Y192 longitude) {
		return new GeoPoint((int)(1e6 * imageGeneralData.getData(latitude).getData().doubleValue()),
				(int)(1e6 * imageGeneralData.getData(longitude).getData().doubleValue()));
	}

	private void resetLayout() {
		processing.set(false);
		lastProgressUpdate = -1;

		applyButton.setVisibility(View.VISIBLE);
		applyButton.setEnabled(true);

		WeatherActivity.progressDialog.hide();
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

	private void applyFilter() {
		// Toast.makeText(WeatherActivity.this, "applyFilter 1",
		// Toast.LENGTH_SHORT).show();
		if (!processing.getAndSet(true)) {
			// Toast.makeText(WeatherActivity.this, "applyFilter 2",
			// Toast.LENGTH_SHORT).show();
			try {
				// processing = true;
				progressThreadHandler.post(processBeginAction);
				final Thread processT = new MainProcess(this,
						progressThreadHandler);
				processT.start();

			} catch (final Exception e) {
				processException(e);
			} finally {
				// progressThreadHandler.post(processEndAction);
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
		Toast.makeText(WeatherActivity.this, error, Toast.LENGTH_SHORT).show();

		((ExceptionActionRunnable) processErrorAction)
		.setException((Exception) e);
		progressThreadHandler.post(processErrorAction);
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

			if (WeatherActivity.progress - lastProgressUpdate > 0) {
				lastProgressUpdate = WeatherActivity.progress;
				progressThreadHandler.post(WeatherActivity.progressThread);
			}
		}
		WeatherActivity.progress = 0;
	}

	@Override
	public void openImage(final Bitmap tempBitmap, final String pngPath,
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
		// TODO informacion de rutas
		return false;
	}

}
