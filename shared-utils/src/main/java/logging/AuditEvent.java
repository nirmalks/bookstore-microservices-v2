package logging;

import java.time.Instant;

public record AuditEvent(String schemaVersion, String eventId, Instant occurredAt, String serviceName,
		String environment, String action, String resource, String resourceId, String status, String principal,
		String traceId, String spanId, String idempotencyKey, String detail, String errorCode, String errorMessage) {

}
