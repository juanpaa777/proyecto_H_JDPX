package com.proyecto.servicios.validation.onboarding;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Optional;
import java.util.regex.Pattern;

@SuppressWarnings("java:S2068")
public class PasswordSeguroValidator implements ConstraintValidator<PasswordSeguro, String> {

    // Al menos 8 caracteres y máximo 100 caracteres, 1 mayúscula, 1 minúscula, 1 número, 1 símbolo (sin espacios)
    private static final String PWD_PATTERN_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,100}$";

    private static final Pattern PATTERN = Pattern.compile(PWD_PATTERN_REGEX);

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        return Optional.ofNullable(password)
                .filter(p -> !p.isBlank())
                .map(p -> PATTERN.matcher(p).matches())
                .orElse(true);
    }
}
