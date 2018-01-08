package com.jsoniter;

import com.jsoniter.any.Any;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.Slice;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * class IterImplForStreaming
 * 
 * @author MaxiBon
 *
 */
class IterImplForStreaming {

	/**
	 * constructor
	 */
	private IterImplForStreaming() {
	}

	/**
	 * readObjectFieldAsHash
	 * 
	 * @param iter
	 * @return
	 * @throws IOException
	 */
	public static final int readObjectFieldAsHash(JsonIterator iter) throws IOException {
		if (nextToken(iter) != '"') {
			throw iter.reportError("readObjectFieldAsHash", "expect \"");
		}
		long hash = 0x811c9dc5;
		for (;;) {
			byte c = 0;
			int i = iter.head;
			for (; i < iter.tail; i++) {
				c = iter.buf[i];
				if (c == '"') {
					break;
				}
				hash ^= c;
				hash *= 0x1000193;
			}
			if (c == '"') {
				iter.head = i + 1;
				if (nextToken(iter) != ':') {
					throw iter.reportError("readObjectFieldAsHash", "expect :");
				}
				return (int) hash;
			}
			if (!loadMore(iter)) {
				throw iter.reportError("readObjectFieldAsHash", "unmatched quote");
			}
		}
	}

	/**
	 * @param iter
	 * @return
	 * @throws IOException
	 */
	public static final Slice readObjectFieldAsSlice(JsonIterator iter) throws IOException {
		Slice field = readSlice(iter);
		boolean notCopied = field != null;
		if (CodegenAccess.skipWhitespacesWithoutLoadMore(iter)) {
			if (notCopied) {
				int len = field.tail() - field.head();
				byte[] newBuf = new byte[len];
				System.arraycopy(field.data(), field.head(), newBuf, 0, len);
				field.reset(newBuf, 0, newBuf.length);
			}
			if (!loadMore(iter)) {
				throw iter.reportError("readObjectFieldAsSlice", "expect : after object field");
			}
		}
		if (iter.buf[iter.head] != ':') {
			throw iter.reportError("readObjectFieldAsSlice", "expect : after object field");
		}
		iter.head++;
		return field;
	}

	final static void skipArray(JsonIterator iter) throws IOException {
		int level = 1;
		for (;;) {
			int i = iter.head;
			while (i < iter.tail) {
				switch (iter.buf[i]) {
				case '"': // If inside string, skip it
					iter.head = i + 1;
					skipString(iter);
					i = iter.head - 1; // it will be i++ soon
					break;
				case '[': // If open symbol, increase level
					level++;
					break;
				case ']': // If close symbol, increase level
					level--;

					// If we have returned to the original level, we're done
					if (level == 0) {
						iter.head = i + 1;
						return;
					}
					break;
				default:
					break;
				}
				i++;
			}
			if (!loadMore(iter)) {
				return;
			}
		}
	}

	final static void skipObject(JsonIterator iter) throws IOException {
		int level = 1;
		for (;;) {
			int i = iter.head;
			while (i < iter.tail) {
				switch (iter.buf[i]) {
				case '"': // If inside string, skip it
					iter.head = i + 1;
					skipString(iter);
					i = iter.head - 1; // it will be i++ soon
					break;
				case '{': // If open symbol, increase level
					level++;
					break;
				case '}': // If close symbol, increase level
					level--;

					// If we have returned to the original level, we're done
					if (level == 0) {
						iter.head = i + 1;
						return;
					}
					break;
				default:
					break;
				}
				i++;
			}
			if (!loadMore(iter)) {
				return;
			}
		}
	}

	final static void skipString(JsonIterator iter) throws IOException {
		for (;;) {
			int end = IterImplSkip.findStringEnd(iter);
			if (end == -1) {
				boolean escaped = true;
				int j = iter.tail - 1;
				// can not just look the last byte is \
				// because it could be \\ or \\\
				for (;;) {
					// walk backward until head
					if (Boolean.logicalOr(j < iter.head, iter.buf[j] != '\\')) {
						// even number of backslashes
						// either end of buffer, or " found
						escaped = false;
						break;
					}
					j--;
					if (Boolean.logicalOr(j < iter.head, iter.buf[j] != '\\')) {
						// odd number of backslashes
						// it is \" or \\\"
						break;
					}
					j--;

				}
				if (!loadMore(iter)) {
					throw iter.reportError("skipString", "incomplete string");
				}
				if (escaped) {
					iter.head = 1; // skip the first char as last char is \
				}
			} else {
				iter.head = end;
				return;
			}
		}
	}

	final static void skipUntilBreak(JsonIterator iter) throws IOException {
		// true, false, null, number
		for (;;) {
			for (int i = iter.head; i < iter.tail; i++) {
				byte c = iter.buf[i];
				if (IterImplSkip.breaks[c]) {
					iter.head = i;
					return;
				}
			}
			if (!loadMore(iter)) {
				iter.head = iter.tail;
				return;
			}
		}
	}

	final static boolean skipNumber(JsonIterator iter) throws IOException {
		// true, false, null, number
		boolean dotFound = false;
		for (;;) {
			for (int i = iter.head; i < iter.tail; i++) {
				byte c = iter.buf[i];
				if (c == '.') {
					dotFound = true;
					continue;
				}
				if (IterImplSkip.breaks[c]) {
					iter.head = i;
					return dotFound;
				}
			}
			if (!loadMore(iter)) {
				iter.head = iter.tail;
				return dotFound;
			}
		}
	}

	// read the bytes between " "
	final static Slice readSlice(JsonIterator iter) throws IOException {
		if (IterImpl.nextToken(iter) != '"') {
			throw iter.reportError("readSlice", "expect \" for string");
		}
		int end = IterImplString.findSliceEnd(iter);
		if (end != -1) {
			// reuse current buffer
			iter.reusableSlice.reset(iter.buf, iter.head, end - 1);
			iter.head = end;
			return iter.reusableSlice;
		}
		byte[] part1 = new byte[iter.tail - iter.head];
		System.arraycopy(iter.buf, iter.head, part1, 0, part1.length);
		byte[] part2 = null;
		for (;;) {
			if (!loadMore(iter)) {
				throw iter.reportError("readSlice", "unmatched quote");
			}
			end = IterImplString.findSliceEnd(iter);
			if (end == -1) {
				part2 = new byte[part1.length + iter.buf.length];
				System.arraycopy(part1, 0, part2, 0, part1.length);
				System.arraycopy(iter.buf, 0, part2, part1.length, iter.buf.length);
				part1 = part2;
			} else {
				part2 = new byte[part1.length + end - 1];
				System.arraycopy(part1, 0, part2, 0, part1.length);
				System.arraycopy(iter.buf, 0, part2, part1.length, end - 1);
				iter.head = end;
				iter.reusableSlice.reset(part2, 0, part2.length);
				return iter.reusableSlice;
			}
		}
	}

	final static byte nextToken(JsonIterator iter) throws IOException {
		for (;;) {
			for (int i = iter.head; i < iter.tail; i++) {
				byte c = iter.buf[i];
				switch (c) {
				case ' ':
					continue;
				case '\n':
					continue;
				case '\t':
					continue;
				case '\r':
					continue;
				default:
					iter.head = i + 1;
					return c;
				}
			}
			if (!loadMore(iter)) {
				return 0;
			}
		}
	}

	public final static boolean loadMore(JsonIterator iter) throws IOException {
		if (iter.in == null) {
			return false;
		}
		if (iter.skipStartedAt != -1) {
			return keepSkippedBytesThenRead(iter);
		}
		int n = iter.in.read(iter.buf);
		if (n < 1) {
			if (n == -1) {
				return false;
			} else {
				throw iter.reportError("loadMore", "read from input stream returned " + n);
			}
		} else {
			iter.head = 0;
			iter.tail = n;
		}
		return true;
	}

	private static boolean keepSkippedBytesThenRead(JsonIterator iter) throws IOException {
		int n = 0;
		int offset = 0;
		if (iter.skipStartedAt == 0 || iter.skipStartedAt < iter.tail / 2) {
			byte[] newBuf = new byte[iter.buf.length * 2];
			offset = iter.tail - iter.skipStartedAt;
			System.arraycopy(iter.buf, iter.skipStartedAt, newBuf, 0, offset);
			iter.buf = newBuf;
			n = iter.in.read(iter.buf, offset, iter.buf.length - offset);
		} else {
			offset = iter.tail - iter.skipStartedAt;
			System.arraycopy(iter.buf, iter.skipStartedAt, iter.buf, 0, offset);
			n = iter.in.read(iter.buf, offset, iter.buf.length - offset);
		}
		iter.skipStartedAt = 0;
		if (n < 1) {
			if (n == -1) {
				return false;
			} else {
				throw iter.reportError("loadMore", "read from input stream returned " + n);
			}
		} else {
			iter.head = offset;
			iter.tail = offset + n;
		}
		return true;
	}

	final static byte readByte(JsonIterator iter) throws IOException {
		if (iter.head == iter.tail) {
			if (!loadMore(iter)) {
				throw iter.reportError("readByte", "no more to read");
			}
		}
		return iter.buf[iter.head++];
	}

	public static Any readAny(JsonIterator iter) throws IOException {
		iter.skipStartedAt = iter.head;
		byte c = nextToken(iter);
		int n1 = 3;
		int n2 = 4;
		switch (c) {
		case '"':
			skipString(iter);
			byte[] copied = copySkippedBytes(iter);
			return Any.lazyString(copied, 0, copied.length);
		case 't':

			skipFixedBytes(iter, n1);
			iter.skipStartedAt = -1;
			return Any.wrap(true);
		case 'f':
			skipFixedBytes(iter, n2);
			iter.skipStartedAt = -1;
			return Any.wrap(false);
		case 'n':
			skipFixedBytes(iter, n1);
			iter.skipStartedAt = -1;
			return Any.wrap(0);
		case '[':
			skipArray(iter);
			copied = copySkippedBytes(iter);
			return Any.lazyArray(copied, 0, copied.length);
		case '{':
			skipObject(iter);
			copied = copySkippedBytes(iter);
			return Any.lazyObject(copied, 0, copied.length);
		default:
			if (skipNumber(iter)) {
				copied = copySkippedBytes(iter);
				return Any.lazyDouble(copied, 0, copied.length);
			} else {
				copied = copySkippedBytes(iter);
				return Any.lazyLong(copied, 0, copied.length);
			}
		}
	}

	private static byte[] copySkippedBytes(JsonIterator iter) {
		int start = iter.skipStartedAt;
		iter.skipStartedAt = -1;
		int end = iter.head;
		byte[] bytes = new byte[end - start];
		System.arraycopy(iter.buf, start, bytes, 0, bytes.length);
		return bytes;
	}

	public static void skipFixedBytes(JsonIterator iter, int n) throws IOException {
		iter.head += n;
		if (iter.head >= iter.tail) {
			int more = iter.head - iter.tail;
			if (!loadMore(iter)) {
				if (more == 0) {
					iter.head = iter.tail;
					return;
				}
				throw iter.reportError("skipFixedBytes", "unexpected end");
			}
			iter.head += more;
		}
	}

	public static int updateStringCopyBound(final JsonIterator iter, final int bound) {
		if (bound > iter.tail - iter.head) {
			return iter.tail - iter.head;
		} else {
			return bound;
		}
	}

	public final static int readStringSlowPath(JsonIterator iter, int j) throws IOException {
		Boolean isExpectingLowSurrogate = false;
		Map<JsonIterator, Integer> support = new TreeMap<JsonIterator, Integer>();
		char[] newBuf = null;
		long f = 0x80;
		for (;;) {
			int bc = readByte(iter);
			if (bc == '"') {
				return j;
			}
			if (bc == '\\') {
				bc = readByte(iter);
				bc = switchSupport(bc, iter, isExpectingLowSurrogate);
			} else if ((Integer
					.getInteger(Long
							.toString(SupportBitwise.bitwise(Long.valueOf(Integer.toString(bc)).longValue(), f, '&')))
					.intValue()) != 0) {
				final int u2 = readByte(iter);
				f = 0xE0;
				if ((Integer
						.getInteger(Long.toString(
								SupportBitwise.bitwise(Long.valueOf(Integer.toString(bc)).longValue(), f, '&')))
						.intValue()) == 0xC0) {
					long l1 = 0x1F;
					long l2 = 0x3F;
					bc = ((Integer
							.getInteger(Long.toString(
									SupportBitwise.bitwise(Long.valueOf(Integer.toString(bc)).longValue(), l1, '&')))
							.intValue()) << 6)
							+ (Integer
									.getInteger(Long.toString(SupportBitwise
											.bitwise(Long.valueOf(Integer.toString(u2)).longValue(), l2, '&')))
									.intValue());
				} else {
					final int u3 = readByte(iter);
					f = 0xF0;
					support = iterImplStreamingSupport(iter, f, bc, u2, u3, j);
					for (JsonIterator je: support.keySet()){
						iter = je;
					}
					bc = support.get(iter);
				}
			}
			if (iter.reusableChars.length == j) {
				newBuf = new char[iter.reusableChars.length * 2];
				System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
				iter.reusableChars = newBuf;
			}
			iter.reusableChars[j++] = Integer.toString(bc).charAt(0);
		}
	}
	
	private static int switchSupport(int bc, JsonIterator iter, Boolean isExpectingLowSurrogate) throws IOException {
		
		int bcCopy = bc;
		boolean booleSupport = isExpectingLowSurrogate;
		int[] valori = {'b','t', 'n', 'f','r','"','\\', '/'};
		int[] risultati = {'\b','\t','\n','\f','\r','"','\\','/'};
		boolean valid = false;
		if(bcCopy=='u') {
			bcCopy = (IterImplString.translateHex(readByte(iter)) << 12) + (IterImplString.translateHex(readByte(iter)) << 8)
			    + (IterImplString.translateHex(readByte(iter)) << 4) + IterImplString.translateHex(readByte(iter));
			char charBc = (char) bcCopy;
			boolean b1 = Boolean.logicalAnd(isExpectingLowSurrogate, Character.isHighSurrogate(charBc));
			boolean b2 = Boolean.logicalAnd(!isExpectingLowSurrogate, Character.isLowSurrogate(charBc));
			if (Boolean.logicalOr(b1, b2)) {
				throw new JsonException("invalid surrogate");
			} else if (Boolean.logicalAnd(!isExpectingLowSurrogate, Character.isHighSurrogate(charBc))) {
				booleSupport = true;
			} else if (Boolean.logicalAnd(isExpectingLowSurrogate, Character.isLowSurrogate(charBc))) {
				booleSupport = false;
			} else {
				throw new JsonException("invalid surrogate");
			}
			valid = true;
			isExpectingLowSurrogate = booleSupport;
		}
		for(int i=0; i<valori.length;i++) {
			if(bcCopy == valori[i]){
				bcCopy = risultati[i];
				valid = true;
			}
		}
		if(!valid) {
			throw iter.reportError("readStringSlowPath", "invalid escape character: " + bcCopy);
		}
		return bcCopy;
	}
	
	private static Map<JsonIterator, Integer> iterImplStreamingSupport(JsonIterator iter, long f, int bc, int u2, int u3, int j) throws IOException{
		Map<JsonIterator, Integer> support = new TreeMap<JsonIterator, Integer>();
		if ((Integer
				.getInteger(Long.toString(
						SupportBitwise.bitwise(Long.valueOf(Integer.toString(bc)).longValue(), f, '&')))
				.intValue()) == 0xE0) {
			long l1 = 0x0F;
			long l2 = 0x3F;
			bc = ((Integer
					.getInteger(Long.toString(SupportBitwise
							.bitwise(Long.valueOf(Integer.toString(bc)).longValue(), l1, '&')))
					.intValue()) << 12)
					+ ((Integer
							.getInteger(Long.toString(SupportBitwise
									.bitwise(Long.valueOf(Integer.toString(u2)).longValue(), l2, '&')))
							.intValue()) << 6)
					+ (Integer
							.getInteger(Long.toString(SupportBitwise
									.bitwise(Long.valueOf(Integer.toString(u3)).longValue(), l2, '&')))
							.intValue());
			support.put(iter, bc);
			return support;
		} else {
			bc = iterStreamingSupport(iter, f, bc, u2, u3);

			if (bc >= 0x10000) {
				// check if valid unicode
				if (bc >= 0x110000) {
					throw iter.reportError("readStringSlowPath", "invalid unicode character");
				}

				// split surrogates
				final int sup = bc - 0x10000;
				if (iter.reusableChars.length == j) {
					char[] newBuf = new char[iter.reusableChars.length * 2];
					System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
					iter.reusableChars = newBuf;
				}
				Integer a = ((sup >>> 10) + 0xd800);
				iter.reusableChars[j++] = a.toString().toCharArray()[0];
				if (iter.reusableChars.length == j) {
					char[] newBuf = new char[iter.reusableChars.length * 2];
					System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
					iter.reusableChars = newBuf;
				}
				f = 0x3ff;
				Integer b = (Integer
						.getInteger(Long.toString(SupportBitwise
								.bitwise(Long.valueOf(Integer.toString(sup)).longValue(), f, '&')))
						.intValue() + 0xdc00);

				iter.reusableChars[j++] = b.toString().toCharArray()[0];
			}
			support.put(iter, bc);
			return support;
		}
	}

	private static int iterStreamingSupport(JsonIterator iter, long f, int bc, int u2, int u3) throws IOException {
		final int u4 = readByte(iter);
		f = 0xF8;
		if ((Integer
				.getInteger(
						Long.toString(SupportBitwise.bitwise(Long.valueOf(Integer.toString(bc)).longValue(), f, '&')))
				.intValue() + 0xdc00) == 0xF0) {
			long l1 = 0x07;
			long l2 = 0x3F;
			long l3 = 0x3F;
			bc = ((Integer
					.getInteger(
							Long.toString(
									SupportBitwise
											.bitwise(
													Long.valueOf(Integer.toString(bc))
															.longValue(),
													l1, '&')))
					.intValue()) << 18)
					+ ((Integer
							.getInteger(Long.toString(
									SupportBitwise.bitwise(Long.valueOf(Integer.toString(u2)).longValue(), l2, '&')))
							.intValue()) << 12)
					+ ((Integer
							.getInteger(Long.toString(
									SupportBitwise.bitwise(Long.valueOf(Integer.toString(u3)).longValue(), l3, '&')))
							.intValue()) << 6)
					+ (Integer
							.getInteger(Long.toString(
									SupportBitwise.bitwise(Long.valueOf(Integer.toString(u4)).longValue(), l3, '&')))
							.intValue());
		} else {
			throw iter.reportError("readStringSlowPath", "invalid unicode character");
		}
		return bc;
	}

	static long readLongSlowPath(final JsonIterator iter, long value) throws IOException {
		value = -value; // add negatives to avoid redundant checks for
						// Long.MIN_VALUE on each iteration
		long multmin = -922337203685477580L; // limit / 10
		for (;;) {
			for (int i = iter.head; i < iter.tail; i++) {
				int ind = IterImplNumber.intDigits[iter.buf[i]];
				if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
					iter.head = i;
					return value;
				}
				if (value < multmin) {
					throw iter.reportError("readLongSlowPath", "value is too large for long");
				}
				value = (value << 3) + (value << 1) - ind;
				if (value >= 0) {
					throw iter.reportError("readLongSlowPath", "value is too large for long");
				}
			}
			if (!IterImpl.loadMore(iter)) {
				iter.head = iter.tail;
				return value;
			}
		}
	}

	static int readIntSlowPath(final JsonIterator iter, int value) throws IOException {
		value = -value; // add negatives to avoid redundant checks for
						// Integer.MIN_VALUE on each
						// iteration
		int multmin = -214748364; // limit / 10
		for (;;) {
			for (int i = iter.head; i < iter.tail; i++) {
				int ind = IterImplNumber.intDigits[iter.buf[i]];
				if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
					iter.head = i;
					return value;
				}
				if (value < multmin) {
					throw iter.reportError("readIntSlowPath", "value is too large for int");
				}
				value = (value << 3) + (value << 1) - ind;
				if (value >= 0) {
					throw iter.reportError("readIntSlowPath", "value is too large for int");
				}
			}
			if (!IterImpl.loadMore(iter)) {
				iter.head = iter.tail;
				return value;
			}
		}
	}

	public static final double readDoubleSlowPath(final JsonIterator iter) throws IOException {
		try {
			String numberAsStr = readNumber(iter);
			return Double.valueOf(numberAsStr);
		} catch (NumberFormatException e) {
			throw iter.reportError("readDoubleSlowPath", e.toString());
		}
	}

	public static final String readNumber(final JsonIterator iter) throws IOException {
		int j = 0;

		String stringa = null;
		char[] newBuf = null;
		for (;;) {
			for (int i = iter.head; i < iter.tail; i++) {
				if (j == iter.reusableChars.length) {
					newBuf = new char[iter.reusableChars.length * 2];
					System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
					iter.reusableChars = newBuf;
				}
				byte c = iter.buf[i];
				if("-+.eE0123456789".contains(Byte.toString(c))){
					iter.reusableChars[j++] = Byte.toString(c).charAt(0);
				} else {
					iter.head = i;
					stringa = new String(iter.reusableChars, 0, j);
					return stringa;
				}
			}
			
			if (!IterImpl.loadMore(iter)) {
				iter.head = iter.tail;
				stringa = new String(iter.reusableChars, 0, j);
				return stringa;
			}
		}
	}

	static final double readDouble(final JsonIterator iter) throws IOException {
		return readDoubleSlowPath(iter);
	}

	static final long readLong(final JsonIterator iter, final byte c) throws IOException {
		long ind = IterImplNumber.intDigits[c];
		if (ind == 0) {
			assertNotLeadingZero(iter);
			return 0;
		}
		if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
			throw iter.reportError("readLong", "expect 0~9");
		}
		return IterImplForStreaming.readLongSlowPath(iter, ind);
	}

	static final int readInt(final JsonIterator iter, final byte c) throws IOException {
		int ind = IterImplNumber.intDigits[c];
		if (ind == 0) {
			assertNotLeadingZero(iter);
			return 0;
		}
		if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
			throw iter.reportError("readInt", "expect 0~9");
		}
		return IterImplForStreaming.readIntSlowPath(iter, ind);
	}

	static void assertNotLeadingZero(JsonIterator iter) throws IOException {
		try {
			byte nextByte = IterImpl.readByte(iter);
			iter.unreadByte();
			int ind2 = IterImplNumber.intDigits[nextByte];
			if (ind2 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
				return;
			}
			throw iter.reportError("assertNotLeadingZero", "leading zero is invalid");
		} catch (ArrayIndexOutOfBoundsException e) {
			iter.head = iter.tail;
			return;
		}
	}
}