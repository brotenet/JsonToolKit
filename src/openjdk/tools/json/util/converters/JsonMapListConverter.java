package openjdk.tools.json.util.converters;

import java.util.ArrayList;
import java.util.HashMap;

import openjdk.tools.json.JsonList;
import openjdk.tools.json.JsonMap;

public class JsonMapListConverter {

	/**
	 * Convert a JsonMap to a HashMap. Any contained JsonList elements are converted
	 * to ArrayList
	 * 
	 * @param json_object
	 * @return HashMap<key:String, value:Object>
	 */
	public static HashMap<String, Object> toHashmap(JsonMap json_object) {
		HashMap<String, Object> output = new HashMap<String, Object>();
		for (String key : json_object.keySet()) {
			Object element = json_object.get(key);
			if (element instanceof JsonMap) {
				output.put(key, toHashmap((JsonMap) element));
			} else if (element instanceof JsonList) {
				output.put(key, toArrayList((JsonList) element));
			} else {
				output.put(key, element);
			}
		}
		return output;
	}

	/**
	 * Convert a JsonList to a ArrayList. Any contained JsonMap elements are
	 * converted to HashMap
	 * 
	 * @param json_array
	 * @return ArrayList<Object>
	 */
	public static ArrayList<Object> toArrayList(JsonList json_array) {
		ArrayList<Object> output = new ArrayList<Object>();
		for (Object element : json_array.toArray()) {
			if (element instanceof JsonMap) {
				output.add(toHashmap((JsonMap) element));
			} else if (element instanceof JsonList) {
				output.add(toArrayList((JsonList) element));
			} else {
				output.add(element);
			}
		}
		return output;
	}

}
