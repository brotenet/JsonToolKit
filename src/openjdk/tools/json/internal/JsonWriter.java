package openjdk.tools.json.internal;

import java.io.IOException;
import openjdk.tools.json.JsonMap;
import openjdk.tools.json.exceptions.JsonException;

public class JsonWriter {
	private static final int maxdepth = 200;

	private boolean comma;

	protected char mode;

	private final JsonMap stack[];

	private int top;

	protected Appendable writer;

	public JsonWriter(Appendable w) {
		this.comma = false;
		this.mode = 'i';
		this.stack = new JsonMap[maxdepth];
		this.top = 0;
		this.writer = w;
	}

	private JsonWriter append(String string) throws JsonException {
		if (string == null) {
			throw new JsonException("Null pointer");
		}
		if (this.mode == 'o' || this.mode == 'a') {
			try {
				if (this.comma && this.mode == 'a') {
					this.writer.append(',');
				}
				this.writer.append(string);
			} catch (IOException e) {
				throw new JsonException(e);
			}
			if (this.mode == 'o') {
				this.mode = 'k';
			}
			this.comma = true;
			return this;
		}
		throw new JsonException("Value out of sequence.");
	}

	public JsonWriter array() throws JsonException {
		if (this.mode == 'i' || this.mode == 'o' || this.mode == 'a') {
			this.push(null);
			this.append("[");
			this.comma = false;
			return this;
		}
		throw new JsonException("Misplaced array.");
	}

	private JsonWriter end(char mode, char c) throws JsonException {
		if (this.mode != mode) {
			throw new JsonException(mode == 'a' ? "Misplaced endArray." : "Misplaced endObject.");
		}
		this.pop(mode);
		try {
			this.writer.append(c);
		} catch (IOException e) {
			throw new JsonException(e);
		}
		this.comma = true;
		return this;
	}

	public JsonWriter endArray() throws JsonException {
		return this.end('a', ']');
	}

	public JsonWriter endObject() throws JsonException {
		return this.end('k', '}');
	}

	public JsonWriter key(String string) throws JsonException {
		if (string == null) {
			throw new JsonException("Null key.");
		}
		if (this.mode == 'k') {
			try {
				this.stack[this.top - 1].putOnce(string, Boolean.TRUE);
				if (this.comma) {
					this.writer.append(',');
				}
				this.writer.append(JsonMap.quote(string));
				this.writer.append(':');
				this.comma = false;
				this.mode = 'o';
				return this;
			} catch (IOException e) {
				throw new JsonException(e);
			}
		}
		throw new JsonException("Misplaced key.");
	}

	public JsonWriter object() throws JsonException {
		if (this.mode == 'i') {
			this.mode = 'o';
		}
		if (this.mode == 'o' || this.mode == 'a') {
			this.append("{");
			this.push(new JsonMap());
			this.comma = false;
			return this;
		}
		throw new JsonException("Misplaced object.");

	}

	private void pop(char c) throws JsonException {
		if (this.top <= 0) {
			throw new JsonException("Nesting error.");
		}
		char m = this.stack[this.top - 1] == null ? 'a' : 'k';
		if (m != c) {
			throw new JsonException("Nesting error.");
		}
		this.top -= 1;
		this.mode = this.top == 0 ? 'd' : this.stack[this.top - 1] == null ? 'a' : 'k';
	}

	private void push(JsonMap jo) throws JsonException {
		if (this.top >= maxdepth) {
			throw new JsonException("Nesting too deep.");
		}
		this.stack[this.top] = jo;
		this.mode = jo == null ? 'a' : 'k';
		this.top += 1;
	}

	public JsonWriter value(boolean b) throws JsonException {
		return this.append(b ? "true" : "false");
	}

	public JsonWriter value(double d) throws JsonException {
		return this.value(Double.valueOf(d));
	}

	public JsonWriter value(long l) throws JsonException {
		return this.append(Long.toString(l));
	}

	public JsonWriter value(Object object) throws JsonException {
		return this.append(JsonMap.valueToString(object));
	}
}
