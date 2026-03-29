package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import openjdk.tools.json.JsonMap;

public class Main {

	public static void main(String[] args) {
		try {
			
			JsonMap jmap = new JsonMap(readFileToString(System.getProperty("user.dir") + File.separator + "test_json_object.json"));
			System.out.println(jmap.toString(1));
			
			

			
		} catch (Exception e) { e.printStackTrace(); }
	}

	public static String readFileToString(String filePath) throws IOException {
		StringBuilder sb = new StringBuilder(); // Use StringBuilder for efficiency
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		}
		return sb.toString();
	}

}
