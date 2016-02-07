package es.revib.server.rest.entities.constraints;

import es.revib.server.rest.util.CodingUtilities;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CheckTemporalValidator implements ConstraintValidator<CheckTemporal, Long> {

    private CodingUtilities.Temporal temporal;

    public void initialize(CheckTemporal constraintAnnotation) {
        this.temporal = constraintAnnotation.value();
    }

    public boolean isValid(Long object, ConstraintValidatorContext constraintContext) {

        if (object == null)
            return false; //not sure about this maybe return true because we can use the @NotNull validator anyways , we don't need it here too

            return new CodingUtilities().checkDate(object,temporal);
    }
}
