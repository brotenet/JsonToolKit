package openjdk.tools.json.internal;

import java.io.StringWriter;

public class JsonStringer extends JsonWriter {

	public JsonStringer() {
		super(new StringWriter());
	}

	public String toString() {
		return this.mode == 'd' ? this.writer.toString() : null;
	}
}
