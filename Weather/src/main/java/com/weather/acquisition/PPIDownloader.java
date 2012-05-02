package com.weather.acquisition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;

import com.weather.Weather;

public class PPIDownloader {

	private static final Logger LOG = Logger.getLogger(PPIDownloader.class);
	private static final String server = "ftpdatos.aemet.es";
	private static final String folder = "radar";

	private FTPClient ftpclient = null;
	private File localFolder = null;

	public PPIDownloader(final File localFolder) {
		this.localFolder = localFolder;
	}

	public String getCurrentUTCFormattedDate() {
		//"" + new Timestamp(System.currentTimeMillis())
		final SimpleDateFormat dataFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dataFormatter.format(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime());
	}

	/**
	 * Get data from observacions AEMET.
	 */
	public void get(final String radarFilter) {
		final String startStr = "Start: {" + getCurrentUTCFormattedDate() + "}";
		System.out.println(startStr);
		//		PPIDownloader.LOG.debug(startStr);

		handleFiles(radarFilter);

		final String endStr = "End: {" + getCurrentUTCFormattedDate() + "}";
		System.out.println(endStr);
		//		PPIDownloader.LOG.debug(endStr);
	}

	private void handleFiles(final String radarFilter) {

		try {
			ftpclient = new FTPClient();

			// Connect to server
			ftpclient.connect(PPIDownloader.server);
			ftpclient.setFileTransferMode(FTP.BINARY_FILE_TYPE);

			// Loggin
			if (!ftpclient.login("anonymous", null)) {
				PPIDownloader.LOG.error("Can't log into FTP");
				return;
			}
			// Change directory
			if (!ftpclient.changeWorkingDirectory(PPIDownloader.folder)) {
				PPIDownloader.LOG.error("Can't change to folder '" + PPIDownloader.folder + "'.");
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
			File folderDay = new File(this.localFolder, dayFolderName);
			folderDay.deleteOnExit();
			if (!folderDay.exists()) {
				if (!folderDay.mkdir()) {
					PPIDownloader.LOG.error("Can''t create the daily folder '" + folderDay.getAbsolutePath() + "'");
					return;
				}
				folderDay.deleteOnExit();
			}

			// Change to day directory
			if (!ftpclient.changeWorkingDirectory(dayFolderName)) {
				PPIDownloader.LOG.error("Can't change to day folder '" + dayFolderName + "'.");
				return;
			}

			FTPFile[] files = ftpclient.listFiles();
			boolean mustBeRead = false;
			FTPFile ftpFile = PPIDownloader.lastFileModified(files, radarFilter);
			File localFile = null;
			if(ftpFile != null) {
				long size = ftpFile.getSize();
				localFile = new File(folderDay, ftpFile.getName());

				//TODO cambiar comprobacion, pues estos ficheros se borran tras la descarga
				if (!localFile.exists()) {
					PPIDownloader.LOG.debug("File '" + ftpFile.getName() + "' doesn't exist locally");
					mustBeRead = true;
				} else if (Math.abs(localFile.length() - size) > 1) {
					PPIDownloader.LOG.debug("File '" + ftpFile.getName() + "' size changed (before: " + localFile.length() + ", after: " + size + ")");
					mustBeRead = true;
				} else {
					PPIDownloader.LOG.debug("Ignored file '" + ftpFile.getName() + "'");
				}
			}
			// If we need to read the file then control if any error occurs.
			if (mustBeRead) {
				downloadFile(ftpFile, localFile);
			}
		} catch (SocketException ex) {
			PPIDownloader.LOG.error("",ex);
		} catch (Exception ex) {
			PPIDownloader.LOG.error("",ex);
		} finally {
			if (ftpclient != null) {
				try {
					ftpclient.disconnect();
				} catch (IOException ex) {
				}
			}
		}

	}

	public static FTPFile lastFileModified(final FTPFile[] files, final String containsFilter) {
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
	 * @throws IOException
	 */
	private void downloadFile(final FTPFile ftpfile, final File localFile) throws Exception {
		try {
			PPIDownloader.LOG.debug("Downloading file '" + ftpfile.getName() + "' at '" + getCurrentUTCFormattedDate() + "'");

			ftpclient.setFileType(FTP.BINARY_FILE_TYPE);
			InputStream is = ftpclient.retrieveFileStream(ftpfile.getName());

			PPIDownloader.LOG.debug("Downloaded finished at '" + getCurrentUTCFormattedDate() + "' , size:'" + ftpfile.getSize() + "'bytes , timestamp: '" + ftpfile.getTimestamp().getTime() + "'.");

			inflate(is, localFile);
		} catch (Exception ex) {
			PPIDownloader.LOG.debug("A problem occurs while downloading file '" + ftpfile.getName() ,ex);
			throw ex;
		}
	}

	private void inflate(final InputStream is, final File localFile) throws FileNotFoundException, IOException, ArchiveException{

		// Uncompress file
		File folderDay = localFile.getParentFile();
		List<File> tarFiles = UncompressUtils.uncompressTarFile(is, folderDay);
		if (tarFiles.size() > 0) {
			for(File untarFile:tarFiles) {
				PPIDownloader.LOG.debug("Untar file: " + untarFile.getName());
				// si .tar seguir descomprimindo, si bppi.gz obtener ficheiro

				if(untarFile.getName().toLowerCase().contains(".bppi")
						&& untarFile.getName().toLowerCase().contains(".gz")) {
					String targetName = untarFile.getName().replaceAll("(?i).gz", "");
					File unzipFile = new File(folderDay, targetName);
					if (UncompressUtils.uncompressGzFile(untarFile, unzipFile)) {
						PPIDownloader.LOG.debug("Unzip file: " + targetName);
						// procesar imagen radar.
						Weather.process(unzipFile);
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
