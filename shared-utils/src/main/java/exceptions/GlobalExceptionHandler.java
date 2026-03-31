package exceptions;

import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private static final String TRACE_ID = "traceId";

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
		logger.debug("Resource not found: {}", ex.getMessage());
		return buildResponse("Resource not found", HttpStatus.NOT_FOUND, List.of(ex.getMessage()));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
		logger.debug("Bad request: {}", ex.getMessage());
		return buildResponse("Invalid request", HttpStatus.BAD_REQUEST, List.of(ex.getMessage()));
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex) {
		logger.warn("Unauthorized access: {}", ex.getMessage());
		return buildResponse("Unauthorized", HttpStatus.UNAUTHORIZED, List.of(ex.getMessage()));
	}

	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(UsernameNotFoundException ex) {
		logger.warn("User not found: {}", ex.getMessage());
		return buildResponse("Unauthorized", HttpStatus.UNAUTHORIZED, List.of("User does not exist"));
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
		logger.warn("Bad credentials: {}", ex.getMessage());
		return buildResponse("Unauthorized", HttpStatus.UNAUTHORIZED, List.of("Invalid credentials"));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
		logger.warn("Access denied: {}", ex.getMessage());
		return buildResponse("Forbidden", HttpStatus.FORBIDDEN,
				List.of("You do not have permission to access this resource"));
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
		logger.warn("Authentication failed: {}", ex.getMessage());
		return buildResponse("Unauthorized", HttpStatus.UNAUTHORIZED, List.of(ex.getMessage()));
	}

	@ExceptionHandler(InsufficientStockException.class)
	public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
		logger.info("Insufficient stock for book {}: available {}, requested {}", ex.getBookId(),
				ex.getAvailableQuantity(), ex.getRequestedQuantity());
		return buildResponse("Insufficient stock", HttpStatus.CONFLICT, List.of(ex.getMessage()));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		logger.error("Data integrity violation: {}", ex.getMessage());
		String detail = ex.getRootCause() != null ? ex.getRootCause().getMessage() : "Data integrity error";
		return buildResponse("Constraint violation", HttpStatus.BAD_REQUEST, List.of(detail));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
		List<String> validationErrors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> error.getDefaultMessage())
			.toList();

		logger.debug("Validation failed: {}", validationErrors);
		return buildResponse("Validation failed", HttpStatus.BAD_REQUEST, validationErrors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
		List<String> validationErrors = ex.getConstraintViolations()
			.stream()
			.map(violation -> violation.getMessage())
			.toList();

		logger.debug("Constraint violation: {}", validationErrors);
		return buildResponse("Validation failed", HttpStatus.BAD_REQUEST, validationErrors);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
		List<String> validationErrors = ex.getAllErrors().stream().map(error -> error.getDefaultMessage()).toList();

		logger.debug("Handler method validation failed: {}", validationErrors);
		return buildResponse("Validation failed", HttpStatus.BAD_REQUEST, validationErrors);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
		String error = String.format("Parameter '%s' should be of type '%s'", ex.getName(), requiredType);
		logger.debug("Type mismatch: {}", error);
		return buildResponse("Type mismatch", HttpStatus.BAD_REQUEST, List.of(error));
	}

	@ExceptionHandler(MissingPathVariableException.class)
	public ResponseEntity<ErrorResponse> handleMissingPathVariable(MissingPathVariableException ex) {
		logger.debug("Missing path variable: {}", ex.getVariableName());
		return buildResponse("Missing path variable", HttpStatus.BAD_REQUEST, List.of(ex.getMessage()));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
			MissingServletRequestParameterException ex) {
		logger.debug("Missing servlet request parameter: {}", ex.getParameterName());
		return buildResponse("Missing parameter", HttpStatus.BAD_REQUEST, List.of(ex.getMessage()));
	}

	@ExceptionHandler(ServiceUnavailableException.class)
	public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex) {
		logger.error("Service unavailable: {}", ex.getMessage());
		return buildResponse("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE, List.of(ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
		logger.error("Unexpected error occurred: ", ex);
		return buildResponse("An internal server error occurred", HttpStatus.INTERNAL_SERVER_ERROR,
				List.of("Please contact support with the trace ID provided"));
	}

	private ResponseEntity<ErrorResponse> buildResponse(String message, HttpStatus status, List<String> errors) {
		String traceId = MDC.get(TRACE_ID);
		ErrorResponse response = new ErrorResponse(message, status.value(), errors, traceId);
		return new ResponseEntity<>(response, status);
	}

}
