package openjdk.tools.json.util.converters;

import java.util.Iterator;
import openjdk.tools.json.JsonList;
import openjdk.tools.json.JsonMap;
import openjdk.tools.json.exceptions.JsonException;
import openjdk.tools.json.internal.tokens.JsonXmlTokener;

/**
 * This provides static methods to convert an XMLConverter text into a JsonMap,
 * and to covert a JsonMap into an XMLConverter text.
 */
public class JsonXmlTextConverter {

	/** The Character '&amp;'. */
	public static final Character AMP = '&';

	/** The Character '''. */
	public static final Character APOS = '\'';

	/** The Character '!'. */
	public static final Character BANG = '!';

	/** The Character '='. */
	public static final Character EQ = '=';

	/** The Character '>'. */
	public static final Character GT = '>';

	/** The Character '&lt;'. */
	public static final Character LT = '<';

	/** The Character '?'. */
	public static final Character QUEST = '?';

	/** The Character '"'. */
	public static final Character QUOT = '"';

	/** The Character '/'. */
	public static final Character SLASH = '/';

	/**
	 * Replace special characters with XMLConverter escapes:
	 * 
	 * <pre>
	 * &amp; <small>(ampersand)</small> is replaced by &amp;amp;
	 * &lt; <small>(less than)</small> is replaced by &amp;lt;
	 * &gt; <small>(greater than)</small> is replaced by &amp;gt;
	 * &quot; <small>(double quote)</small> is replaced by &amp;quot;
	 * </pre>
	 * 
	 * @param string The string to be escaped.
	 * @return The escaped string.
	 */
	public static String escape(String string) {
		StringBuilder sb = new StringBuilder(string.length());
		for (int i = 0, length = string.length(); i < length; i++) {
			char c = string.charAt(i);
			switch (c) {
			case '&':
				sb.append("&amp;");
				break;
			case '<':
				sb.append("&lt;");
				break;
			case '>':
				sb.append("&gt;");
				break;
			case '"':
				sb.append("&quot;");
				break;
			case '\'':
				sb.append("&apos;");
				break;
			default:
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Throw an exception if the string contains whitespace. Whitespace is not
	 * allowed in tagNames and attributes.
	 * 
	 * @param string A string.
	 * @throws JsonException Thrown if the string contains whitespace or is empty.
	 */
	public static void noSpace(String string) throws JsonException {
		int i, length = string.length();
		if (length == 0) {
			throw new JsonException("Empty string.");
		}
		for (i = 0; i < length; i += 1) {
			if (Character.isWhitespace(string.charAt(i))) {
				throw new JsonException("'" + string + "' contains a space character.");
			}
		}
	}

	/**
	 * Scan the content following the named tag, attaching it to the context.
	 * 
	 * @param x       The XMLTokener containing the source string.
	 * @param context The JsonMap that will include the new material.
	 * @param name    The tag name.
	 * @return true if the close tag is processed.
	 * @throws JsonException
	 */
	private static boolean parse(JsonXmlTokener x, JsonMap context, String name, boolean keepStrings)
			throws JsonException {
		char c;
		int i;
		JsonMap jssonMap = null;
		String string;
		String tagName;
		Object token;
		token = x.nextToken();
		if (token == BANG) {
			c = x.next();
			if (c == '-') {
				if (x.next() == '-') {
					x.skipPast("-->");
					return false;
				}
				x.back();
			} else if (c == '[') {
				token = x.nextToken();
				if ("CDATA".equals(token)) {
					if (x.next() == '[') {
						string = x.nextCDATA();
						if (string.length() > 0) {
							context.accumulate("content", string);
						}
						return false;
					}
				}
				throw x.syntaxError("Expected 'CDATA['");
			}
			i = 1;
			do {
				token = x.nextMeta();
				if (token == null) {
					throw x.syntaxError("Missing '>' after '<!'.");
				} else if (token == LT) {
					i += 1;
				} else if (token == GT) {
					i -= 1;
				}
			} while (i > 0);
			return false;
		} else if (token == QUEST) {
			x.skipPast("?>");
			return false;
		} else if (token == SLASH) {
			token = x.nextToken();
			if (name == null) {
				throw x.syntaxError("Mismatched close tag " + token);
			}
			if (!token.equals(name)) {
				throw x.syntaxError("Mismatched " + name + " and " + token);
			}
			if (x.nextToken() != GT) {
				throw x.syntaxError("Misshaped close tag");
			}
			return true;

		} else if (token instanceof Character) {
			throw x.syntaxError("Misshaped tag");
		} else {
			tagName = (String) token;
			token = null;
			jssonMap = new JsonMap();
			for (;;) {
				if (token == null) {
					token = x.nextToken();
				}
				if (token instanceof String) {
					string = (String) token;
					token = x.nextToken();
					if (token == EQ) {
						token = x.nextToken();
						if (!(token instanceof String)) {
							throw x.syntaxError("Missing value");
						}
						jssonMap.accumulate(string, keepStrings ? token : JsonMap.stringToValue((String) token));
						token = null;
					} else {
						jssonMap.accumulate(string, "");
					}

				} else if (token == SLASH) {
					if (x.nextToken() != GT) {
						throw x.syntaxError("Misshaped tag");
					}
					if (jssonMap.length() > 0) {
						context.accumulate(tagName, jssonMap);
					} else {
						context.accumulate(tagName, "");
					}
					return false;

				} else if (token == GT) {
					for (;;) {
						token = x.nextContent();
						if (token == null) {
							if (tagName != null) {
								throw x.syntaxError("Unclosed tag " + tagName);
							}
							return false;
						} else if (token instanceof String) {
							string = (String) token;
							if (string.length() > 0) {
								jssonMap.accumulate("content", keepStrings ? token : JsonMap.stringToValue(string));
							}

						} else if (token == LT) {
							if (parse(x, jssonMap, tagName, keepStrings)) {
								if (jssonMap.length() == 0) {
									context.accumulate(tagName, "");
								} else if (jssonMap.length() == 1 && jssonMap.opt("content") != null) {
									context.accumulate(tagName, jssonMap.opt("content"));
								} else {
									context.accumulate(tagName, jssonMap);
								}
								return false;
							}
						}
					}
				} else {
					throw x.syntaxError("Misshaped tag");
				}
			}
		}
	}

	/**
	 * This method has been deprecated in favor of the
	 * {@link JsonMap.stringToValue(String)} method. Use it instead.
	 * 
	 * @deprecated Use JsonMap#stringToValue(String) instead.
	 * @param string String to convert
	 * @return JSON value of this string or the string
	 */
	@Deprecated
	public static Object stringToValue(String string) {
		return JsonMap.stringToValue(string);
	}

	/**
	 * Convert a well-formed (but not necessarily valid) XMLConverter string into a
	 * JsonMap. Some information may be lost in this transformation because JSON is
	 * a data format and XMLConverter is a document format. XMLConverter uses
	 * elements, attributes, and content text, while JSON uses unordered collections
	 * of name/value pairs and arrays of values. JSON does not does not like to
	 * distinguish between elements and attributes. Sequences of similar elements
	 * are represented as JsonLists. Content text may be placed in a "content"
	 * member. Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
	 * 
	 * @param string The source string.
	 * @return A JsonMap containing the structured data from the XMLConverter
	 *         string.
	 * @throws JsonException Thrown if there is an errors while parsing the string
	 */
	public static JsonMap toJsonMap(String string) throws JsonException {
		return toJsonMap(string, false);
	}

	/**
	 * Convert a well-formed (but not necessarily valid) XMLConverter string into a
	 * JsonMap. Some information may be lost in this transformation because JSON is
	 * a data format and XMLConverter is a document format. XMLConverter uses
	 * elements, attributes, and content text, while JSON uses unordered collections
	 * of name/value pairs and arrays of values. JSON does not does not like to
	 * distinguish between elements and attributes. Sequences of similar elements
	 * are represented as JsonLists. Content text may be placed in a "content"
	 * member. Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
	 * 
	 * All values are converted as strings, for 1, 01, 29.0 will not be coerced to
	 * numbers but will instead be the exact value as seen in the XMLConverter
	 * document.
	 * 
	 * @param string      The source string.
	 * @param keepStrings If true, then values will not be coerced into boolean or
	 *                    numeric values and will instead be left as strings
	 * @return A JsonMap containing the structured data from the XMLConverter
	 *         string.
	 * @throws JsonException Thrown if there is an errors while parsing the string
	 */
	public static JsonMap toJsonMap(String string, boolean keepStrings) throws JsonException {
		JsonMap jo = new JsonMap();
		JsonXmlTokener x = new JsonXmlTokener(string);
		while (x.more() && x.skipPast("<")) {
			parse(x, jo, null, keepStrings);
		}
		return jo;
	}

	/**
	 * Convert a JsonMap into a well-formed, element-normal XMLConverter string.
	 * 
	 * @param object A JsonMap.
	 * @return A string.
	 * @throws JsonException Thrown if there is an error parsing the string
	 */
	public static String toString(Object object) throws JsonException {
		return toString(object, null);
	}

	/**
	 * Convert a JsonMap into a well-formed, element-normal XMLConverter string.
	 * 
	 * @param object  A JsonMap.
	 * @param tagName The optional name of the enclosing tag.
	 * @return A string.
	 * @throws JsonException Thrown if there is an error parsing the string
	 */
	public static String toString(Object object, String tagName) throws JsonException {
		StringBuilder sb = new StringBuilder();
		JsonList ja;
		JsonMap jo;
		String key;
		Iterator<String> keys;
		String string;
		Object value;

		if (object instanceof JsonMap) {
			if (tagName != null) {
				sb.append('<');
				sb.append(tagName);
				sb.append('>');
			}
			jo = (JsonMap) object;
			keys = jo.keys();
			while (keys.hasNext()) {
				key = keys.next();
				value = jo.opt(key);
				if (value == null) {
					value = "";
				} else if (value.getClass().isArray()) {
					value = new JsonList(value);
				}
				string = value instanceof String ? (String) value : null;
				if ("content".equals(key)) {
					if (value instanceof JsonList) {
						ja = (JsonList) value;
						int i = 0;
						for (Object val : ja) {
							if (i > 0) {
								sb.append('\n');
							}
							sb.append(escape(val.toString()));
							i++;
						}
					} else {
						sb.append(escape(value.toString()));
					}
				} else if (value instanceof JsonList) {
					ja = (JsonList) value;
					for (Object val : ja) {
						if (val instanceof JsonList) {
							sb.append('<');
							sb.append(key);
							sb.append('>');
							sb.append(toString(val));
							sb.append("</");
							sb.append(key);
							sb.append('>');
						} else {
							sb.append(toString(val, key));
						}
					}
				} else if ("".equals(value)) {
					sb.append('<');
					sb.append(key);
					sb.append("/>");
				} else {
					sb.append(toString(value, key));
				}
			}
			if (tagName != null) {
				sb.append("</");
				sb.append(tagName);
				sb.append('>');
			}
			return sb.toString();
		}
		if (object != null) {
			if (object.getClass().isArray()) {
				object = new JsonList(object);
			}
			if (object instanceof JsonList) {
				ja = (JsonList) object;
				for (Object val : ja) {
					sb.append(toString(val, tagName == null ? "array" : tagName));
				}
				return sb.toString();
			}
		}
		string = (object == null) ? "null" : escape(object.toString());
		return (tagName == null) ? "\"" + string + "\""
				: (string.length() == 0) ? "<" + tagName + "/>" : "<" + tagName + ">" + string + "</" + tagName + ">";
	}
}
