package Chambing.demo.controller;
 
import java.util.HashMap;
import java.util.List;
import java.util.Map;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
 
import Chambing.demo.Model.Empresa;
import Chambing.demo.Model.Genero;
import Chambing.demo.Model.Postulacion;
import Chambing.demo.Model.Propuesta;
import Chambing.demo.Model.TipoDocumento;
import Chambing.demo.Model.Usuario;
import Chambing.demo.dto.EmpresaUpdateDTO;
import Chambing.demo.dto.UsuarioUpdateDTO;
import Chambing.demo.repository.EmpresaRepository;
import Chambing.demo.repository.GeneroRepository;
import Chambing.demo.repository.PostulacionRepository;
import Chambing.demo.repository.PropuestaRepository;
import Chambing.demo.repository.TipoDocumentoRepository;
import Chambing.demo.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
 
@RestController
@RequestMapping("/admin")
@CrossOrigin("*")
public class AdminController {
 
    @Autowired private UsuarioRepository    usuarioRepo;
    @Autowired private EmpresaRepository    empresaRepo;
    @Autowired private PropuestaRepository  propuestaRepo;
    @Autowired private PostulacionRepository postulacionRepo;
    @Autowired private GeneroRepository      generoRepo;
    @Autowired private TipoDocumentoRepository tipoDocumentoRepo;
 
    @Value("${app.login.user}")     private String loginUser;
    @Value("${app.login.password}") private String loginPassword;
 
 
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpSession session) {
        String user = body.get("user");
        String pass = body.get("password");
 
        if (user.equals(loginUser) && pass.equals(loginPassword)) {
            session.setAttribute("adminLogged", true);
            Map<String, Object> ok = new HashMap<>();
            ok.put("status", "ok");
            ok.put("redirect", "/admin/panel");
            return ResponseEntity.ok(ok);
        }
        return ResponseEntity.status(401).body("Credenciales incorrectas");
    }
 
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Sesión cerrada");
    }
 

    @GetMapping("/usuarios")
    public ResponseEntity<?> listarUsuarios(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "10") int    size,
            @RequestParam(defaultValue = "")   String q) {
 
        size = Math.min(size, 100);
 
        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
 
        Page<Usuario> pageResult;
 
        if (q.isBlank()) {
            pageResult = usuarioRepo.findAll(pageable);
        } else {
            pageResult = usuarioRepo.findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    q, q, q, pageable);
        }
 
        pageResult.forEach(u -> {
            u.setContrasena(null);
            u.setPassword(null);
        });
 
        return ResponseEntity.ok(pageResult);
    }
 
    @PutMapping("/usuario/{id}")
    public ResponseEntity<?> editarUsuario(
            @PathVariable String id,
            @RequestBody UsuarioUpdateDTO nuevo) {
 
        Usuario u = usuarioRepo.findById(id).orElse(null);
        if (u == null) return ResponseEntity.badRequest().body("Usuario no encontrado");
 
        u.setNombre(nuevo.getNombre());
        u.setApellido(nuevo.getApellido());
        u.setEmail(nuevo.getEmail());
        u.setDocumento(nuevo.getDocumento());
        u.setEdad(nuevo.getEdad());
 
        if (nuevo.getContrasena() != null && !nuevo.getContrasena().trim().isEmpty()) {
            u.setContrasena(nuevo.getContrasena());
        }
 
        return ResponseEntity.ok(usuarioRepo.save(u));
    }
 
    @DeleteMapping("/usuario/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable String id) {
        if (!usuarioRepo.existsById(id))
            return ResponseEntity.badRequest().body("Usuario no encontrado");
        usuarioRepo.deleteById(id);
        return ResponseEntity.ok("Usuario eliminado");
    }
 
 
    @GetMapping("/empresas")
    public ResponseEntity<?> listarEmpresas(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "10") int    size,
            @RequestParam(defaultValue = "")   String q) {
 
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("nombreEmpresa").ascending());
 
        Page<Empresa> pageResult = q.isBlank()
                ? empresaRepo.findAll(pageable)
                : empresaRepo.findByNombreEmpresaContainingIgnoreCaseOrCorreoContainingIgnoreCase(q, q, pageable);
 
        pageResult.forEach(e -> e.setContrasena(null));
 
        return ResponseEntity.ok(pageResult);
    }
 
    @PutMapping("/empresa/{id}")
    public ResponseEntity<?> editarEmpresa(
            @PathVariable String id,
            @RequestBody EmpresaUpdateDTO nueva) {
 
        Empresa e = empresaRepo.findById(id).orElse(null);
        if (e == null) return ResponseEntity.badRequest().body("Empresa no encontrada");
 
        e.setNit(nueva.getNit());
        e.setNombreEmpresa(nueva.getNombreEmpresa());
        e.setCorreo(nueva.getCorreo());
 
        if (nueva.getContrasena() != null && !nueva.getContrasena().trim().isEmpty()) {
            e.setContrasena(nueva.getContrasena());
        }
 
        return ResponseEntity.ok(empresaRepo.save(e));
    }
 
    @DeleteMapping("/empresa/{id}")
    public ResponseEntity<?> eliminarEmpresa(@PathVariable String id) {
        if (!empresaRepo.existsById(id))
            return ResponseEntity.badRequest().body("Empresa no encontrada");
        empresaRepo.deleteById(id);
        return ResponseEntity.ok("Empresa eliminada");
    }
 
 
    @GetMapping("/propuestas")
    public ResponseEntity<?> listarPropuestas(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "10") int    size,
            @RequestParam(defaultValue = "")   String q) {
 
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("titulo").ascending());
 
        Page<Propuesta> pageResult = q.isBlank()
                ? propuestaRepo.findAll(pageable)
                : propuestaRepo.findByTituloContainingIgnoreCaseOrCategoriaContainingIgnoreCase(q, q, pageable);
 
        return ResponseEntity.ok(pageResult);
    }
 
    @PutMapping("/propuesta/{id}")
    public ResponseEntity<?> editarPropuesta(@PathVariable String id, @RequestBody Propuesta nuevo) {
        Propuesta p = propuestaRepo.findById(id).orElse(null);
        if (p == null) return ResponseEntity.badRequest().body("Propuesta no encontrada");
 
        p.setTitulo(nuevo.getTitulo());
        p.setDescripcion(nuevo.getDescripcion());
        p.setRequisitos(nuevo.getRequisitos());
        p.setSalario(nuevo.getSalario());
        p.setEstado(nuevo.getEstado());
        p.setCategoria(nuevo.getCategoria());
 
        return ResponseEntity.ok(propuestaRepo.save(p));
    }
 
    @DeleteMapping("/propuesta/{id}")
    public ResponseEntity<?> eliminarPropuesta(@PathVariable String id) {
        if (!propuestaRepo.existsById(id))
            return ResponseEntity.badRequest().body("Propuesta no encontrada");
        propuestaRepo.deleteById(id);
        return ResponseEntity.ok("Propuesta eliminada");
    }
 
 
    @GetMapping("/postulaciones")
    public ResponseEntity<?> listarPostulaciones(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
 
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaPostulacion").descending());
        return ResponseEntity.ok(postulacionRepo.findAll(pageable));
    }
 
    @PutMapping("/postulacion/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
 
        Postulacion p = postulacionRepo.findById(id).orElse(null);
        if (p == null) return ResponseEntity.badRequest().body("Postulación no encontrada");
 
        String nuevoEstado = body.get("estado");
        if (nuevoEstado == null || nuevoEstado.isBlank())
            return ResponseEntity.badRequest().body("Debes enviar el campo 'estado'");
 
        p.setEstadoPostulacion(nuevoEstado);
        return ResponseEntity.ok(postulacionRepo.save(p));
    }
 
    @DeleteMapping("/postulacion/{id}")
    public ResponseEntity<?> eliminarPostulacion(@PathVariable String id) {
        if (!postulacionRepo.existsById(id))
            return ResponseEntity.badRequest().body("Postulación no encontrada");
        postulacionRepo.deleteById(id);
        return ResponseEntity.ok("Postulación eliminada");
    }


    @GetMapping("/generos")
    public ResponseEntity<?> listarGeneros(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "10") int    size,
            @RequestParam(defaultValue = "")   String q) {

        size = Math.min(size, 100);
        List<Genero> todos = generoRepo.findAll();

        if (!q.isBlank()) {
            String ql = q.toLowerCase();
            todos = todos.stream()
                .filter(g -> (g.getNombre() != null && g.getNombre().toLowerCase().contains(ql)))
                .collect(java.util.stream.Collectors.toList());
        }

        int total  = todos.size();
        int from   = page * size;
        int to     = Math.min(from + size, total);
        List<Genero> slice = from >= total ? List.of() : todos.subList(from, to);

        Map<String, Object> result = new HashMap<>();
        result.put("content",        slice);
        result.put("totalElements",  total);
        result.put("totalPages",     (int) Math.ceil((double) total / size));
        result.put("number",         page);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/generos")
    public ResponseEntity<?> crearGenero(@RequestBody Genero nuevo) {
        if (nuevo.getNombre() == null || nuevo.getNombre().isBlank())
            return ResponseEntity.badRequest().body("El nombre es obligatorio");
        if (generoRepo.existsByNombreIgnoreCase(nuevo.getNombre()))
            return ResponseEntity.badRequest().body("Ya existe un género con ese nombre");
        return ResponseEntity.ok(generoRepo.save(nuevo));
    }

    @PutMapping("/generos/{id}")
    public ResponseEntity<?> editarGenero(@PathVariable Long id, @RequestBody Genero nuevo) {
        Genero g = generoRepo.findById(id).orElse(null);
        if (g == null) return ResponseEntity.badRequest().body("Género no encontrado");
        g.setNombre(nuevo.getNombre());
        g.setDescripcion(nuevo.getDescripcion());
        g.setActivo(nuevo.isActivo());
        return ResponseEntity.ok(generoRepo.save(g));
    }

    @DeleteMapping("/generos/{id}")
    public ResponseEntity<?> eliminarGenero(@PathVariable Long id) {
        if (!generoRepo.existsById(id))
            return ResponseEntity.badRequest().body("Género no encontrado");
        generoRepo.deleteById(id);
        return ResponseEntity.ok("Género eliminado");
    }



    @GetMapping("/tipos-documento")
    public ResponseEntity<?> listarTiposDocumento(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "10") int    size,
            @RequestParam(defaultValue = "")   String q) {

        size = Math.min(size, 100);
        List<TipoDocumento> todos = tipoDocumentoRepo.findAll();

        if (!q.isBlank()) {
            String ql = q.toLowerCase();
            todos = todos.stream()
                .filter(t ->
                    (t.getNombre() != null && t.getNombre().toLowerCase().contains(ql)) ||
                    (t.getCodigo() != null && t.getCodigo().toLowerCase().contains(ql)))
                .collect(java.util.stream.Collectors.toList());
        }

        int total  = todos.size();
        int from   = page * size;
        int to     = Math.min(from + size, total);
        List<TipoDocumento> slice = from >= total ? List.of() : todos.subList(from, to);

        Map<String, Object> result = new HashMap<>();
        result.put("content",        slice);
        result.put("totalElements",  total);
        result.put("totalPages",     (int) Math.ceil((double) total / size));
        result.put("number",         page);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/tipos-documento")
    public ResponseEntity<?> crearTipoDocumento(@RequestBody TipoDocumento nuevo) {
        if (nuevo.getCodigo() == null || nuevo.getCodigo().isBlank())
            return ResponseEntity.badRequest().body("El código es obligatorio");
        if (nuevo.getNombre() == null || nuevo.getNombre().isBlank())
            return ResponseEntity.badRequest().body("El nombre es obligatorio");
        if (tipoDocumentoRepo.existsByCodigoIgnoreCase(nuevo.getCodigo()))
            return ResponseEntity.badRequest().body("Ya existe un tipo de documento con ese código");
        return ResponseEntity.ok(tipoDocumentoRepo.save(nuevo));
    }

    @PutMapping("/tipos-documento/{id}")
    public ResponseEntity<?> editarTipoDocumento(@PathVariable Long id, @RequestBody TipoDocumento nuevo) {
        TipoDocumento td = tipoDocumentoRepo.findById(id).orElse(null);
        if (td == null) return ResponseEntity.badRequest().body("Tipo de documento no encontrado");
        td.setCodigo(nuevo.getCodigo());
        td.setNombre(nuevo.getNombre());
        td.setDescripcion(nuevo.getDescripcion());
        td.setActivo(nuevo.isActivo());
        return ResponseEntity.ok(tipoDocumentoRepo.save(td));
    }

    @DeleteMapping("/tipos-documento/{id}")
    public ResponseEntity<?> eliminarTipoDocumento(@PathVariable Long id) {
        if (!tipoDocumentoRepo.existsById(id))
            return ResponseEntity.badRequest().body("Tipo de documento no encontrado");
        tipoDocumentoRepo.deleteById(id);
        return ResponseEntity.ok("Tipo de documento eliminado");
    }
}