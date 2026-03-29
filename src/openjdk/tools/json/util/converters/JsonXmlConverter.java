package openjdk.tools.json.util.converters;

import java.util.Iterator;
import openjdk.tools.json.JsonList;
import openjdk.tools.json.JsonMap;
import openjdk.tools.json.exceptions.JsonException;
import openjdk.tools.json.internal.tokens.JsonXmlTokener;

/**
 * This provides static methods to convert an XMLConverter text into a JsonList
 * or JsonMap, and to covert a JsonList or JsonMap into an XMLConverter text
 * using the JsonML transform.
 */
public class JsonXmlConverter {
	/**
	 * Parse XMLConverter values and store them in a JsonList.
	 * 
	 * @param x           The XMLTokener containing the source string.
	 * @param arrayForm   true if array form, false if object form.
	 * @param ja          The JsonList that is containing the current tag or null if
	 *                    we are at the outermost level.
	 * @param keepStrings Don't type-convert text nodes and attribute values
	 * @return A JsonList if the value is the outermost tag, otherwise null.
	 * @throws JsonException
	 */
	private static Object parse(JsonXmlTokener x, boolean arrayForm, JsonList ja, boolean keepStrings)
			throws JsonException {
		String attribute;
		char c;
		String closeTag = null;
		int i;
		JsonList newja = null;
		JsonMap newjo = null;
		Object token;
		String tagName = null;
		while (true) {
			if (!x.more()) {
				throw x.syntaxError("Bad XMLConverter");
			}
			token = x.nextContent();
			if (token == JsonXmlTextConverter.LT) {
				token = x.nextToken();
				if (token instanceof Character) {
					if (token == JsonXmlTextConverter.SLASH) {
						token = x.nextToken();
						if (!(token instanceof String)) {
							throw new JsonException("Expected a closing name instead of '" + token + "'.");
						}
						if (x.nextToken() != JsonXmlTextConverter.GT) {
							throw x.syntaxError("Misshaped close tag");
						}
						return token;
					} else if (token == JsonXmlTextConverter.BANG) {
						c = x.next();
						if (c == '-') {
							if (x.next() == '-') {
								x.skipPast("-->");
							} else {
								x.back();
							}
						} else if (c == '[') {
							token = x.nextToken();
							if (token.equals("CDATA") && x.next() == '[') {
								if (ja != null) {
									ja.put(x.nextCDATA());
								}
							} else {
								throw x.syntaxError("Expected 'CDATA['");
							}
						} else {
							i = 1;
							do {
								token = x.nextMeta();
								if (token == null) {
									throw x.syntaxError("Missing '>' after '<!'.");
								} else if (token == JsonXmlTextConverter.LT) {
									i += 1;
								} else if (token == JsonXmlTextConverter.GT) {
									i -= 1;
								}
							} while (i > 0);
						}
					} else if (token == JsonXmlTextConverter.QUEST) {
						x.skipPast("?>");
					} else {
						throw x.syntaxError("Misshaped tag");
					}
				} else {
					if (!(token instanceof String)) {
						throw x.syntaxError("Bad tagName '" + token + "'.");
					}
					tagName = (String) token;
					newja = new JsonList();
					newjo = new JsonMap();
					if (arrayForm) {
						newja.put(tagName);
						if (ja != null) {
							ja.put(newja);
						}
					} else {
						newjo.put("tagName", tagName);
						if (ja != null) {
							ja.put(newjo);
						}
					}
					token = null;
					for (;;) {
						if (token == null) {
							token = x.nextToken();
						}
						if (token == null) {
							throw x.syntaxError("Misshaped tag");
						}
						if (!(token instanceof String)) {
							break;
						}
						attribute = (String) token;
						if (!arrayForm && ("tagName".equals(attribute) || "childNode".equals(attribute))) {
							throw x.syntaxError("Reserved attribute.");
						}
						token = x.nextToken();
						if (token == JsonXmlTextConverter.EQ) {
							token = x.nextToken();
							if (!(token instanceof String)) {
								throw x.syntaxError("Missing value");
							}
							newjo.accumulate(attribute, keepStrings ? token : JsonMap.stringToValue((String) token));
							token = null;
						} else {
							newjo.accumulate(attribute, "");
						}
					}
					if (arrayForm && newjo.length() > 0) {
						newja.put(newjo);
					}
					if (token == JsonXmlTextConverter.SLASH) {
						if (x.nextToken() != JsonXmlTextConverter.GT) {
							throw x.syntaxError("Misshaped tag");
						}
						if (ja == null) {
							if (arrayForm) {
								return newja;
							}
							return newjo;
						}
					} else {
						if (token != JsonXmlTextConverter.GT) {
							throw x.syntaxError("Misshaped tag");
						}
						closeTag = (String) parse(x, arrayForm, newja, keepStrings);
						if (closeTag != null) {
							if (!closeTag.equals(tagName)) {
								throw x.syntaxError("Mismatched '" + tagName + "' and '" + closeTag + "'");
							}
							tagName = null;
							if (!arrayForm && newja.length() > 0) {
								newjo.put("childNodes", newja);
							}
							if (ja == null) {
								if (arrayForm) {
									return newja;
								}
								return newjo;
							}
						}
					}
				}
			} else {
				if (ja != null) {
					ja.put(token instanceof String ? keepStrings ? token : JsonMap.stringToValue((String) token)
							: token);
				}
			}
		}
	}

	/**
	 * Convert a well-formed (but not necessarily valid) XMLConverter string into a
	 * JsonList using the JsonML transform. Each XMLConverter tag is represented as
	 * a JsonList in which the first element is the tag name. If the tag has
	 * attributes, then the second element will be JsonMap containing the name/value
	 * pairs. If the tag contains children, then strings and JsonLists will
	 * represent the child tags. Comments, prologs, DTDs, and
	 * <code>&lt;[ [ ]]></code> are ignored.
	 * 
	 * @param string The source string.
	 * @return A JsonList containing the structured data from the XMLConverter
	 *         string.
	 * @throws JsonException Thrown on error converting to a JsonList
	 */
	public static JsonList toJsonList(String string) throws JsonException {
		return (JsonList) parse(new JsonXmlTokener(string), true, null, false);
	}

	/**
	 * Convert a well-formed (but not necessarily valid) XMLConverter string into a
	 * JsonList using the JsonML transform. Each XMLConverter tag is represented as
	 * a JsonList in which the first element is the tag name. If the tag has
	 * attributes, then the second element will be JsonMap containing the name/value
	 * pairs. If the tag contains children, then strings and JsonLists will
	 * represent the child tags. As opposed to toJsonList this method does not
	 * attempt to convert any text node or attribute value to any type but just
	 * leaves it as a string. Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code>
	 * are ignored.
	 * 
	 * @param string      The source string.
	 * @param keepStrings If true, then values will not be coerced into boolean or
	 *                    numeric values and will instead be left as strings
	 * @return A JsonList containing the structured data from the XMLConverter
	 *         string.
	 * @throws JsonException Thrown on error converting to a JsonList
	 */
	public static JsonList toJsonList(String string, boolean keepStrings) throws JsonException {
		return (JsonList) parse(new JsonXmlTokener(string), true, null, keepStrings);
	}

	/**
	 * Convert a well-formed (but not necessarily valid) XMLConverter string into a
	 * JsonList using the JsonML transform. Each XMLConverter tag is represented as
	 * a JsonList in which the first element is the tag name. If the tag has
	 * attributes, then the second element will be JsonMap containing the name/value
	 * pairs. If the tag contains children, then strings and JsonLists will
	 * represent the child content and tags. As opposed to toJsonList this method
	 * does not attempt to convert any text node or attribute value to any type but
	 * just leaves it as a string. Comments, prologs, DTDs, and
	 * <code>&lt;[ [ ]]></code> are ignored.
	 * 
	 * @param x           An XMLTokener.
	 * @param keepStrings If true, then values will not be coerced into boolean or
	 *                    numeric values and will instead be left as strings
	 * @return A JsonList containing the structured data from the XMLConverter
	 *         string.
	 * @throws JsonException Thrown on error converting to a JsonList
	 */
	public static JsonList toJsonList(JsonXmlTokener x, boolean keepStrings) throws JsonException {
		return (JsonList) parse(x, true, null, keepStrings);
	}

	/**
	 * Convert a well-formed (but not necessarily valid) XMLConverter string into a
	 * JsonList using the JsonML transform. Each XMLConverter tag is represented as
	 * a JsonList in which the first element is the tag name. If the tag has
	 * attributes, then the second element will be JsonMap containing the name/value
	 * pairs. If the tag contains children, then strings and JsonLists will
	 * represent the child content and tags. Comments, prologs, DTDs, and
	 * <code>&lt;[ [ ]]></code> are ignored.
	 * 
	 * @param x An XMLTokener.
	 * @return A JsonList containing the structured data from the XMLConverter
	 *         string.
	 * @throws JsonException Thrown on error converting to a JsonList
	 */
	public static JsonList toJsonList(JsonXmlTokener x) throws JsonException {
		return (JsonList) parse(x, true, null, false);
	}

	/**
	 * Convert a well-formed (but not necessarily valid) XMLConverter string into a
	 * JsonMap using the JsonML transform. Each XMLConverter tag is represented as a
	 * JsonMap with a "tagName" property. If the tag has attributes, then the
	 * attributes will be in the JsonMap as properties. If the tag contains
	 * children, the object will have a "childNodes" property which will be an array
	 * of strings and JsonML JsonMaps.
	 * 
	 * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
	 * 
	 * @param string The XMLConverter source text.
	 * @return A JsonMap containing the structured data from the XMLConverter
	 *         string.
	 * @throws JsonException Thrown on error converting to a JsonMap
	 */
	public static JsonMap toJsonMap(String string) throws JsonException {
		return (JsonMap) parse(new JsonXmlTokener(string), false, null, false);
	}

	/**
	 * Convert a well-formed (but not necessarily valid) XMLConverter string into a
	 * JsonMap using the JsonML transform. Each XMLConverter tag is represented as a
	 * JsonMap with a "tagName" property. If the tag has attributes, then the
	 * attributes will be in the JsonMap as properties. If the tag contains
	 * children, the object will have a "childNodes" property which will be an array
	 * of strings and JsonML JsonMaps.
	 * 
	 * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
	 * 
	 * @param string      The XMLConverter source text.
	 * @param keepStrings If true, then values will not be coerced into boolean or
	 *                    numeric values and will instead be left as strings
	 * @return A JsonMap containing the structured data from the XMLConverter
	 *         string.
	 * @throws JsonException Thrown on error converting to a JsonMap
	 */
	public static JsonMap toJsonMap(String string, boolean keepStrings) throws JsonException {
		return (JsonMap) parse(new JsonXmlTokener(string), false, null, keepStrings);
	}

	/**
	 * Convert a well-formed (but not necessarily valid) XMLConverter string into a
	 * JsonMap using the JsonML transform. Each XMLConverter tag is represented as a
	 * JsonMap with a "tagName" property. If the tag has attributes, then the
	 * attributes will be in the JsonMap as properties. If the tag contains
	 * children, the object will have a "childNodes" property which will be an array
	 * of strings and JsonML JsonMaps.
	 * 
	 * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
	 * 
	 * @param x An XMLTokener of the XMLConverter source text.
	 * @return A JsonMap containing the structured data from the XMLConverter
	 *         string.
	 * @throws JsonException Thrown on error converting to a JsonMap
	 */
	public static JsonMap toJsonMap(JsonXmlTokener x) throws JsonException {
		return (JsonMap) parse(x, false, null, false);
	}

	/**
	 * Convert a well-formed (but not necessarily valid) XMLConverter string into a
	 * JsonMap using the JsonML transform. Each XMLConverter tag is represented as a
	 * JsonMap with a "tagName" property. If the tag has attributes, then the
	 * attributes will be in the JsonMap as properties. If the tag contains
	 * children, the object will have a "childNodes" property which will be an array
	 * of strings and JsonML JsonMaps.
	 * 
	 * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
	 * 
	 * @param x           An XMLTokener of the XMLConverter source text.
	 * @param keepStrings If true, then values will not be coerced into boolean or
	 *                    numeric values and will instead be left as strings
	 * @return A JsonMap containing the structured data from the XMLConverter
	 *         string.
	 * @throws JsonException Thrown on error converting to a JsonMap
	 */
	public static JsonMap toJsonMap(JsonXmlTokener x, boolean keepStrings) throws JsonException {
		return (JsonMap) parse(x, false, null, keepStrings);
	}

	/**
	 * Reverse the JSONMLConverter transformation, making an XMLConverter text from
	 * a JsonList.
	 * 
	 * @param ja A JsonList.
	 * @return An XMLConverter string.
	 * @throws JsonException Thrown on error converting to a string
	 */
	public static String toString(JsonList ja) throws JsonException {
		int i;
		JsonMap jo;
		String key;
		Iterator<String> keys;
		int length;
		Object object;
		StringBuilder sb = new StringBuilder();
		String tagName;
		String value;
		tagName = ja.getString(0);
		JsonXmlTextConverter.noSpace(tagName);
		tagName = JsonXmlTextConverter.escape(tagName);
		sb.append('<');
		sb.append(tagName);

		object = ja.opt(1);
		if (object instanceof JsonMap) {
			i = 2;
			jo = (JsonMap) object;
			keys = jo.keys();
			while (keys.hasNext()) {
				key = keys.next();
				JsonXmlTextConverter.noSpace(key);
				value = jo.optString(key);
				if (value != null) {
					sb.append(' ');
					sb.append(JsonXmlTextConverter.escape(key));
					sb.append('=');
					sb.append('"');
					sb.append(JsonXmlTextConverter.escape(value));
					sb.append('"');
				}
			}
		} else {
			i = 1;
		}
		length = ja.length();
		if (i >= length) {
			sb.append('/');
			sb.append('>');
		} else {
			sb.append('>');
			do {
				object = ja.get(i);
				i += 1;
				if (object != null) {
					if (object instanceof String) {
						sb.append(JsonXmlTextConverter.escape(object.toString()));
					} else if (object instanceof JsonMap) {
						sb.append(toString((JsonMap) object));
					} else if (object instanceof JsonList) {
						sb.append(toString((JsonList) object));
					} else {
						sb.append(object.toString());
					}
				}
			} while (i < length);
			sb.append('<');
			sb.append('/');
			sb.append(tagName);
			sb.append('>');
		}
		return sb.toString();
	}

	/**
	 * Reverse the JSONMLConverter transformation, making an XMLConverter text from
	 * a JsonMap. The JsonMap must contain a "tagName" property. If it has children,
	 * then it must have a "childNodes" property containing an array of objects. The
	 * other properties are attributes with string values.
	 * 
	 * @param jo A JsonMap.
	 * @return An XMLConverter string.
	 * @throws JsonException Thrown on error converting to a string
	 */
	public static String toString(JsonMap jo) throws JsonException {
		StringBuilder sb = new StringBuilder();
		int i;
		JsonList ja;
		String key;
		Iterator<String> keys;
		int length;
		Object object;
		String tagName;
		String value;
		tagName = jo.optString("tagName");
		if (tagName == null) {
			return JsonXmlTextConverter.escape(jo.toString());
		}
		JsonXmlTextConverter.noSpace(tagName);
		tagName = JsonXmlTextConverter.escape(tagName);
		sb.append('<');
		sb.append(tagName);
		keys = jo.keys();
		while (keys.hasNext()) {
			key = keys.next();
			if (!"tagName".equals(key) && !"childNodes".equals(key)) {
				JsonXmlTextConverter.noSpace(key);
				value = jo.optString(key);
				if (value != null) {
					sb.append(' ');
					sb.append(JsonXmlTextConverter.escape(key));
					sb.append('=');
					sb.append('"');
					sb.append(JsonXmlTextConverter.escape(value));
					sb.append('"');
				}
			}
		}
		ja = jo.optJsonList("childNodes");
		if (ja == null) {
			sb.append('/');
			sb.append('>');
		} else {
			sb.append('>');
			length = ja.length();
			for (i = 0; i < length; i += 1) {
				object = ja.get(i);
				if (object != null) {
					if (object instanceof String) {
						sb.append(JsonXmlTextConverter.escape(object.toString()));
					} else if (object instanceof JsonMap) {
						sb.append(toString((JsonMap) object));
					} else if (object instanceof JsonList) {
						sb.append(toString((JsonList) object));
					} else {
						sb.append(object.toString());
					}
				}
			}
			sb.append('<');
			sb.append('/');
			sb.append(tagName);
			sb.append('>');
		}
		return sb.toString();
	}
}
