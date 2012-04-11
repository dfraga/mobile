package com.weather;

public enum SectionType {
	INDICATOR_SECTION(0),
	IDENTIFICATION_SECTION(1),
	OPTIONAL_SECTION(2),
	LABELS_SECTION(3),
	DATA_SECTION(4),
	END_SECTION(5);

	final int id;
	SectionType(final int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

}
