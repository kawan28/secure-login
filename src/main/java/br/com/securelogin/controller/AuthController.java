package br.com.securelogin.controller;

import br.com.securelogin.dto.RegistrationForm;
import br.com.securelogin.exception.EmailAlreadyUsedException;
import br.com.securelogin.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Telas de login e cadastro.
 *
 * <p>Observação: o POST de /login e o POST de /logout NÃO estão aqui — eles são
 * processados diretamente pelos filtros do Spring Security (ver {@code SecurityConfig}).</p>
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        // Usuário já autenticado não precisa ver a tela de login novamente
        if (isLoggedIn(authentication)) {
            return "redirect:/painel";
        }
        return "auth/login";
    }

    @GetMapping("/cadastro")
    public String registerForm(Authentication authentication, Model model) {
        if (isLoggedIn(authentication)) {
            return "redirect:/painel";
        }
        model.addAttribute("form", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/cadastro")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult result,
                           RedirectAttributes redirect) {
        // 1) Erros de validação (campos obrigatórios, formato de e-mail, senha fraca...)
        if (result.hasErrors()) {
            clearPasswords(form);
            return "auth/register";
        }
        // 2) Regras de negócio (e-mail duplicado)
        try {
            userService.register(form);
        } catch (EmailAlreadyUsedException e) {
            result.rejectValue("email", "email.duplicado", e.getMessage());
            clearPasswords(form);
            return "auth/register";
        }
        // Padrão Post/Redirect/Get: evita reenvio do formulário ao atualizar a página
        redirect.addFlashAttribute("successMessage", "Cadastro realizado! Faça login para continuar.");
        return "redirect:/login";
    }

    /** As senhas nunca são devolvidas preenchidas para a página. */
    private static void clearPasswords(RegistrationForm form) {
        form.setPassword(null);
        form.setConfirmPassword(null);
    }

    private static boolean isLoggedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
