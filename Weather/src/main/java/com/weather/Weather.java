package com.weather;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Weather {

	private static final int FINAL_STEP = (((byte) 0x00 & 0xFF) << 24)
			+ (((byte) 0x37 & 0xFF) << 16) + (((byte) 0x37 & 0xFF) << 8)
			+ (((byte) 0x37 & 0xFF) << 0);

	private static int BUFR_SECTION = 0;
	public static final Properties props = new Properties();

	public static void main(final String[] args) {


		try {
			// Cargamos properties

			final InputStream porpertiesIs = Weather.class.getClassLoader()
					.getResourceAsStream("sizes2.properties");
			Weather.props.load(porpertiesIs);

			// Cargamos fichero de datos
			final InputStream is = Weather.class.getClassLoader()
					//					.getResourceAsStream("COR110915203801.BPPI001");
					.getResourceAsStream("COR120420161801.BPPI001");

			boolean section = false;

			int step = 8;
			byte[] data = new byte[step];
			List<Label> labels = new ArrayList<Label>();
			boolean optionalPresence = false;
			while (is.read(data, 0, step) != -1) {
				if (section) {
					section = false;

					step = Weather.int3(data);

					step = step == Weather.FINAL_STEP ? 1 : step - 3;
					Weather.BUFR_SECTION++;

				} else {
					if (Weather.BUFR_SECTION == SectionType.OPTIONAL_SECTION.getId()
							&& !optionalPresence) {
						Weather.BUFR_SECTION++;
					}
					section = true;
					step = 3;
				}

				final StringBuffer sb = new StringBuffer();

				if (section) {
					if (Weather.BUFR_SECTION == SectionType.INDICATOR_SECTION
							.getId()) {
						ByteBuffer bb = ByteBuffer.wrap(data);
						byte[] dst = new byte[4];
						bb.get(dst, 0, 4);
						sb.append("\n\t· ").append(Weather.getChars(dst));

						dst = new byte[3];
						bb.get(dst, 0, 3);
						int totalLength = Weather.int3(dst);
						byte bufrVersion = bb.get();
						sb.append("\n\t· totalLength ").append(totalLength)
						.append("\n\t· bufrVersion ")
						.append(bufrVersion);

					} else if (Weather.BUFR_SECTION == SectionType.IDENTIFICATION_SECTION
							.getId()) {
						ByteBuffer bb = ByteBuffer.wrap(data);
						// Bufr master table (zero if standard WMO FM 94-IX BUFR
						// tables are used)
						byte masterTableNumber = bb.get();
						// Centro y subcentro a (Byte.MAX_VALUE -
						// Byte.MIN_VALUE) = 255 para standard tables
						short centre = bb.getShort();
						short subCentre = bb.getShort();
						byte updateSequence = bb.get();
						byte optionalSection = bb.get();
						byte dataCategoryTableA = bb.get();
						byte internationalDataSubCategory = bb.get();
						byte localSubCategory = bb.get();
						byte masterTableVersion = bb.get();
						byte localTableVersion = bb.get();
						short year = bb.getShort();
						byte month = bb.get();
						byte day = bb.get();
						byte hour = bb.get();
						byte minute = bb.get();
						byte second = bb.get();

						if (optionalSection != (byte) 0) {
							optionalPresence = true;
						}
						sb
						.append("\n\t· masterTableNumber ").append(masterTableNumber)
						.append("\n\t· centre ").append(centre)
						.append("\n\t· subCentre ").append(subCentre)
						.append("\n\t· updateSequence ").append(updateSequence)
						.append("\n\t· optionalSection ").append(optionalSection)
						.append("\n\t· dataCategoryTableA ").append(dataCategoryTableA)
						.append("\n\t· internationalDataSubCategory ").append(internationalDataSubCategory)
						.append("\n\t· localSubCategory ").append(localSubCategory)
						.append("\n\t· masterTableVersion ").append(masterTableVersion)
						.append("\n\t· localTableVersion ").append(localTableVersion)
						.append("\n\t· year ").append(year)
						.append("\n\t· month ").append(month)
						.append("\n\t· day ").append(day)
						.append("\n\t· hour ").append(hour)
						.append("\n\t· minute ").append(minute)
						.append("\n\t· second ").append(second);

					} else if (Weather.BUFR_SECTION == SectionType.LABELS_SECTION
							.getId()) {
						ByteBuffer bb = ByteBuffer.wrap(data);
						// reserved 0
						bb.get();
						short dataSubsets = bb.getShort();
						byte dataType = bb.get();
						byte observedDataMask = (byte) 0x80;
						byte compressedDataMask = (byte) 0x40;
						boolean observed = (dataType & observedDataMask) == observedDataMask;
						boolean compressed = (dataType & compressedDataMask) == compressedDataMask;
						sb.append("\n\t· dataSubsets ").append(dataSubsets)
						.append("\n\t· observed ").append(observed)
						.append("\n\t· compressed ").append(compressed).append("\n");

						while (bb.position() < bb.limit() - 1) {
							Weather.getLabel(bb.get(), bb.get(), labels, 1);
						}
						for (Label label : labels) {
							sb.append("\t").append(label).append("\n");
						}
					} else if (Weather.BUFR_SECTION == SectionType.OPTIONAL_SECTION
							.getId()) {
						sb.append("\n\t").append(Weather.getHex(data));
					} else if (Weather.BUFR_SECTION == SectionType.DATA_SECTION
							.getId()) {

						// bloque pruebas
						System.out.println("DataBitSet:" + Weather.formattedBitSet(data));

						//Primer byte reservado --> index = 8
						int index = 8;
						for (Label label : labels) {
							System.out.println(label + " SIZE:" + label.size + " SCALE:" + label.scale);

							if(label.type == LabelType.DESCRIPTOR) {
								// escala asociada a etiquetas:  9087 con escala 2 representa a valor 90.87
								System.out.println("\t" + (
										label.size < 33 ?
												"--> INT:" + Weather.bitSetToInt(data, index, label.size, label.scale)
												: "--> LONG:" + Weather.bitSetToLong(data, index, label.size, label.scale)
										)
										+ "\n\t--> DOUBLE:" + Weather.bitSetToDouble(data, index, label.size, label.scale)
										+ "\tBitSet: " + Weather.printBitSet(data, index, label.size));
							}
							if(label.type == LabelType.MULTIPLE) {
								// XXX LabelType.MULTIPLE; 1.Y.0 --> la siguiente etiqueta es el contador de repeticiones, las Y-siguientes la secuencia a repetir
								int labelIndex = labels.indexOf(label);
								StringBuffer sbm = new StringBuffer("@@ MULTIPLE index de labels:" + labelIndex
										+ " Valor del contador dentro de etiqueta " + labels.get(labelIndex+1) + " secuencia --> ");
								for(int i = labelIndex+2; i< labelIndex+2+label.x; i++){
									sbm.append(labels.get(i)).append(" | ");
								}
								System.out.println(sbm);
								//TODO proceso de etiquetas en funciones.
								//TODO proceso de etiquetas anidadas (para multiples)
							}

							index += label.size;
						}
						// bloque pruebas
						sb.append("\n\t Bits:" + index + " BYTES:" + ((index/8)+(index%8==0?0:1)) + " Data[" + data.length + "]:\t")
						.append(Weather.getHex(data));
					} else if (Weather.BUFR_SECTION == SectionType.END_SECTION
							.getId()) {
						// Nada
					}
				} else {
					// Nada sb.append(Weather.getHex(data));
				}

				if(sb.length() > 0 ) {
					System.out.println("@@[" + SectionType.values()[Weather.BUFR_SECTION] + "]" + sb + "\n\n");
				}

				data = new byte[step];

			}
			System.out.println("Fin de proceso");

		} catch (final Exception e) {
			e.printStackTrace();
		}

	}




	private static void getLabel(final byte b0, final byte b1, final List<Label> labels, final int nTimes) {
		final Label currentLabel = new Label(new byte[] { b0, b1 });
		Weather.getLabel(currentLabel.type.id, currentLabel.x, currentLabel.y, labels, nTimes);
	}

	private static void getLabel(final int labelType, final int x, final int y, final List<Label> labels, final int nTimes) {
		Label currentLabel = new Label(labelType,x,y);
		for(int k = 0; k<nTimes; k++) {
			labels.add(currentLabel);

			if(currentLabel.type == LabelType.SEQUENCE_DESCRIPTOR_TABLE_D) {
				Weather.getSequenceLabel(currentLabel, labels, 1);
			}
			// XXX LabelType.MULTIPLE; realmente, representan una estructura, la repeticion es en tiempo de interpretacion de datos  *ver Info_desarrollos/ProtocoloBUFR/bufr_sw_desc.pdf
		}
	}

	private static void getSequenceLabel(final Label sequenceLabel, final List<Label> labels, final int nTimes) {
		for(int k = 0; k<nTimes; k++) {
			if(Weather.props.containsKey(sequenceLabel.labelPropKey)) {
				final String sequence = Weather.props.getProperty(sequenceLabel.labelPropKey);
				final String [] subLabels = sequence.split(",");
				for(final String subLabel : subLabels) {
					final String[] labelKey = subLabel.split(":")[0].split("\\.");
					final int instances = subLabel.contains(":") ? Integer.valueOf(subLabel.split(":")[1]): 1;

					Weather.getLabel(
							Integer.valueOf(labelKey[0]), Integer.valueOf(labelKey[1]), Integer.valueOf(labelKey[2]),
							labels, instances);
				}
			} else {
				System.out.println("@@@ NO EXISTE PROPERTY " + sequenceLabel.labelPropKey);
			}
		}
	}




	private static int int3(final byte[] data) {
		return data.length < 3 ? Integer.MIN_VALUE
				: (((byte) 0x00 & 0xFF) << 24) + ((data[0] & 0xFF) << 16)
				+ ((data[1] & 0xFF) << 8) + ((data[2] & 0xFF) << 0);
	}

	public static boolean getBit(final byte[] bitSet, final int i) {
		return (bitSet[i / 8] & (1 << (8 - (i % 8)))) != 0;
	}

	public static int bitSetToInt(final byte[] bitSet, final int beginBit, final int offSet, final int scale) {
		int bitInteger = 0;
		for (int i = 0; i < offSet; i++) {
			bitInteger += Weather.getBit(bitSet, beginBit + i)
					? (1 << (offSet - (i + 1)))
							: 0;
		}
		return bitInteger / (scale==0? 1 : (scale*10));
	}

	public static long bitSetToLong(final byte[] bitSet, final int beginBit, final int offSet, final int scale) {
		long bitLong = 0;
		for (int i = 0; i < offSet; i++) {
			bitLong += Weather.getBit(bitSet, beginBit + i)
					? (1L << (offSet - (i + 1)))
							: 0L;
		}
		return bitLong / (scale==0? 1l : (scale*10));
	}

	public static double bitSetToDouble(final byte[] bitSet, final int beginBit, final int offSet, final int scale) {
		double bitLong = 0;
		for (int i = 0; i < offSet; i++) {
			bitLong += Weather.getBit(bitSet, beginBit + i)
					? (1L << (offSet - (i + 1)))
							: 0L;
		}
		return bitLong / (scale==0? 1 : (scale*10));
	}

	private static String printBitSet(final byte[] bitSet, final int beginBit, final int offSet) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < offSet; i++) {
			sb.append((Weather.getBit(bitSet, beginBit + i)) ? "1": "0");
		}
		return sb.toString();
	}

	private static String formattedBitSet(final byte[] bitSet) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < bitSet.length * 8; i++) {
			sb.append((i % 8 == 0) ? "\n\t" : "");
			sb.append((Weather.getBit(bitSet, i)) ? "1": "0");
		}
		return sb.toString();
	}


	private static final String HEXES = "0123456789ABCDEF";

	private static String getHex(final byte[] raw) {
		if (raw == null) {
			return null;
		}
		final StringBuilder hex = new StringBuilder(2 * raw.length);
		for (final byte b : raw) {
			hex.append(Weather.HEXES.charAt((b & 0xF0) >> 4))
			.append(Weather.HEXES.charAt((b & 0x0F))).append(" ");
		}
		return hex.toString();
	}

	private static String getChars(final byte[] raw) {
		StringBuffer sb = new StringBuffer();
		for (final byte b : raw) {
			sb.append((char) b);
		}
		return sb.toString();
	}

}
