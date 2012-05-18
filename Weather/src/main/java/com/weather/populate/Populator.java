package com.weather.populate;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class Populator<C extends Enum<C> & EnumIdInterface> implements Serializable {

	private static final long serialVersionUID = -1454022183235134058L;
	private final Map<C, LabelData> populatorData = new LinkedHashMap<C, LabelData>();
	private C lastOffer = null;

	public void offer(final C key, final LabelData value) {
		lastOffer = key;
		populatorData.put(key,value);
	}

	public C getLastOffer() {
		return lastOffer;
	}

	public void setLastOffer(final C lastOffer) {
		this.lastOffer = lastOffer;
	}

	public LabelData getData(final C key) {
		return this.populatorData.get(key);
	}

	@Override
	public String toString() {
		lastOffer.getClass();
		StringBuffer sb = new StringBuffer("[\n");
		for(C key :populatorData.keySet()) {
			sb.append(key.name()).append(" : ").append(populatorData.get(key)).append("\n");
		}
		sb.append("]");
		return sb.toString();
	}

}
