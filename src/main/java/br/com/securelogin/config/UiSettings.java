package br.com.securelogin.config;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Expõe as configurações visuais para os templates Thymeleaf.
 *
 * <p>Registrado com o nome {@code ui}, pode ser usado em qualquer template como
 * {@code ${@ui.appName}} ou {@code ${@ui.cssPath}}. Assim os templates não precisam
 * de nada no Model para saber o tema ativo (funciona até nas páginas de erro).</p>
 */
@Component("ui")
public class UiSettings {

    private final AppProperties props;

    public UiSettings(AppProperties props) {
        this.props = props;
    }

    public String getAppName() {
        return props.name();
    }

    public String getThemeName() {
        return props.theme().name();
    }

    /** CSS do tema ativo, ex.: /themes/escuro/theme.css */
    public String getCssPath() {
        return "/themes/" + props.theme().name() + "/theme.css";
    }

    /** Formata datas para exibição: {@code ${@ui.format(user.createdAt)}}. */
    public String format(Instant instant) {
        return instant == null ? "—" : DATE_FORMAT.format(instant);
    }

    /** Fuso horário usado na exibição de datas (o banco guarda sempre em UTC). */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));
}
