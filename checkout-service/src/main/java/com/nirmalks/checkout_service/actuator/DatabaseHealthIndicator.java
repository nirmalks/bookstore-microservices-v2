package com.nirmalks.checkout_service.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Custom health indicator for database connectivity and query performance
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

	private final DataSource dataSource;

	public DatabaseHealthIndicator(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public Health health() {
		try {
			long startTime = System.currentTimeMillis();

			try (Connection connection = dataSource.getConnection();
					Statement statement = connection.createStatement();
					ResultSet resultSet = statement.executeQuery("SELECT 1")) {

				long responseTime = System.currentTimeMillis() - startTime;

				if (resultSet.next()) {
					return Health.up()
						.withDetail("database", "PostgreSQL")
						.withDetail("responseTime", responseTime + "ms")
						.withDetail("status", "Connection successful")
						.withDetail("catalog", connection.getCatalog())
						.build();
				}
				else {
					return Health.down().withDetail("error", "Query returned no results").build();
				}
			}
		}
		catch (Exception e) {
			return Health.down().withDetail("error", e.getMessage()).withException(e).build();
		}
	}

}
