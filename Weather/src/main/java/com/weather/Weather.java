package com.weather;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Properties;

public class Weather {

	private static final int FINAL_STEP = (((byte) 0x00 & 0xFF) << 24)
			+ (((byte) 0x37 & 0xFF) << 16) + (((byte) 0x37 & 0xFF) << 8)
			+ (((byte) 0x37 & 0xFF) << 0);

	public static Properties props;

	public static void main(final String[] args) {

		// Cargamos properties
		props = new Properties();
		try {
			props.load(new FileInputStream("resources/sizes.properties"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			final InputStream is = Weather.class.getClassLoader()
					.getResourceAsStream("COR110915203801.BPPI001");

			// final byte[] oneB = new byte[1];
			// final StringBuffer tsb = new StringBuffer();
			// while(is.read(oneB, 0, 1) != -1) {
			// tsb.append( getHex(oneB) );
			// }
			// final BufferedWriter out = new BufferedWriter(new
			// FileWriter("resources/salida.txt"));
			// out.write(tsb.toString());
			// out.close();
			// System.exit(1);

			boolean section = false;

			int step = 8;
			byte[] data = new byte[step];
			int readedBytes = 0;
			int jump = 0;
			List<Label> labels = new ArrayList<Label>();
			while (is.read(data, 0, step) != -1) {
				if (section) {
					section = false;

					step = (((byte) 0x00 & 0xFF) << 24)
							+ ((data[0] & 0xFF) << 16)
							+ ((data[1] & 0xFF) << 8) + ((data[2] & 0xFF) << 0);

					step = step == FINAL_STEP ? 1 : step - 3;
					jump++;

				} else {
					section = true;
					step = 3;
				}
				final StringBuffer sb = new StringBuffer();
				if (jump == 3 && section) {
					ByteBuffer bb = ByteBuffer.wrap(data);
					while (bb.position() < bb.limit() - 1) {
						// TODO descartar primeros
						labels.add(new Label(new byte[] { bb.get(), bb.get() }));
					}
				} else if (jump == 4 && section) {

					
					//bloque pruebas 
					BitSet dataBitSet = BitSet.valueOf(data);

					BitSet midato = new BitSet();
					Integer index = 0;
					for (Label label : labels) {
						System.out.println(label + " SIZE:" + label.size);
						midato.clear();

						midato = dataBitSet.get(index, index + label.size);
						index = index + label.size;

						System.out.println("--> INT:" + bitSetToInt(midato)
								+ " INT2:" + bitSetToInt2(midato) + " LONG:"
								+ bitSetToLong(midato) + " FLOAT:"
								+ bitSetToFloat(midato) + " DOUBLE:"
								+ bitSetToDouble(midato));
					}
					//bloque pruebas 
				} else {
					for (final byte b : data) {
						readedBytes++;
						if (readedBytes < 4) {
							sb.append((char) b);
						} else if (readedBytes == 4) {
							sb.append((char) b).append("\n");
						}
					}
				}
				sb.append(getHex(data));
				System.out.println("--> " + sb);
				System.out.println("Readed bytes[" + jump + "]--> "
						+ readedBytes + " next step: " + step);

				data = new byte[step];

				if (jump == 2) {
					// TODO if opcional
					jump++;
				}
			}
			System.out.println("Final Readed bytes--> " + readedBytes);

		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

	public static int unsignedByteToInt(final byte b) {
		return b & 0xFF;
	}

	public static int bitSetToInt(BitSet bitSet) {
		int bitInteger = 0;
		for (int i = 0; i < 32; i++)
			if (bitSet.get(i))
				bitInteger |= (1 << i);
		return bitInteger;
	}

	public static int bitSetToInt2(BitSet bitSet) {
		try {
			String s = "";
			for (int i = 0; i < 32; i++)
				if (bitSet.get(i))
					s = /* s + */"1" + s;
				else
					s = /* s + */"0" + s;
			return Integer.parseInt(s, 2);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static double bitSetToDouble(BitSet bitSet) {
		try {
			String s = "";
			for (int i = 0; i < 32; i++)
				if (bitSet.get(i))
					s = /* s + */"1" + s;
				else
					s = /* s + */"0" + s;
			return Double.parseDouble(s, 2);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static float bitSetToFloat(BitSet bitSet) {
		float bitInteger = 0;
		for (int i = 0; i < 32; i++)
			if (bitSet.get(i))
				bitInteger |= (1 << i);
		return bitInteger;
	}

	public static long bitSetToLong(BitSet bitSet) {
		long bitInteger = 0;
		for (int i = 0; i < 64; i++)
			if (bitSet.get(i))
				bitInteger |= (1 << i);
		return bitInteger;
	}

	static final String HEXES = "0123456789ABCDEF";

	public static String getHex(final byte[] raw) {
		if (raw == null) {
			return null;
		}
		final StringBuilder hex = new StringBuilder(2 * raw.length);
		for (final byte b : raw) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4))
					.append(HEXES.charAt((b & 0x0F))).append(" ");
		}
		return hex.toString();
	}

}
