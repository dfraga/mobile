package com.weather;

import android.util.Log;


public class Label {

	final LabelType type;
	final int x;
	final int y;

	final String labelPropKey;
	final int size;
	final int scale;
	final int referenceValue;

	Label(final byte[] label) {
		this( LabelType.getLabelType(label[0]).getId(),

				/*x*/ (((byte) 0x00 & 0xFF) << 24) + (((byte) 0x00 & 0xFF) << 16)
				+ (((byte) 0x00 & 0xFF) << 8) + ((label[0] & 0x3F) << 0),

				/*y*/ (((byte) 0x00 & 0xFF) << 24) + (((byte) 0x00 & 0xFF) << 16)
				+ (((byte) 0x00 & 0xFF) << 8) + ((label[1] & 0xFF) << 0));

	}

	Label(final int typeId, final int x, final int y) {
		this.type = LabelType.getLabelType(typeId);;
		this.x = x;
		this.y = y;

		labelPropKey = type.getId()+ "." + x + "." + y;

		// si label de tipo SEQUENCE_DESCRIPTOR_TABLE_D, se compone de varias subetiquetas
		// ejemplo en properties 3.1.1=0.1.1,0.1.2   (separadas por comas. si una etiqueta es de la forma 'f.x.y':'N' indica que se repite N veces consecutivas.
		// dentro de un mismo sequence pueden repetirse etiquetas

		if(type != LabelType.SEQUENCE_DESCRIPTOR_TABLE_D
				&& type != LabelType.MULTIPLE) {

			if( Weather.props.containsKey(labelPropKey)) {
				String labelProps = Weather.props.getProperty(labelPropKey);
				//Separado por ; tamaño;escala
				size = Integer.parseInt(labelProps.split(";")[0]);
				scale = labelProps.contains(";") ? Integer.valueOf(labelProps.split(";")[1]): 0;
				referenceValue = labelProps.contains(";") ? Integer.valueOf(labelProps.split(";")[2]): 0;
			} else {
				Log.d(Label.class.getSimpleName(),"@@@ NO EXISTE PROPERTY " + labelPropKey);
				size = 0;
				scale = 0;
				referenceValue = 0;
			}
		} else {
			size = 0;
			scale = 0;
			referenceValue = 0;
		}
	}

	@Override
	public String toString() {
		return "[" + type.name() + " , " + labelPropKey + "]";
	}

	/**
	 * Nos interesa que el equals compare exactamente el objeto, sin sobreescribirlo por claves
	 */
	@Override
	public boolean equals(final Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}


}
