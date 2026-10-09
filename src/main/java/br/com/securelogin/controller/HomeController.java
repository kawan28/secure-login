package br.com.securelogin.controller;

import br.com.securelogin.security.SecurityUser;
import br.com.securelogin.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Página inicial pública, painel do usuário logado e página de acesso negado. */
@Controller
public class HomeController {

    private final UserService userService;

    public HomeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    /** Painel comum a todos os perfis; o conteúdo varia conforme o perfil (sec:authorize no template). */
    @GetMapping("/painel")
    public String dashboard(@AuthenticationPrincipal SecurityUser principal, Model model) {
        model.addAttribute("user", userService.findByEmail(principal.getUsername()));
        return "dashboard";
    }

    /** Exibida quando um usuário logado tenta acessar uma área sem permissão (HTTP 403). */
    @GetMapping("/acesso-negado")
    public String accessDenied(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return "error/403";
    }
}
