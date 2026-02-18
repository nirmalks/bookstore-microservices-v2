package logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

	String action();

	String resource();

	String resourceId() default ""; // SpEL expression to extract ID from args/result

	String detail() default ""; // Optional lightweight note to avoid PII personal info

}
