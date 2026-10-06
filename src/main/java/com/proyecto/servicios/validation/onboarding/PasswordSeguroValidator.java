package com.proyecto.servicios.validation.onboarding;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PasswordSeguroValidator implements ConstraintValidator<PasswordSeguro, String> {

    // Al menos 8 caracteres, 1 mayúscula, 1 minúscula, 1 número, 1 carácter especial
    private static final String PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._#+\\-])[A-Za-z\\d@$!%*?&._#+\\-]{8,}$";

    private static final Pattern PATTERN = Pattern.compile(PASSWORD_REGEX);

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        return PATTERN.matcher(password).matches();
    }
}
