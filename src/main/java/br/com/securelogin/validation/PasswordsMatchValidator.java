package br.com.securelogin.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;

/** Implementação de {@link PasswordsMatch}. */
public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, PasswordConfirmation> {

    private String message;

    @Override
    public void initialize(PasswordsMatch annotation) {
        this.message = annotation.message();
    }

    @Override
    public boolean isValid(PasswordConfirmation form, ConstraintValidatorContext context) {
        if (form == null || Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            return true;
        }
        // Associa o erro ao campo de confirmação (em vez de um erro global do formulário)
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("confirmPassword")
                .addConstraintViolation();
        return false;
    }
}
