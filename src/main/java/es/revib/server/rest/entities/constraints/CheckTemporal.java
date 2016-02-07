package es.revib.server.rest.entities.constraints;

import es.revib.server.rest.util.CodingUtilities;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target( { METHOD, FIELD, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = CheckTemporalValidator.class)
@Documented
public @interface CheckTemporal {

    String message() default "{es.revib.server.rest.entities.constraints.checkTemporal}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    CodingUtilities.Temporal value();

}