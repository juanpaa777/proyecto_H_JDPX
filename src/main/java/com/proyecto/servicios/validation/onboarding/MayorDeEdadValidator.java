package com.proyecto.servicios.validation.onboarding;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

public class MayorDeEdadValidator implements ConstraintValidator<MayorDeEdad, LocalDate> {

    @Override
    public boolean isValid(LocalDate fechaNacimiento, ConstraintValidatorContext context) {
        // En Bean Validation estándar, null es válido para permitir campos opcionales;
        // la obligatoriedad se controla con @NotNull
        if (fechaNacimiento == null) {
            return true;
        }
        LocalDate hoy = LocalDate.now();
        if (fechaNacimiento.isAfter(hoy)) {
            return false;
        }
        return Period.between(fechaNacimiento, hoy).getYears() >= 18;
    }
}
