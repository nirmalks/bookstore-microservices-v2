package com.nirmalks.catalog_service.idempotency.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nirmalks.catalog_service.idempotency.repository.ProcessedEventRepository;

import locking.DistributedLockService;

@ExtendWith(MockitoExtension.class)
class ProcessedEventCleanupServiceTest {

	@InjectMocks
	private ProcessedEventCleanupService processedEventCleanupService;

	@Mock
	private ProcessedEventRepository processedEventRepository;

	@Mock
	private DistributedLockService distributedLockService;

	@Test
	void cleanupOldProcessedEvents_should_cleanup_old_processed_events_if_lock_is_acquired() {
		when(distributedLockService.tryExecuteWithLock(anyString(), anyLong(), anyLong(), any(), any()))
			.thenAnswer(invocation -> {
				invocation.getArgument(4, Runnable.class).run();
				return true;
			});
		processedEventCleanupService.cleanupOldProcessedEvents();
		verify(processedEventRepository).deleteByProcessedAtBefore(any());
	}

	@Test
	void cleanupOldProcessedEvents_should_not_cleanup_old_processed_events_if_lock_is_not_acquired() {
		when(distributedLockService.tryExecuteWithLock(anyString(), anyLong(), anyLong(), any(), any()))
			.thenReturn(false);

		processedEventCleanupService.cleanupOldProcessedEvents();

		verify(distributedLockService).tryExecuteWithLock(anyString(), anyLong(), anyLong(), any(), any());
		verifyNoInteractions(processedEventRepository);
	}

}
