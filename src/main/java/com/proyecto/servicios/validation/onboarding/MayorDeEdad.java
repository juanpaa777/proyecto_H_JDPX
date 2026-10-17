package com.proyecto.servicios.validation.onboarding;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MayorDeEdadValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MayorDeEdad {
    String message() default "El cliente debe ser mayor de edad (18 años o más) y la fecha de nacimiento no puede ser futura";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
