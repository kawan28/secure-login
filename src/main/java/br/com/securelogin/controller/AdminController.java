package br.com.securelogin.controller;

import br.com.securelogin.domain.Role;
import br.com.securelogin.exception.BusinessException;
import br.com.securelogin.security.SecurityUser;
import br.com.securelogin.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

/**
 * Administração de usuários (somente ADMIN): listar, alterar perfis,
 * ativar/desativar contas e desbloquear contas bloqueadas por tentativas.
 *
 * <p>Todas as ações que alteram dados são POST (protegidas por token CSRF).</p>
 */
@Controller
@RequestMapping("/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("allRoles", Role.values());
        return "admin/users";
    }

    @PostMapping("/{id}/perfis")
    public String updateRoles(@PathVariable String id,
                              @RequestParam(name = "roles", required = false) Set<Role> roles,
                              @AuthenticationPrincipal SecurityUser principal,
                              RedirectAttributes redirect) {
        return execute(redirect, "Perfis atualizados.",
                () -> userService.updateRoles(id, roles, principal.getUsername()));
    }

    @PostMapping("/{id}/ativar")
    public String enable(@PathVariable String id, @AuthenticationPrincipal SecurityUser principal,
                         RedirectAttributes redirect) {
        return execute(redirect, "Conta ativada.",
                () -> userService.setEnabled(id, true, principal.getUsername()));
    }

    @PostMapping("/{id}/desativar")
    public String disable(@PathVariable String id, @AuthenticationPrincipal SecurityUser principal,
                          RedirectAttributes redirect) {
        return execute(redirect, "Conta desativada.",
                () -> userService.setEnabled(id, false, principal.getUsername()));
    }

    @PostMapping("/{id}/desbloquear")
    public String unlock(@PathVariable String id, RedirectAttributes redirect) {
        return execute(redirect, "Conta desbloqueada.", () -> userService.unlock(id));
    }

    /** Executa a ação e converte o resultado em mensagem de sucesso/erro na listagem. */
    private String execute(RedirectAttributes redirect, String successMessage, Runnable action) {
        try {
            action.run();
            redirect.addFlashAttribute("successMessage", successMessage);
        } catch (BusinessException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }
}
