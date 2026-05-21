package Chambing.demo.security.oauth2;

import Chambing.demo.Model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Wrapper de OidcUser para el login con Google.
 *
 * Delega los datos OIDC (idToken, userInfo, claims, atributos) al OidcUser real
 * que devolvio Spring, pero sobreescribe getAuthorities() y getName():
 *
 *  - getAuthorities() -> siempre "USER" (sin prefijo ROLE_). Quien entra por
 *    Google es USER, sin importar lo que tenga en BD.
 *  - getName() -> el email del Usuario de Mongo, no el "sub" numerico de Google.
 *
 * Conservar idToken y userInfo es obligatorio: Spring Security los usa
 * internamente en el flujo OIDC.
 */
public class CustomOidcUser implements OidcUser {

    private static final SimpleGrantedAuthority USER_AUTHORITY =
            new SimpleGrantedAuthority("USER");

    private final OidcUser delegate;
    private final Usuario  usuario;

    public CustomOidcUser(OidcUser delegate, Usuario usuario) {
        this.delegate = delegate;
        this.usuario  = usuario;
    }

    // ── Authorities y nombre: lo que personalizamos ──────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(USER_AUTHORITY);
    }

    @Override
    public String getName() {
        return usuario.getEmail();
    }

    public Usuario getUsuario() {
        return usuario;
    }

    // ── Resto: delegado tal cual al OidcUser real ────────────────────────────

    @Override
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }
}
