package com.android.weather;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.android.utils.expandableList.ExpandableRadarSelectionAdapter;
import com.android.utils.expandableList.ExpandableRadarSelectionListener;
import com.android.utils.map.DoubleTapMapActivity;
import com.android.utils.map.DoubleTapMapView;
import com.android.utils.menu.OptionsMenu;
import com.android.utils.menu.OptionsMenuListener;
import com.google.android.maps.GeoPoint;
import com.weather.acquisition.LRUFtpFiles;
import com.weather.acquisition.MapOverlayImageInfo;
import com.weather.populate.Class3X01Y192;
import com.weather.populate.Populator;

public class WeatherActivity extends DoubleTapMapActivity implements WeatherProcessListener, OptionsMenuListener {

	private OptionsMenu optionsMenu;
	private final boolean blockingProcessDialog = false;
	private boolean showScale = true;
	private boolean satelliteView = false;

	{
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(final Thread thread, final Throwable ex) {
				processException(ex);
			}
		});
	}

	private Button applyButton;
	private ImageView gradImage;
	private ProgressBar executing;
	private ProgressDialog progressDialog;

	private final AtomicBoolean processing = new AtomicBoolean(false);
	private int progress = 0;
	private String message = "";
	private Bitmap imBitmap;
	private String downloadTarFileName;
	private long downloadTarFileSize;
	private Populator<Class3X01Y192> imageGeneralData;

	private ExpandableRadarSelectionAdapter mapAdapter;
	private RadarCenter selectedRadar = RadarCenter.MADRID;

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
		super.initMapView((DoubleTapMapView) findViewById(R.id.mapView));

		try {

			gradImage = (ImageView) findViewById(R.id.gradImage);
			gradImage.setVisibility(showScale ? View.VISIBLE:View.GONE);
			gradImage.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					gradImage.setVisibility(View.GONE);
				}
			});

			applyButton = (Button) findViewById(R.id.execute);
			applyButton.setEnabled(true);
			applyButton.setVisibility(View.VISIBLE);
			//applyButton.setText(R.string.execute);
			applyButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					applyFilter();
				}
			});

			executing = (ProgressBar) findViewById(R.id.executing);
			hideExecuting();

			final ExpandableListView radarComboList = (ExpandableListView)findViewById(R.id.radarComboList);
			radarComboList.setItemsCanFocus(false);
			radarComboList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

			final ExpandableRadarSelectionListener listener = new ExpandableRadarSelectionListener() {
				@Override
				public void itemSelected(final RadarCenter selectedRadar) {
					setSelectedRadar(selectedRadar);
					setMapRadarCenter(false, false);
				}
			};
			mapAdapter = new ExpandableRadarSelectionAdapter(radarComboList, listener);
			radarComboList.setOnGroupExpandListener(new OnGroupExpandListener() {

				@Override
				public void onGroupExpand(final int groupPosition) {
					applyButton.setVisibility(View.GONE);
				}
			});
			radarComboList.setOnGroupCollapseListener(new OnGroupCollapseListener() {

				@Override
				public void onGroupCollapse(final int groupPosition) {
					if(executing == null || !executing.isEnabled()) {
						applyButton.setVisibility(View.VISIBLE);
					}
				}
			});

			progressDialog = new ProgressDialog(WeatherActivity.this.applyButton.getContext());
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setCancelable(true);
			progressDialog.setMessage("Comprobando datos.");

			processing.set(false);
			progress = 0;

			getMapView().setSatellite(satelliteView);
			setMapRadarCenter(true, true);

			//Carga de lru
			LRUFtpFiles.getInstance();

			//Primer proceso
			applyFilter();
		} catch (final Exception e) {
			// No compatible file manager.
			processException(e);
		}

	}

	private void hideExecuting() {
		if(executing != null) {
			executing.setEnabled(false);
			executing.setVisibility(View.GONE);
		}
	}

	private void showExecuting() {
		if(executing != null) {
			executing.setEnabled(true);
			executing.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public RadarCenter getSelectedRadar() {
		return selectedRadar;
	}

	private void setMapRadarCenter(final boolean fine, final boolean user) {
		final GeoPoint center = setMapCenter(fine, user);
		if(user) {
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
		}
		mapAdapter.setSelected(this.selectedRadar, false);
		getMapView().invalidate();
	}

	private final Runnable processBeginAction = new Runnable() {

		@Override
		public void run() {
			applyButton.setEnabled(false);
			applyButton.setVisibility(View.GONE);
			showExecuting();
			progress = 0;

			progressDialog.setMax(100);
			if(blockingProcessDialog) {
				progressDialog.show();
			}
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
				drawOverlay(imBitmap,
						getGeoPoint(Class3X01Y192.RADAR_LATITUDE, Class3X01Y192.RADAR_LONGITUDE),
						getGeoPoint(Class3X01Y192.LATITUDE_NW, Class3X01Y192.LONGITUDE_NW),
						getGeoPoint(Class3X01Y192.LATITUDE_SE, Class3X01Y192.LONGITUDE_SE));
			}

		}

	};

	private GeoPoint getGeoPoint(final Class3X01Y192 latitude, final Class3X01Y192 longitude) {
		return new GeoPoint((int)(1e6 * imageGeneralData.getData(latitude).getData().doubleValue()),
				(int)(1e6 * imageGeneralData.getData(longitude).getData().doubleValue()));
	}

	private void resetLayout() {
		processing.set(false);
		lastProgressUpdate = -1;

		if(applyButton != null) {
			applyButton.setVisibility(View.VISIBLE);
			applyButton.setEnabled(true);
			gradImage.setVisibility(showScale ? View.VISIBLE:View.GONE);
		}

		hideExecuting();

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

	private void setSelectedRadar(final RadarCenter selectedRadar) {
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

	private static String getStackTrace(final String prefix,
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
		lastProgressUpdate = 0;
		progress = 0;
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
		//LRUFtpFiles.getInstance().serialize();

		progressThreadHandler.post(processEndAction);

	}

	@Override
	public Context getContext() {
		return WeatherActivity.this;
	}


	/* Create Menu Items */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		optionsMenu = new OptionsMenu(this, menu, getContext());
		return true;
	}

	/* Handles Item Selection */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		return optionsMenu.onOptionsItemSelected(item);
	}

	@Override
	public int getShowScaleMenuId() {
		return showScale ? R.string.hideScale : R.string.showScale;
	}
	@Override
	public int getSatelliteViewMenuId() {
		return satelliteView ? R.string.hideSatelliteView : R.string.showSatelliteView;
	}
	@Override
	public int getSatelliteViewMenuIconId() {
		return satelliteView ? R.drawable.mapa : R.drawable.landscape;
	}

	@Override
	public boolean showProgressMenuSelected(final MenuItem item) {
		if(this.processing != null && this.processing.get()) {
			progressDialog.show();
		}
		return true;
	}

	@Override
	public boolean showScaleMenuSelected(final MenuItem item) {
		showScale = !showScale;
		gradImage.setVisibility(showScale ? View.VISIBLE:View.GONE);
		item.setTitle(getShowScaleMenuId());
		return true;
	}

	@Override
	public boolean satelliteViewMenuSelected(final MenuItem item) {
		satelliteView = !satelliteView;
		getMapView().setSatellite(satelliteView);
		item.setTitle(getSatelliteViewMenuId());
		item.setIcon(getSatelliteViewMenuIconId());
		return true;
	}

	@Override
	public boolean centerMenuSelected(final MenuItem item) {
		setMapRadarCenter(true,true);
		return true;
	}

	@Override
	public boolean quitMenuSelected(final MenuItem item) {
		finish();
		return true;
	}

}
