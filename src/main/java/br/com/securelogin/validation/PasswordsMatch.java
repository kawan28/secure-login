package br.com.securelogin.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validação de classe: garante que senha e confirmação são iguais.
 * O erro é associado ao campo {@code confirmPassword} para aparecer ao lado dele no formulário.
 */
@Documented
@Constraint(validatedBy = PasswordsMatchValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordsMatch {

    String message() default "As senhas não conferem.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
