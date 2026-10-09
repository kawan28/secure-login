package br.com.securelogin.config;

import br.com.securelogin.security.LoginFailureHandler;
import jakarta.servlet.DispatcherType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Configuração central do Spring Security: QUEM pode acessar O QUÊ.
 *
 * <h2>Mapa de rotas x perfis</h2>
 * <pre>
 *  Público ............ /, /login, /cadastro, arquivos estáticos (/themes/**, /css/**)
 *  USER, MANAGER, ADMIN /painel, /usuario/**
 *  MANAGER, ADMIN ..... /gestao/**
 *  ADMIN .............. /admin/**
 * </pre>
 *
 * <p>Para adaptar ao seu PFC, altere apenas as regras de {@code authorizeHttpRequests}
 * e o enum {@code Role}. Os controllers também usam {@code @PreAuthorize} como segunda
 * camada de proteção (defesa em profundidade).</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize nos controllers/serviços
public class SecurityConfig {

    /** Recursos estáticos e páginas que não exigem login. */
    private static final String[] PUBLIC_PATHS = {
            "/", "/login", "/cadastro",
            "/themes/**", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   LoginFailureHandler loginFailureHandler) throws Exception {
        http
            // ---------------- AUTORIZAÇÃO (controle de acesso por perfil) ----------------
            .authorizeHttpRequests(auth -> auth
                // Encaminhamentos internos (páginas de erro) não devem ser bloqueados
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/gestao/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/painel", "/usuario/**").hasAnyRole("ADMIN", "MANAGER", "USER")
                // Qualquer rota nova exige login por padrão (secure by default)
                .anyRequest().authenticated()
            )

            // ---------------- LOGIN ----------------
            .formLogin(form -> form
                .loginPage("/login")                 // nossa página Thymeleaf
                .loginProcessingUrl("/login")        // POST tratado pelo próprio Spring Security
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/painel", true)
                .failureHandler(loginFailureHandler)
                .permitAll()
            )

            // ---------------- LOGOUT ----------------
            // Somente via POST (com token CSRF), evitando logout forçado por links maliciosos.
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)         // remove a sessão do MongoDB
                .clearAuthentication(true)
                .deleteCookies("SESSION")            // cookie criado pelo Spring Session
                .permitAll()
            )

            // ---------------- ACESSO NEGADO (HTTP 403) ----------------
            .exceptionHandling(ex -> ex.accessDeniedPage("/acesso-negado"))

            // ---------------- SESSÃO ----------------
            // Gera um novo ID de sessão a cada login (proteção contra session fixation).
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.changeSessionId())
            )

            // ---------------- CABEÇALHOS DE SEGURANÇA ----------------
            // CSRF já vem habilitado por padrão; o Thymeleaf insere o token nos formulários com th:action.
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; "
                        + "form-action 'self'; frame-ancestors 'none'; base-uri 'self'"))
                .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .frameOptions(frame -> frame.deny())
            );

        return http.build();
    }

    /**
     * BCrypt com fator de custo 12: cada hash leva ~250 ms, o que torna ataques de
     * força bruta offline muito caros, mas não prejudica o usuário no login.
     * O "salt" aleatório é gerado e embutido no próprio hash automaticamente.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Publica eventos de sucesso/falha de autenticação, consumidos pelo
     * {@code LoginAttemptListener} para bloquear contas após várias tentativas erradas.
     */
    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher publisher) {
        return new DefaultAuthenticationEventPublisher(publisher);
    }
}
