package Chambing.demo.Model;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Document(collection = "usuarios")
public class Usuario implements UserDetails {

    @Id
    private String idUsuario;

    // sparse = true: el índice ignora documentos donde "documento" es null.
    // Sin esto, MongoDB solo permite UN null → DuplicateKeyException al crear usuario OAuth2.
    @Indexed(unique = true, sparse = true)
    private Integer documento;

    private String tipoDocumento;

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @Min(18)
    private Integer edad;

    private String genero;

    private Direccion direccion;

    @NotBlank
    @Email
    @Indexed(unique = true)
    private String email;

    // Sin @NotBlank: los usuarios OAuth2 no tienen contraseña local
    private String contrasena;

    // Origen: "local" | "google" | "github"
    private String provider;

    private boolean enabled = true;

    private Set<Rol> roles;

    // ── UserDetails ──────────────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Null-safe: si roles es null (usuario incompleto en BD), devuelve lista vacía
        // en vez de lanzar NullPointerException que Spring Security convierte en /login?error
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(rol -> new SimpleGrantedAuthority(rol.name()))
                .toList();
    }

    @Override public String  getPassword()              { return contrasena; }
    @Override public String  getUsername()              { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return enabled; }

    // ── Getters y Setters ────────────────────────────────────────────────────

    public String  getIdUsuario()                          { return idUsuario; }
    public void    setIdUsuario(String idUsuario)          { this.idUsuario = idUsuario; }

    public Integer getDocumento()                          { return documento; }
    public void    setDocumento(Integer documento)         { this.documento = documento; }

    public String  getTipoDocumento()                      { return tipoDocumento; }
    public void    setTipoDocumento(String tipoDocumento)  { this.tipoDocumento = tipoDocumento; }

    public String  getNombre()                             { return nombre; }
    public void    setNombre(String nombre)                { this.nombre = nombre; }

    public String  getApellido()                           { return apellido; }
    public void    setApellido(String apellido)            { this.apellido = apellido; }

    public Integer getEdad()                               { return edad; }
    public void    setEdad(Integer edad)                   { this.edad = edad; }

    public String  getGenero()                             { return genero; }
    public void    setGenero(String genero)                { this.genero = genero; }

    public Direccion getDireccion()                        { return direccion; }
    public void      setDireccion(Direccion direccion)     { this.direccion = direccion; }

    public String  getEmail()                              { return email; }
    public void    setEmail(String email)                  { this.email = email; }

    public String  getContrasena()                         { return contrasena; }
    public void    setContrasena(String contrasena)        { this.contrasena = contrasena; }
    public void    setPassword(String password)            { this.contrasena = password; }

    public String  getProvider()                           { return provider; }
    public void    setProvider(String provider)            { this.provider = provider; }

    public boolean getEnabled()                            { return enabled; }
    public void    setEnabled(boolean enabled)             { this.enabled = enabled; }

    public Set<Rol> getRoles()                             { return roles; }
    public void     setRoles(Set<Rol> roles)               { this.roles = roles; }
}
