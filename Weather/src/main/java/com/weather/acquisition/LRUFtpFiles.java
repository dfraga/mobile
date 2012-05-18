package com.weather.acquisition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import android.os.Environment;

import com.weather.Weather;

public class LRUFtpFiles extends LinkedHashMap<String, MapOverlayImageInfo> {

	private static final long serialVersionUID = 7144522953221272444L;
	private static final String DATA_FILE = "lru.dat";
	private static LRUFtpFiles INSTANCE;
	private int limit;

	public static synchronized LRUFtpFiles getInstance() {
		if(LRUFtpFiles.INSTANCE == null) {
			try {
				File ruta_sd = Environment.getExternalStorageDirectory();
				File localFolder = new File(ruta_sd.getAbsolutePath(), Weather.WORKING_DIRECTORY);
				FileInputStream fin = new FileInputStream(new File(localFolder, LRUFtpFiles.DATA_FILE));
				ObjectInputStream ois = new ObjectInputStream(fin);
				LRUFtpFiles.INSTANCE = (LRUFtpFiles) ois.readObject();
				ois.close();
			}
			catch (Exception e) {
				//Nada
			} finally {
				if(LRUFtpFiles.INSTANCE == null) {
					LRUFtpFiles.INSTANCE = new LRUFtpFiles(5);
				}
			}
		}
		return LRUFtpFiles.INSTANCE;
	}

	private LRUFtpFiles(final int maxLimit) {
		this.limit = maxLimit;
	}

	@Override
	protected boolean removeEldestEntry(final Map.Entry<String, MapOverlayImageInfo> eldest) {
		return size() > this.limit;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(final int limit) {
		this.limit = limit;
	}

	public void serialize() {
		try {
			File ruta_sd = Environment.getExternalStorageDirectory();
			File localFolder = new File(ruta_sd.getAbsolutePath(), Weather.WORKING_DIRECTORY);
			FileOutputStream fout = new FileOutputStream(new File(localFolder, LRUFtpFiles.DATA_FILE));
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(this);
			oos.close();
		}
		catch (Exception e) {
			//FIXME bitmap no serializable
			//Log.e(LRUFtpFiles.class.getSimpleName(), "error", e);
		}
	}


}
