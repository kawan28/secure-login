package br.com.securelogin.exception;

/**
 * Erro de regra de negócio cuja mensagem pode ser exibida ao usuário
 * (ex.: "e-mail já cadastrado", "não é possível remover o último administrador").
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
