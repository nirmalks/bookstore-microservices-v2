package saga;

/**
 * Result of a saga step execution.
 */
public record SagaStepResult(boolean success, String message, String data) {
	public static SagaStepResult ofSuccess() {
		return new SagaStepResult(true, "success", null);
	}

	public static SagaStepResult ofSuccess(String message, String data) {
		return new SagaStepResult(true, message, data);
	}

	public static SagaStepResult ofFailure(String message) {
		return new SagaStepResult(false, message, null);
	}
}
