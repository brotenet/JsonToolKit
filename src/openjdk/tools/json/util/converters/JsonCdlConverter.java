package openjdk.tools.json.util.converters;

import openjdk.tools.json.JsonList;
import openjdk.tools.json.JsonMap;
import openjdk.tools.json.exceptions.JsonException;
import openjdk.tools.json.internal.tokens.JsonTokener;

/**
 * This provides static methods to convert comma delimited text into a
 * JsonList, and to convert a JsonList into comma delimited text. Comma
 * delimited text is a very popular format for data interchange. It is
 * understood by most database, spreadsheet, and organizer programs.
 * <p>
 * Each row of text represents a row in a table or a data record. Each row
 * ends with a NEWLINE character. Each row contains one or more values.
 * Values are separated by commas. A value can contain any character except
 * for comma, unless is is wrapped in single quotes or double quotes.
 * <p>
 * The first row usually contains the names of the columns.
 * <p>
 * A comma delimited list can be converted into a JsonList of JsonMaps.
 * The names for the elements in the JsonMaps can be taken from the names
 * in the first row.
 */
public class JsonCdlConverter {

    /**
     * Get the next value. The value can be wrapped in quotes. The value can
     * be empty.
     * @param x A JSONTokener of the source text.
     * @return The value string, or null if empty.
     * @throws JsonException if the quoted string is badly formed.
     */
    private static String getValue(JsonTokener x) throws JsonException {
        char c;
        char q;
        StringBuffer sb;
        do {
            c = x.next();
        } while (c == ' ' || c == '\t');
        switch (c) {
        case 0:
            return null;
        case '"':
        case '\'':
            q = c;
            sb = new StringBuffer();
            for (;;) {
                c = x.next();
                if (c == q) {
                    //Handle escaped double-quote
                    if(x.next() != '\"')
                    {
                        x.back();
                        break;
                    }
                }
                if (c == 0 || c == '\n' || c == '\r') {
                    throw x.syntaxError("Missing close quote '" + q + "'.");
                }
                sb.append(c);
            }
            return sb.toString();
        case ',':
            x.back();
            return "";
        default:
            x.back();
            return x.nextTo(',');
        }
    }

    /**
     * Produce a JsonList of strings from a row of comma delimited values.
     * @param x A JSONTokener of the source text.
     * @return A JsonList of strings.
     * @throws JsonException
     */
    public static JsonList rowToJsonList(JsonTokener x) throws JsonException {
        JsonList ja = new JsonList();
        for (;;) {
            String value = getValue(x);
            char c = x.next();
            if (value == null ||
                    (ja.length() == 0 && value.length() == 0 && c != ',')) {
                return null;
            }
            ja.put(value);
            for (;;) {
                if (c == ',') {
                    break;
                }
                if (c != ' ') {
                    if (c == '\n' || c == '\r' || c == 0) {
                        return ja;
                    }
                    throw x.syntaxError("Bad character '" + c + "' (" +
                            (int)c + ").");
                }
                c = x.next();
            }
        }
    }

    /**
     * Produce a JsonMap from a row of comma delimited text, using a
     * parallel JsonList of strings to provides the names of the elements.
     * @param names A JsonList of names. This is commonly obtained from the
     *  first row of a comma delimited text file using the rowToJsonList
     *  method.
     * @param x A JSONTokener of the source text.
     * @return A JsonMap combining the names and values.
     * @throws JsonException
     */
    public static JsonMap rowToJsonMap(JsonList names, JsonTokener x)
            throws JsonException {
        JsonList ja = rowToJsonList(x);
        return ja != null ? ja.toJsonMap(names) :  null;
    }

    /**
     * Produce a comma delimited text row from a JsonList. Values containing
     * the comma character will be quoted. Troublesome characters may be
     * removed.
     * @param ja A JsonList of strings.
     * @return A string ending in NEWLINE.
     */
    public static String rowToString(JsonList ja) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ja.length(); i += 1) {
            if (i > 0) {
                sb.append(',');
            }
            Object object = ja.opt(i);
            if (object != null) {
                String string = object.toString();
                if (string.length() > 0 && (string.indexOf(',') >= 0 ||
                        string.indexOf('\n') >= 0 || string.indexOf('\r') >= 0 ||
                        string.indexOf(0) >= 0 || string.charAt(0) == '"')) {
                    sb.append('"');
                    int length = string.length();
                    for (int j = 0; j < length; j += 1) {
                        char c = string.charAt(j);
                        if (c >= ' ' && c != '"') {
                            sb.append(c);
                        }
                    }
                    sb.append('"');
                } else {
                    sb.append(string);
                }
            }
        }
        sb.append('\n');
        return sb.toString();
    }

    /**
     * Produce a JsonList of JsonMaps from a comma delimited text string,
     * using the first row as a source of names.
     * @param string The comma delimited text.
     * @return A JsonList of JsonMaps.
     * @throws JsonException
     */
    public static JsonList toJsonList(String string) throws JsonException {
        return toJsonList(new JsonTokener(string));
    }

    /**
     * Produce a JsonList of JsonMaps from a comma delimited text string,
     * using the first row as a source of names.
     * @param x The JSONTokener containing the comma delimited text.
     * @return A JsonList of JsonMaps.
     * @throws JsonException
     */
    public static JsonList toJsonList(JsonTokener x) throws JsonException {
        return toJsonList(rowToJsonList(x), x);
    }

    /**
     * Produce a JsonList of JsonMaps from a comma delimited text string
     * using a supplied JsonList as the source of element names.
     * @param names A JsonList of strings.
     * @param string The comma delimited text.
     * @return A JsonList of JsonMaps.
     * @throws JsonException
     */
    public static JsonList toJsonList(JsonList names, String string)
            throws JsonException {
        return toJsonList(names, new JsonTokener(string));
    }

    /**
	 * Produce a JsonList of JsonMaps from a comma delimited text string using a
	 * supplied JsonList as the source of element names.
	 * 
	 * @param names A JsonList of strings.
	 * @param x     A JSONTokener of the source text.
	 * @return A JsonList of JsonMaps.
	 * @throws JsonException
	 */
    public static JsonList toJsonList(JsonList names, JsonTokener x)
            throws JsonException {
        if (names == null || names.length() == 0) {
            return null;
        }
        JsonList ja = new JsonList();
        for (;;) {
            JsonMap jo = rowToJsonMap(names, x);
            if (jo == null) {
                break;
            }
            ja.put(jo);
        }
        if (ja.length() == 0) {
            return null;
        }
        return ja;
    }


    /**
     * Produce a comma delimited text from a JsonList of JsonMaps. The
     * first row will be a list of names obtained by inspecting the first
     * JsonMap.
     * @param ja A JsonList of JsonMaps.
     * @return A comma delimited text.
     * @throws JsonException
     */
    public static String toString(JsonList ja) throws JsonException {
        JsonMap jo = ja.optJsonMap(0);
        if (jo != null) {
            JsonList names = jo.names();
            if (names != null) {
                return rowToString(names) + toString(names, ja);
            }
        }
        return null;
    }

    /**
     * Produce a comma delimited text from a JsonList of JsonMaps using
     * a provided list of names. The list of names is not included in the
     * output.
     * @param names A JsonList of strings.
     * @param ja A JsonList of JsonMaps.
     * @return A comma delimited text.
     * @throws JsonException
     */
    public static String toString(JsonList names, JsonList ja)
            throws JsonException {
        if (names == null || names.length() == 0) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ja.length(); i += 1) {
            JsonMap jo = ja.optJsonMap(i);
            if (jo != null) {
                sb.append(rowToString(jo.toJsonList(names)));
            }
        }
        return sb.toString();
    }
}
