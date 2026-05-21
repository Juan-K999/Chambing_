package Chambing.demo.security.oauth2;

import Chambing.demo.Model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Wrapper que une el OAuth2User del proveedor (Google/GitHub)
 * con nuestro objeto Usuario de MongoDB.
 *
 * REGLA DE NEGOCIO:
 *   Quien entra por Google/GitHub es USER, punto.
 *   Aunque el documento de Mongo tenga ademas EMPRESA o ADMIN,
 *   por esta puerta se autentica unicamente con authority "USER".
 *   Esto cumple con: "no uses ROLE_, solo USER a los que inicien
 *   sesion con google".
 *
 * Sin prefijo ROLE_ — authorities planas tal como las espera SecurityConfig
 * (hasAuthority("USER") / hasAuthority("EMPRESA")).
 */
public class CustomOAuth2User implements OAuth2User {

    private static final SimpleGrantedAuthority USER_AUTHORITY =
            new SimpleGrantedAuthority("USER");

    private final OAuth2User delegate;
    private final Usuario    usuario;

    public CustomOAuth2User(OAuth2User delegate, Usuario usuario) {
        this.delegate = delegate;
        this.usuario  = usuario;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    /**
     * Login social → siempre devuelve unicamente "USER".
     * No consultamos usuario.getAuthorities() a proposito: aunque tenga
     * EMPRESA o ADMIN en BD, por OAuth2 entra como USER.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(USER_AUTHORITY);
    }

    /** El identificador principal es el email */
    @Override
    public String getName() {
        return usuario.getEmail();
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
