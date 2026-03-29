package openjdk.tools.json.internal.pojo;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import openjdk.tools.json.exceptions.JsonInputOutputException;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class JsonPojoObjectResolver extends JsonPojoResolver {
	private final ClassLoader classLoader;
	protected JsonPojoReader.MissingFieldHandler missingFieldHandler;

	protected JsonPojoObjectResolver(JsonPojoReader reader, ClassLoader classLoader) {
		super(reader);
		this.classLoader = classLoader;
		missingFieldHandler = reader.getMissingFieldHandler();
	}

	public void traverseFields(final Deque<JsonPojoElement<String, Object>> stack,
			final JsonPojoElement<String, Object> jsonObj) {
		final Object javaMate = jsonObj.target;
		final Iterator<Map.Entry<String, Object>> i = jsonObj.entrySet().iterator();
		final Class cls = javaMate.getClass();

		while (i.hasNext()) {
			Map.Entry<String, Object> e = i.next();
			String key = e.getKey();
			final Field field = JsonPojoMetaUtils.getField(cls, key);
			Object rhs = e.getValue();
			if (field != null) {
				assignField(stack, jsonObj, field, rhs);
			} else if (missingFieldHandler != null) {
				handleMissingField(stack, jsonObj, rhs, key);
			}
		}
	}

	protected void assignField(final Deque<JsonPojoElement<String, Object>> stack, final JsonPojoElement jsonObj,
			final Field field, final Object rhs) {
		final Object target = jsonObj.target;
		try {
			final Class fieldType = field.getType();
			if (rhs == null) {
				if (fieldType.isPrimitive()) {
					field.set(target, JsonPojoMetaUtils.convert(fieldType, "0"));
				} else {
					field.set(target, null);
				}
				return;
			}
			if (rhs instanceof JsonPojoElement) {
				if (field.getGenericType() instanceof ParameterizedType) {
					markUntypedObjects(field.getGenericType(), rhs, JsonPojoMetaUtils.getDeepDeclaredFields(fieldType));
				}
				final JsonPojoElement job = (JsonPojoElement) rhs;
				final String type = job.type;
				if (type == null || type.isEmpty()) {
					job.setType(fieldType.getName());
				}
			}

			Object special;
			if (rhs == JsonPojoParser.EMPTY_OBJECT) {
				final JsonPojoElement jObj = new JsonPojoElement();
				jObj.type = fieldType.getName();
				Object value = createJavaObjectInstance(fieldType, jObj);
				field.set(target, value);
			} else if ((special = readIfMatching(rhs, fieldType, stack)) != null) {
				field.set(target, special);
			} else if (rhs.getClass().isArray()) {
				final Object[] elements = (Object[]) rhs;
				JsonPojoElement<String, Object> jsonArray = new JsonPojoElement<String, Object>();
				if (char[].class == fieldType) {
					if (elements.length == 0) {
						field.set(target, new char[] {});
					} else {
						field.set(target, ((String) elements[0]).toCharArray());
					}
				} else {
					jsonArray.put("@items", elements);
					createJavaObjectInstance(fieldType, jsonArray);
					field.set(target, jsonArray.target);
					stack.addFirst(jsonArray);
				}
			} else if (rhs instanceof JsonPojoElement) {
				final JsonPojoElement<String, Object> jObj = (JsonPojoElement) rhs;
				final Long ref = jObj.getReferenceId();

				if (ref != null) {
					final JsonPojoElement refObject = getReferencedObj(ref);

					if (refObject.target != null) {
						field.set(target, refObject.target);
					} else {
						unresolvedRefs.add(new UnresolvedReference(jsonObj, field.getName(), ref));
					}
				} else {
					field.set(target, createJavaObjectInstance(fieldType, jObj));
					if (!JsonPojoMetaUtils.isLogicalPrimitive(jObj.getTargetClass())) {
						stack.addFirst((JsonPojoElement) rhs);
					}
				}
			} else {
				if (JsonPojoMetaUtils.isPrimitive(fieldType)) {
					field.set(target, JsonPojoMetaUtils.convert(fieldType, rhs));
				} else if (rhs instanceof String && "".equals(((String) rhs).trim()) && fieldType != String.class) {
					field.set(target, null);
				} else {
					field.set(target, rhs);
				}
			}
		} catch (Exception e) {
			String message = e.getClass().getSimpleName() + " setting field '" + field.getName() + "' on target: "
					+ safeToString(target) + " with value: " + rhs;
			if (JsonPojoMetaUtils.loadClassException != null) {
				message += " Caused by: " + JsonPojoMetaUtils.loadClassException
						+ " (which created a LinkedHashMap instead of the desired class)";
			}
			throw new JsonInputOutputException(message, e);
		}
	}

	protected void handleMissingField(final Deque<JsonPojoElement<String, Object>> stack, final JsonPojoElement jsonObj,
			final Object rhs, final String missingField) {
		final Object target = jsonObj.target;
		try {
			if (rhs == null) {
				storeMissingField(target, missingField, null);
				return;
			}
			Object special;
			if (rhs == JsonPojoParser.EMPTY_OBJECT) {
				storeMissingField(target, missingField, null);
			} else if ((special = readIfMatching(rhs, null, stack)) != null) {
				storeMissingField(target, missingField, special);
			} else if (rhs.getClass().isArray()) {
				storeMissingField(target, missingField, null);
			} else if (rhs instanceof JsonPojoElement) {
				final JsonPojoElement<String, Object> jObj = (JsonPojoElement) rhs;
				final Long ref = jObj.getReferenceId();

				if (ref != null) {
					final JsonPojoElement refObject = getReferencedObj(ref);
					storeMissingField(target, missingField, refObject.target);
				} else {
					if (jObj.getType() != null) {
						Object createJavaObjectInstance = createJavaObjectInstance(null, jObj);
						if (!JsonPojoMetaUtils.isLogicalPrimitive(jObj.getTargetClass())) {
							stack.addFirst((JsonPojoElement) rhs);
						}
						storeMissingField(target, missingField, createJavaObjectInstance);
					} else {
						storeMissingField(target, missingField, null);
					}
				}
			} else {
				storeMissingField(target, missingField, rhs);
			}
		} catch (Exception e) {
			String message = e.getClass().getSimpleName() + " missing field '" + missingField + "' on target: "
					+ safeToString(target) + " with value: " + rhs;
			if (JsonPojoMetaUtils.loadClassException != null) {
				message += " Caused by: " + JsonPojoMetaUtils.loadClassException
						+ " (which created a LinkedHashMap instead of the desired class)";
			}
			throw new JsonInputOutputException(message, e);
		}
	}

	private void storeMissingField(Object target, String missingField, Object value) {
		missingFields.add(new Missingfields(target, missingField, value));
	}

	private static String safeToString(Object o) {
		if (o == null) {
			return "null";
		}
		try {
			return o.toString();
		} catch (Exception e) {
			return o.getClass().toString();
		}
	}

	protected void traverseCollection(final Deque<JsonPojoElement<String, Object>> stack,
			final JsonPojoElement<String, Object> jsonObj) {
		final Object[] items = jsonObj.getArray();
		if (items == null || items.length == 0) {
			return;
		}
		final Collection col = (Collection) jsonObj.target;
		final boolean isList = col instanceof List;
		int idx = 0;

		for (final Object element : items) {
			Object special;
			if (element == null) {
				col.add(null);
			} else if (element == JsonPojoParser.EMPTY_OBJECT) {
				col.add(new JsonPojoElement());
			} else if ((special = readIfMatching(element, null, stack)) != null) {
				col.add(special);
			} else if (element instanceof String || element instanceof Boolean || element instanceof Double
					|| element instanceof Long) {
				col.add(element);
			} else if (element.getClass().isArray()) {
				final JsonPojoElement jObj = new JsonPojoElement();
				jObj.put("@items", element);
				createJavaObjectInstance(Object.class, jObj);
				col.add(jObj.target);
				convertMapsToObjects(jObj);
			} else {
				final JsonPojoElement jObj = (JsonPojoElement) element;
				final Long ref = jObj.getReferenceId();

				if (ref != null) {
					JsonPojoElement refObject = getReferencedObj(ref);

					if (refObject.target != null) {
						col.add(refObject.target);
					} else {
						unresolvedRefs.add(new UnresolvedReference(jsonObj, idx, ref));
						if (isList) {
							col.add(null);
						}
					}
				} else {
					createJavaObjectInstance(Object.class, jObj);

					if (!JsonPojoMetaUtils.isLogicalPrimitive(jObj.getTargetClass())) {
						convertMapsToObjects(jObj);
					}
					col.add(jObj.target);
				}
			}
			idx++;
		}

		jsonObj.remove("@items");
	}

	protected void traverseArray(final Deque<JsonPojoElement<String, Object>> stack,
			final JsonPojoElement<String, Object> jsonObj) {
		final int len = jsonObj.getLength();
		if (len == 0) {
			return;
		}

		final Class compType = jsonObj.getComponentType();

		if (char.class == compType) {
			return;
		}

		if (byte.class == compType) {
			jsonObj.moveBytesToMate();
			jsonObj.clearArray();
			return;
		}

		final boolean isPrimitive = JsonPojoMetaUtils.isPrimitive(compType);
		final Object array = jsonObj.target;
		final Object[] items = jsonObj.getArray();

		for (int i = 0; i < len; i++) {
			final Object element = items[i];

			Object special;
			if (element == null) {
				Array.set(array, i, null);
			} else if (element == JsonPojoParser.EMPTY_OBJECT) {
				Object arrayElement = createJavaObjectInstance(compType, new JsonPojoElement());
				Array.set(array, i, arrayElement);
			} else if ((special = readIfMatching(element, compType, stack)) != null) {
				Array.set(array, i, special);
			} else if (isPrimitive) {
				Array.set(array, i, JsonPojoMetaUtils.convert(compType, element));
			} else if (element.getClass().isArray()) {
				if (char[].class == compType) {
					Object[] jsonArray = (Object[]) element;
					if (jsonArray.length == 0) {
						Array.set(array, i, new char[] {});
					} else {
						final String value = (String) jsonArray[0];
						final int numChars = value.length();
						final char[] chars = new char[numChars];
						for (int j = 0; j < numChars; j++) {
							chars[j] = value.charAt(j);
						}
						Array.set(array, i, chars);
					}
				} else {
					JsonPojoElement<String, Object> jsonObject = new JsonPojoElement<String, Object>();
					jsonObject.put("@items", element);
					Array.set(array, i, createJavaObjectInstance(compType, jsonObject));
					stack.addFirst(jsonObject);
				}
			} else if (element instanceof JsonPojoElement) {
				JsonPojoElement<String, Object> jsonObject = (JsonPojoElement<String, Object>) element;
				Long ref = jsonObject.getReferenceId();

				if (ref != null) {
					JsonPojoElement refObject = getReferencedObj(ref);
					if (refObject.target != null) {
						Array.set(array, i, refObject.target);
					} else {
						unresolvedRefs.add(new UnresolvedReference(jsonObj, i, ref));
					}
				} else {
					Object arrayElement = createJavaObjectInstance(compType, jsonObject);
					Array.set(array, i, arrayElement);
					if (!JsonPojoMetaUtils.isLogicalPrimitive(arrayElement.getClass())) {
						stack.addFirst(jsonObject);
					}
				}
			} else {
				if (element instanceof String && "".equals(((String) element).trim()) && compType != String.class
						&& compType != Object.class) {
					Array.set(array, i, null);
				} else {
					Array.set(array, i, element);
				}
			}
		}
		jsonObj.clearArray();
	}

	protected Object readIfMatching(final Object o, final Class compType,
			final Deque<JsonPojoElement<String, Object>> stack) {
		if (o == null) {
			throw new JsonInputOutputException("Bug in json-io, null must be checked before calling this method.");
		}

		if (compType != null && notCustom(compType)) {
			return null;
		}

		final boolean isJsonObject = o instanceof JsonPojoElement;
		if (!isJsonObject && compType == null) {
			return null;
		}

		Class c;
		boolean needsType = false;

		if (isJsonObject) {
			JsonPojoElement jObj = (JsonPojoElement) o;
			if (jObj.isReference()) {
				return null;
			}

			if (jObj.target == null) {
				String typeStr = null;
				try {
					Object type = jObj.type;
					if (type != null) {
						typeStr = (String) type;
						c = JsonPojoMetaUtils.classForName((String) type, classLoader);
					} else {
						if (compType != null) {
							c = compType;
							needsType = true;
						} else {
							return null;
						}
					}
					createJavaObjectInstance(c, jObj);
				} catch (Exception e) {
					throw new JsonInputOutputException("Class listed in @type [" + typeStr + "] is not found", e);
				}
			} else {
				c = jObj.target.getClass();
			}
		} else {
			c = compType;
		}

		if (notCustom(c)) {
			return null;
		}

		JsonPojoReader.JsonClassReaderBase closestReader = getCustomReader(c);

		if (closestReader == null) {
			return null;
		}

		if (needsType) {
			((JsonPojoElement) o).setType(c.getName());
		}

		Object read;
		if (closestReader instanceof JsonPojoReader.JsonClassReaderEx) {
			read = ((JsonPojoReader.JsonClassReaderEx) closestReader).read(o, stack, getReader().getArgs());
		} else {
			read = ((JsonPojoReader.JsonClassReader) closestReader).read(o, stack);
		}
		return read;
	}

	private void markUntypedObjects(final Type type, final Object rhs, final Map<String, Field> classFields) {
		final Deque<Object[]> stack = new ArrayDeque<Object[]>();
		stack.addFirst(new Object[] { type, rhs });

		while (!stack.isEmpty()) {
			Object[] item = stack.removeFirst();
			final Type t = (Type) item[0];
			final Object instance = item[1];
			if (t instanceof ParameterizedType) {
				final Class clazz = getRawType(t);
				final ParameterizedType pType = (ParameterizedType) t;
				final Type[] typeArgs = pType.getActualTypeArguments();

				if (typeArgs == null || typeArgs.length < 1 || clazz == null) {
					continue;
				}

				stampTypeOnJsonObject(instance, t);

				if (Map.class.isAssignableFrom(clazz)) {
					Map map = (Map) instance;
					if (!map.containsKey("@keys") && !map.containsKey("@items") && map instanceof JsonPojoElement) {
						convertMapToKeysItems((JsonPojoElement) map);
					}

					Object[] keys = (Object[]) map.get("@keys");
					getTemplateTraverseWorkItem(stack, keys, typeArgs[0]);

					Object[] items = (Object[]) map.get("@items");
					getTemplateTraverseWorkItem(stack, items, typeArgs[1]);
				} else if (Collection.class.isAssignableFrom(clazz)) {
					if (instance instanceof Object[]) {
						Object[] array = (Object[]) instance;
						for (int i = 0; i < array.length; i++) {
							Object vals = array[i];
							stack.addFirst(new Object[] { t, vals });

							if (vals instanceof JsonPojoElement) {
								stack.addFirst(new Object[] { t, vals });
							} else if (vals instanceof Object[]) {
								JsonPojoElement coll = new JsonPojoElement();
								coll.type = clazz.getName();
								List items = Arrays.asList((Object[]) vals);
								coll.put("@items", items.toArray());
								stack.addFirst(new Object[] { t, items });
								array[i] = coll;
							} else {
								stack.addFirst(new Object[] { t, vals });
							}
						}
					} else if (instance instanceof Collection) {
						final Collection col = (Collection) instance;
						for (Object o : col) {
							stack.addFirst(new Object[] { typeArgs[0], o });
						}
					} else if (instance instanceof JsonPojoElement) {
						final JsonPojoElement jObj = (JsonPojoElement) instance;
						final Object[] array = jObj.getArray();
						if (array != null) {
							for (Object o : array) {
								stack.addFirst(new Object[] { typeArgs[0], o });
							}
						}
					}
				} else {
					if (instance instanceof JsonPojoElement) {
						final JsonPojoElement<String, Object> jObj = (JsonPojoElement) instance;

						for (Map.Entry<String, Object> entry : jObj.entrySet()) {
							final String fieldName = entry.getKey();
							if (!fieldName.startsWith("this$")) {
								Field field = classFields.get(fieldName);

								if (field != null && (field.getType().getTypeParameters().length > 0
										|| field.getGenericType() instanceof TypeVariable)) {
									stack.addFirst(new Object[] { typeArgs[0], entry.getValue() });
								}
							}
						}
					}
				}
			} else {
				stampTypeOnJsonObject(instance, t);
			}
		}
	}

	private static void getTemplateTraverseWorkItem(final Deque<Object[]> stack, final Object[] items,
			final Type type) {
		if (items == null || items.length < 1) {
			return;
		}
		Class rawType = getRawType(type);
		if (rawType != null && Collection.class.isAssignableFrom(rawType)) {
			stack.add(new Object[] { type, items });
		} else {
			for (Object o : items) {
				stack.add(new Object[] { type, o });
			}
		}
	}

	private static void stampTypeOnJsonObject(final Object o, final Type t) {
		Class clazz = t instanceof Class ? (Class) t : getRawType(t);

		if (o instanceof JsonPojoElement && clazz != null) {
			JsonPojoElement jObj = (JsonPojoElement) o;
			if ((jObj.type == null || jObj.type.isEmpty()) && jObj.target == null) {
				jObj.type = clazz.getName();
			}
		}
	}

	public static Class getRawType(final Type t) {
		if (t instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType) t;

			if (pType.getRawType() instanceof Class) {
				return (Class) pType.getRawType();
			}
		}
		return null;
	}
}
