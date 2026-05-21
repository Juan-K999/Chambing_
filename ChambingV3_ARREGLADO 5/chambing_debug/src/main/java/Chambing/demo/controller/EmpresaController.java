package Chambing.demo.controller;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import Chambing.demo.Model.Empresa;
import Chambing.demo.Model.Propuesta;
import Chambing.demo.Model.Rol;
import Chambing.demo.repository.EmpresaRepository;
import Chambing.demo.repository.PostulacionRepository;
import Chambing.demo.repository.PropuestaRepository;

@RestController
@RequestMapping("/empresa")
public class EmpresaController {

    private static final Logger log = LoggerFactory.getLogger(EmpresaController.class);

    @Autowired private EmpresaRepository empresaRepo;
    @Autowired private PropuestaRepository propuestaRepo;
    @Autowired private PostulacionRepository postulacionRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null)
            return ResponseEntity.status(401).body("No autenticado");
        return empresaRepo.findByCorreo(principal.getUsername())
                .map(e -> {
                    e.setContrasena(null);
                    return ResponseEntity.ok((Object) e);
                })
                .orElse(ResponseEntity.status(404).body("Empresa no encontrada"));
    }

    /**
     * Datos publicos de una empresa por ID.
     * Lo usa el user-dashboard al entrar al perfil de una empresa.
     * No expone la contraseña.
     */
    @GetMapping("/{idEmpresa}")
    public ResponseEntity<?> verEmpresa(@PathVariable String idEmpresa) {
        return empresaRepo.findById(idEmpresa)
                .map(e -> {
                    e.setContrasena(null);
                    return ResponseEntity.ok((Object) e);
                })
                .orElse(ResponseEntity.status(404).body("Empresa no encontrada"));
    }

    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Empresa e) {
        log.info("[REGISTRO EMPRESA] llamada recibida — correo='{}'", e.getCorreo());

        // Normalizar correo — mismo motivo que con Usuario.email.
        if (e.getCorreo() == null || e.getCorreo().isBlank())
            return ResponseEntity.status(400).body("El correo es obligatorio");
        e.setCorreo(e.getCorreo().toLowerCase().trim());

        if (empresaRepo.existsByCorreo(e.getCorreo()))
            return ResponseEntity.status(400).body("El correo ya está registrado");

        if (e.getContrasena() == null || e.getContrasena().length() < 4)
            return ResponseEntity.badRequest().body("La contraseña debe tener mínimo 4 caracteres");

        e.setContrasena(passwordEncoder.encode(e.getContrasena()));
        e.setEnabled(true);

        // Forzar Rol.EMPRESA siempre. Sin esto, la empresa entra al sistema con
        // authorities vacias y no puede acceder a /company-dashboard ni a sus
        // propios endpoints /empresa/**.
        Set<Rol> roles = e.getRoles();
        if (roles == null || roles.isEmpty()) {
            e.setRoles(new HashSet<>(Set.of(Rol.EMPRESA)));
        } else if (!roles.contains(Rol.EMPRESA)) {
            HashSet<Rol> mutables = new HashSet<>(roles);
            mutables.add(Rol.EMPRESA);
            e.setRoles(mutables);
        }

        Empresa saved = empresaRepo.save(e);
        log.info("[REGISTRO EMPRESA] guardada en Mongo — id={}, correo={}, roles={}",
                saved.getIdEmpresa(), saved.getCorreo(), saved.getRoles());
        saved.setContrasena(null);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{idEmpresa}")
    public ResponseEntity<?> actualizar(@PathVariable String idEmpresa, @RequestBody Empresa data) {
        return empresaRepo.findById(idEmpresa).map(emp -> {
            if (data.getNombreEmpresa() != null) emp.setNombreEmpresa(data.getNombreEmpresa());
            if (data.getCorreo() != null)        emp.setCorreo(data.getCorreo());
            if (data.getDireccion() != null)     emp.setDireccion(data.getDireccion());
            if (data.getContrasena() != null && !data.getContrasena().isBlank())
                emp.setContrasena(passwordEncoder.encode(data.getContrasena()));

            Empresa saved = empresaRepo.save(emp);
            saved.setContrasena(null);
            return ResponseEntity.ok((Object) saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{idEmpresa}/propuestas")
    public ResponseEntity<?> crearPropuesta(@PathVariable String idEmpresa, @RequestBody Propuesta p) {
        Empresa emp = empresaRepo.findById(idEmpresa).orElse(null);
        if (emp == null) return ResponseEntity.badRequest().body("Empresa no encontrada");

        p.setEmpresaId(idEmpresa);
        p.setEmpresaNombre(emp.getNombreEmpresa());
        p.setEstado("Activa");

        return ResponseEntity.ok(propuestaRepo.save(p));
    }

    @PutMapping("/{idEmpresa}/propuestas/{idPropuesta}")
    public ResponseEntity<?> editarPropuesta(
            @PathVariable String idEmpresa,
            @PathVariable String idPropuesta,
            @RequestBody Propuesta nueva
    ) {
        return propuestaRepo.findById(idPropuesta).map(p -> {
            if (!p.getEmpresaId().equals(idEmpresa))
                return ResponseEntity.status(403).body((Object)"No puedes modificar propuestas que no son tuyas");

            if (nueva.getTitulo() != null)      p.setTitulo(nueva.getTitulo());
            if (nueva.getDescripcion() != null) p.setDescripcion(nueva.getDescripcion());
            if (nueva.getRequisitos() != null)  p.setRequisitos(nueva.getRequisitos());
            if (nueva.getSalario() != null)     p.setSalario(nueva.getSalario());
            if (nueva.getEstado() != null)      p.setEstado(nueva.getEstado());
            if (nueva.getCategoria() != null)   p.setCategoria(nueva.getCategoria());

            return ResponseEntity.ok((Object) propuestaRepo.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{idEmpresa}/propuestas/{idPropuesta}")
    public ResponseEntity<?> borrarPropuesta(
            @PathVariable String idEmpresa,
            @PathVariable String idPropuesta
    ) {
        return propuestaRepo.findById(idPropuesta).map(p -> {
            if (!p.getEmpresaId().equals(idEmpresa))
                return ResponseEntity.status(403).body((Object)"No puedes eliminar esta propuesta");
            propuestaRepo.delete(p);
            return ResponseEntity.ok((Object)"Propuesta eliminada");
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{idEmpresa}/propuestas")
    public ResponseEntity<?> listarPropuestas(@PathVariable String idEmpresa) {
        return ResponseEntity.ok(propuestaRepo.findByEmpresaId(idEmpresa));
    }

    @GetMapping("/{idEmpresa}/postulaciones/{idPropuesta}")
    public ResponseEntity<?> verPostulaciones(
            @PathVariable String idEmpresa,
            @PathVariable String idPropuesta
    ) {
        Propuesta p = propuestaRepo.findById(idPropuesta).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        if (!p.getEmpresaId().equals(idEmpresa))
            return ResponseEntity.status(403).body("No puedes ver postulaciones de propuestas que no son tuyas");
        return ResponseEntity.ok(postulacionRepo.findByPropuestaId(idPropuesta));
    }

    @PutMapping("/{idEmpresa}/postulaciones/{idPostulacion}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable String idEmpresa,
            @PathVariable String idPostulacion,
            @RequestBody Map<String, String> body
    ) {
        return postulacionRepo.findById(idPostulacion).map(po -> {
            Propuesta p = propuestaRepo.findById(po.getPropuestaId()).orElse(null);
            if (p == null || !p.getEmpresaId().equals(idEmpresa))
                return ResponseEntity.status(403).body((Object)"No puedes modificar postulaciones que no te pertenecen");

            String nuevoEstado = body.get("estado");
            if (nuevoEstado == null || nuevoEstado.isBlank())
                return ResponseEntity.badRequest().body((Object)"Debes enviar el campo 'estado'");

            po.setEstadoPostulacion(nuevoEstado);
            return ResponseEntity.ok((Object) postulacionRepo.save(po));
        }).orElse(ResponseEntity.notFound().build());
    }
    
}
