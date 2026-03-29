package openjdk.tools.json.util.converters;

import java.util.Iterator;

import openjdk.tools.json.JsonMap;
import openjdk.tools.json.exceptions.JsonException;
import openjdk.tools.json.internal.tokens.JsonHttpTokener;

/**
 * Convert an HTTPHeaderConverter header to a JsonMap and back.
 */
public class JsonHttpHeaderConverter {

	/** Carriage return/line feed. */
	public static final String CRLF = "\r\n";

	/**
	 * Convert an HTTPHeaderConverter header string into a JsonMap. It can be a
	 * request header or a response header. A request header will contain
	 * 
	 * <pre>
	 * {
	 *    Method: "POST" (for example),
	 *    "Request-URI": "/" (for example),
	 *    "HTTPHeaderConverter-Version": "HTTPHeaderConverter/1.1" (for example)
	 * }
	 * </pre>
	 * 
	 * A response header will contain
	 * 
	 * <pre>
	 * {
	 *    "HTTPHeaderConverter-Version": "HTTPHeaderConverter/1.1" (for example),
	 *    "Status-Code": "200" (for example),
	 *    "Reason-Phrase": "OK" (for example)
	 * }
	 * </pre>
	 * 
	 * In addition, the other parameters in the header will be captured, using the
	 * HTTPHeaderConverter field names as JSON names, so that
	 * 
	 * <pre>
	 *    Date: Sun, 26 May 2002 18:06:04 GMT
	 *    CookieConverter: Q=q2=PPEAsg--; B=677gi6ouf29bn&b=2&f=s
	 *    Cache-Control: no-cache
	 * </pre>
	 * 
	 * become
	 * 
	 * <pre>
	 * {...
	 *    Date: "Sun, 26 May 2002 18:06:04 GMT",
	 *    CookieConverter: "Q=q2=PPEAsg--; B=677gi6ouf29bn&b=2&f=s",
	 *    "Cache-Control": "no-cache",
	 * ...}
	 * </pre>
	 * 
	 * It does no further checking or conversion. It does not parse dates. It does
	 * not do '%' transforms on URLs.
	 * 
	 * @param string An HTTPHeaderConverter header string.
	 * @return A JsonMap containing the elements and attributes of the XMLConverter
	 *         string.
	 * @throws JsonException
	 */
	public static JsonMap toJsonMap(String string) throws JsonException {
		JsonMap jo = new JsonMap();
		JsonHttpTokener x = new JsonHttpTokener(string);
		String token;

		token = x.nextToken();
		if (token.toUpperCase().startsWith("HTTPHeaderConverter")) {
			jo.put("HTTPHeaderConverter-Version", token);
			jo.put("Status-Code", x.nextToken());
			jo.put("Reason-Phrase", x.nextTo('\0'));
			x.next();
		} else {
			jo.put("Method", token);
			jo.put("Request-URI", x.nextToken());
			jo.put("HTTPHeaderConverter-Version", x.nextToken());
		}
		while (x.more()) {
			String name = x.nextTo(':');
			x.next(':');
			jo.put(name, x.nextTo('\0'));
			x.next();
		}
		return jo;
	}

	/**
	 * Convert a JsonMap into an HTTPHeaderConverter header. A request header must
	 * contain
	 * 
	 * <pre>
	 * {
	 *    Method: "POST" (for example),
	 *    "Request-URI": "/" (for example),
	 *    "HTTPHeaderConverter-Version": "HTTPHeaderConverter/1.1" (for example)
	 * }
	 * </pre>
	 * 
	 * A response header must contain
	 * 
	 * <pre>
	 * {
	 *    "HTTPHeaderConverter-Version": "HTTPHeaderConverter/1.1" (for example),
	 *    "Status-Code": "200" (for example),
	 *    "Reason-Phrase": "OK" (for example)
	 * }
	 * </pre>
	 * 
	 * Any other members of the JsonMap will be output as HTTPHeaderConverter
	 * fields. The result will end with two CRLF pairs.
	 * 
	 * @param jo A JsonMap
	 * @return An HTTPHeaderConverter header string.
	 * @throws JsonException if the object does not contain enough information.
	 */
	public static String toString(JsonMap jo) throws JsonException {
		Iterator<String> keys = jo.keys();
		String string;
		StringBuilder sb = new StringBuilder();
		if (jo.has("Status-Code") && jo.has("Reason-Phrase")) {
			sb.append(jo.getString("HTTPHeaderConverter-Version"));
			sb.append(' ');
			sb.append(jo.getString("Status-Code"));
			sb.append(' ');
			sb.append(jo.getString("Reason-Phrase"));
		} else if (jo.has("Method") && jo.has("Request-URI")) {
			sb.append(jo.getString("Method"));
			sb.append(' ');
			sb.append('"');
			sb.append(jo.getString("Request-URI"));
			sb.append('"');
			sb.append(' ');
			sb.append(jo.getString("HTTPHeaderConverter-Version"));
		} else {
			throw new JsonException("Not enough material for an HTTPHeaderConverter header.");
		}
		sb.append(CRLF);
		while (keys.hasNext()) {
			string = keys.next();
			if (!"HTTPHeaderConverter-Version".equals(string) && !"Status-Code".equals(string)
					&& !"Reason-Phrase".equals(string) && !"Method".equals(string) && !"Request-URI".equals(string)
					&& !jo.isNull(string)) {
				sb.append(string);
				sb.append(": ");
				sb.append(jo.getString(string));
				sb.append(CRLF);
			}
		}
		sb.append(CRLF);
		return sb.toString();
	}
}
