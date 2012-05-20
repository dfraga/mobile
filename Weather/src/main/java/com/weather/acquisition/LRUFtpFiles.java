package com.weather.acquisition;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUFtpFiles extends LinkedHashMap<String, MapOverlayImageInfo> {

	private static final long serialVersionUID = 7144522953221272444L;
	private static LRUFtpFiles INSTANCE;
	private int limit;

	public static synchronized LRUFtpFiles getInstance() {
		if(LRUFtpFiles.INSTANCE == null) {
			LRUFtpFiles.INSTANCE = new LRUFtpFiles(5);
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

}
