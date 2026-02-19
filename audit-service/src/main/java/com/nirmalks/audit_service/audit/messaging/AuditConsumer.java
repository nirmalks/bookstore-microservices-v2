package com.nirmalks.audit_service.audit.messaging;

import com.nirmalks.audit_service.audit.service.AuditIngestionService;
import logging.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditConsumer {

	private final AuditIngestionService ingestionService;

	@RabbitListener(queues = "audit.queue")
	public void onAuditEvent(AuditEvent event) {
		log.info("Received audit event [eventId={}, action={}, resource={}, status={}, traceId={}]", event.eventId(),
				event.action(), event.resource(), event.status(), event.traceId());
		ingestionService.ingest(event);
	}

}
