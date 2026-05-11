package org.egov.infra.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SanitizeHtmlValidator.class)
public @interface SanitizeHtml {

    String message() default "Invalid HTML content detected";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}