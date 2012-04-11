package com.weather;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Weather {

	private static final int FINAL_STEP = (((byte) 0x00 & 0xFF) << 24)
			+ (((byte) 0x37 & 0xFF) << 16) + (((byte) 0x37 & 0xFF) << 8)
			+ (((byte) 0x37 & 0xFF) << 0);

	private static int BUFR_SECTION = 0;

	public static void main(final String[] args) {

		try {
			final InputStream is = Weather.class.getClassLoader()
					.getResourceAsStream("COR110915203801.BPPI001");

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
					if (Weather.BUFR_SECTION == SectionType.INDICATOR_SECTION.getId()) {
						ByteBuffer bb = ByteBuffer.wrap(data);
						byte[] dst = new byte[4];
						bb.get(dst, 0, 4);
						sb.append("\n\t· ").append(Weather.getChars(dst));

						dst = new byte[3];
						bb.get(dst, 0, 3);
						int totalLength = Weather.int3(dst);
						byte bufrVersion = bb.get();
						sb.append("\n\t· totalLength ").append(totalLength).append("\n\t· bufrVersion ").append(bufrVersion);

					} else if (Weather.BUFR_SECTION == SectionType.IDENTIFICATION_SECTION.getId()) {
						ByteBuffer bb = ByteBuffer.wrap(data);
						//Bufr master table (zero if standard WMO FM 94-IX BUFR tables are used)
						byte masterTableNumber = bb.get();
						//Centro y subcentro a (Byte.MAX_VALUE - Byte.MIN_VALUE) = 255 para standard tables
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

						if(optionalSection != (byte) 0){
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

					} else if (Weather.BUFR_SECTION == SectionType.LABELS_SECTION.getId()) {
						ByteBuffer bb = ByteBuffer.wrap(data);
						//reserved 0
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
							labels.add(new Label(new byte[] { bb.get(), bb.get() }));
						}
						for (Label label : labels) {
							sb.append("\t").append(label).append("\n");
						}
					} else if (Weather.BUFR_SECTION == SectionType.OPTIONAL_SECTION.getId()) {
						sb.append("\n\t").append(Weather.getHex(data));
					} else if (Weather.BUFR_SECTION == SectionType.DATA_SECTION.getId()) {
						//TODO generar imagen
						sb.append("\n\t").append(Weather.getHex(data));
					} else if (Weather.BUFR_SECTION == SectionType.END_SECTION.getId()) {
						// Nada
					}
				} else {
					//Nada sb.append(Weather.getHex(data));
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

	private static int int3(final byte[] data){
		return data.length < 3 ? Integer.MIN_VALUE:(((byte) 0x00 & 0xFF) << 24)
				+ ((data[0] & 0xFF) << 16)
				+ ((data[1] & 0xFF) << 8) + ((data[2] & 0xFF) << 0);
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
