package com.nirmalks.checkout_service.saga;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class SagaMetrics {

	private final Counter sagasStarted;

	private final Counter sagasCompleted;

	private final Counter sagasCompensated;

	private final Counter sagasFailed;

	private final Timer sagaDuration;

	private final MeterRegistry registry;

	public SagaMetrics(MeterRegistry registry) {
		this.registry = registry;

		this.sagasStarted = Counter.builder("saga.started").description("Number of sagas started").register(registry);

		this.sagasCompleted = Counter.builder("saga.completed")
			.description("Number of sagas completed successfully")
			.register(registry);

		this.sagasCompensated = Counter.builder("saga.compensated")
			.description("Number of sagas that required compensation")
			.register(registry);

		this.sagasFailed = Counter.builder("saga.failed").description("Number of sagas that failed").register(registry);

		this.sagaDuration = Timer.builder("saga.duration").description("Duration of saga execution").register(registry);
	}

	public void incrementSagasStarted() {
		sagasStarted.increment();
	}

	public void incrementSagasCompleted() {
		sagasCompleted.increment();
	}

	public void incrementSagasCompensated() {
		sagasCompensated.increment();
	}

	public void incrementSagasFailed() {
		sagasFailed.increment();
	}

	public void recordSagaDuration(LocalDateTime startTime, LocalDateTime endTime) {
		if (startTime != null && endTime != null) {
			Duration duration = Duration.between(startTime, endTime);
			sagaDuration.record(duration);
		}
	}

	public void recordStateTransition(String fromState, String toState) {
		Counter.builder("saga.state.transition")
			.tag("from", fromState)
			.tag("to", toState)
			.register(registry)
			.increment();
	}

}
