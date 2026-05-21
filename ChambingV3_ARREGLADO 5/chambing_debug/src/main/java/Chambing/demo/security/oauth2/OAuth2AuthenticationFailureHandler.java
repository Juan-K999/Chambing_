package Chambing.demo.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Captura el error REAL de OAuth2 y lo pasa a la URL de login.
 * Verás el mensaje en: /login?error=<motivo>
 * Esto permite diagnosticar exactamente qué falla sin necesidad de ver los logs del servidor.
 */
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest  request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        // Extraer mensaje de la excepción y sus causas anidadas
        String mensaje = extractMensaje(exception);
        String encoded = URLEncoder.encode(mensaje, StandardCharsets.UTF_8);

        // Redirige a /login?error=<mensaje_real>
        // Así puedes ver en el navegador exactamente qué falló
        getRedirectStrategy().sendRedirect(request, response, "/login?error=" + encoded);
    }

    private String extractMensaje(Throwable t) {
        if (t == null) return "error_desconocido";

        StringBuilder sb = new StringBuilder();
        sb.append(t.getClass().getSimpleName()).append(": ").append(t.getMessage());

        // Incluir la causa raíz si existe
        if (t.getCause() != null && t.getCause() != t) {
            sb.append(" | Causa: ")
              .append(t.getCause().getClass().getSimpleName())
              .append(": ")
              .append(t.getCause().getMessage());
        }
        return sb.toString();
    }
}
