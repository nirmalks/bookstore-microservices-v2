package com.nirmalks.bookstore.notification_service.idempotency.service;

import com.nirmalks.bookstore.notification_service.idempotency.repository.ProcessedEventRepository;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import locking.DistributedLockService;
import locking.LockKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessedEventCleanupServiceTest {

	@Mock
	private ProcessedEventRepository processedEventRepository;

	@Mock
	private DistributedLockService distributedLockService;

	@InjectMocks
	private ProcessedEventCleanupService cleanupService;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(cleanupService, "retentionDays", 7);
	}

	@Test
	void cleanupOldProcessedEvents_deletes_old_processed_events_when_lock_is_acquired() {
		Instant beforeInvocation = Instant.now();
		when(distributedLockService.tryExecuteWithLock(eq(LockKeys.IDEMPOTENCY_CLEANUP_NOTIFICATION), eq(0L), eq(300L),
				eq(TimeUnit.SECONDS), any(Runnable.class)))
			.thenAnswer(invocation -> {
				Runnable runnable = invocation.getArgument(4);
				runnable.run();
				return true;
			});

		when(processedEventRepository.deleteByProcessedAtBefore(any(Instant.class))).thenReturn(10L);

		cleanupService.cleanupOldProcessedEvents();
		Instant afterInvocation = Instant.now();

		ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);

		verify(distributedLockService).tryExecuteWithLock(eq(LockKeys.IDEMPOTENCY_CLEANUP_NOTIFICATION), eq(0L),
				eq(300L), eq(TimeUnit.SECONDS), any(Runnable.class));
		verify(processedEventRepository).deleteByProcessedAtBefore(cutoffCaptor.capture());

		Instant capturedCutoff = cutoffCaptor.getValue();
		assertFalse(capturedCutoff.isBefore(beforeInvocation.minusSeconds(TimeUnit.DAYS.toSeconds(7) + 1)));
		assertFalse(capturedCutoff.isAfter(afterInvocation.minusSeconds(TimeUnit.DAYS.toSeconds(7) - 1)));
	}

	@Test
	void cleanupOldProcessedEvents_does_not_delete_when_lock_is_not_acquired() {
		when(distributedLockService.tryExecuteWithLock(eq(LockKeys.IDEMPOTENCY_CLEANUP_NOTIFICATION), eq(0L), eq(300L),
				eq(TimeUnit.SECONDS), any(Runnable.class)))
			.thenReturn(false);

		cleanupService.cleanupOldProcessedEvents();

		verify(distributedLockService).tryExecuteWithLock(eq(LockKeys.IDEMPOTENCY_CLEANUP_NOTIFICATION), eq(0L),
				eq(300L), eq(TimeUnit.SECONDS), any(Runnable.class));
		verify(processedEventRepository, never()).deleteByProcessedAtBefore(any(Instant.class));
	}

	@Test
	void cleanupOldProcessedEvents_does_not_fail_when_repository_throws_exception() {
		when(distributedLockService.tryExecuteWithLock(eq(LockKeys.IDEMPOTENCY_CLEANUP_NOTIFICATION), eq(0L), eq(300L),
				eq(TimeUnit.SECONDS), any(Runnable.class)))
			.thenAnswer(invocation -> {
				Runnable runnable = invocation.getArgument(4);
				runnable.run();
				return true;
			});
		when(processedEventRepository.deleteByProcessedAtBefore(any(Instant.class)))
			.thenThrow(new RuntimeException("cleanup failed"));

		Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(ProcessedEventCleanupService.class);
		Level previousLevel = logger.getLevel();
		logger.setLevel(Level.OFF);
		try {
			assertDoesNotThrow(() -> cleanupService.cleanupOldProcessedEvents());
		}
		finally {
			logger.setLevel(previousLevel);
		}

		verify(processedEventRepository).deleteByProcessedAtBefore(any(Instant.class));
	}

}
