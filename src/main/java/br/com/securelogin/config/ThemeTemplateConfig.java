package br.com.securelogin.config;

import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;

/**
 * Sistema de temas por "sobrescrita de templates".
 *
 * <p>Registra um resolvedor de templates com prioridade maior que o padrão, que procura
 * primeiro em {@code templates/themes/<tema>/}. Se o arquivo existir lá, ele é usado;
 * caso contrário o Thymeleaf cai no template padrão em {@code templates/}.</p>
 *
 * <p>Exemplo: com {@code app.theme.name=escuro}, a página {@code auth/login} é procurada em
 * {@code templates/themes/escuro/auth/login.html} e, se não existir, em
 * {@code templates/auth/login.html}. Um tema pode portanto trocar só o CSS, só o layout,
 * ou qualquer página — sem alterar controllers ou regras de negócio.</p>
 */
@Configuration
public class ThemeTemplateConfig {

    @Bean
    public ClassLoaderTemplateResolver themeTemplateResolver(AppProperties appProperties,
                                                             ThymeleafProperties thymeleafProperties) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/themes/" + appProperties.theme().name() + "/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(thymeleafProperties.isCache());
        resolver.setOrder(0);              // consultado ANTES do resolvedor padrão do Spring Boot
        resolver.setCheckExistence(true);  // se não existir no tema, passa para o próximo resolvedor
        return resolver;
    }
}
