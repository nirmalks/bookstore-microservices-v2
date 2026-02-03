package locking;

public class LockKeys {

	private LockKeys() {
	}

	public static String bookStock(Long bookId) {
		return "lock:book:stock:" + bookId;
	}

	public static String userCart(Long userId) {
		return "lock:cart:user:" + userId;
	}

	public static String orderStatus(String orderId) {
		return "lock:order:status:" + orderId;
	}

	public static final String OUTBOX_RELAY = "lock:outbox:relay";

	public static final String IDEMPOTENCY_CLEANUP_CHECKOUT = "lock:cleanup:idempotency:checkout";

	public static final String IDEMPOTENCY_CLEANUP_CATALOG = "lock:cleanup:idempotency:catalog";

	public static final String IDEMPOTENCY_CLEANUP_NOTIFICATION = "lock:cleanup:idempotency:notification";

}
