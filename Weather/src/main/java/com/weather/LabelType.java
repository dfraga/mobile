package com.weather;

public enum LabelType {
	DESCRIPTOR((byte) (0x00 << 6),0), MULTIPLE((byte) (0x01 << 6),1), OPERARTOR_DESCRIPTOR_TABLE_C(
			(byte) (0x02 << 6),2), SEQUENCE_DESCRIPTOR_TABLE_D((byte) (0x03 << 6),3);

	final byte maskId;
	final int id;
	final static byte FILTER = (byte) (0x03 << 6);

	LabelType(final byte maskId, final int id) {
		this.maskId = maskId;
		this.id = id;
	}

	public byte getMaskId() {
		return maskId;
	}

	public int getId() {
		return id;
	}

	/*
	 * F = 0 descriptor
	 * F = 1 implica X = numero de descriptores que se repiten, Y = numero de veces que se repite el descriptor
	 * F = 2 operartor descriptor "Internal table C"
	 * F = 3 sequence descriptor "Buffr table D"
	 */
	static LabelType getLabelType(final byte b) {
		for (LabelType type : LabelType.values()) {
			if (type != DESCRIPTOR
					&& (b & LabelType.FILTER) == type.getMaskId()) {
				return type;
			}
		}
		return DESCRIPTOR;
	}

	static LabelType getLabelType(final int id) {
		for (LabelType type : LabelType.values()) {
			if (id == type.getId()) {
				return type;
			}
		}
		return DESCRIPTOR;
	}

}
