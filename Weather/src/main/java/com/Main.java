package com;

import java.io.File;

import org.apache.log4j.Logger;

import com.weather.acquisition.PPIDownloader;

public class Main {

	public static void main(final String[] args) {
		try {

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


			PPIDownloader adq = new PPIDownloader(localFolderObservations);
			adq.get(containsFilter);

		} catch (Exception ex) {
			Logger.getLogger(Main.class).error("", ex);
		}
	}
}
