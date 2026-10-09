package br.com.securelogin.dto;

import br.com.securelogin.validation.PasswordConfirmation;
import br.com.securelogin.validation.PasswordsMatch;
import br.com.securelogin.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

/** Formulário de troca de senha na página de perfil. */
@PasswordsMatch
public class ChangePasswordForm implements PasswordConfirmation {

    @NotBlank(message = "Informe a senha atual.")
    private String currentPassword;

    @NotBlank(message = "Informe a nova senha.")
    @StrongPassword
    private String password;

    @NotBlank(message = "Confirme a nova senha.")
    private String confirmPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    @Override
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    @Override
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
