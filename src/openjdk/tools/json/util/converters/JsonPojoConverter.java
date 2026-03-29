package openjdk.tools.json.util.converters;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import openjdk.tools.json.JsonList;
import openjdk.tools.json.JsonMap;
import openjdk.tools.json.internal.pojo.JsonPojoReader;
import openjdk.tools.json.internal.pojo.JsonPojoWriter;

public class JsonPojoConverter {

	public static String toJSON(Object pojo) {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put(JsonPojoWriter.DATE_FORMAT, "yyyy/MM/dd HH:mm");
		options.put(JsonPojoWriter.PRETTY_PRINT, true);
		options.put(JsonPojoWriter.WRITE_LONGS_AS_STRINGS, true);
		options.put(JsonPojoWriter.SKIP_NULL_FIELDS, false);
		return JsonPojoWriter.objectToJson(pojo, options);
	}

	public static String toJSON(Object pojo, String date_format, boolean longs_as_strings, boolean skip_nulls,
			boolean prettify) {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put(JsonPojoWriter.DATE_FORMAT, date_format);
		options.put(JsonPojoWriter.PRETTY_PRINT, prettify);
		options.put(JsonPojoWriter.WRITE_LONGS_AS_STRINGS, longs_as_strings);
		options.put(JsonPojoWriter.SKIP_NULL_FIELDS, skip_nulls);
		return JsonPojoWriter.objectToJson(pojo, options);
	}

	public static JsonMap toJsonMap(Object pojo) {
		return new JsonMap(toJSON(pojo));
	}

	public static JsonMap toJsonMap(Object pojo, String date_format, boolean longs_as_strings, boolean skip_nulls) {
		return new JsonMap(toJSON(pojo, date_format, longs_as_strings, skip_nulls, false));
	}

	public static JsonList toJsonList(Object[] pojos) {
		JsonList output = new JsonList();
		for (Object pojo : pojos) {
			output.put(toJsonMap(pojo));
		}
		return output;
	}

	public static JsonList toJsonList(Object[] pojos, String date_format, boolean longs_as_strings,
			boolean skip_nulls) {
		JsonList output = new JsonList();
		for (Object pojo : pojos) {
			output.put(toJsonMap(pojo, date_format, longs_as_strings, skip_nulls));
		}
		return output;
	}

	public static Object toObject(String json, boolean use_maps) {
		if (use_maps == true) {
			Map<String, Object> options = new HashMap<String, Object>();
			options.put(JsonPojoReader.USE_MAPS, true);
			return JsonPojoReader.jsonToJava(json, options);
		} else {
			return JsonPojoReader.jsonToJava(json);
		}
	}

	public static Object toObject(String json) {
		return JsonPojoReader.jsonToJava(json);
	}

	public static Object toObject(InputStream json, boolean use_maps) {
		if (use_maps == true) {
			Map<String, Object> options = new HashMap<String, Object>();
			options.put(JsonPojoReader.USE_MAPS, true);
			return JsonPojoReader.jsonToJava(json, options);
		} else {
			return JsonPojoReader.jsonToJava(json, new HashMap<>());
		}
	}

	public static Object toObject(InputStream json) {
		return JsonPojoReader.jsonToJava(json, new HashMap<>());
	}
}
