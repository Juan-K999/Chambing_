package Chambing.demo.security.oauth2;

import Chambing.demo.Model.Rol;
import Chambing.demo.Model.Usuario;
import Chambing.demo.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Servicio OIDC — el que de verdad usa Google.
 *
 * POR QUE EXISTE ESTA CLASE (causa raiz del bug "no crea usuarios con Google"):
 *   Google se configura con scope "openid", asi que el login NO es OAuth2 plano,
 *   es OpenID Connect (OIDC). Spring Security trata OIDC y OAuth2 con servicios
 *   distintos:
 *     - OAuth2 plano  -> userInfoEndpoint().userService(...)        [GitHub, etc.]
 *     - OIDC          -> userInfoEndpoint().oidcUserService(...)    [Google]
 *
 *   El proyecto solo registraba userService(...), por eso con Google se usaba
 *   el OidcUserService POR DEFECTO de Spring, que nunca toca MongoDB. Resultado:
 *   el usuario de Google nunca se guardaba, el JWT apuntaba a un email inexistente
 *   y /user-dashboard rebotaba a /login.
 *
 * Esta clase extiende OidcUserService (el de Spring) y, tras obtener los datos
 * de Google, crea o actualiza el Usuario en Mongo con Rol.USER. Es el equivalente
 * OIDC de CustomOAuth2UserService.
 *
 * REGLA DE ROLES: authorities sin prefijo ROLE_; quien entra por Google es USER.
 */
@Service
public class CustomOidcUserService extends OidcUserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOidcUserService.class);

    private final UsuarioRepository usuarioRepository;

    public CustomOidcUserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        log.info("[OIDC] ====== loadUser INICIO (Google) ======");

        // 1. Datos del proveedor via el OidcUserService estandar
        OidcUser oidcUser;
        try {
            oidcUser = super.loadUser(userRequest);
            log.info("[OIDC] atributos recibidos = {}", oidcUser.getAttributes().keySet());
        } catch (Exception e) {
            log.error("[OIDC] fallo obteniendo datos OIDC del proveedor", e);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("oidc_provider_error"),
                    "Error obteniendo datos de Google: " + e.getMessage());
        }

        // 2. Email — Google siempre lo entrega con scope "email"
        String email = oidcUser.getEmail();
        if (email == null || email.isBlank()) {
            Object emailAttr = oidcUser.getAttributes().get("email");
            email = emailAttr != null ? emailAttr.toString() : null;
        }
        if (email == null || email.isBlank()) {
            log.error("[OIDC] Google no entrego email. Atributos: {}", oidcUser.getAttributes().keySet());
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"), "Google no entrego un email");
        }
        email = email.toLowerCase().trim();
        log.info("[OIDC] email resuelto = '{}'", email);

        // 3. Crear o cargar el Usuario en Mongo
        Usuario usuario;
        try {
            usuario = cargarOCrearUsuario(email, oidcUser);
        } catch (OAuth2AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[OIDC] EXCEPCION guardando usuario '{}'", email, e);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_persist_error"),
                    "No se pudo crear el usuario: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        log.info("[OIDC] ====== loadUser OK: id={}, email={}, roles={} ======",
                usuario.getIdUsuario(), usuario.getEmail(), usuario.getRoles());

        // 4. Devolver un OidcUser que reporta authority USER (sin ROLE_).
        //    Conservamos el idToken y userInfo originales — Spring los necesita.
        return new CustomOidcUser(oidcUser, usuario);
    }

    // ── Crear o cargar ───────────────────────────────────────────────────────

    private Usuario cargarOCrearUsuario(String email, OidcUser oidcUser) {

        Usuario existente = usuarioRepository.findByEmail(email).orElse(null);
        if (existente != null) {
            log.info("[OIDC] usuario YA existe: id={}, roles={}",
                    existente.getIdUsuario(), existente.getRoles());
            return asegurarRolUser(existente);
        }

        long antes = usuarioRepository.count();
        log.info("[OIDC] usuario NUEVO, se creara. (usuarios en BD antes = {})", antes);

        Usuario nuevo = new Usuario();
        nuevo.setEmail(email);
        nuevo.setProvider("google");
        nuevo.setContrasena("");
        nuevo.setEnabled(true);
        nuevo.setRoles(new HashSet<>(Set.of(Rol.USER)));

        String givenName  = oidcUser.getGivenName();
        String familyName = oidcUser.getFamilyName();
        nuevo.setNombre(notBlankOr(givenName,  "Usuario"));
        nuevo.setApellido(notBlankOr(familyName, "Google"));

        log.info("[OIDC] documento a insertar -> email={}, provider={}, roles={}, nombre={}, apellido={}",
                nuevo.getEmail(), nuevo.getProvider(), nuevo.getRoles(),
                nuevo.getNombre(), nuevo.getApellido());

        Usuario guardado = usuarioRepository.save(nuevo);
        long despues = usuarioRepository.count();
        log.info("[OIDC] save() ejecutado -> id={}, usuarios en BD ahora={}",
                guardado.getIdUsuario(), despues);

        if (guardado.getIdUsuario() == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("save_no_id"), "El usuario no se persistio (id nulo)");
        }

        Usuario verificado = usuarioRepository.findByEmail(email).orElse(null);
        if (verificado == null) {
            log.error("[OIDC] tras save(), findByEmail('{}') no lo encuentra", email);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("save_not_found"), "El usuario no aparece en Mongo tras guardarlo");
        }

        log.info("[OIDC] CONFIRMADO en Mongo: id={}, roles={}",
                verificado.getIdUsuario(), verificado.getRoles());
        return verificado;
    }

    private Usuario asegurarRolUser(Usuario usuario) {
        Set<Rol> roles = usuario.getRoles();

        if (roles == null || roles.isEmpty()) {
            usuario.setRoles(new HashSet<>(Set.of(Rol.USER)));
            log.info("[OIDC] usuario '{}' sin roles -> asignando USER", usuario.getEmail());
            return usuarioRepository.save(usuario);
        }
        if (!roles.contains(Rol.USER)) {
            HashSet<Rol> mutables = new HashSet<>(roles);
            mutables.add(Rol.USER);
            usuario.setRoles(mutables);
            log.info("[OIDC] usuario '{}' con roles {} -> añadiendo USER", usuario.getEmail(), roles);
            return usuarioRepository.save(usuario);
        }
        return usuario;
    }

    private String notBlankOr(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
