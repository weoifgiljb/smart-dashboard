package com.selfdiscipline.exception;

public class ImageGenerationException extends RuntimeException {

	public enum ErrorType {
		CONFIG_MISSING,
		RATE_LIMITED,
		UPSTREAM_4XX,
		UPSTREAM_5XX,
		TIMEOUT,
		PARSING,
		UNAUTHORIZED,
		FORBIDDEN,
		BAD_REQUEST
	}

	private final ErrorType type;
	private final int statusCode;

	public ImageGenerationException(ErrorType type, String message) {
		super(message);
		this.type = type;
		this.statusCode = -1;
	}

	public ImageGenerationException(ErrorType type, String message, int statusCode) {
		super(message);
		this.type = type;
		this.statusCode = statusCode;
	}

	public ErrorType getType() {
		return type;
	}

	public int getStatusCode() {
		return statusCode;
	}
}



