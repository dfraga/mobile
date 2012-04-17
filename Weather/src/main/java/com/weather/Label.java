package com.weather;

public class Label {

	final LabelType type;
	final int x;
	final int y;

	final String labelPropKey;
	final int size;

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

		//XXX si label de tipo SEQUENCE_DESCRIPTOR_TABLE_D, se compone de varias subetiquetas
		// ejemplo en properties 3.1.1=0.1.1,0.1.2   (separadas por comas. si una etiqueta es de la forma 'f.x.y':'N' indica que se repite N veces consecutivas.
		// dentro de un mismo sequence pueden repetirse etiquetas

		//XXX MULTIPLE indica replica de datos x veces de la siguiente etiqueta a leer. Confirmar esto ultimo
		if(type != LabelType.SEQUENCE_DESCRIPTOR_TABLE_D
				&& type != LabelType.MULTIPLE) {

			boolean exist = Weather.props.containsKey(labelPropKey);
			if(!exist) {
				System.out.println("@@@ NO EXISTE PROPERTY " + labelPropKey);
			}
			size = exist ? Integer.parseInt(Weather.props.getProperty(labelPropKey)):0;
		} else {
			size = 0;
		}
	}

	@Override
	public String toString() {
		return type.name() + " , " + labelPropKey;
	}

}
