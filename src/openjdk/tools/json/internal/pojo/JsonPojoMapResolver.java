package openjdk.tools.json.internal.pojo;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public class JsonPojoMapResolver extends JsonPojoResolver {
	protected JsonPojoMapResolver(JsonPojoReader reader) {
		super(reader);
	}

	@SuppressWarnings("rawtypes")
	protected Object readIfMatching(Object o, Class compType, Deque<JsonPojoElement<String, Object>> stack) {
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void traverseFields(final Deque<JsonPojoElement<String, Object>> stack,
			final JsonPojoElement<String, Object> jsonObj) {
		final Object target = jsonObj.target;
		for (Map.Entry<String, Object> e : jsonObj.entrySet()) {
			final String fieldName = e.getKey();
			final Field field = (target != null) ? JsonPojoMetaUtils.getField(target.getClass(), fieldName) : null;
			final Object rhs = e.getValue();

			if (rhs == null) {
				jsonObj.put(fieldName, null);
			} else if (rhs == JsonPojoParser.EMPTY_OBJECT) {
				jsonObj.put(fieldName, new JsonPojoElement());
			} else if (rhs.getClass().isArray()) {
				JsonPojoElement<String, Object> jsonArray = new JsonPojoElement<String, Object>();
				jsonArray.put("@items", rhs);
				stack.addFirst(jsonArray);
				jsonObj.put(fieldName, rhs);
			} else if (rhs instanceof JsonPojoElement) {
				JsonPojoElement<String, Object> jObj = (JsonPojoElement) rhs;

				if (field != null && JsonPojoMetaUtils.isLogicalPrimitive(field.getType())) {
					jObj.put("value", JsonPojoMetaUtils.convert(field.getType(), jObj.get("value")));
					continue;
				}
				Long refId = jObj.getReferenceId();

				if (refId != null) {
					JsonPojoElement refObject = getReferencedObj(refId);
					jsonObj.put(fieldName, refObject);
				} else {
					stack.addFirst(jObj);
				}
			} else if (field != null) {
				final Class fieldType = field.getType();
				if (JsonPojoMetaUtils.isPrimitive(fieldType) || BigDecimal.class.equals(fieldType)
						|| BigInteger.class.equals(fieldType) || Date.class.equals(fieldType)) {
					jsonObj.put(fieldName, JsonPojoMetaUtils.convert(fieldType, rhs));
				} else if (rhs instanceof String) {
					if (fieldType != String.class && fieldType != StringBuilder.class
							&& fieldType != StringBuffer.class) {
						if ("".equals(((String) rhs).trim())) {
							jsonObj.put(fieldName, null);
						}
					}
				}
			}
		}
		jsonObj.target = null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void traverseCollection(final Deque<JsonPojoElement<String, Object>> stack,
			final JsonPojoElement<String, Object> jsonObj) {
		final Object[] items = jsonObj.getArray();
		if (items == null || items.length == 0) {
			return;
		}

		int idx = 0;
		final List copy = new ArrayList(items.length);

		for (Object element : items) {
			if (element == JsonPojoParser.EMPTY_OBJECT) {
				copy.add(new JsonPojoElement());
				continue;
			}

			copy.add(element);

			if (element instanceof Object[]) {
				JsonPojoElement<String, Object> jsonObject = new JsonPojoElement<String, Object>();
				jsonObject.put("@items", element);
				stack.addFirst(jsonObject);
			} else if (element instanceof JsonPojoElement) {
				JsonPojoElement<String, Object> jsonObject = (JsonPojoElement<String, Object>) element;
				Long refId = jsonObject.getReferenceId();

				if (refId != null) {
					JsonPojoElement refObject = getReferencedObj(refId);
					copy.set(idx, refObject);
				} else {
					stack.addFirst(jsonObject);
				}
			}
			idx++;
		}
		jsonObj.target = null;

		for (int i = 0; i < items.length; i++) {
			items[i] = copy.get(i);
		}
	}

	protected void traverseArray(Deque<JsonPojoElement<String, Object>> stack,
			JsonPojoElement<String, Object> jsonObj) {
		traverseCollection(stack, jsonObj);
	}
}
