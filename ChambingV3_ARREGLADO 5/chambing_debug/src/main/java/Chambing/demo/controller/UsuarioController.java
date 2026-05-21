package Chambing.demo.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Chambing.demo.Model.Postulacion;
import Chambing.demo.Model.Propuesta;
import Chambing.demo.Model.Rol;
import Chambing.demo.Model.Usuario;
import Chambing.demo.repository.EmpresaRepository;
import Chambing.demo.repository.PostulacionRepository;
import Chambing.demo.repository.PropuestaRepository;
import Chambing.demo.repository.UsuarioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/usuario")
public class UsuarioController {

    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);

    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private EmpresaRepository empresaRepo;
    @Autowired private PropuestaRepository propuestaRepo;
    @Autowired private PostulacionRepository postulacionRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null)
            return ResponseEntity.status(401).body("No autenticado");
        return usuarioRepo.findByEmail(principal.getUsername())
                .map(u -> {

                    u.setContrasena(null);
                    return ResponseEntity.ok((Object) u);
                })
                .orElse(ResponseEntity.status(404).body("Usuario no encontrado"));
    }

    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Usuario u) {
        log.info("[REGISTRO USUARIO] llamada recibida — email='{}'", u.getEmail());

        // Normalizar email — sin esto, "Pedro@Gmail.com" y "pedro@gmail.com"
        // se tratan como usuarios distintos y findByEmail falla en login.
        if (u.getEmail() == null || u.getEmail().isBlank())
            return ResponseEntity.status(400).body("El email es obligatorio");
        u.setEmail(u.getEmail().toLowerCase().trim());

        if (usuarioRepo.existsByEmail(u.getEmail()))
            return ResponseEntity.status(400).body("El email ya está registrado");

        if (u.getContrasena() == null || u.getContrasena().length() < 4)
            return ResponseEntity.status(400).body("La contraseña debe tener mínimo 4 caracteres");

        u.setContrasena(passwordEncoder.encode(u.getContrasena()));
        u.setEnabled(true);

        // Forzar Rol.USER siempre. Sin esto, un POST sin "roles" en el body
        // crea un usuario sin authorities → 403 al entrar a /user-dashboard.
        // Si el cliente mando otros roles, los respetamos pero garantizamos USER.
        Set<Rol> roles = u.getRoles();
        if (roles == null || roles.isEmpty()) {
            u.setRoles(new HashSet<>(Set.of(Rol.USER)));
        } else if (!roles.contains(Rol.USER)) {
            HashSet<Rol> mutables = new HashSet<>(roles);
            mutables.add(Rol.USER);
            u.setRoles(mutables);
        }

        Usuario saved = usuarioRepo.save(u);
        log.info("[REGISTRO USUARIO] guardado en Mongo — id={}, email={}, roles={}",
                saved.getIdUsuario(), saved.getEmail(), saved.getRoles());
        saved.setContrasena(null);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{idUsuario}")
    public ResponseEntity<?> actualizar(
            @PathVariable String idUsuario,
            @RequestBody Usuario nuevosDatos,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return usuarioRepo.findById(idUsuario).map(u -> {
            if (nuevosDatos.getNombre() != null)   u.setNombre(nuevosDatos.getNombre());
            if (nuevosDatos.getApellido() != null) u.setApellido(nuevosDatos.getApellido());
            if (nuevosDatos.getEdad() != null)     u.setEdad(nuevosDatos.getEdad());
            if (nuevosDatos.getGenero() != null)   u.setGenero(nuevosDatos.getGenero());
            if (nuevosDatos.getDireccion() != null) u.setDireccion(nuevosDatos.getDireccion());

            if (nuevosDatos.getContrasena() != null && !nuevosDatos.getContrasena().isBlank())
                u.setContrasena(passwordEncoder.encode(nuevosDatos.getContrasena()));

            Usuario saved = usuarioRepo.save(u);
            saved.setContrasena(null);
            return ResponseEntity.ok((Object) saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{idUsuario}")
    public ResponseEntity<?> eliminar(@PathVariable String idUsuario) {
        if (!usuarioRepo.existsById(idUsuario))
            return ResponseEntity.notFound().build();
        usuarioRepo.deleteById(idUsuario);
        return ResponseEntity.ok("Usuario eliminado");
    }

    @GetMapping("/empresas")
    public ResponseEntity<?> listarEmpresas() {
        var empresas = empresaRepo.findAll();
        empresas.forEach(e -> e.setContrasena(null));
        return ResponseEntity.ok(empresas);
    }

    @GetMapping("/propuestas")
    public ResponseEntity<?> listarPropuestas() {
        return ResponseEntity.ok(propuestaRepo.findAll());
    }

    @PostMapping("/aplicar/{idUsuario}/{idPropuesta}")
    public ResponseEntity<?> aplicar(
            @PathVariable String idUsuario,
            @PathVariable String idPropuesta
    ) {
        Usuario usuario = usuarioRepo.findById(idUsuario).orElse(null);
        Propuesta propuesta = propuestaRepo.findById(idPropuesta).orElse(null);

        if (usuario == null || propuesta == null)
            return ResponseEntity.badRequest().body("Usuario o propuesta no encontrados");

        if (postulacionRepo.existsByUsuarioIdAndPropuestaId(idUsuario, idPropuesta))
            return ResponseEntity.status(409).body("Ya aplicaste a esta propuesta anteriormente");

        Postulacion po = new Postulacion();
        po.setUsuarioId(idUsuario);
        po.setPropuestaId(idPropuesta);
        po.setUsuarioNombre(usuario.getNombre() + " " + usuario.getApellido());
        po.setUsuarioEmail(usuario.getEmail());
        po.setPropuestaTitulo(propuesta.getTitulo());
        po.setEstadoPostulacion("Pendiente");
        po.setFechaPostulacion(LocalDateTime.now());

        return ResponseEntity.ok(postulacionRepo.save(po));
    }

    @GetMapping("/postulaciones/{idUsuario}")
    public ResponseEntity<?> verMisPostulaciones(@PathVariable String idUsuario) {
        if (!usuarioRepo.existsById(idUsuario))
            return ResponseEntity.badRequest().body("Usuario no existe");
        return ResponseEntity.ok(postulacionRepo.findByUsuarioId(idUsuario));
    }
}