package Chambing.demo.security.jwt;

import Chambing.demo.repository.EmpresaRepository;
import Chambing.demo.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT — se ejecuta una vez por request.
 *
 * Busca el token en dos lugares (en este orden):
 *  1. Header "Authorization: Bearer <token>"  → clientes API (Postman, SPA, móvil)
 *  2. Cookie "AUTH_TOKEN"                     → navegador tras login OAuth2
 *
 * Logs detallados a nivel DEBUG para diagnosticar 401/403:
 *   logging.level.Chambing.demo.security.jwt=DEBUG
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   UsuarioRepository usuarioRepository,
                                   EmpresaRepository empresaRepository) {
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Si ya hay autenticacion (form-login recien hecho, OAuth2 en handshake), no tocar
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            log.debug("[JWT] {} → ya hay autenticacion en contexto, paso de largo", path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        if (token == null) {
            log.debug("[JWT] {} → sin token (ni header Bearer ni cookie {})", path, JwtUtil.JWT_COOKIE_NAME);
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtUtil.validateToken(token)) {
            log.warn("[JWT] {} → token presente pero INVALIDO (firma mala o expirado)", path);
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtUtil.extractUsername(token);
        log.debug("[JWT] {} → token valido, subject='{}'", path, username);

        // Busca primero en usuarios, luego en empresas
        UserDetails userDetails =
                usuarioRepository.findByEmail(username)
                        .map(u -> (UserDetails) u)
                        .or(() -> empresaRepository.findByCorreo(username)
                                .map(e -> (UserDetails) e))
                        .orElse(null);

        if (userDetails == null) {
            log.warn("[JWT] {} → token valido para '{}' pero no se encontro en Mongo (ni en usuarios ni en empresas)",
                    path, username);
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("[JWT] {} → autenticando a '{}' con authorities {}",
                path, username, userDetails.getAuthorities());

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

    /**
     * Busca el token en el header Authorization y, si no aparece, en la cookie AUTH_TOKEN.
     */
    private String resolveToken(HttpServletRequest request) {

        // 1. Header Authorization
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String value = header.substring(7).trim();
            if (!value.isEmpty()) {
                log.debug("[JWT] token tomado del header Authorization");
                return value;
            }
        }

        // 2. Cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (JwtUtil.JWT_COOKIE_NAME.equals(c.getName())) {
                    String value = c.getValue();
                    if (value != null && !value.isBlank()) {
                        log.debug("[JWT] token tomado de la cookie {}", JwtUtil.JWT_COOKIE_NAME);
                        return value;
                    }
                }
            }
        }

        return null;
    }
}
