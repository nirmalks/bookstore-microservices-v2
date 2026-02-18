package logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuditAspectTest {

	private final AuditPublisher publisher = Mockito.mock(AuditPublisher.class);

	private final AuditProperties props = new AuditProperties(true, "audit.exchange", "audit.event", "test-service",
			"test");

	@AfterEach
	void cleanup() {
		MDC.clear();
		SecurityContextHolder.clearContext();
	}

	@Test
	void publishesSuccessEvent() {
		MDC.put("traceId", "trace-1");
		MDC.put("spanId", "span-1");
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("alice", "pwd"));

		TestService target = new TestService();
		TestService proxy = proxied(target);

		Result result = proxy.create(123L);
		assertEquals(123L, result.getId());

		ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
		verify(publisher, times(1)).publish(captor.capture());
		AuditEvent event = captor.getValue();

		assertEquals("SUCCESS", event.status());
		assertEquals("CREATE_BOOK", event.action());
		assertEquals("BOOK", event.resource());
		assertEquals("123", event.resourceId());
		assertEquals("alice", event.principal());
		assertEquals("trace-1", event.traceId());
		assertEquals("span-1", event.spanId());
	}

	@Test
	void publishesFailureEventAndRethrows() {
		TestService proxy = proxied(new TestService());

		assertThrows(IllegalStateException.class, () -> proxy.fail(7L));

		ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
		verify(publisher, times(1)).publish(captor.capture());
		AuditEvent event = captor.getValue();

		assertEquals("FAILURE", event.status());
		assertEquals("FAIL_BOOK", event.action());
		assertEquals("7", event.resourceId());
		assertEquals("IllegalStateException", event.errorCode());
	}

	private TestService proxied(TestService target) {
		AspectJProxyFactory factory = new AspectJProxyFactory(target);
		factory.addAspect(new AuditAspect(publisher, props));
		return factory.getProxy();
	}

	static class TestService {

		@Auditable(action = "CREATE_BOOK", resource = "BOOK", resourceId = "#result.id", detail = "create")
		Result create(Long id) {
			return new Result(id);
		}

		@Auditable(action = "FAIL_BOOK", resource = "BOOK", resourceId = "#id", detail = "fail")
		void fail(Long id) {
			throw new IllegalStateException("boom");
		}

	}

	static class Result {

		private final Long id;

		Result(Long id) {
			this.id = id;
		}

		public Long getId() {
			return this.id;
		}

	}

}
