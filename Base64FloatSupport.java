package com.jsoniter.extra;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.jsoniter.CodegenAccess;
import com.jsoniter.JsonIterator;
import com.jsoniter.SupportBitwise;
import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.JsoniterSpi;
import com.jsoniter.spi.Slice;

/**
 * encode float/double as base64, faster than PreciseFloatSupport
 */
public class Base64FloatSupport {

	/**
	 * costruttore
	 */
	private Base64FloatSupport() {
	}

	/**
	 * DODICI
	 */
	final static int DODICI = 12;
	/**
	 * UNDICI
	 */
	final static int UNDICI = 11;
	/**
	 * DIECI
	 */
	final static int DIECI = 10;
	/**
	 * NOVE
	 */
	final static int NOVE = 9;
	/**
	 * OTTO
	 */
	final static int OTTO = 8;
	/**
	 * SETTE
	 */
	final static int SETTE = 7;
	/**
	 * SEI
	 */
	final static int SEI = 6;
	/**
	 * CINQUE
	 */
	final static int CINQUE = 5;
	/**
	 * QUATTRO
	 */
	final static int QUATTRO = 4;
	/**
	 * TRE
	 */
	final static int TRE = 3;
	/**
	 * DUE
	 */
	final static int DUE = 2;
	/**
	 * UNO
	 */
	final static int UNO = 1;
	/**
	 * ZERO
	 */
	final static int ZERO = 0;

	/**
	 * static int[] DIGITS
	 */
	final static int[] DIGITS = new int[256];
	/**
	 * static int[] HEX
	 */
	final static int[] HEX = new int[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
			'f' };
	/**
	 * static int[] DEC
	 */
	final static int[] DEC = new int[127];

	static {
		long f = 0xf;
		for (int i = 0; i < 256; i++) {
			int first = HEX[i >> 4] << 8;
			int second = HEX[Integer
					.getInteger(Long
							.toString(SupportBitwise.bitwise(Long.valueOf(Integer.toString(i)).longValue(), f, '&')))
					.intValue()];
			DIGITS[i] = Integer
					.getInteger(Long.toString(SupportBitwise.bitwise(Long.valueOf(Integer.toString(first)).longValue(),
							Long.valueOf(Integer.toString(second)).longValue(), '|')))
					.intValue();
		}
		DEC['0'] = 0;
		DEC['1'] = 1;
		DEC['2'] = 2;
		DEC['3'] = 3;
		DEC['4'] = 4;
		DEC['5'] = 5;
		DEC['6'] = 6;
		DEC['7'] = 7;
		DEC['8'] = 8;
		DEC['9'] = 9;
		DEC['a'] = 10;
		DEC['b'] = 11;
		DEC['c'] = 12;
		DEC['d'] = 13;
		DEC['e'] = 14;
		DEC['f'] = 15;
	}

	/**
	 * enableEncodersAndDecoders.
	 * @throws IOException
	 */
	private static void enableSupp1() {
		JsoniterSpi.registerTypeEncoder(Double.class, new com.jsoniter.spi.Encoder.ReflectionEncoder() {
			@Override
			public void encode(Object obj, JsonStream stream) throws IOException {
				Double number = null;
				if (obj instanceof Double) {
					number = (Double) obj;
				}
				long bits = Double.doubleToRawLongBits(number.doubleValue());
				Base64.encodeLongBits(bits, stream);
			}

			@Override
			public Any wrap(Object obj) {
				Double number = null;
				if (obj instanceof Double) {
					number = (Double) obj;
				}
				return Any.wrap(number.doubleValue());
			}
		});
		JsoniterSpi.registerTypeEncoder(double.class, new com.jsoniter.spi.Encoder.DoubleEncoder() {
			@Override
			public void encodeDouble(double obj, JsonStream stream) throws IOException {
				long bits = Double.doubleToRawLongBits(obj);
				Base64.encodeLongBits(bits, stream);
			}
		});
	}

	/**
	 * enableSupp2
	 * @throws IOException
	 */
	private static void enableSupp2() {
		JsoniterSpi.registerTypeEncoder(Float.class, new com.jsoniter.spi.Encoder.ReflectionEncoder() {
			@Override
			public void encode(Object obj, JsonStream stream) throws IOException {
				Float number = null;
				if (obj instanceof Float) {
					number = (Float) obj;
				}
				long bits = Double.doubleToRawLongBits(number.doubleValue());
				Base64.encodeLongBits(bits, stream);
			}

			@Override
			public Any wrap(Object obj) {
				Any f = null;
				try {
					if (obj instanceof Float) {
						f = Any.wrap(((Float) obj).floatValue());
					}
				} catch (Exception e) {
					System.out.print("Error: Exception.");
				} finally {
					System.out.print("");
				}
				return f;
			}
		});
	}

	/**
	 * enableEncodersAndDecoders
	 * @throws JsonException
	 * @throws IOException
	 */
	public static void enableEncodersAndDecoders() {
		boolean enabled = false;
		synchronized (Base64FloatSupport.class) {
			if (enabled) {
				throw new JsonException("BinaryFloatSupport.enable can only be called once");
			}
			enabled = true;
			enableDecoders();
			enableSupp1();
			enableSupp2();

			JsoniterSpi.registerTypeEncoder(float.class, new com.jsoniter.spi.Encoder.FloatEncoder() {
				@Override
				public void encodeFloat(float obj, JsonStream stream) throws IOException {
					long bits = Double.doubleToRawLongBits(obj);
					Base64.encodeLongBits(bits, stream);
				}
			});
		}
	}

	/**
	 * enableDecoders1
	 * @throws IOException
	 */
	public static void enableDecoders1() {
		JsoniterSpi.registerTypeDecoder(Float.class, new Decoder() {
			@Override
			public Object decode(JsonIterator iter) throws IOException {
				byte token = CodegenAccess.nextToken(iter);
				CodegenAccess.unreadByte(iter);
				Double d = null;
				if (token == '"') {
					d = Double.longBitsToDouble(Base64.decodeLongBits(iter));
				} else {
					d = iter.readDouble();

				}
				return d.floatValue();
			}
		});
		JsoniterSpi.registerTypeDecoder(float.class, new Decoder.FloatDecoder() {
			@Override
			public float decodeFloat(JsonIterator iter) throws IOException {
				byte token = CodegenAccess.nextToken(iter);
				CodegenAccess.unreadByte(iter);
				Double d = null;
				if (token == '"') {
					d = Double.longBitsToDouble(Base64.decodeLongBits(iter));
				} else {
					d = iter.readDouble();
				}
				return d.floatValue();
			}
		});
	}

	/**
	 * enableDecoders
	 * @see com.jsoniter.spi.Decoder#decode(com.jsoniter.JsonIterator)
	 */
	public static void enableDecoders() {
		JsoniterSpi.registerTypeDecoder(Double.class, new Decoder() {
			@Override
			public Object decode(JsonIterator iter) throws IOException {
				Double doub = null;
				byte token = CodegenAccess.nextToken(iter);
				CodegenAccess.unreadByte(iter);
				if (token == '"') {
					doub = Double.longBitsToDouble(Base64.decodeLongBits(iter));
				} else {
					doub = iter.readDouble();
				}
				return doub;
			}
		});
		JsoniterSpi.registerTypeDecoder(double.class, new Decoder.DoubleDecoder() {
			@Override
			public double decodeDouble(JsonIterator iter) throws IOException {
				Double doub = null;
				byte token = CodegenAccess.nextToken(iter);
				CodegenAccess.unreadByte(iter);
				if (token == '"') {
					doub = Double.longBitsToDouble(Base64.decodeLongBits(iter));
				} else {
					doub = iter.readDouble();
				}
				return doub;
			}
		});
		enableDecoders1();
	}

	/**
	 * readLongBits
	 * 
	 * @param iter
	 * @return
	 * @throws IOException
	 */
	static long readLongBits(JsonIterator iter) throws IOException {
		Slice slice = iter.readStringAsSlice();
		byte[] data = slice.data();
		long val = 0;
		int tail = slice.tail();
		int n = QUATTRO;
		for (int i = slice.head(); i < tail; i++) {
			byte b = data[i];
			val = SupportBitwise.bitwise(val << n, Long.valueOf(Integer.toString(DEC[b])).longValue(), '|');
		}
		return val;
	}

	/**
	 * writeStream4
	 * 
	 * @param bits
	 * @param stream
	 * @param arrayByte
	 * @param longdigit
	 * @return
	 * @throws IOException
	 */
	static JsonStream writeStream4(long bits, JsonStream stream, List<Byte> arrayByte, Long longdigit)
			throws IOException {
		int digit = DIGITS[longdigit.intValue()];
		Integer intero = digit >> OTTO;
		byte b14 = intero.toString().getBytes()[ZERO];
		byte b13 = Integer.valueOf(digit).byteValue();
		arrayByte.add(b13);
		arrayByte.add(b14);
		long bit = bits >> OTTO;
		if (bit == 0) {
			stream.write(arrayByte.get(ZERO), b14, b13, arrayByte.get(DODICI), arrayByte.get(UNDICI),
					arrayByte.get(DIECI));
			stream.write(arrayByte.get(NOVE), arrayByte.get(OTTO), arrayByte.get(SETTE), arrayByte.get(SEI),
					arrayByte.get(CINQUE), arrayByte.get(QUATTRO));
			stream.write(arrayByte.get(TRE), arrayByte.get(DUE), arrayByte.get(UNO), arrayByte.get(ZERO));
		}
		digit = DIGITS[longdigit.intValue()];
		intero = digit >> OTTO;
		byte b16 = intero.toString().getBytes()[ZERO];
		byte b15 = Integer.valueOf(digit).byteValue();
		arrayByte.add(b15);
		arrayByte.add(b16);
		stream.write(arrayByte.get(ZERO), b16, b15, b14, b13, arrayByte.get(DODICI));
		stream.write(arrayByte.get(UNDICI), arrayByte.get(DIECI), arrayByte.get(NOVE), arrayByte.get(OTTO),
				arrayByte.get(SETTE), arrayByte.get(SEI));
		stream.write(arrayByte.get(CINQUE), arrayByte.get(QUATTRO), arrayByte.get(TRE), arrayByte.get(DUE),
				arrayByte.get(UNO), arrayByte.get(ZERO));

		return stream;
	}

	/**
	 * writeStream3
	 * 
	 * @param bits
	 * @param stream
	 * @param arrayByte
	 * @param longdigit
	 * @return
	 * @throws IOException
	 */
	static JsonStream writeStream3(long bits, JsonStream stream, List<Byte> arrayByte, Long longdigit)
			throws IOException {

		int digit = DIGITS[longdigit.intValue()];
		Integer intero = digit >> OTTO;
		byte b10 = intero.toString().getBytes()[ZERO];
		byte b9 = Integer.valueOf(digit).byteValue();
		arrayByte.add(b9);
		arrayByte.add(b10);
		long bit = bits >> OTTO;
		if (bit == 0) {
			stream.write(arrayByte.get(ZERO), b10, b9, arrayByte.get(OTTO), arrayByte.get(SETTE), arrayByte.get(SEI));
			stream.write(arrayByte.get(CINQUE), arrayByte.get(QUATTRO), arrayByte.get(TRE), arrayByte.get(DUE),
					arrayByte.get(UNO), arrayByte.get(ZERO));
		}
		digit = DIGITS[longdigit.intValue()];
		intero = digit >> OTTO;
		byte b12 = intero.toString().getBytes()[0];
		byte b11 = Integer.valueOf(digit).byteValue();
		arrayByte.add(b11);
		arrayByte.add(b12);
		bit = bit >> OTTO;
		if (bit == 0) {
			stream.write(arrayByte.get(ZERO), b12, b11, b10, b9, arrayByte.get(OTTO));
			stream.write(arrayByte.get(SETTE), arrayByte.get(SEI), arrayByte.get(CINQUE), arrayByte.get(QUATTRO),
					arrayByte.get(TRE), arrayByte.get(DUE));
			stream.write(arrayByte.get(UNO), arrayByte.get(ZERO));
		}
		JsonStream strea = writeStream4(bit, stream, arrayByte, longdigit);
		return strea;

	}

	/**
	 * writeStream2
	 * 
	 * @param bits
	 * @param stream
	 * @param arrayByte
	 * @param longdigit
	 * @return
	 * @throws IOException
	 */
	static JsonStream writeStream2(long bits, JsonStream stream, List<Byte> arrayByte, Long longdigit)
			throws IOException {

		int digit = DIGITS[longdigit.intValue()];
		Integer intero = digit >> OTTO;
		byte b6 = intero.toString().getBytes()[ZERO];
		byte b5 = Integer.valueOf(digit).byteValue();
		arrayByte.add(b5);
		arrayByte.add(b6);
		long bit = bits >> OTTO;
		if (bit == 0) {
			stream.write(arrayByte.get(ZERO), b6, b5, arrayByte.get(QUATTRO), arrayByte.get(TRE));
			stream.write(arrayByte.get(DUE), arrayByte.get(UNO), arrayByte.get(ZERO));
		}
		digit = DIGITS[longdigit.intValue()];
		intero = digit >> OTTO;
		byte b8 = intero.toString().getBytes()[ZERO];
		byte b7 = Integer.valueOf(digit).byteValue();
		arrayByte.add(b7);
		arrayByte.add(b8);
		bit = bit >> OTTO;
		if (bit == 0) {
			stream.write(arrayByte.get(ZERO), b8, b7, b6, b5, arrayByte.get(QUATTRO));
			stream.write(arrayByte.get(TRE), arrayByte.get(DUE), arrayByte.get(UNO), arrayByte.get(ZERO));
		}
		JsonStream strea = writeStream3(bit, stream, arrayByte, longdigit);
		return strea;
	}

	/**
	 * writeStream1
	 * 
	 * @param bits
	 * @param stream
	 * @param arrayByte
	 * @param longdigit
	 * @return
	 * @throws IOException
	 */
	static JsonStream writeStream1(long bits, JsonStream stream, List<Byte> arrayByte, Long longdigit)
			throws IOException {

		long bit = bits >> OTTO;
		if (bit == 0) {
			stream.write(arrayByte.get(ZERO), arrayByte.get(UNO), arrayByte.get(DUE), arrayByte.get(ZERO));
		}
		int digit = DIGITS[longdigit.intValue()];
		Integer intero = digit >> OTTO;
		byte b4 = intero.toString().getBytes()[0];
		byte b3 = Integer.valueOf(digit).byteValue();
		arrayByte.add(b3);
		arrayByte.add(b3);
		bit = bit >> OTTO;
		if (bit == 0) {
			stream.write(arrayByte.get(ZERO), b4, b3, arrayByte.get(DUE), arrayByte.get(UNO), arrayByte.get(ZERO));
		}
		JsonStream strea = writeStream2(bit, stream, arrayByte, longdigit);
		return strea;
	}

	/**
	 * writeLongBits
	 * 
	 * @param bits
	 * @param stream
	 * @return
	 * @throws IOException
	 */
	static JsonStream writeLongBits(long bits, JsonStream stream) throws IOException {
		Character c = '"';
		byte ch = c.toString().getBytes()[0];
		Integer intero = null;
		long ff = 0xff;
		Long longdigit = SupportBitwise.bitwise(bits, ff, '&');
		int digit = DIGITS[longdigit.intValue()];
		intero = digit >> OTTO;

		List<Byte> arrayByte = new ArrayList<Byte>();
		byte b2 = intero.toString().getBytes()[0];
		byte b1 = Integer.valueOf(digit).byteValue();
		arrayByte.add(ch);
		arrayByte.add(b1);
		arrayByte.add(b2);

		return writeStream1(bits, stream, arrayByte, longdigit);
	}
}
