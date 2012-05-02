package com.weather.acquisition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.log4j.Logger;

public class UncompressUtils {

	private static final Logger LOG = Logger.getLogger(UncompressUtils.class);

	public static boolean uncompressGzFile(final File localfile, final File targetlocalfile) {
		GZIPInputStream in = null;
		OutputStream out = null;
		try {
			in = new GZIPInputStream(new FileInputStream(localfile));
			out = new FileOutputStream(targetlocalfile);
			// Transfer bytes from the compressed file to the output file
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			return true;
		} catch (IOException ex) {
			UncompressUtils.LOG.error("There was a problem while uncompressing file", ex);
			return false;
		} finally {
			// Close the file and stream
			if (in != null) {
				try {
					in.close();
				} catch (IOException ex) {
				}
			}
			if (out != null) {
				try {
					out.close();
					targetlocalfile.deleteOnExit();
				} catch (IOException ex) {
				}
			}
		}
	}

	public static List<File> uncompressTarFile(final InputStream inputStream, final File outputDir) throws FileNotFoundException, IOException, ArchiveException {

		final List<File> untaredFiles = new LinkedList<File>();
		final TarArchiveInputStream debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.TAR, inputStream);
		TarArchiveEntry entry = debInputStream.getNextTarEntry();
		while (entry != null) {
			final File outputFile = new File(outputDir, entry.getName());
			if (entry.isDirectory()) {
				UncompressUtils.LOG.info(String.format("Attempting to write output directory %s.", outputFile.getAbsolutePath()));
				if (!outputFile.exists()) {
					UncompressUtils.LOG.info(String.format("Attempting to create output directory %s.", outputFile.getAbsolutePath()));
					if (!outputFile.mkdirs()) {
						throw new IllegalStateException(String.format("Couldn't create directory %s.", outputFile.getAbsolutePath()));
					}
				}
			} else if(entry.isFile()) {
				UncompressUtils.LOG.info(String.format("Creating output file %s.", outputFile.getAbsolutePath()));
				final OutputStream outputFileStream = new FileOutputStream(outputFile);
				IOUtils.copy(debInputStream, outputFileStream);
				outputFileStream.close();
			}
			untaredFiles.add(outputFile);
			entry = debInputStream.getNextTarEntry();
		}
		debInputStream.close();

		return untaredFiles;
	}


}
