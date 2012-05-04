package com.weather.populate;

import com.weather.Label;

public class LabelData {

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
