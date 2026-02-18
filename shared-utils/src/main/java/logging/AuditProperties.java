package logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "audit")
public record AuditProperties(boolean enabled, String exchange, String routingKey, String serviceName,
		String environment) {
}
