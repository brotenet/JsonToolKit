package openjdk.tools.json.util.converters;

import openjdk.tools.json.JsonMap;
import openjdk.tools.json.exceptions.JsonException;
import openjdk.tools.json.internal.tokens.JsonTokener;


/**
 * Convert a web browser cookie specification to a JsonMap and back.
 * JSON and Cookies are both notations for name/value pairs.
 */
public class JsonCookieConverter {

    /**
     * Produce a copy of a string in which the characters '+', '%', '=', ';'
     * and control characters are replaced with "%hh". This is a gentle form
     * of URL encoding, attempting to cause as little distortion to the
     * string as possible. The characters '=' and ';' are meta characters in
     * cookies. By convention, they are escaped using the URL-encoding. This is
     * only a convention, not a standard. Often, cookies are expected to have
     * encoded values. We encode '=' and ';' because we must. We encode '%' and
     * '+' because they are meta characters in URL encoding.
     * @param string The source string.
     * @return       The escaped result.
     */
    public static String escape(String string) {
        char            c;
        String          s = string.trim();
        int             length = s.length();
        StringBuilder   sb = new StringBuilder(length);
        for (int i = 0; i < length; i += 1) {
            c = s.charAt(i);
            if (c < ' ' || c == '+' || c == '%' || c == '=' || c == ';') {
                sb.append('%');
                sb.append(Character.forDigit((char)((c >>> 4) & 0x0f), 16));
                sb.append(Character.forDigit((char)(c & 0x0f), 16));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }


    /**
     * Convert a cookie specification string into a JsonMap. The string
     * will contain a name value pair separated by '='. The name and the value
     * will be unescaped, possibly converting '+' and '%' sequences. The
     * cookie properties may follow, separated by ';', also represented as
     * name=value (except the secure property, which does not have a value).
     * The name will be stored under the key "name", and the value will be
     * stored under the key "value". This method does not do checking or
     * validation of the parameters. It only converts the cookie string into
     * a JsonMap.
     * @param string The cookie specification string.
     * @return A JsonMap containing "name", "value", and possibly other
     *  members.
     * @throws JsonException
     */
    public static JsonMap toJsonMap(String string) throws JsonException {
        String         name;
        JsonMap     jo = new JsonMap();
        Object         value;
        JsonTokener x = new JsonTokener(string);
        jo.put("name", x.nextTo('='));
        x.next('=');
        jo.put("value", x.nextTo(';'));
        x.next();
        while (x.more()) {
            name = unescape(x.nextTo("=;"));
            if (x.next() != '=') {
                if (name.equals("secure")) {
                    value = Boolean.TRUE;
                } else {
                    throw x.syntaxError("Missing '=' in cookie parameter.");
                }
            } else {
                value = unescape(x.nextTo(';'));
                x.next();
            }
            jo.put(name, value);
        }
        return jo;
    }


    /**
     * Convert a JsonMap into a cookie specification string. The JsonMap
     * must contain "name" and "value" members.
     * If the JsonMap contains "expires", "domain", "path", or "secure"
     * members, they will be appended to the cookie specification string.
     * All other members are ignored.
     * @param jo A JsonMap
     * @return A cookie specification string
     * @throws JsonException
     */
    public static String toString(JsonMap jo) throws JsonException {
        StringBuilder sb = new StringBuilder();

        sb.append(escape(jo.getString("name")));
        sb.append("=");
        sb.append(escape(jo.getString("value")));
        if (jo.has("expires")) {
            sb.append(";expires=");
            sb.append(jo.getString("expires"));
        }
        if (jo.has("domain")) {
            sb.append(";domain=");
            sb.append(escape(jo.getString("domain")));
        }
        if (jo.has("path")) {
            sb.append(";path=");
            sb.append(escape(jo.getString("path")));
        }
        if (jo.optBoolean("secure")) {
            sb.append(";secure");
        }
        return sb.toString();
    }

    /**
     * Convert <code>%</code><i>hh</i> sequences to single characters, and
     * convert plus to space.
     * @param string A string that may contain
     *      <code>+</code>&nbsp;<small>(plus)</small> and
     *      <code>%</code><i>hh</i> sequences.
     * @return The unescaped string.
     */
    public static String unescape(String string) {
        int length = string.length();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            char c = string.charAt(i);
            if (c == '+') {
                c = ' ';
            } else if (c == '%' && i + 2 < length) {
                int d = JsonTokener.dehexchar(string.charAt(i + 1));
                int e = JsonTokener.dehexchar(string.charAt(i + 2));
                if (d >= 0 && e >= 0) {
                    c = (char)(d * 16 + e);
                    i += 2;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
