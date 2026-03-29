package openjdk.tools.json.internal;

import static java.lang.String.format;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

import openjdk.tools.json.JsonList;
import openjdk.tools.json.JsonMap;
import openjdk.tools.json.exceptions.JsonPointerException;

public class JsonPointer {

	private static final String ENCODING = "utf-8";

	public static class Builder {

		private final List<String> refTokens = new ArrayList<String>();

		public JsonPointer build() {
			return new JsonPointer(refTokens);
		}

		public Builder append(String token) {
			if (token == null) {
				throw new NullPointerException("token cannot be null");
			}
			refTokens.add(token);
			return this;
		}

		public Builder append(int arrayIndex) {
			refTokens.add(String.valueOf(arrayIndex));
			return this;
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	private final List<String> refTokens;

	public JsonPointer(String pointer) {
		if (pointer == null) {
			throw new NullPointerException("pointer cannot be null");
		}
		if (pointer.isEmpty() || pointer.equals("#")) {
			refTokens = Collections.emptyList();
			return;
		}
		if (pointer.startsWith("#/")) {
			pointer = pointer.substring(2);
			try {
				pointer = URLDecoder.decode(pointer, ENCODING);
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		} else if (pointer.startsWith("/")) {
			pointer = pointer.substring(1);
		} else {
			throw new IllegalArgumentException("a JSON pointer should start with '/' or '#/'");
		}
		refTokens = new ArrayList<String>();
		for (String token : pointer.split("/")) {
			refTokens.add(unescape(token));
		}
	}

	public JsonPointer(List<String> refTokens) {
		this.refTokens = new ArrayList<String>(refTokens);
	}

	private String unescape(String token) {
		return token.replace("~1", "/").replace("~0", "~").replace("\\\"", "\"").replace("\\\\", "\\");
	}

	public Object queryFrom(Object document) {
		if (refTokens.isEmpty()) {
			return document;
		}
		Object current = document;
		for (String token : refTokens) {
			if (current instanceof JsonMap) {
				current = ((JsonMap) current).opt(unescape(token));
			} else if (current instanceof JsonList) {
				current = readByIndexToken(current, token);
			} else {
				throw new JsonPointerException(
						format("value [%s] is not an array or object therefore its key %s cannot be resolved", current,
								token));
			}
		}
		return current;
	}

	private Object readByIndexToken(Object current, String indexToken) {
		try {
			int index = Integer.parseInt(indexToken);
			JsonList currentArr = (JsonList) current;
			if (index >= currentArr.length()) {
				throw new JsonPointerException(
						format("index %d is out of bounds - the array has %d elements", index, currentArr.length()));
			}
			return currentArr.get(index);
		} catch (NumberFormatException e) {
			throw new JsonPointerException(format("%s is not an array index", indexToken), e);
		}
	}

	@Override
	public String toString() {
		StringBuilder rval = new StringBuilder("");
		for (String token : refTokens) {
			rval.append('/').append(escape(token));
		}
		return rval.toString();
	}

	private String escape(String token) {
		return token.replace("~", "~0").replace("/", "~1").replace("\\", "\\\\").replace("\"", "\\\"");
	}

	public String toURIFragment() {
		try {
			StringBuilder rval = new StringBuilder("#");
			for (String token : refTokens) {
				rval.append('/').append(URLEncoder.encode(token, ENCODING));
			}
			return rval.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
