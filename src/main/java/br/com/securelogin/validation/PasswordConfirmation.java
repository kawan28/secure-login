package br.com.securelogin.validation;

/**
 * Contrato para formulários que possuem os campos "senha" e "confirmação de senha".
 * Permite reutilizar {@link PasswordsMatch} em qualquer formulário.
 */
public interface PasswordConfirmation {

    String getPassword();

    String getConfirmPassword();
}
