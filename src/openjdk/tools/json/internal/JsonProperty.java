package openjdk.tools.json.internal;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import openjdk.tools.json.JsonMap;
import openjdk.tools.json.exceptions.JsonException;

public class JsonProperty {

	public static JsonMap toJSONObject(java.util.Properties properties) throws JsonException {
		JsonMap jo = new JsonMap();
		if (properties != null && !properties.isEmpty()) {
			Enumeration<?> enumProperties = properties.propertyNames();
			while (enumProperties.hasMoreElements()) {
				String name = (String) enumProperties.nextElement();
				jo.put(name, properties.getProperty(name));
			}
		}
		return jo;
	}

	public static Properties toProperties(JsonMap jo) throws JsonException {
		Properties properties = new Properties();
		if (jo != null) {
			Iterator<String> keys = jo.keys();
			while (keys.hasNext()) {
				String name = keys.next();
				properties.put(name, jo.getString(name));
			}
		}
		return properties;
	}
}
