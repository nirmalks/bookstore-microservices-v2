package logging;

public interface AuditPublisher {

	void publish(AuditEvent auditEvent);

}
