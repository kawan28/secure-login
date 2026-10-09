package br.com.securelogin.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da política de senha forte (não precisam de banco nem do Spring). */
class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @ParameterizedTest
    @ValueSource(strings = {"Senha@123", "Abcdef1!", "Muito$egura2026"})
    void aceitaSenhasFortes(String senha) {
        assertTrue(validator.isValid(senha, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Ab1!",            // curta demais
            "senha@123",       // sem maiúscula
            "SENHA@123",       // sem minúscula
            "Senha@abc",       // sem número
            "Senha1234",       // sem caractere especial
            "Senha @123"       // contém espaço
    })
    void rejeitaSenhasFracas(String senha) {
        assertFalse(validator.isValid(senha, null));
    }

    @Test
    void rejeitaSenhaMaiorQueOLimiteDoBcrypt() {
        String longa = "Aa1!" + "x".repeat(StrongPasswordValidator.MAX_LENGTH);
        assertFalse(validator.isValid(longa, null));
    }
}
