package Chambing.demo.security.oauth2;

import Chambing.demo.Model.Rol;
import Chambing.demo.Model.Usuario;
import Chambing.demo.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Servicio OAuth2 — carga o crea el usuario de Google/GitHub en MongoDB.
 *
 * Garantiza que tras un login social SIEMPRE exista en Mongo un documento
 * Usuario con el email del proveedor y con Rol.USER. Es lo que necesita el
 * JwtAuthenticationFilter para autenticar las requests siguientes.
 *
 * Diseño a prueba de fallos:
 *  - Cualquier excepcion se loguea COMPLETA antes de propagarse.
 *  - El guardado se hace con el repositorio estandar; si algo falla,
 *    el mensaje real viaja al navegador via OAuth2AuthenticationFailureHandler
 *    (lo veras en /login?error=...).
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UsuarioRepository usuarioRepository;

    public CustomOAuth2UserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        log.info("[OAuth2] ====== loadUser INICIO ======");

        OAuth2User oAuth2User;
        String registrationId;
        try {
            registrationId = userRequest.getClientRegistration().getRegistrationId();
            oAuth2User     = super.loadUser(userRequest);
            log.info("[OAuth2] provider={}, atributos recibidos={}", registrationId,
                    oAuth2User.getAttributes().keySet());
        } catch (Exception e) {
            log.error("[OAuth2] fallo al obtener datos del proveedor", e);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("provider_error"),
                    "Error obteniendo datos del proveedor: " + e.getMessage());
        }

        // Resolver email — siempre lowercase + trim
        String email = resolveEmail(oAuth2User, registrationId);
        log.info("[OAuth2] email resuelto = '{}'", email);

        // Buscar o crear el usuario en Mongo
        Usuario usuario;
        try {
            usuario = cargarOCrearUsuario(email, registrationId, oAuth2User);
        } catch (OAuth2AuthenticationException e) {
            throw e; // ya logueada y con mensaje claro
        } catch (Exception e) {
            log.error("[OAuth2] EXCEPCION INESPERADA en cargarOCrearUsuario para '{}'", email, e);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_persist_error"),
                    "No se pudo crear/cargar el usuario: " + e.getClass().getSimpleName()
                            + " - " + e.getMessage());
        }

        log.info("[OAuth2] ====== loadUser OK: id={}, email={}, roles={} ======",
                usuario.getIdUsuario(), usuario.getEmail(), usuario.getRoles());

        return new CustomOAuth2User(oAuth2User, usuario);
    }

    // ── Buscar o crear ───────────────────────────────────────────────────────

    private Usuario cargarOCrearUsuario(String email, String registrationId, OAuth2User oAuth2User) {

        Usuario existente = usuarioRepository.findByEmail(email).orElse(null);

        if (existente != null) {
            log.info("[OAuth2] usuario YA existe en Mongo: id={}, roles={}",
                    existente.getIdUsuario(), existente.getRoles());
            return asegurarRolUser(existente);
        }

        // Crear nuevo
        long antes = usuarioRepository.count();
        log.info("[OAuth2] usuario NUEVO, se creara. (usuarios en BD antes = {})", antes);

        Usuario nuevo = construirUsuarioOAuth2(email, registrationId, oAuth2User);
        log.info("[OAuth2] documento a insertar -> email={}, provider={}, enabled={}, roles={}, nombre={}, apellido={}",
                nuevo.getEmail(), nuevo.getProvider(), nuevo.getEnabled(),
                nuevo.getRoles(), nuevo.getNombre(), nuevo.getApellido());

        Usuario guardado = usuarioRepository.save(nuevo);

        long despues = usuarioRepository.count();
        log.info("[OAuth2] save() ejecutado -> id asignado={}, usuarios en BD ahora={}",
                guardado.getIdUsuario(), despues);

        if (guardado.getIdUsuario() == null) {
            log.error("[OAuth2] save() devolvio un usuario SIN id — la insercion no persistio");
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("save_no_id"),
                    "El usuario no se persistio en Mongo (id nulo tras save)");
        }

        // Releer para confirmar
        Usuario verificado = usuarioRepository.findByEmail(email).orElse(null);
        if (verificado == null) {
            log.error("[OAuth2] tras save(), findByEmail('{}') NO lo encuentra — insercion fallida", email);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("save_not_found"),
                    "El usuario no aparece en Mongo tras guardarlo");
        }

        log.info("[OAuth2] CONFIRMADO en Mongo: id={}, email={}, roles={}",
                verificado.getIdUsuario(), verificado.getEmail(), verificado.getRoles());
        return verificado;
    }

    /**
     * Asegura que el usuario tenga Rol.USER. Idempotente.
     */
    private Usuario asegurarRolUser(Usuario usuario) {
        Set<Rol> roles = usuario.getRoles();

        if (roles == null || roles.isEmpty()) {
            usuario.setRoles(new HashSet<>(Set.of(Rol.USER)));
            log.info("[OAuth2] usuario '{}' sin roles -> asignando USER", usuario.getEmail());
            return usuarioRepository.save(usuario);
        }

        if (!roles.contains(Rol.USER)) {
            HashSet<Rol> mutables = new HashSet<>(roles);
            mutables.add(Rol.USER);
            usuario.setRoles(mutables);
            log.info("[OAuth2] usuario '{}' con roles {} -> añadiendo USER", usuario.getEmail(), roles);
            return usuarioRepository.save(usuario);
        }

        return usuario;
    }

    /**
     * Construye un nuevo Usuario para login social.
     * Solo se rellenan los campos necesarios; documento/edad/direccion quedan null
     * (son opcionales y sus indices son sparse).
     */
    private Usuario construirUsuarioOAuth2(String email, String registrationId, OAuth2User oAuth2User) {
        Usuario nuevo = new Usuario();
        nuevo.setEmail(email);
        nuevo.setProvider(registrationId);
        nuevo.setContrasena("");                              // sin contraseña local
        nuevo.setEnabled(true);
        nuevo.setRoles(new HashSet<>(Set.of(Rol.USER)));      // HashSet mutable
        resolverNombreYApellido(nuevo, registrationId, oAuth2User);
        return nuevo;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String resolveEmail(OAuth2User user, String registrationId) {

        Map<String, Object> attrs = user.getAttributes();
        Object emailAttr = attrs.get("email");
        String email = emailAttr != null ? emailAttr.toString() : null;

        if ((email == null || email.isBlank()) && "github".equals(registrationId)) {
            Object login = attrs.get("login");
            if (login != null) {
                email = login + "@github.noemail";
            }
        }

        if (email == null || email.isBlank()) {
            log.error("[OAuth2] el proveedor {} no devolvio email. Atributos: {}",
                    registrationId, attrs.keySet());
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "El proveedor " + registrationId + " no entrego un email");
        }

        return email.toLowerCase().trim();
    }

    private void resolverNombreYApellido(Usuario usuario, String registrationId, OAuth2User user) {

        if ("google".equals(registrationId)) {
            String givenName  = user.getAttribute("given_name");
            String familyName = user.getAttribute("family_name");
            usuario.setNombre(notBlankOr(givenName,  "Usuario"));
            usuario.setApellido(notBlankOr(familyName, "Google"));

        } else { // github u otros
            String name  = user.getAttribute("name");
            String login = user.getAttribute("login");
            String displayName = notBlankOr(name, login);

            if (displayName != null && displayName.contains(" ")) {
                String[] parts = displayName.split(" ", 2);
                usuario.setNombre(parts[0]);
                usuario.setApellido(parts[1]);
            } else {
                usuario.setNombre(notBlankOr(displayName, "Usuario"));
                usuario.setApellido("GitHub");
            }
        }
    }

    private String notBlankOr(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
