package com.nirmalks.catalog_service.actuator;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for RabbitMQ messaging system Checks connection status and
 * broker availability
 */
@Component
public class RabbitMQHealthIndicator implements HealthIndicator {

	private final RabbitTemplate rabbitTemplate;

	public RabbitMQHealthIndicator(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	@Override
	public Health health() {
		try {
			var connectionFactory = rabbitTemplate.getConnectionFactory();
			var connection = connectionFactory.createConnection();

			if (connection.isOpen()) {
				var channelMax = connection.toString().contains("channelMax")
						? connection.toString().split("channelMax=")[1].split(",")[0] : "N/A";

				connection.close();

				return Health.up()
					.withDetail("broker", "RabbitMQ")
					.withDetail("status", "Connected")
					.withDetail("channelMax", channelMax)
					.withDetail("host", connectionFactory.getHost())
					.withDetail("port", connectionFactory.getPort())
					.withDetail("virtualHost", connectionFactory.getVirtualHost())
					.build();
			}
			else {
				return Health.down().withDetail("error", "Connection not open").build();
			}
		}
		catch (Exception e) {
			return Health.down().withDetail("error", e.getMessage()).withException(e).build();
		}
	}

}
