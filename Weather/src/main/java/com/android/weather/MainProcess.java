package com.android.weather;

import java.io.File;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.weather.acquisition.PPIDownloader;

public class MainProcess extends Thread {

	private final WeatherProcessListener listener;
	private final Handler handler;

	@SuppressWarnings("unused")
	private MainProcess(){
		this.listener = null;
		this.handler = null;
	}
	public MainProcess(final WeatherProcessListener listener, final Handler handler) {
		this.listener = listener;
		this.handler = handler;
	}

	@Override
	public void run() {
		try {
			Looper.prepare();
			handler.sendEmptyMessage(1);

			Toast.makeText(listener.getContext(), "RUNNING", Toast.LENGTH_LONG).show();
			File localFolder = new File("Salida");
			localFolder.mkdir();

			File localFolderObservations = new File(localFolder, "weatherTempDownloads");
			localFolderObservations.deleteOnExit();
			localFolderObservations.mkdir();

			/* TODO combo de centros:
					_almeria.tar
					_barcelona.tar
					_caceres.tar
					_corunha.tar
					_donosti.tar
					_gran_canaria.tar
					_madrid.tar
					_malaga.tar
					_mallorca.tar
					_murcia.tar
					_palencia.tar
					_santander.tar
					_sevilla.tar
					_valencia.tar
					_zaragoza.tar
			 */
			final String containsFilter = "corunha.tar";


			PPIDownloader adq = new PPIDownloader(localFolderObservations, this.listener);

			Toast.makeText(listener.getContext(), "GETTING", Toast.LENGTH_LONG).show();

			adq.get(containsFilter, this.listener);

		} catch (Exception ex) {
			listener.processException(ex);
			Log.e(MainProcess.class.getSimpleName(),"", ex);
		}
	}
}
