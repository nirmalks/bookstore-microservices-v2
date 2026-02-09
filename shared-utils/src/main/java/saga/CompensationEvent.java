package saga;

import java.io.Serializable;

public record CompensationEvent(String sagaId, String eventId, String compensationType, String originalEventId,
		String payload, String reason) implements Serializable {

}
