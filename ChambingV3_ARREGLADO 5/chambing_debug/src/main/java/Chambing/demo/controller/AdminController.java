package Chambing.demo.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
 
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.springframework.data.mongodb.core.query.Criteria;
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
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MongoTemplate mongoTemplate;

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


    /**
     * Resumen para la pestaña "Estadísticas" del panel.
     * Mongo hace todos los conteos con un único $facet por colección (4 consultas en total)
     * y sólo viajan los totales, sin importar cuántos registros existan.
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<?> estadisticas() {
        ZoneId zona = ZoneId.systemDefault();

        // ── Usuarios ──
        Document usuarios = facet("usuarios", Aggregation
                .facet(Aggregation.count().as("n")).as("total")
                .and(Aggregation.group("genero").count().as("value")).as("porGenero")
                .and(Aggregation.match(Criteria.where("edad").gt(0)),
                     Aggregation.bucket("edad").withBoundaries(0, 25, 35, 45, 55)
                                .withDefaultBucket(55).andOutputCount().as("value")).as("porEdad"));

        // ── Empresas ──
        Document empresas = facet("empresas", Aggregation
                .facet(Aggregation.count().as("n")).as("total")
                .and(Aggregation.group("direccion.ciudad").count().as("value")).as("porCiudad"));

        // ── Propuestas ──
        Document propuestas = facet("propuestas", Aggregation
                .facet(Aggregation.count().as("n")).as("total")
                .and(Aggregation.group("estado").count().as("value")).as("porEstado")
                .and(Aggregation.group("categoria").count().as("value")).as("porCategoria")
                .and(Aggregation.group("empresaNombre").count().as("value")).as("porEmpresa")
                .and(Aggregation.match(Criteria.where("salario").gt(0)),
                     Aggregation.bucket("salario").withBoundaries(0, 1_000_000, 2_000_000, 3_000_000, 5_000_000)
                                .withDefaultBucket(5_000_000).andOutputCount().as("value")).as("salarioRangos")
                .and(Aggregation.match(Criteria.where("salario").gt(0)),
                     Aggregation.group().avg("salario").as("promedio").count().as("n")).as("salarioResumen"));

        // ── Postulaciones (los últimos 6 meses, contando el actual) ──
        LocalDate inicio = LocalDate.now(zona).withDayOfMonth(1).minusMonths(5);
        DateOperators.Timezone tz = DateOperators.Timezone.valueOf(zona.getId());
        Document postulaciones = facet("postulaciones", Aggregation
                .facet(Aggregation.count().as("n")).as("total")
                .and(Aggregation.group("estadoPostulacion").count().as("value")).as("porEstado")
                .and(Aggregation.match(Criteria.where("fechaPostulacion").gte(Date.from(inicio.atStartOfDay(zona).toInstant()))),
                     Aggregation.project()
                                .and(DateOperators.Year.yearOf("fechaPostulacion").withTimezone(tz)).as("anio")
                                .and(DateOperators.Month.monthOf("fechaPostulacion").withTimezone(tz)).as("mes"),
                     Aggregation.group("anio", "mes").count().as("value")).as("porMes"));

        // ── Armar respuesta ──
        Map<String, Object> totales = new LinkedHashMap<>();
        totales.put("usuarios",      total(usuarios));
        totales.put("empresas",      total(empresas));
        totales.put("propuestas",    total(propuestas));
        totales.put("postulaciones", total(postulaciones));

        // Meses con 0 postulaciones también se devuelven, para que la gráfica no tenga huecos
        Map<String, Long> conteoMes = new HashMap<>();
        for (Document d : postulaciones.getList("porMes", Document.class, List.of())) {
            Document id = (Document) d.get("_id");
            conteoMes.put(id.get("anio") + "-" + id.get("mes"), numero(d.get("value")));
        }
        List<Map<String, Object>> porMes = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            LocalDate m = inicio.plusMonths(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("anio",  m.getYear());
            item.put("mes",   m.getMonthValue());
            item.put("value", conteoMes.getOrDefault(m.getYear() + "-" + m.getMonthValue(), 0L));
            porMes.add(item);
        }

        List<Document> salarioResumen = propuestas.getList("salarioResumen", Document.class, List.of());
        Document resumen = salarioResumen.isEmpty() ? new Document() : salarioResumen.get(0);

        Map<String, Object> rangosEdad = new LinkedHashMap<>();
        rangosEdad.put("0", "18–24"); rangosEdad.put("25", "25–34"); rangosEdad.put("35", "35–44");
        rangosEdad.put("45", "45–54"); rangosEdad.put("55", "55+");

        Map<String, Object> rangosSalario = new LinkedHashMap<>();
        rangosSalario.put("0", "< 1 M"); rangosSalario.put("1000000", "1–2 M"); rangosSalario.put("2000000", "2–3 M");
        rangosSalario.put("3000000", "3–5 M"); rangosSalario.put("5000000", "> 5 M");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totales", totales);
        out.put("usuarios", Map.of(
                "porGenero", serie(usuarios, "porGenero"),
                "porEdad",   rangos(usuarios, "porEdad", rangosEdad)));
        out.put("empresas", Map.of(
                "porCiudad", serie(empresas, "porCiudad")));
        out.put("propuestas", Map.of(
                "porEstado",       serie(propuestas, "porEstado"),
                "porCategoria",    serie(propuestas, "porCategoria"),
                "porEmpresa",      serie(propuestas, "porEmpresa"),
                "salarioRangos",   rangos(propuestas, "salarioRangos", rangosSalario),
                "salarioPromedio", resumen.get("promedio") == null ? 0 : numeroDecimal(resumen.get("promedio")),
                "conSalario",      numero(resumen.get("n"))));
        out.put("postulaciones", Map.of(
                "porEstado", serie(postulaciones, "porEstado"),
                "porMes",    porMes));
        return ResponseEntity.ok(out);
    }

    // ── Helpers de estadísticas ──

    private Document facet(String coleccion, FacetOperation facet) {
        Document d = mongoTemplate
                .aggregate(Aggregation.newAggregation(facet), coleccion, Document.class)
                .getUniqueMappedResult();
        return d != null ? d : new Document();
    }

    private long total(Document facet) {
        List<Document> l = facet.getList("total", Document.class, List.of());
        return l.isEmpty() ? 0 : numero(l.get(0).get("n"));
    }

    /** Resultado de un $group → [{label, value}] ordenado de mayor a menor. */
    private List<Map<String, Object>> serie(Document facet, String campo) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Document d : facet.getList(campo, Document.class, List.of())) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("label", d.get("_id"));          // puede ser null → el panel lo muestra como "Sin …"
            item.put("value", numero(d.get("value")));
            out.add(item);
        }
        out.sort((a, b) -> Long.compare((Long) b.get("value"), (Long) a.get("value")));
        return out;
    }

    /** Resultado de un $bucket → todos los rangos en orden, incluidos los que quedaron en 0. */
    private List<Map<String, Object>> rangos(Document facet, String campo, Map<String, Object> etiquetas) {
        Map<String, Long> conteo = new HashMap<>();
        for (Document d : facet.getList(campo, Document.class, List.of())) {
            conteo.put(String.valueOf(numero(d.get("_id"))), numero(d.get("value")));
        }
        List<Map<String, Object>> out = new ArrayList<>();
        etiquetas.forEach((limite, label) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("label", label);
            item.put("value", conteo.getOrDefault(limite, 0L));
            out.add(item);
        });
        return out;
    }

    private long numero(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    private double numeroDecimal(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0d;
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
        if (nuevo.getEmail() != null) u.setEmail(nuevo.getEmail().toLowerCase().trim());
        u.setDocumento(nuevo.getDocumento());
        u.setEdad(nuevo.getEdad());
        if (nuevo.getEnabled() != null)   u.setEnabled(nuevo.getEnabled());
        if (nuevo.getDireccion() != null) u.setDireccion(nuevo.getDireccion());

        if (nuevo.getContrasena() != null && !nuevo.getContrasena().trim().isEmpty()) {
            // Encriptar con BCrypt: el login compara contra hash BCrypt.
            // Guardarla en texto plano hacia que el usuario no pudiera volver a entrar.
            u.setContrasena(passwordEncoder.encode(nuevo.getContrasena()));
        }
 
        Usuario saved = usuarioRepo.save(u);
        saved.setContrasena(null);
        return ResponseEntity.ok(saved);
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
        if (nueva.getCorreo() != null) e.setCorreo(nueva.getCorreo().toLowerCase().trim());
        if (nueva.getEnabled() != null)   e.setEnabled(nueva.getEnabled());
        if (nueva.getDireccion() != null) e.setDireccion(nueva.getDireccion());

        if (nueva.getContrasena() != null && !nueva.getContrasena().trim().isEmpty()) {
            e.setContrasena(passwordEncoder.encode(nueva.getContrasena()));
        }
 
        Empresa saved = empresaRepo.save(e);
        saved.setContrasena(null);
        return ResponseEntity.ok(saved);
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