package openjdk.tools.json.internal.pojo;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import openjdk.tools.json.exceptions.JsonInputOutputException;

public class JsonPojoElement<K, V> extends LinkedHashMap<K, V> {
	private static final long serialVersionUID = 1L;
	static Set<String> primitives = new HashSet<String>();
	static Set<String> primitiveWrappers = new HashSet<String>();

	Object target;
	boolean isMap = false;
	String type;
	long id = -1;
	int line;
	int col;

	static {
		primitives.add("boolean");
		primitives.add("byte");
		primitives.add("char");
		primitives.add("double");
		primitives.add("float");
		primitives.add("int");
		primitives.add("long");
		primitives.add("short");

		primitiveWrappers.add("java.lang.Boolean");
		primitiveWrappers.add("java.lang.Byte");
		primitiveWrappers.add("java.lang.Character");
		primitiveWrappers.add("java.lang.Double");
		primitiveWrappers.add("java.lang.Float");
		primitiveWrappers.add("java.lang.Integer");
		primitiveWrappers.add("java.lang.Long");
		primitiveWrappers.add("java.lang.Short");
	}

	public long getId() {
		return id;
	}

	public boolean hasId() {
		return id != -1;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public Object getTarget() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	@SuppressWarnings("rawtypes")
	public Class getTargetClass() {
		return target.getClass();
	}

	public boolean isLogicalPrimitive() {
		return primitiveWrappers.contains(type) || primitives.contains(type) || "date".equals(type)
				|| "java.math.BigInteger".equals(type) || "java.math.BigDecimal".equals(type);
	}

	public Object getPrimitiveValue() {
		if ("boolean".equals(type) || "double".equals(type) || "long".equals(type)) {
			return get("value");
		} else if ("byte".equals(type)) {
			Number b = (Number) get("value");
			return b.byteValue();
		} else if ("char".equals(type)) {
			String c = (String) get("value");
			return c.charAt(0);
		} else if ("float".equals(type)) {
			Number f = (Number) get("value");
			return f.floatValue();
		} else if ("int".equals(type)) {
			Number integer = (Number) get("value");
			return integer.intValue();
		} else if ("short".equals(type)) {
			Number s = (Number) get("value");
			return s.shortValue();
		} else if ("date".equals(type)) {
			Object date = get("value");
			if (date instanceof Long) {
				return new Date((Long) (date));
			} else if (date instanceof String) {
				return JsonPojoReaders.DateReader.parseDate((String) date);
			} else {
				throw new JsonInputOutputException("Unknown date type: " + type);
			}
		} else if ("java.math.BigInteger".equals(type)) {
			Object value = get("value");
			return JsonPojoReaders.bigIntegerFrom(value);
		} else if ("java.math.BigDecimal".equals(type)) {
			Object value = get("value");
			return JsonPojoReaders.bigDecimalFrom(value);
		} else {
			throw new JsonInputOutputException("Invalid primitive type, line " + line + ", col " + col);
		}
	}

	public boolean isReference() {
		return containsKey("@ref");
	}

	public Long getReferenceId() {
		return (Long) get("@ref");
	}

	public boolean isMap() {
		return isMap || target instanceof Map;
	}

	public boolean isCollection() {
		if (target instanceof Collection) {
			return true;
		}
		if (containsKey("@items") && !containsKey("@keys")) {
			return type != null && !type.contains("[");
		}
		return false;
	}

	public boolean isArray() {
		if (target == null) {
			if (type != null) {
				return type.contains("[");
			}
			return containsKey("@items") && !containsKey("@keys");
		}
		return target.getClass().isArray();
	}

	public Object[] getArray() {
		return (Object[]) get("@items");
	}

	public int getLength() {
		if (isArray()) {
			if (target == null) {
				Object[] items = (Object[]) get("@items");
				return items == null ? 0 : items.length;
			}
			return Array.getLength(target);
		}
		if (isCollection() || isMap()) {
			Object[] items = (Object[]) get("@items");
			return items == null ? 0 : items.length;
		}
		throw new JsonInputOutputException("getLength() called on a non-collection, line " + line + ", col " + col);
	}

	public Class<?> getComponentType() {
		return target.getClass().getComponentType();
	}

	void moveBytesToMate() {
		final byte[] bytes = (byte[]) target;
		final Object[] items = getArray();
		final int len = items.length;

		for (int i = 0; i < len; i++) {
			bytes[i] = ((Number) items[i]).byteValue();
		}
	}

	void moveCharsToMate() {
		Object[] items = getArray();
		if (items == null) {
			target = null;
		} else if (items.length == 0) {
			target = new char[0];
		} else if (items.length == 1) {
			String s = (String) items[0];
			target = s.toCharArray();
		} else {
			throw new JsonInputOutputException("char[] should only have one String in the [], found " + items.length
					+ ", line " + line + ", col " + col);
		}
	}

	@SuppressWarnings("unchecked")
	public V put(K key, V value) {
		if (key == null) {
			return super.put(null, value);
		}

		if (key.equals("@type")) {
			String oldType = type;
			type = (String) value;
			return (V) oldType;
		} else if (key.equals("@id")) {
			Long oldId = id;
			id = (Long) value;
			return (V) oldId;
		} else if (("@items".equals(key) && containsKey("@keys")) || ("@keys".equals(key) && containsKey("@items"))) {
			isMap = true;
		}
		return super.put(key, value);
	}

	public void clear() {
		super.clear();
		type = null;
	}

	void clearArray() {
		remove("@items");
	}

	public int getLine() {
		return line;
	}

	public int getCol() {
		return col;
	}

	public int size() {
		if (containsKey("@items")) {
			Object value = get("@items");
			if (value instanceof Object[]) {
				return ((Object[]) value).length;
			} else if (value == null) {
				return 0;
			} else {
				throw new JsonInputOutputException(
						"JsonMap with @items, but no array [] associated to it, line " + line + ", col " + col);
			}
		} else if (containsKey("@ref")) {
			return 0;
		}

		return super.size();
	}
}
