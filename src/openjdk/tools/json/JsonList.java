package openjdk.tools.json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import openjdk.tools.json.exceptions.JsonException;
import openjdk.tools.json.exceptions.JsonPointerException;
import openjdk.tools.json.internal.JsonPointer;
import openjdk.tools.json.internal.tokens.JsonTokener;

public class JsonList implements Iterable<Object> {

	private final ArrayList<Object> myArrayList;

	public static final Object NULL = new JsonMap.Null();
	
	public JsonList() {
		this.myArrayList = new ArrayList<Object>();
	}

	public JsonList(JsonTokener x) throws JsonException {
		this();
		if (x.nextClean() != '[') {
			throw x.syntaxError("A JsonList text must start with '['");
		}
		if (x.nextClean() != ']') {
			x.back();
			for (;;) {
				if (x.nextClean() == ',') {
					x.back();
					this.myArrayList.add(JsonMap.NULL);
				} else {
					x.back();
					this.myArrayList.add(x.nextValue());
				}
				switch (x.nextClean()) {
				case ',':
					if (x.nextClean() == ']') {
						return;
					}
					x.back();
					break;
				case ']':
					return;
				default:
					throw x.syntaxError("Expected a ',' or ']'");
				}
			}
		}
	}

	public JsonList(String source) throws JsonException {
		this(new JsonTokener(source));
	}

	public JsonList(Collection<?> collection) {
		this.myArrayList = new ArrayList<Object>();
		if (collection != null) {
			for (Object o : collection) {
				this.myArrayList.add(JsonMap.wrap(o));
			}
		}
	}

	public JsonList(Object array) throws JsonException {
		this();
		if (array.getClass().isArray()) {
			int length = Array.getLength(array);
			for (int i = 0; i < length; i += 1) {
				this.put(JsonMap.wrap(Array.get(array, i)));
			}
		} else {
			throw new JsonException("JsonList initial value should be a string or collection or array.");
		}
	}

	@Override
	public Iterator<Object> iterator() {
		return myArrayList.iterator();
	}

	public Object get(int index) throws JsonException {
		Object object = this.opt(index);
		if (object == null) {
			throw new JsonException("[" + index + "] not found.");
		}
		return object;
	}

	public boolean getBoolean(int index) throws JsonException {
		Object object = this.get(index);
		if (object.equals(Boolean.FALSE) || (object instanceof String && ((String) object).equalsIgnoreCase("false"))) {
			return false;
		} else if (object.equals(Boolean.TRUE)
				|| (object instanceof String && ((String) object).equalsIgnoreCase("true"))) {
			return true;
		}
		throw new JsonException("[" + index + "] is not a boolean.");
	}

	public double getDouble(int index) throws JsonException {
		Object object = this.get(index);
		try {
			return object instanceof Number ? ((Number) object).doubleValue() : Double.parseDouble((String) object);
		} catch (Exception e) {
			throw new JsonException("[" + index + "] is not a number.");
		}
	}

	public <E extends Enum<E>> E getEnum(Class<E> clazz, int index) throws JsonException {
		E val = optEnum(clazz, index);
		if (val == null) {
			throw new JsonException("[" + JsonMap.quote(Integer.toString(index)) + "] is not an enum of type "
					+ JsonMap.quote(clazz.getSimpleName()) + ".");
		}
		return val;
	}

	public BigDecimal getBigDecimal(int index) throws JsonException {
		Object object = this.get(index);
		try {
			return new BigDecimal(object.toString());
		} catch (Exception e) {
			throw new JsonException("[" + index + "] could not convert to BigDecimal.");
		}
	}

	public BigInteger getBigInteger(int index) throws JsonException {
		Object object = this.get(index);
		try {
			return new BigInteger(object.toString());
		} catch (Exception e) {
			throw new JsonException("[" + index + "] could not convert to BigInteger.");
		}
	}

	public int getInt(int index) throws JsonException {
		Object object = this.get(index);
		try {
			return object instanceof Number ? ((Number) object).intValue() : Integer.parseInt((String) object);
		} catch (Exception e) {
			throw new JsonException("[" + index + "] is not a number.");
		}
	}

	public JsonList getJsonList(int index) throws JsonException {
		Object object = this.get(index);
		if (object instanceof JsonList) {
			return (JsonList) object;
		}
		throw new JsonException("[" + index + "] is not a JsonList.");
	}

	public JsonMap getJsonMap(int index) throws JsonException {
		Object object = this.get(index);
		if (object instanceof JsonMap) {
			return (JsonMap) object;
		}
		throw new JsonException("[" + index + "] is not a JsonMap.");
	}

	public long getLong(int index) throws JsonException {
		Object object = this.get(index);
		try {
			return object instanceof Number ? ((Number) object).longValue() : Long.parseLong((String) object);
		} catch (Exception e) {
			throw new JsonException("[" + index + "] is not a number.");
		}
	}

	public String getString(int index) throws JsonException {
		Object object = this.get(index);
		if (object instanceof String) {
			return (String) object;
		}
		throw new JsonException("[" + index + "] not a string.");
	}

	public boolean isNull() {
		return JsonMap.NULL.equals(this);
	}
	
	public boolean isNull(int index) {
		return JsonMap.NULL.equals(this.opt(index));
	}

	public String join(String separator) throws JsonException {
		int len = this.length();
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < len; i += 1) {
			if (i > 0) {
				sb.append(separator);
			}
			sb.append(JsonMap.valueToString(this.myArrayList.get(i)));
		}
		return sb.toString();
	}

	public int length() {
		return this.myArrayList.size();
	}

	public Object opt(int index) {
		return (index < 0 || index >= this.length()) ? null : this.myArrayList.get(index);
	}

	public boolean optBoolean(int index) {
		return this.optBoolean(index, false);
	}

	public boolean optBoolean(int index, boolean defaultValue) {
		try {
			return this.getBoolean(index);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public double optDouble(int index) {
		return this.optDouble(index, Double.NaN);
	}

	public double optDouble(int index, double defaultValue) {
		try {
			return this.getDouble(index);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public int optInt(int index) {
		return this.optInt(index, 0);
	}

	public int optInt(int index, int defaultValue) {
		try {
			return this.getInt(index);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public <E extends Enum<E>> E optEnum(Class<E> clazz, int index) {
		return this.optEnum(clazz, index, null);
	}

	public <E extends Enum<E>> E optEnum(Class<E> clazz, int index, E defaultValue) {
		try {
			Object val = this.opt(index);
			if (JsonMap.NULL.equals(val)) {
				return defaultValue;
			}
			if (clazz.isAssignableFrom(val.getClass())) {
				// we just checked it!
				@SuppressWarnings("unchecked")
				E myE = (E) val;
				return myE;
			}
			return Enum.valueOf(clazz, val.toString());
		} catch (IllegalArgumentException e) {
			return defaultValue;
		} catch (NullPointerException e) {
			return defaultValue;
		}
	}

	public BigInteger optBigInteger(int index, BigInteger defaultValue) {
		try {
			return this.getBigInteger(index);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public BigDecimal optBigDecimal(int index, BigDecimal defaultValue) {
		try {
			return this.getBigDecimal(index);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public JsonList optJsonList(int index) {
		Object o = this.opt(index);
		return o instanceof JsonList ? (JsonList) o : null;
	}

	public JsonMap optJsonMap(int index) {
		Object o = this.opt(index);
		return o instanceof JsonMap ? (JsonMap) o : null;
	}

	public long optLong(int index) {
		return this.optLong(index, 0);
	}

	public long optLong(int index, long defaultValue) {
		try {
			return this.getLong(index);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public String optString(int index) {
		return this.optString(index, "");
	}

	public String optString(int index, String defaultValue) {
		Object object = this.opt(index);
		return JsonMap.NULL.equals(object) ? defaultValue : object.toString();
	}

	public JsonList put(boolean value) {
		this.put(value ? Boolean.TRUE : Boolean.FALSE);
		return this;
	}

	public JsonList put(Collection<?> value) {
		this.put(new JsonList(value));
		return this;
	}

	public JsonList put(double value) throws JsonException {
		JsonMap.testValidity(Double.valueOf(value));
		this.put(Double.valueOf(value));
		return this;
	}

	public JsonList put(int value) {
		this.put(Integer.valueOf(value));
		return this;
	}

	public JsonList put(long value) {
		this.put(Long.valueOf(value));
		return this;
	}

	public JsonList put(Map<?, ?> value) {
		this.put(new JsonMap(value));
		return this;
	}

	public JsonList put(Object value) {
		this.myArrayList.add(value);
		return this;
	}

	public JsonList put(int index, boolean value) throws JsonException {
		this.put(index, value ? Boolean.TRUE : Boolean.FALSE);
		return this;
	}

	public JsonList put(int index, Collection<?> value) throws JsonException {
		this.put(index, new JsonList(value));
		return this;
	}

	public JsonList put(int index, double value) throws JsonException {
		this.put(index, Double.valueOf(value));
		return this;
	}

	public JsonList put(int index, int value) throws JsonException {
		this.put(index, Integer.valueOf(value));
		return this;
	}

	public JsonList put(int index, long value) throws JsonException {
		this.put(index, Long.valueOf(value));
		return this;
	}

	public JsonList put(int index, Map<?, ?> value) throws JsonException {
		this.put(index, new JsonMap(value));
		return this;
	}

	public JsonList put(int index, Object value) throws JsonException {
		JsonMap.testValidity(value);
		if (index < 0) {
			throw new JsonException("[" + index + "] not found.");
		}
		if (index < this.length()) {
			this.myArrayList.set(index, value);
		} else {
			while (index != this.length()) {
				this.put(JsonMap.NULL);
			}
			this.put(value);
		}
		return this;
	}

	public Object query(String jsonPointer) {
		return new JsonPointer(jsonPointer).queryFrom(this);
	}

	public Object optQuery(String jsonPointer) {
		JsonPointer pointer = new JsonPointer(jsonPointer);
		try {
			return pointer.queryFrom(this);
		} catch (JsonPointerException e) {
			return null;
		}
	}

	public Object remove(int index) {
		return index >= 0 && index < this.length() ? this.myArrayList.remove(index) : null;
	}

	public boolean similar(Object other) {
		if (!(other instanceof JsonList)) {
			return false;
		}
		int len = this.length();
		if (len != ((JsonList) other).length()) {
			return false;
		}
		for (int i = 0; i < len; i += 1) {
			Object valueThis = this.get(i);
			Object valueOther = ((JsonList) other).get(i);
			if (valueThis instanceof JsonMap) {
				if (!((JsonMap) valueThis).similar(valueOther)) {
					return false;
				}
			} else if (valueThis instanceof JsonList) {
				if (!((JsonList) valueThis).similar(valueOther)) {
					return false;
				}
			} else if (!valueThis.equals(valueOther)) {
				return false;
			}
		}
		return true;
	}
	
	public JsonMap toJsonMap() {
		return this.toJsonMap(new String[0]);
	}
	
	public JsonMap toJsonMap(String... matchJsonMapKeys) {
		JsonMap output = new JsonMap();
		int count = 0;
		for(Object item : this.toArray()) {
			if(item.getClass().getName().equals(JsonMap.class.getName()) && matchJsonMapKeys.length > 0) {
				boolean found = false;
				for(int i = 0; i < matchJsonMapKeys.length; i++) {
					if(((JsonMap) item).has(matchJsonMapKeys[i])) {
						String keyValue = String.valueOf(((JsonMap) item).get(matchJsonMapKeys[i]));
						if(keyValue.trim().length() > 0) {
							output.put(keyValue, item);
						}else {
							output.put(String.valueOf(count), item);
						}						
						i = matchJsonMapKeys.length;
						found = true;
					}
				}
				if(!found) {
					output.put(String.valueOf(count), item);
				}
			}else {
				output.put(String.valueOf(count), item);
			}
			count = count + 1;
		}
		return output;
	}
	
	public JsonMap toJsonMap(JsonList names) throws JsonException {
		if (names == null || names.length() == 0 || this.length() == 0) {
			return null;
		}
		JsonMap jo = new JsonMap();
		for (int i = 0; i < names.length(); i += 1) {
			jo.put(names.getString(i), this.opt(i));
		}
		return jo;
	}

	public String toString() {
		try {
			return this.toString(0);
		} catch (Exception e) {
			return null;
		}
	}

	public String toString(int indentFactor) throws JsonException {
		StringWriter sw = new StringWriter();
		synchronized (sw.getBuffer()) {
			return this.write(sw, indentFactor, 0).toString();
		}
	}

	public Writer write(Writer writer) throws JsonException {
		return this.write(writer, 0, 0);
	}

	public Writer write(Writer writer, int indentFactor, int indent) throws JsonException {
		try {
			boolean commanate = false;
			int length = this.length();
			writer.write('[');

			if (length == 1) {
				JsonMap.writeValue(writer, this.myArrayList.get(0), indentFactor, indent);
			} else if (length != 0) {
				final int newindent = indent + indentFactor;

				for (int i = 0; i < length; i += 1) {
					if (commanate) {
						writer.write(',');
					}
					if (indentFactor > 0) {
						writer.write('\n');
					}
					JsonMap.indent(writer, newindent);
					JsonMap.writeValue(writer, this.myArrayList.get(i), indentFactor, newindent);
					commanate = true;
				}
				if (indentFactor > 0) {
					writer.write('\n');
				}
				JsonMap.indent(writer, indent);
			}
			writer.write(']');
			return writer;
		} catch (IOException e) {
			throw new JsonException(e);
		}
	}

	public List<Object> toList() {
		List<Object> results = new ArrayList<Object>(this.myArrayList.size());
		for (Object element : this.myArrayList) {
			if (element == null || JsonMap.NULL.equals(element)) {
				results.add(null);
			} else if (element instanceof JsonList) {
				results.add(((JsonList) element).toList());
			} else if (element instanceof JsonMap) {
				results.add(((JsonMap) element).toMap());
			} else {
				results.add(element);
			}
		}
		return results;
	}

	public Object[] toArray() {
		Object[] output = new Object[length()];
		if (length() > 0) {
			for (int i = 0; i < length(); i = i + 1) {
				output[i] = get(i);
			}
		} else {
			output = null;
		}
		return output;
	}

	@SuppressWarnings("unchecked")
	public <T> T[] toArray(Class<T> array_type) {
		T[] output = null;
		if (length() > 0) {
			output = (T[]) Array.newInstance(array_type, length());
			for (int i = 0; i < length(); i = i + 1) {
				output[i] = (T) get(i);
			}
		} else {
			output = null;
		}
		return output;
	}
	
	public JsonList clone() throws JsonException{
		if(!this.isNull()) {
			return new JsonList(this.toString());
		}else {
			throw new JsonException("Cannot clone a null JsonList");
		}		
	}
}
