package logging;

import java.time.Instant;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Aspect
public class AuditAspect {

	private static final Logger logger = LoggerFactory.getLogger(AuditAspect.class);

	private static final String SCHEMA_VERSION = "1.0";

	private final AuditPublisher publisher;

	private final AuditProperties props;

	private final ExpressionParser spel = new SpelExpressionParser();

	private final DefaultParameterNameDiscoverer names = new DefaultParameterNameDiscoverer();

	public AuditAspect(AuditPublisher publisher, AuditProperties props) {
		this.publisher = publisher;
		this.props = props;
	}

	@Around("@annotation(auditable)")
	public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
		Object result = null;
		String status = "SUCCESS";
		String errorCode = null;
		String errorMessage = null;

		try {
			result = joinPoint.proceed();
			return result;
		}
		catch (Exception ex) {
			status = "FAILURE";
			errorCode = ex.getClass().getSimpleName();
			errorMessage = sanitize(ex.getMessage());
			throw ex;
		}
		finally {
			try {
				String resourceId = evalResourceId(auditable.resourceId(), joinPoint, result);
				String principal = resolvePrincipal();
				String traceId = MDC.get("traceId");
				String spanId = MDC.get("spanId");
				AuditEvent event = new AuditEvent(SCHEMA_VERSION, UUID.randomUUID().toString(), Instant.now(),
						safe(props.serviceName()), safe(props.environment()), auditable.action(), auditable.resource(),
						resourceId, status, principal, traceId, spanId,
						buildIdempotencyKey(auditable.action(), auditable.resource(), resourceId, traceId),
						sanitize(auditable.detail()), errorCode, errorMessage);

				publisher.publish(event);
				logger.info("Published audit event: {}", event);
			}
			catch (Exception publishEx) {
				// Never fail business flow due to audit transport failure
				logger.error("Audit publish failed: {}", publishEx.getMessage(), publishEx);
			}

		}
	}

	private String evalResourceId(String expr, ProceedingJoinPoint joinPoint, Object result) {
		if (expr == null || expr.isBlank()) {
			return "";
		}
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		String[] paramNames = names.getParameterNames(signature.getMethod());

		Object[] args = joinPoint.getArgs();
		EvaluationContext ctx = new StandardEvaluationContext();
		if (paramNames != null) {
			for (int i = 0; i < paramNames.length; i++) {
				ctx.setVariable(paramNames[i], args[i]);
				ctx.setVariable("p" + i, args[i]);
			}
		}
		ctx.setVariable("result", result);
		Object val = spel.parseExpression(expr).getValue(ctx);
		return val == null ? "" : String.valueOf(val);
	}

	private String resolvePrincipal() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return (auth == null || auth.getName() == null) ? "anonymous" : auth.getName();
	}

	private String buildIdempotencyKey(String action, String resource, String resourceId, String traceId) {
		return String.join("|", safe(action), safe(resource), safe(resourceId), safe(traceId));
	}

	private String sanitize(String value) {
		if (value == null) {
			return null;
		}
		return value.length() > 300 ? value.substring(0, 300) : value;
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}

}
