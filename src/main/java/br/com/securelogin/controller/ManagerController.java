package br.com.securelogin.controller;

import br.com.securelogin.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Área de gestão (perfis MANAGER e ADMIN).
 *
 * <p>No PFC, este é o lugar para as funcionalidades intermediárias do seu tema
 * (ex.: relatórios, aprovação de pedidos, gestão de turmas).</p>
 */
@Controller
@RequestMapping("/gestao")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ManagerController {

    private final UserService userService;

    public ManagerController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String panel(Model model) {
        model.addAttribute("totalUsers", userService.countAll());
        model.addAttribute("usersByRole", userService.countByRole());
        return "manager/panel";
    }
}
