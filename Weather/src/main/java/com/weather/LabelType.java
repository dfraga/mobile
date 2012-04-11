package com.weather;

public enum LabelType {
	DESCRIPTOR((byte)(0x00<<6)),
	MULTIPLE((byte)(0x01<<6)),
	OPERARTOR_DESCRIPTOR_TABLE_C((byte)(0x02<<6)),
	SEQUENCE_DESCRIPTOR_TABLE_D((byte)(0x03<<6));

	final byte maskId;
	LabelType(byte maskId) {
		this.maskId = maskId;
	}

	public byte getMaskId() {
		return maskId;
	}


	/*
	    F = 0 descriptor
    	F = 1 implica X = numero de descriptores que se repiten, Y = numero de veces que se repite el descriptor
    	F = 2 operartor descriptor "Internal table C"
    	F = 3 sequence descriptor "Buffr table D"
	 */
	static LabelType getLabelType(byte b) {
		for(LabelType type:LabelType.values()) {
			if(type != DESCRIPTOR && (b & type.getMaskId()) == type.getMaskId()) {
				return type;
			}
		}
		return DESCRIPTOR;
	}
}
