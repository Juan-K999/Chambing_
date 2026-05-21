package Chambing.demo.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import Chambing.demo.Model.Propuesta;

public interface PropuestaRepository extends MongoRepository<Propuesta, String> {

    List<Propuesta> findByEmpresaId(String idEmpresa);

    List<Propuesta> findByEstado(String estado);

    List<Propuesta> findByCategoria(String categoria);

    Page<Propuesta> findByTituloContainingIgnoreCaseOrCategoriaContainingIgnoreCase(
        String titulo, String categoria, Pageable pageable);
}