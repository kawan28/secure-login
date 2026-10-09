package br.com.securelogin.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validação de senha forte (anotação reutilizável em qualquer formulário).
 *
 * <p>Regras: 8 a 64 caracteres, ao menos uma letra maiúscula, uma minúscula, um número,
 * um caractere especial e nenhum espaço. O limite de 64 respeita o máximo de 72 bytes
 * do BCrypt.</p>
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "A senha deve ter de 8 a 64 caracteres, com letra maiúscula, minúscula, número e caractere especial, sem espaços.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
