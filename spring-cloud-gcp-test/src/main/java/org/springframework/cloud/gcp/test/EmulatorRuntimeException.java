package org.springframework.cloud.gcp.test;

public class EmulatorRuntimeException extends RuntimeException {
	public EmulatorRuntimeException(String message) {
		super(message);
	}

	public EmulatorRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public EmulatorRuntimeException(Throwable cause) {
		super(cause);
	}
}
