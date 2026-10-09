package br.com.securelogin.security;

import br.com.securelogin.service.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Proteção contra ataques de força bruta.
 *
 * <p>Escuta os eventos de autenticação publicados pelo Spring Security:</p>
 * <ul>
 *   <li>senha errada → incrementa o contador; ao atingir o limite, bloqueia a conta
 *       temporariamente ({@code app.security.max-failed-attempts} / {@code lock-minutes});</li>
 *   <li>login com sucesso → zera o contador e registra a data do último acesso.</li>
 * </ul>
 */
@Component
public class LoginAttemptListener {

    private final UserService userService;

    public LoginAttemptListener(UserService userService) {
        this.userService = userService;
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        userService.registerFailedLogin(event.getAuthentication().getName());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        userService.registerSuccessfulLogin(event.getAuthentication().getName());
    }
}
