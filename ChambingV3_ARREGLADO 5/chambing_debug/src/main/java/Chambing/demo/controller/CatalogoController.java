package Chambing.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Chambing.demo.repository.GeneroRepository;
import Chambing.demo.repository.TipoDocumentoRepository;

/**
 * Endpoints públicos para que las vistas (login, register) puedan
 * cargar los catálogos de géneros y tipos de documento desde MySQL
 * sin necesidad de estar autenticadas.
 *
 * Sólo expone los registros con activo = true.
 */
@RestController
@RequestMapping("/api/catalogos")
@CrossOrigin("*")
public class CatalogoController {

    @Autowired private GeneroRepository generoRepo;
    @Autowired private TipoDocumentoRepository tipoDocumentoRepo;

    @GetMapping("/generos")
    public ResponseEntity<?> generosActivos() {
        return ResponseEntity.ok(generoRepo.findByActivoTrue());
    }

    @GetMapping("/tipos-documento")
    public ResponseEntity<?> tiposDocumentoActivos() {
        return ResponseEntity.ok(tipoDocumentoRepo.findByActivoTrue());
    }
}
