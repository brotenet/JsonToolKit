package openjdk.tools.json.exceptions;

public class JsonPointerException extends JsonException {
	private static final long serialVersionUID = 8872944667561856751L;

	public JsonPointerException(String message) {
		super(message);
	}

	public JsonPointerException(String message, Throwable cause) {
		super(message, cause);
	}

}
