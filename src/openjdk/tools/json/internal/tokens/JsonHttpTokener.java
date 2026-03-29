package openjdk.tools.json.internal.tokens;

import openjdk.tools.json.exceptions.JsonException;

public class JsonHttpTokener extends JsonTokener {

	public JsonHttpTokener(String string) {
		super(string);
	}

	public String nextToken() throws JsonException {
		char c;
		char q;
		StringBuilder sb = new StringBuilder();
		do {
			c = next();
		} while (Character.isWhitespace(c));
		if (c == '"' || c == '\'') {
			q = c;
			for (;;) {
				c = next();
				if (c < ' ') {
					throw syntaxError("Unterminated string.");
				}
				if (c == q) {
					return sb.toString();
				}
				sb.append(c);
			}
		}
		for (;;) {
			if (c == 0 || Character.isWhitespace(c)) {
				return sb.toString();
			}
			sb.append(c);
			c = next();
		}
	}
}
