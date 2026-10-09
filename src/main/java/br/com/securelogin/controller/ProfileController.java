package br.com.securelogin.controller;

import br.com.securelogin.dto.ChangePasswordForm;
import br.com.securelogin.exception.BusinessException;
import br.com.securelogin.security.SecurityUser;
import br.com.securelogin.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Área do próprio usuário (qualquer perfil): dados da conta e troca de senha. */
@Controller
@RequestMapping("/usuario")
@PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/perfil")
    public String profile(@AuthenticationPrincipal SecurityUser principal, Model model) {
        model.addAttribute("user", userService.findByEmail(principal.getUsername()));
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new ChangePasswordForm());
        }
        return "user/profile";
    }

    @PostMapping("/senha")
    public String changePassword(@AuthenticationPrincipal SecurityUser principal,
                                 @Valid @ModelAttribute("passwordForm") ChangePasswordForm form,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                userService.changePassword(principal.getUsername(), form);
                redirect.addFlashAttribute("successMessage", "Senha alterada com sucesso.");
                return "redirect:/usuario/perfil";
            } catch (BusinessException e) {
                result.reject("senha.invalida", e.getMessage());
            }
        }
        // Reexibe a página com os erros, sem devolver as senhas digitadas
        form.setCurrentPassword(null);
        form.setPassword(null);
        form.setConfirmPassword(null);
        model.addAttribute("user", userService.findByEmail(principal.getUsername()));
        return "user/profile";
    }
}
