package saga;

import java.io.Serializable;
import java.time.Instant;

public record SagaEvent(String sagaId, String eventId, SagaState currenState, SagaState targetState,
		String sourceService, String payload, Instant timestamp, String errorMessage) implements Serializable {

}
