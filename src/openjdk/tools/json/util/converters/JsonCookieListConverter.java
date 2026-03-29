package openjdk.tools.json.util.converters;

import java.util.Iterator;

import openjdk.tools.json.JsonMap;
import openjdk.tools.json.exceptions.JsonException;
import openjdk.tools.json.internal.tokens.JsonTokener;

/**
 * Convert a web browser cookie list string to a JsonMap and back.
 */
public class JsonCookieListConverter {

    /**
     * Convert a cookie list into a JsonMap. A cookie list is a sequence
     * of name/value pairs. The names are separated from the values by '='.
     * The pairs are separated by ';'. The names and the values
     * will be unescaped, possibly converting '+' and '%' sequences.
     *
     * To add a cookie to a cooklist,
     * cookielistJsonMap.put(cookieJsonMap.getString("name"),
     *     cookieJsonMap.getString("value"));
     * @param string  A cookie list string
     * @return A JsonMap
     * @throws JsonException
     */
    public static JsonMap toJsonMap(String string) throws JsonException {
        JsonMap jo = new JsonMap();
        JsonTokener x = new JsonTokener(string);
        while (x.more()) {
            String name = JsonCookieConverter.unescape(x.nextTo('='));
            x.next('=');
            jo.put(name, JsonCookieConverter.unescape(x.nextTo(';')));
            x.next();
        }
        return jo;
    }

    /**
     * Convert a JsonMap into a cookie list. A cookie list is a sequence
     * of name/value pairs. The names are separated from the values by '='.
     * The pairs are separated by ';'. The characters '%', '+', '=', and ';'
     * in the names and values are replaced by "%hh".
     * @param jo A JsonMap
     * @return A cookie list string
     * @throws JsonException
     */
    public static String toString(JsonMap jo) throws JsonException {
        boolean             b = false;
        Iterator<String>    keys = jo.keys();
        String              string;
        StringBuilder sb = new StringBuilder();
        while (keys.hasNext()) {
            string = keys.next();
            if (!jo.isNull(string)) {
                if (b) {
                    sb.append(';');
                }
                sb.append(JsonCookieConverter.escape(string));
                sb.append("=");
                sb.append(JsonCookieConverter.escape(jo.getString(string)));
                b = true;
            }
        }
        return sb.toString();
    }
}
