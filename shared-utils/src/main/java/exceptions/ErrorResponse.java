package exceptions;

import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponse {

	private String message;

	private int status;

	private List<String> errors;

	private LocalDateTime timestamp;

	private String traceId;

	public ErrorResponse(String message, int statusCode, List<String> errors, String traceId) {
		this.message = message;
		this.status = statusCode;
		this.errors = errors;
		this.timestamp = LocalDateTime.now();
		this.traceId = traceId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

}