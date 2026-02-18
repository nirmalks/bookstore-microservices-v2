package logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class RabbitAuditPublisher implements AuditPublisher {

	private final RabbitTemplate rabbitTemplate;

	private final AuditProperties props;

	private final ObjectMapper objectMapper;

	public RabbitAuditPublisher(RabbitTemplate rabbitTemplate, AuditProperties props) {
		this.rabbitTemplate = rabbitTemplate;
		this.props = props;
		this.objectMapper = new ObjectMapper();
		this.objectMapper.registerModule(new JavaTimeModule());
	}

	@Override
	public void publish(AuditEvent auditEvent) {
		try {
			byte[] body = objectMapper.writeValueAsBytes(auditEvent);
			Message message = MessageBuilder.withBody(body).setContentType(MessageProperties.CONTENT_TYPE_JSON).build();
			rabbitTemplate.send(props.exchange(), props.routingKey(), message);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to serialize audit event", e);
		}
	}

}
