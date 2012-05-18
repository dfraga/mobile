package com.weather.acquisition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import android.util.Log;
import android.widget.Toast;

import com.android.weather.WeatherProcessListener;
import com.weather.Weather;

public class PPIDownloader {

	private static final String server = "ftpdatos.aemet.es";
	private static final String folder = "radar";
	private final WeatherProcessListener listener;

	private FTPClient ftpclient = null;
	private File localFolder = null;

	public PPIDownloader(final File localFolder, final WeatherProcessListener listener) {
		this.listener = listener;
		this.localFolder = localFolder;
	}

	public String getCurrentUTCFormattedDate() {
		//"" + new Timestamp(System.currentTimeMillis())
		final SimpleDateFormat dataFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dataFormatter.format(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime());
	}

	/**
	 * Get data from observacions AEMET.
	 * @throws Exception
	 */
	public void get(final String radarFilter, final WeatherProcessListener listener) throws Exception {
		final String startStr = "Start: {" + getCurrentUTCFormattedDate() + "}";
		System.out.println(startStr);
		//		Log.d(PPIDownloader.class.getSimpleName(),startStr);

		handleFiles(radarFilter);

		final String endStr = "End: {" + getCurrentUTCFormattedDate() + "}";
		System.out.println(endStr);
		//		Log.d(PPIDownloader.class.getSimpleName(),endStr);
		listener.processGetFtpEnded();
	}

	private void handleFiles(final String radarFilter) throws Exception {
		File folderDay = null;
		try {
			listener.setPercentageMessage("Recopilando datos...");
			ftpclient = new FTPClient();

			listener.setPercentageProgression(1, 5);
			// Connect to server
			ftpclient.connect(PPIDownloader.server);
			ftpclient.setFileTransferMode(FTP.BINARY_FILE_TYPE);
			ftpclient.enterLocalPassiveMode();
			//ftpclient.setConnectTimeout(30000);

			// Loggin
			if (!ftpclient.login("anonymous", null)) {
				Toast.makeText(listener.getContext(), "Can't log into FTP", Toast.LENGTH_LONG).show();
				Log.e(PPIDownloader.class.getSimpleName(),"Can't log into FTP");
				return;
			}

			listener.setPercentageProgression(2, 5);
			// Change directory
			if (!ftpclient.changeWorkingDirectory(PPIDownloader.folder)) {
				Toast.makeText(listener.getContext(), "Can't change to folder '" + PPIDownloader.folder + "'.", Toast.LENGTH_LONG).show();
				Log.e(PPIDownloader.class.getSimpleName(),"Can't change to folder '" + PPIDownloader.folder + "'.");
				return;
			}

			String dayFolderName = "";
			for(FTPFile ftpDir : ftpclient.listDirectories()) {
				String dirName = ftpDir.getName();
				if(dirName.matches("\\d{8}")
						&& dirName.compareTo(dayFolderName) > 0) {
					dayFolderName = dirName;
				}
			}

			// Create local directory for the day.
			folderDay = new File(this.localFolder, dayFolderName);
			folderDay.deleteOnExit();
			if (!folderDay.exists()) {
				if (!folderDay.mkdir()) {
					Toast.makeText(listener.getContext(), "Can't create the daily folder '" + folderDay.getAbsolutePath() + "'", Toast.LENGTH_LONG).show();
					Log.e(PPIDownloader.class.getSimpleName(),"Can't create the daily folder '" + folderDay.getAbsolutePath() + "'");
					return;
				}
				folderDay.deleteOnExit();
			}

			// Change to day directory
			if (!ftpclient.changeWorkingDirectory(dayFolderName)) {
				Toast.makeText(listener.getContext(), "Can't cchange to day folder '" + dayFolderName + "'", Toast.LENGTH_LONG).show();
				Log.e(PPIDownloader.class.getSimpleName(),"Can't change to day folder '" + dayFolderName + "'.");
				return;
			}

			FTPFile[] files = ftpclient.listFiles();
			listener.setPercentageProgression(3, 5);
			boolean mustBeRead = false;
			FTPFile ftpFile = PPIDownloader.lastFileModified(files, radarFilter);
			if(ftpFile != null) {
				String ftpFileName = ftpFile.getName();
				long ftpFileSize = ftpFile.getSize();

				LRUFtpFiles lru = LRUFtpFiles.getInstance();
				if (!lru.containsKey(ftpFileName)) {
					Log.d(PPIDownloader.class.getSimpleName(),"File '" + ftpFile.getName() + "' doesn't exist locally");
					mustBeRead = true;
				} else {
					long lruTarSize = lru.get(ftpFileName).getDownloadTarFileSize();
					if (Math.abs(lruTarSize - ftpFileSize) > 1) {
						Log.d(PPIDownloader.class.getSimpleName(),"File '" + ftpFile.getName() + "' size changed (before: " + lruTarSize + ", after: " + ftpFileSize + ")");
						mustBeRead = true;
					} else {
						Log.d(PPIDownloader.class.getSimpleName(),"Ignored file '" + ftpFile.getName() + "'");
					}
				}

				listener.setPercentageProgression(3, 5);
				// If we need to read the file then control if any error occurs.
				if (mustBeRead) {
					listener.setDownloadTarFile(ftpFileName, ftpFileSize);
					File localFile = new File(folderDay, ftpFileName);
					localFile.deleteOnExit();
					downloadFile(ftpFile, localFile);
					listener.setPercentageProgression(4, 5);
				} else {
					listener.setDownloadTarFile(ftpFileName, lru.get(ftpFileName).getDownloadTarFileSize());
					listener.setProcessedImage(lru.get(ftpFileName).getImBitmap(), lru.get(ftpFileName).getImageGeneralData());
				}
			}

		} finally {
			if (folderDay != null && folderDay.exists()) {
				folderDay.delete();
			}
			if (ftpclient != null) {
				ftpclient.disconnect();
			}
		}

	}

	public static FTPFile lastFileModified(final FTPFile[] files, final String containsFilter) {
		//FIXME a veces modifican archivos antiguos para comprimir las lecturas de una hora en un solo fichero (dos dias anteriores por ejemplo). Esto hace que el ultimo modificado no siempre sea el ultimo barrido del radar.
		Date lastMod = files[0].getTimestamp().getTime();
		FTPFile choice = null;
		for (FTPFile file : files) {
			if (!file.getName().toLowerCase().contains(containsFilter.toLowerCase())) {
				continue;
			}
			if (file.getTimestamp().getTime().after(lastMod)) {
				choice = file;
				lastMod = file.getTimestamp().getTime();
			}
		}
		return choice;
	}

	/**
	 * Stores locally the specified FTP file if it has changes or doesn't exists.
	 * @param ftpfile
	 * @param localfile
	 * @throws Exception
	 */
	private void downloadFile(final FTPFile ftpfile, final File localFile) throws Exception {
		try {
			Log.d(PPIDownloader.class.getSimpleName(),"Downloading file '" + ftpfile.getName() + "' at '" + getCurrentUTCFormattedDate() + "'");

			ftpclient.setFileType(FTP.BINARY_FILE_TYPE);
			InputStream is = ftpclient.retrieveFileStream(ftpfile.getName());

			Log.d(PPIDownloader.class.getSimpleName(),"Downloaded finished at '" + getCurrentUTCFormattedDate() + "' , size:'" + ftpfile.getSize() + "'bytes , timestamp: '" + ftpfile.getTimestamp().getTime() + "'.");

			inflate(is, localFile);
		} catch (Throwable ex) {
			Log.d(PPIDownloader.class.getSimpleName(),"A problem occurs while downloading file '" + ftpfile.getName() ,ex);
			throw (Exception)ex;
		}
	}

	private void inflate(final InputStream is, final File localFile) throws FileNotFoundException, IOException, ArchiveException{

		// Uncompress file
		File folderDay = localFile.getParentFile();
		List<File> tarFiles = UncompressUtils.uncompressTarFile(is, folderDay);
		if (tarFiles.size() > 0) {
			for(File untarFile:tarFiles) {
				Log.d(PPIDownloader.class.getSimpleName(),"Untar file: " + untarFile.getName());
				// si .tar seguir descomprimindo, si bppi.gz obtener ficheiro

				if(untarFile.getName().toLowerCase().contains(".bppi")
						&& untarFile.getName().toLowerCase().contains(".gz")) {
					String targetName = untarFile.getName().replaceAll("(?i).gz", "");
					File unzipFile = new File(folderDay, targetName);
					unzipFile.deleteOnExit();
					if (UncompressUtils.uncompressGzFile(untarFile, unzipFile)) {
						Log.d(PPIDownloader.class.getSimpleName(),"Unzip file: " + targetName);
						// procesar imagen radar.
						try {
							new Weather(listener).process(unzipFile);
						} finally {
							unzipFile.delete();
						}
					} else {
						// If there is any error uncompressing file then remove files to
						// ensure it will be downloaded again.
						localFile.delete();
					}
				} else if(untarFile.getName().toLowerCase().contains(".tar")) {
					inflate(new FileInputStream(untarFile), localFile);
				}
				untarFile.delete();
			}
		} else {
			// If there is any error uncompressing file then remove files to
			// ensure it will be downloaded again.
			localFile.delete();
			//ftpfile.delete();
		}
	}
}
