package com.android.weather;

import java.io.File;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.weather.Weather;
import com.weather.acquisition.PPIDownloader;

public class MainProcess extends Thread {

	private final WeatherProcessListener listener;
	private final Handler handler;
	private final RadarCenter radar;

	@SuppressWarnings("unused")
	private MainProcess(){
		this.listener = null;
		this.handler = null;
		this.radar = null;
	}
	public MainProcess(final RadarCenter radar, final WeatherProcessListener listener, final Handler handler) {
		this.listener = listener;
		this.handler = handler;
		this.radar = radar;
	}

	@Override
	public void run() {
		try {
			Looper.prepare();
			handler.sendEmptyMessage(1);

			Toast.makeText(listener.getContext(), "RUNNING", Toast.LENGTH_LONG).show();
			File ruta_sd = Environment.getExternalStorageDirectory();
			File localFolder = new File(ruta_sd.getAbsolutePath(), Weather.WORKING_DIRECTORY);
			localFolder.mkdir();

			File localFolderObservations = new File(localFolder, "weatherTempDownloads");
			localFolderObservations.deleteOnExit();
			localFolderObservations.mkdir();


			PPIDownloader adq = new PPIDownloader(localFolderObservations, this.listener);

			Toast.makeText(listener.getContext(), "GETTING", Toast.LENGTH_LONG).show();

			adq.get(this.radar.getFilter(), this.listener);

		} catch (Throwable ex) {
			Log.e(MainProcess.class.getSimpleName(),"", ex);
			listener.processException(ex);
		}
	}
}
