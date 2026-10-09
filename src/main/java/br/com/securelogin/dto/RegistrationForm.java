package br.com.securelogin.dto;

import br.com.securelogin.validation.PasswordConfirmation;
import br.com.securelogin.validation.PasswordsMatch;
import br.com.securelogin.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados do formulário de cadastro (DTO).
 *
 * <p>Separar o formulário da entidade {@code User} impede que o usuário envie campos
 * que não deveria (ex.: "roles=ADMIN") — ataque conhecido como <i>mass assignment</i>.</p>
 */
@PasswordsMatch
public class RegistrationForm implements PasswordConfirmation {

    @NotBlank(message = "Informe seu nome.")
    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres.")
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "O nome contém caracteres inválidos.")
    private String name;

    @NotBlank(message = "Informe seu e-mail.")
    @Email(message = "E-mail inválido.")
    @Size(max = 150, message = "E-mail muito longo.")
    private String email;

    @NotBlank(message = "Informe uma senha.")
    @StrongPassword
    private String password;

    @NotBlank(message = "Confirme a senha.")
    private String confirmPassword;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    @Override
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
