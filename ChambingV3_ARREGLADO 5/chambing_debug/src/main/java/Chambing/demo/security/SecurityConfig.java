package Chambing.demo.security;

import Chambing.demo.repository.EmpresaRepository;
import Chambing.demo.repository.UsuarioRepository;
import Chambing.demo.security.jwt.JwtAuthenticationFilter;
import Chambing.demo.security.jwt.JwtUtil;
import Chambing.demo.security.oauth2.CustomOAuth2UserService;
import Chambing.demo.security.oauth2.CustomOidcUserService;
import Chambing.demo.security.oauth2.OAuth2AuthenticationFailureHandler;
import Chambing.demo.security.oauth2.OAuth2LoginSuccessHandler;

import jakarta.servlet.http.Cookie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuracion central de Spring Security — STATELESS (solo JWT).
 *
 * Tres mecanismos de autenticacion, los tres terminan emitiendo el mismo JWT:
 *  1. Form Login   — vistas Thymeleaf (POST /login)        → cookie AUTH_TOKEN
 *  2. JWT Filter   — clientes REST/API (header Authorization: Bearer)
 *  3. OAuth2 Login — Google / GitHub                       → cookie AUTH_TOKEN
 *
 * No usamos sesion HTTP:
 *   - SessionCreationPolicy.STATELESS
 *   - sin JSESSIONID
 *   - sin sessionManagement.maximumSessions
 *
 * REGLA DE ROLES:
 *  Enum Rol { USER, EMPRESA, ADMIN } → authorities SIN prefijo ROLE_.
 *  Siempre: hasAuthority("USER"), roles.contains("USER").
 *  Nunca:   hasRole("USER"), ROLE_USER.
 */
@Configuration
public class SecurityConfig {

    private final UsuarioRepository               usuarioRepository;
    private final EmpresaRepository               empresaRepository;
    private final JwtUtil                         jwtUtil;
    private final CustomOAuth2UserService         oAuth2UserService;
    private final CustomOidcUserService           oidcUserService;
    private final OAuth2LoginSuccessHandler       oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    public SecurityConfig(UsuarioRepository               usuarioRepository,
                          EmpresaRepository               empresaRepository,
                          JwtUtil                         jwtUtil,
                          CustomOAuth2UserService         oAuth2UserService,
                          CustomOidcUserService           oidcUserService,
                          OAuth2LoginSuccessHandler       oAuth2SuccessHandler,
                          OAuth2AuthenticationFailureHandler oAuth2FailureHandler) {
        this.usuarioRepository  = usuarioRepository;
        this.empresaRepository  = empresaRepository;
        this.jwtUtil            = jwtUtil;
        this.oAuth2UserService  = oAuth2UserService;
        this.oidcUserService    = oidcUserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
    }

    // ── Beans ────────────────────────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS para la app Flutter web (carpeta Chambing-Flutter).
     *
     * Conexion: la app Flutter corre en el navegador en http://localhost: por que3 al momento no tenemos
     * servidor y llama a esta API en http://localhost:9999 con
     * "Authorization: Bearer <jwt>" (POST /api/auth/login, /usuario/**, /empresa/**).
     * Al ser otro origen, el navegador primero manda un OPTIONS (preflight); sin
     * esta configuracion Spring lo rechazaba con 403 "Invalid CORS request" y la
     * app mostraba "ClientException: Failed to fetch".
     *
     * - Solo se aceptan origenes localhost / 127.0.0.1 (cualquier puerto, porque
     *   flutter run cambia el puerto en cada ejecucion).
     * - allowCredentials=false: la app usa el header Bearer, no cookies.
     * - Las vistas Thymeleaf no se afectan (son del mismo origen).
     * - Si la app se publica en otro dominio, agregarlo en allowedOriginPatterns.
     * - Android / Windows (escritorio) no necesitan CORS; solo el navegador.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, usuarioRepository, empresaRepository);
    }

    /**
     * Tras un login por formulario exitoso:
     *   - emite el JWT como cookie AUTH_TOKEN (igual que OAuth2)
     *   - redirige segun rol real (USER → user-dashboard, EMPRESA → company-dashboard)
     *
     * Aqui SI respetamos el rol real porque es el login propio de la app;
     * la regla "todo Google = USER" solo aplica al handler de OAuth2.
     */
    @Bean
    public AuthenticationSuccessHandler formLoginSuccessHandler() {
        return (request, response, authentication) -> {
            // 1. Emitir cookie JWT — sin esto, las siguientes requests no
            //    estarian autenticadas (Spring Security esta en modo stateless
            //    desde el punto de vista del SecurityContext).
            String token = jwtUtil.generateToken(authentication.getName());
            Cookie jwt = new Cookie(JwtUtil.JWT_COOKIE_NAME, token);
            jwt.setHttpOnly(true);
            jwt.setPath("/");
            jwt.setMaxAge((int) jwtUtil.getExpirationSeconds());
            jwt.setSecure(request.isSecure());
            response.addCookie(jwt);

            // 2. Matar JSESSIONID si existe — solo nos importa la cookie JWT.
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            Cookie killSession = new Cookie("JSESSIONID", "");
            killSession.setPath("/");
            killSession.setMaxAge(0);
            killSession.setHttpOnly(true);
            response.addCookie(killSession);

            // 3. Redirect segun rol
            Set<String> roles = authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            if (roles.contains("EMPRESA")) {
                response.sendRedirect("/company-dashboard");
                return;
            }
            if (roles.contains("USER")) {
                response.sendRedirect("/user-dashboard");
                return;
            }
            response.sendRedirect("/login?error");
        };
    }

    // ── Cadena de filtros ─────────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            // CORS para la app Flutter web: usa el bean corsConfigurationSource().
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // OAuth2 necesita sesion durante el handshake (state + AuthorizationRequest
            // viven en HttpSession entre /oauth2/authorization/google y el callback).
            // No podemos forzar STATELESS aqui o OAuth2 se rompe.
            //
            // Lo que SI hacemos: el OAuth2LoginSuccessHandler invalida la sesion
            // despues de emitir la cookie JWT. El form-login no crea sesion porque
            // su successHandler tampoco la usa.
            //
            // Resultado practico: la unica credencial viva tras login es la cookie
            // AUTH_TOKEN. Cada request se autentica solo por JWT.
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            .userDetailsService(email -> {
                UserDetails usuario = usuarioRepository.findByEmail(email).orElse(null);
                if (usuario != null) return usuario;

                UserDetails empresa = empresaRepository.findByCorreo(email).orElse(null);
                if (empresa != null) return empresa;

                throw new UsernameNotFoundException("No encontrado: " + email);
            })

            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

            .authorizeHttpRequests(auth -> auth
                // ── Recursos estaticos y paginas publicas ────────────────────
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers(
                        "/", "/index", "/about", "/blog", "/contacto",
                        "/login", "/login**", "/register", "/403", "/error"
                ).permitAll()

                // ── Endpoints publicos de registro (POST) ────────────────────
                // IMPORTANTE: deben ir ANTES de las reglas /usuario/** y /empresa/**.
                // Sin esto, el registro era bloqueado y Spring devolvia 302 al login;
                // el frontend leia 200 OK (de la pagina de login) y mostraba "exito"
                // aunque no se guardaba nada en Mongo.
                .requestMatchers(HttpMethod.POST, "/usuario", "/usuario/", "/empresa", "/empresa/").permitAll()

                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/catalogos/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/admin", "/admin/login", "/admin/logout").permitAll()
                .requestMatchers("/admin/panel", "/admin/**").permitAll()

                // ── Lecturas publicas para usuarios logueados (USER y EMPRESA) ──
                // Estas las usa el user-dashboard para mostrar empresas y propuestas.
                // Sin esto, un USER no podia ver propuestas ni datos de empresas.
                // OJO: tienen que ir ANTES de "/empresa/**".hasAuthority("EMPRESA").
                .requestMatchers(HttpMethod.GET, "/empresa/{idEmpresa}").authenticated()
                .requestMatchers(HttpMethod.GET, "/empresa/{idEmpresa}/propuestas").authenticated()

                // ── Empresa — authority "EMPRESA" (sin prefijo ROLE_) ──────────
                .requestMatchers(
                        "/company-dashboard", "/company-profile", "/company-proposals",
                        "/company-proposal-edit", "/edit-propuesta", "/proposals-answer"
                ).hasAuthority("EMPRESA")
                .requestMatchers("/empresa/me", "/empresa/**").hasAuthority("EMPRESA")

                // ── Usuario — authority "USER" (sin prefijo ROLE_) ─────────────
                .requestMatchers(
                        "/user-dashboard", "/user-begin", "/only-proporsals",
                        "/companies-registers", "/companies-personals-profiles", "/proposals-states"
                ).hasAuthority("USER")
                .requestMatchers("/usuario/me", "/usuario/**").hasAuthority("USER")

                .anyRequest().authenticated()
            )

            .formLogin(login -> login
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .usernameParameter("username")
                    .passwordParameter("password")
                    .successHandler(formLoginSuccessHandler())
                    .failureUrl("/login?error")
                    .permitAll()
            )

            // OAuth2 / OIDC Login — Google (y GitHub si se reactiva).
            // El SuccessHandler emite la cookie AUTH_TOKEN y redirige a /user-dashboard.
            // El FailureHandler muestra el error REAL en la URL: /login?error=<mensaje>
            //
            // userService     -> OAuth2 plano (GitHub).
            // oidcUserService -> OpenID Connect (Google, por usar scope "openid").
            //   AMBOS deben registrarse. Google llega como OIDC; sin oidcUserService
            //   se usaba el service por defecto de Spring y el usuario NUNCA se
            //   guardaba en Mongo.
            .oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .userInfoEndpoint(info -> info
                            .userService(oAuth2UserService)
                            .oidcUserService(oidcUserService)
                    )
                    .successHandler(oAuth2SuccessHandler)
                    .failureHandler(oAuth2FailureHandler)
            )

            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    // Stateless: no hay sesion que invalidar.
                    // Solo borramos la cookie JWT — esa es la unica credencial viva.
                    .deleteCookies(JwtUtil.JWT_COOKIE_NAME)
                    .permitAll()
            )

            .exceptionHandling(ex -> ex.accessDeniedPage("/403"));

        return http.build();
    }
}
