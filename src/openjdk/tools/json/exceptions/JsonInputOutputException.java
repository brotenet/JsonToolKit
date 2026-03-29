package openjdk.tools.json.exceptions;

public class JsonInputOutputException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public JsonInputOutputException() {
		super();
	}

	public JsonInputOutputException(String message) {
		super(message);
	}

	public JsonInputOutputException(String message, Throwable cause) {
		super(message, cause);
	}

	public JsonInputOutputException(Throwable cause) {
		super(cause);
	}
}
