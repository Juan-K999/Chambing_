package Chambing.demo.security.oauth2;

import Chambing.demo.security.jwt.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handler ejecutado tras un login exitoso con Google o GitHub.
 *
 * Acciones (en orden):
 *  1. Genera un JWT con el email del usuario.
 *  2. Lo escribe como cookie HttpOnly llamada AUTH_TOKEN
 *     (el header X-Auth-Token se perderia en el redirect del navegador,
 *      por eso usamos cookie — sobrevive al sendRedirect y a las
 *      navegaciones siguientes).
 *  3. Invalida la HttpSession y borra JSESSIONID del navegador.
 *     La sesion solo existio porque Spring la necesita durante el handshake
 *     OAuth2 (guarda state + AuthorizationRequest). Una vez tenemos el JWT
 *     no hace falta — vamos full stateless de aqui en adelante.
 *  4. Redirige a /user-dashboard.
 *
 * REGLA DE NEGOCIO:
 *   Todo login via OAuth2 (Google/GitHub) entra como USER.
 *   No importa que el usuario tenga ademas EMPRESA o ADMIN en BD:
 *   por esta puerta solo se accede como USER → /user-dashboard.
 *
 * REGLA DE ROLES: el enum Rol usa "USER" y "EMPRESA" sin prefijo ROLE_.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final JwtUtil jwtUtil;

    public OAuth2LoginSuccessHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest  request,
                                        HttpServletResponse response,
                                        Authentication      authentication)
            throws IOException, ServletException {

        // Resolver email del usuario de forma defensiva.
        //
        // El principal puede ser:
        //   - CustomOidcUser   -> login Google (OIDC). getUsuario().getEmail() es lo bueno.
        //   - CustomOAuth2User -> login OAuth2 plano (GitHub).
        //   - otro OAuth2User  -> fallback al attribute "email".
        //
        // Orden de preferencia:
        //   1. principal.getUsuario().getEmail()  — el camino correcto
        //   2. principal.getAttribute("email")    — directo del proveedor
        //   3. authentication.getName()           — ultimo recurso
        String email = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomOidcUser ou && ou.getUsuario() != null) {
            email = ou.getUsuario().getEmail();
            log.debug("[OAuth2] email tomado de CustomOidcUser.usuario: {}", email);
        }

        if ((email == null || email.isBlank())
                && principal instanceof CustomOAuth2User cu && cu.getUsuario() != null) {
            email = cu.getUsuario().getEmail();
            log.debug("[OAuth2] email tomado de CustomOAuth2User.usuario: {}", email);
        }

        if ((email == null || email.isBlank())
                && principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oUser) {
            Object emailAttr = oUser.getAttribute("email");
            if (emailAttr != null) {
                email = emailAttr.toString();
                log.debug("[OAuth2] email tomado del attribute 'email': {}", email);
            }
        }

        if (email == null || email.isBlank()) {
            email = authentication.getName();
            log.warn("[OAuth2] email no resuelto desde usuario ni atributos, cayendo a authentication.getName()='{}'", email);
        }

        // 1. Generar JWT con el EMAIL (no con el sub numerico de Google)
        String token = jwtUtil.generateToken(email);

        // 2. Escribir cookie HttpOnly con el JWT
        //    HttpOnly: inaccesible desde JS → protege contra XSS
        //    Path "/" : valida para toda la app
        //    Max-Age : igual al expiracion del JWT (mantiene cookie y token sincronizados)
        //    Secure : solo si la conexion es HTTPS (en localhost http funciona sin Secure)
        Cookie jwtCookie = new Cookie(JwtUtil.JWT_COOKIE_NAME, token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge((int) jwtUtil.getExpirationSeconds());
        jwtCookie.setSecure(request.isSecure());
        response.addCookie(jwtCookie);

        // 3. Tirar la sesion HTTP y borrar JSESSIONID.
        //    Despues de esto el navegador solo conserva AUTH_TOKEN — el JwtAuthenticationFilter
        //    re-autentica desde Mongo en cada request.
        //
        //    Importante hacerlo DESPUES de setear la cookie JWT, no antes.
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        Cookie killSession = new Cookie("JSESSIONID", "");
        killSession.setPath("/");
        killSession.setMaxAge(0);
        killSession.setHttpOnly(true);
        response.addCookie(killSession);

        // Limpiar tambien el SecurityContext en memoria por higiene.
        SecurityContextHolder.clearContext();

        // 4. Toda persona que entra por Google/GitHub es USER → user-dashboard.
        //    El JwtAuthenticationFilter leera la cookie AUTH_TOKEN, encontrara
        //    al Usuario en Mongo (con Rol.USER) y le concedera authority "USER".
        log.info("[OAuth2] login OK para '{}' — cookie {} emitida, sesion invalidada, redirigiendo a /user-dashboard",
                email, JwtUtil.JWT_COOKIE_NAME);
        response.sendRedirect("/user-dashboard");
    }
}
