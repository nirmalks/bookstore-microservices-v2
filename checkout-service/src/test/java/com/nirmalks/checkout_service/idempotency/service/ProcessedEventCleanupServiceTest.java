package com.nirmalks.checkout_service.idempotency.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nirmalks.checkout_service.idempotency.repository.ProcessedEventRepository;

import locking.DistributedLockService;

@ExtendWith(MockitoExtension.class)
class ProcessedEventCleanupServiceTest {

	@InjectMocks
	ProcessedEventCleanupService processedEventCleanupService;

	@Mock
	ProcessedEventRepository processedEventRepository;

	@Mock
	DistributedLockService distributedLockService;

	@Test
	void cleanupOldProcessedEvents_should_cleanup_old_processed_events_successfully() {
		doAnswer(invocation -> {
			Runnable runnable = invocation.getArgument(4);
			runnable.run();
			return true;
		}).when(distributedLockService)
			.tryExecuteWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));

		processedEventCleanupService.cleanupOldProcessedEvents();

		verify(distributedLockService, times(1)).tryExecuteWithLock(anyString(), anyLong(), anyLong(),
				any(TimeUnit.class), any(Runnable.class));
		verify(processedEventRepository, times(1)).deleteByProcessedAtBefore(any(Instant.class));
	}

	@Test
	void cleanupOldProcessedEvents_should_skip_cleanup_when_lock_already_held() {
		doReturn(false).when(distributedLockService)
			.tryExecuteWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));

		processedEventCleanupService.cleanupOldProcessedEvents();

		verify(distributedLockService, times(1)).tryExecuteWithLock(anyString(), anyLong(), anyLong(),
				any(TimeUnit.class), any(Runnable.class));
		verify(processedEventRepository, never()).deleteByProcessedAtBefore(any(Instant.class));
	}

}
