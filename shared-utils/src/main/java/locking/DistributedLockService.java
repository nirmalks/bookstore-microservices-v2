package locking;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DistributedLockService {

	private static final Logger logger = LoggerFactory.getLogger(DistributedLockService.class);

	private final RedissonClient redissonClient;

	public DistributedLockService(RedissonClient redissonClient) {
		this.redissonClient = redissonClient;
	}

	public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> action) {
		RLock lock = redissonClient.getLock(lockKey);
		var isLockAcquired = false;

		try {
			isLockAcquired = lock.tryLock(waitTime, leaseTime, unit);
			if (isLockAcquired) {
				logger.debug("Lock acquired for key: {}", lockKey);
				return action.get();
			}
			else {
				logger.warn("Could not acquire lock for key: {} within {} {}", lockKey, waitTime, unit);
				throw new LockAcquisitionException("Could not acquire lock for: " + lockKey);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LockAcquisitionException("Interrupted while acquiring lock: " + lockKey, e);
		}
		finally {
			if (isLockAcquired && lock.isHeldByCurrentThread()) {
				lock.unlock();
				logger.debug("Lock released for key: {}", lockKey);
			}
		}
	}

	public void executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Runnable action) {
		executeWithLock(lockKey, waitTime, leaseTime, unit, () -> {
			action.run();
			return null;
		});
	}

	public boolean tryExecuteWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Runnable action) {
		RLock lock = redissonClient.getLock(lockKey);
		var isLockAcquired = false;

		try {
			isLockAcquired = lock.tryLock(waitTime, leaseTime, unit);
			if (isLockAcquired) {
				logger.debug("Lock acquired for key: {}", lockKey);
				action.run();
				return true;
			}
			else {
				logger.debug("Lock not available for key: {}, skipping", lockKey);
				return false;
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
		finally {
			if (isLockAcquired && lock.isHeldByCurrentThread()) {
				lock.unlock();
				logger.debug("Lock released for key: {}", lockKey);
			}
		}
	}

}
