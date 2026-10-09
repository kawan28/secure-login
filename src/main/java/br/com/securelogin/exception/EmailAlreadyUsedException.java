package br.com.securelogin.exception;

/** Lançada no cadastro quando o e-mail informado já pertence a outro usuário. */
public class EmailAlreadyUsedException extends BusinessException {

    public EmailAlreadyUsedException() {
        super("Este e-mail já está cadastrado.");
    }
}
