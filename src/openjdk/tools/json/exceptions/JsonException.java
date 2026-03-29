package openjdk.tools.json.exceptions;

public class JsonException extends RuntimeException {
	private static final long serialVersionUID = 0;

	public JsonException(final String message) {
		super(message);
	}

	public JsonException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public JsonException(final Throwable cause) {
		super(cause.getMessage(), cause);
	}

}
