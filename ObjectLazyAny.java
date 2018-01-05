package com.jsoniter.any;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.jsoniter.CodegenAccess;
import com.jsoniter.JsonIterator;
import com.jsoniter.JsonIteratorPool;
import com.jsoniter.ValueType;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.TypeLiteral;

/**
 * ObjectLazyAny
 * 
 * @author
 *
 */
class ObjectLazyAny extends LazyAny {

	private final static TypeLiteral<Map<String, Any>> typeLiteral = new TypeLiteral<Map<String, Any>>() {
	};
	private Map<String, Any> cache;
	private int lastParsedPos;
	private final String err = "Error: IOException";

	/**
	 * ObjectLazyAny.
	 * 
	 * @param data
	 * @param head
	 * @param tail
	 */
	ObjectLazyAny(byte[] data, int head, int tail) {
		super(data, head, tail);
		lastParsedPos = head;
	}

	/**
	 * valueType.
	 */
	@Override
	public ValueType valueType() {
		return ValueType.OBJECT;
	}

	@Override
	public Object object() {
		fillCache();
		return cache;
	}

	@Override
	public boolean toBoolean() {
		try {
			JsonIterator iter = parse();
			try {
				return CodegenAccess.readObjectStart(iter);
			} finally {
				JsonIteratorPool.returnJsonIterator(iter);
			}
		} catch (IOException e) {
			throw new JsonException(err);
		}
	}

	@Override
	public int toInt() {
		return size();
	}

	@Override
	public long toLong() {
		return size();
	}

	@Override
	public float toFloat() {
		return size();
	}

	@Override
	public double toDouble() {
		return size();
	}

	@Override
	public int size() {
		fillCache();
		return cache.size();
	}

	@Override
	public Set<String> keys() {
		fillCache();
		if (cache.keySet() instanceof Set) {
			return (Set) cache.keySet();
		}
		return null;

	}

	@Override
	public Any get(Object key) {
		Any element = fillCacheUntil(key);
		if (element == null) {
			return new NotFoundAny(key, object());
		}
		return element;
	}

	@Override
	public Any get(Object[] keysArray, int idx) {
		if (idx == keysArray.length) {
			return this;
		}
		Object key = keysArray[idx];
		if (isWildcard(key)) {
			fillCache();
			HashMap<String, Any> result = new HashMap<String, Any>();
			for (Map.Entry<String, Any> entry : cache.entrySet()) {
				Any mapped = entry.getValue().get(keysArray, idx + 1);
				if (mapped.valueType() != ValueType.INVALID) {
					result.put(entry.getKey(), mapped);
				}
			}
			return Any.rewrap(result);
		}
		Any child = fillCacheUntil(key);
		if (child == null) {
			return new NotFoundAny(keysArray, idx, object());
		}
		return child.get(keysArray, idx + 1);
	}

	/**
	 * fillCacheUntilSupport.
	 * 
	 * @param target
	 * @return
	 */
	private Any fillCacheUntilSupport(Object target) {
		if (lastParsedPos == tail) {
			return cache.get(target);
		}
		if (cache == null) {
			int n = 4;
			cache = new HashMap<String, Any>(n);
		}
		Any value = cache.get(target);
		if (value != null) {
			return value;
		} else
			return null;
	}

	private Any fillCacheUntil(Object target) {
		Any value = fillCacheUntilSupport(target);
		JsonIterator iter = JsonIteratorPool.borrowJsonIterator();
		try {
			iter.reset(data, lastParsedPos, tail);
			if (lastParsedPos == head) {
				if (!CodegenAccess.readObjectStart(iter)) {
					lastParsedPos = tail;
					return null;
				}
				String field = CodegenAccess.readObjectFieldAsString(iter);
				value = iter.readAny();
				cache.put(field, value);
				if (field.hashCode() == target.hashCode() && field.equals(target)) {
					lastParsedPos = CodegenAccess.head(iter);
					return value;
				}
			}
			byte b = CodegenAccess.nextToken(iter);
			int intero = b;
			return value = fillCacheUntilSupport(target, intero, value, iter);
		} catch (IOException e) {
			throw new JsonException(err);
		} finally {
			JsonIteratorPool.returnJsonIterator(iter);
		}
	}

	private Any fillCacheUntilSupport(Object target, int intero, Any value, JsonIterator iter) throws IOException {
		while (intero == ',') {
			String field = CodegenAccess.readObjectFieldAsString(iter);
			value = iter.readAny();
			cache.put(field, value);
			if (field.hashCode() == target.hashCode() && field.equals(target)) {
				lastParsedPos = CodegenAccess.head(iter);
				return value;
			}
		}
		lastParsedPos = tail;
		return null;
	}

	private void fillCache() {
		if (lastParsedPos == tail) {
			return;
		}
		if (cache == null) {
			int n = 4;
			cache = new HashMap<String, Any>(n);
		}
		JsonIterator iter = JsonIteratorPool.borrowJsonIterator();
		fillCacheSupport(iter);
	}

	public void fillCacheSupport(JsonIterator iter) {
		try {
			iter.reset(data, lastParsedPos, tail);
			if (lastParsedPos == head) {
				if (!CodegenAccess.readObjectStart(iter)) {
					lastParsedPos = tail;
					return;
				}
				String field = CodegenAccess.readObjectFieldAsString(iter);
				cache.put(field, iter.readAny());
			}
			byte b = CodegenAccess.nextToken(iter);
			int intero = b;
			while (intero == ',') {

				String field = CodegenAccess.readObjectFieldAsString(iter);
				cache.put(field, iter.readAny());
			}
			lastParsedPos = tail;
		} catch (IOException e) {
			throw new JsonException(err);
		} finally {
			JsonIteratorPool.returnJsonIterator(iter);
		}

	}

	/**
	 * entries
	 */
	@Override
	public EntryIterator entries() {
		return new LazyIterator();
	}

	private class LazyIterator implements EntryIterator {

		private Iterator<Map.Entry<String, Any>> mapIter;
		private String keyObject;
		private Any valueObject;

		/**
		 * LazyIterator.
		 */
		LazyIterator() {
			if (cache == null) {
				cache = new HashMap<String, Any>();
			}
			mapIter = new HashMap<String, Any>(cache).entrySet().iterator();
			try {
				if (lastParsedPos == head) {
					JsonIterator iter = JsonIteratorPool.borrowJsonIterator();
					try {
						iter.reset(data, lastParsedPos, tail);
						if (!CodegenAccess.readObjectStart(iter)) {
							lastParsedPos = tail;
						} else {
							lastParsedPos = CodegenAccess.head(iter);
						}
					} finally {
						JsonIteratorPool.returnJsonIterator(iter);
					}
				}
			} catch (IOException e) {
				throw new JsonException(err);
			}
		}

		/**
		 * next.
		 */
		@Override
		public boolean next() {
			if (lastParsedPos == tail) {
				return false;
			}
			if (mapIter != null) {
				if (mapIter.hasNext()) {
					Map.Entry<String, Any> entry = mapIter.next();
					keyObject = entry.getKey();
					valueObject = entry.getValue();
					return true;
				} else {
					mapIter = null;
				}
			}
			JsonIterator iter = JsonIteratorPool.borrowJsonIterator();
			nextSupport(iter);
			return true;
		}

		public void nextSupport(JsonIterator iter) {
			try {
				iter.reset(data, lastParsedPos, tail);
				keyObject = CodegenAccess.readObjectFieldAsString(iter);
				valueObject = iter.readAny();
				cache.put(keyObject, valueObject);
				if (CodegenAccess.nextToken(iter) == ',') {
					lastParsedPos = CodegenAccess.head(iter);
				} else {
					lastParsedPos = tail;
				}
			} catch (IOException e) {
				throw new JsonException("IO: Exception");
			} finally {
				JsonIteratorPool.returnJsonIterator(iter);
			}
		}

		@Override
		public String key() {
			return keyObject;
		}

		@Override
		public Any value() {
			return valueObject;
		}
	}

	@Override
	public void writeTo(JsonStream stream) throws IOException {
		if (lastParsedPos == head) {
			super.writeTo(stream);
		} else {
			// there might be modification
			fillCache();
			stream.writeVal(typeLiteral, cache);
		}
	}

	@Override
	public String toString() {
		if (lastParsedPos == head) {
			return super.toString();
		} else {
			fillCache();
			return JsonStream.serialize(cache);
		}
	}
}
