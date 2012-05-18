package com.weather.populate;

import java.io.Serializable;

import com.weather.Label;

public class LabelData implements Serializable {

	private static final long serialVersionUID = -8031635002548543310L;
	private final Label label;
	private final Number data;

	public LabelData(final Label label, final Number data) {
		this.label = label;
		this.data = data;
	}

	public Label getLabel() {
		return label;
	}

	public Number getData() {
		return data;
	}

	@Override
	public String toString() {
		return label + " = " + data;
	}

}
