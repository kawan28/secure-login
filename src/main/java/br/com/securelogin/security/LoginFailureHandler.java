package br.com.securelogin.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Decide para onde redirecionar quando o login falha, informando o motivo à tela
 * de login por meio de um código (a mensagem amigável fica no template).
 *
 * <p>Para credenciais inválidas a mensagem é sempre genérica ("e-mail ou senha
 * inválidos"), para não revelar se o e-mail existe.</p>
 */
@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String reason;
        if (exception instanceof LockedException) {
            reason = "bloqueado";
        } else if (exception instanceof DisabledException) {
            reason = "desativado";
        } else {
            reason = "credenciais";
        }
        getRedirectStrategy().sendRedirect(request, response, "/login?erro=" + reason);
    }
}
