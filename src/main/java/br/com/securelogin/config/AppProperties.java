package br.com.securelogin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações customizáveis da aplicação, lidas do prefixo {@code app.*}
 * em {@code application.properties} (ou variáveis de ambiente, ex.: APP_THEME_NAME).
 *
 * <p>Centralizar aqui o que muda de projeto para projeto (nome, tema, layout, política
 * de bloqueio) permite adaptar o sistema ao PFC sem mexer na lógica central.</p>
 *
 * @param name     nome exibido no cabeçalho e no título das páginas
 * @param theme    configuração visual (tema ativo)
 * @param security parâmetros de segurança ajustáveis
 * @param admin    credenciais do administrador inicial (criado se não houver nenhum)
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String name, Theme theme, Security security, Admin admin) {

    public AppProperties {
        if (name == null || name.isBlank()) name = "Secure Login";
        if (theme == null) theme = new Theme(null);
        if (security == null) security = new Security(0, 0);
        if (admin == null) admin = new Admin(null, null, null);
    }

    /**
     * @param name nome do tema ativo. Define a pasta do CSS ({@code static/themes/<name>/theme.css})
     *             e a pasta de templates que sobrescrevem os padrões ({@code templates/themes/<name>/}).
     */
    public record Theme(String name) {
        public Theme {
            if (name == null || name.isBlank()) name = "padrao";
        }
    }

    /**
     * @param maxFailedAttempts tentativas de login erradas antes de bloquear a conta
     * @param lockMinutes       minutos de bloqueio temporário
     */
    public record Security(int maxFailedAttempts, int lockMinutes) {
        public Security {
            if (maxFailedAttempts <= 0) maxFailedAttempts = 5;
            if (lockMinutes <= 0) lockMinutes = 15;
        }
    }

    /** Administrador inicial (opcional). Sem senha definida, uma senha aleatória é gerada e exibida no log. */
    public record Admin(String name, String email, String password) {
    }
}
