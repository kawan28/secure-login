package br.com.securelogin.config;

import br.com.securelogin.domain.Role;
import br.com.securelogin.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;

/**
 * Cria o primeiro administrador quando o banco ainda não possui nenhum.
 *
 * <p>As credenciais vêm de {@code app.admin.*} (variáveis ADMIN_EMAIL / ADMIN_PASSWORD).
 * Se a senha não for informada, uma senha aleatória forte é gerada e exibida UMA vez
 * no log — troque-a no primeiro acesso em "Meu perfil".</p>
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserService userService;
    private final AppProperties.Admin adminProps;

    public AdminInitializer(UserService userService, AppProperties appProperties) {
        this.userService = userService;
        this.adminProps = appProperties.admin();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userService.adminExists()) {
            return;
        }
        String email = isBlank(adminProps.email()) ? "admin@admin.com" : adminProps.email();
        String name = isBlank(adminProps.name()) ? "Administrador" : adminProps.name();
        String password = adminProps.password();
        boolean generated = isBlank(password);
        if (generated) {
            password = generatePassword();
        }

        userService.createUser(name, email, password, EnumSet.of(Role.ADMIN));

        if (generated) {
            log.warn("""

                    ==============================================================
                     Administrador inicial criado
                     E-mail: {}
                     Senha : {}
                     (defina ADMIN_PASSWORD para escolher a senha; troque-a após o 1º login)
                    ==============================================================""", email, password);
        } else {
            log.info("Administrador inicial criado com o e-mail {}", email);
        }
    }

    /** Senha aleatória que sempre atende à política de senha forte. */
    private static String generatePassword() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return "Adm#" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + "9a";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
