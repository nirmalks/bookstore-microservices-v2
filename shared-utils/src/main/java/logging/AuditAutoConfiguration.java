package logging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = RabbitAutoConfiguration.class)
@EnableConfigurationProperties(AuditProperties.class)
@ConditionalOnProperty(prefix = "audit", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnBean(RabbitTemplate.class)
public class AuditAutoConfiguration {

	public static final String AUDIT_EXCHANGE = "audit.exchange";

	public static final String AUDIT_QUEUE = "audit.queue";

	public static final String AUDIT_ROUTING_KEY = "audit.event";

	@Bean
	@ConditionalOnMissingBean(name = "auditExchange")
	public TopicExchange auditExchange() {
		return ExchangeBuilder.topicExchange(AUDIT_EXCHANGE).durable(true).build();
	}

	@Bean
	@ConditionalOnMissingBean(name = "auditQueue")
	public Queue auditQueue() {
		return QueueBuilder.durable(AUDIT_QUEUE).build();
	}

	@Bean
	@ConditionalOnMissingBean(name = "auditBinding")
	public Binding auditBinding() {
		return BindingBuilder.bind(auditQueue()).to(auditExchange()).with("audit.#");
	}

	@Bean
	@ConditionalOnMissingBean
	public AuditPublisher auditPublisher(RabbitTemplate rabbitTemplate, AuditProperties props) {
		return new RabbitAuditPublisher(rabbitTemplate, props);
	}

	@Bean
	@ConditionalOnMissingBean
	public AuditAspect auditAspect(AuditPublisher publisher, AuditProperties props) {
		return new AuditAspect(publisher, props);
	}

}
