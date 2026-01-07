package com.nirmalks.catalog_service.idempotency.service;

import com.nirmalks.catalog_service.idempotency.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled service to clean up old processed event records. This prevents the
 * processed_events table from growing unboundedly while still maintaining idempotency
 * guarantees for recent messages.
 */
@Service
public class ProcessedEventCleanupService {

	private static final Logger logger = LoggerFactory.getLogger(ProcessedEventCleanupService.class);

	private final ProcessedEventRepository processedEventRepository;

	@Value("${idempotency.cleanup.retention-days:7}")
	private int retentionDays;

	public ProcessedEventCleanupService(ProcessedEventRepository processedEventRepository) {
		this.processedEventRepository = processedEventRepository;
	}

	/**
	 * Runs daily at 2 AM to clean up old processed events. Events older than the
	 * configured retention period will be deleted.
	 */
	@Scheduled(cron = "${idempotency.cleanup.cron:0 0 2 * * ?}")
	@Transactional
	public void cleanupOldProcessedEvents() {
		Instant cutoffTime = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
		logger.info("Starting processed events cleanup. Deleting events older than {} (retention: {} days)", cutoffTime,
				retentionDays);

		try {
			long deletedCount = processedEventRepository.deleteByProcessedAtBefore(cutoffTime);
			logger.info("Processed events cleanup completed. Deleted {} old records", deletedCount);
		}
		catch (Exception e) {
			logger.error("Failed to cleanup processed events", e);
		}
	}

}
