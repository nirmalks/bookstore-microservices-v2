package saga;

/**
 * Interface for saga step implementations. Each service implements this for their saga
 * participation.
 */

public interface SagaStep<T> {

	/**
	 * Execute the forward action of this saga step.
	 */
	SagaStepResult execute(T context);

	/**
	 * Execute the compensation action if a later step fails.
	 */
	SagaStepResult compensate(T context);

	/**
	 * Get the name of this step for logging.
	 */
	String getStepName();

}
