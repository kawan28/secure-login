package br.com.securelogin.domain;

/**
 * Perfis (roles) de acesso do sistema.
 *
 * <p>Para adaptar ao tema do PFC basta renomear/acrescentar constantes aqui
 * (ex.: PROFESSOR, ALUNO, COORDENADOR) e ajustar as regras de rota em
 * {@code SecurityConfig}. O restante do código (cadastro, painel admin, templates)
 * já trabalha de forma genérica sobre {@code Role.values()}.</p>
 */
public enum Role {

    /** Acesso total: gerencia usuários e perfis. */
    ADMIN("Administrador"),

    /** Acesso intermediário: área de gestão/relatórios. */
    MANAGER("Gestor"),

    /** Acesso básico: perfil próprio e área comum. Perfil padrão no cadastro. */
    USER("Usuário");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    /** Nome amigável exibido nas telas. */
    public String getLabel() {
        return label;
    }

    /** Nome da autoridade no formato esperado pelo Spring Security (prefixo ROLE_). */
    public String authority() {
        return "ROLE_" + name();
    }
}
