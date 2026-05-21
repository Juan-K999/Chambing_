package Chambing.demo.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import Chambing.demo.Model.Empresa;

public interface EmpresaRepository extends MongoRepository<Empresa, String> {

    boolean existsByCorreo(String correo);

    Optional<Empresa> findByCorreoAndContrasena(String correo, String contrasena);

    Optional<Empresa> findByNit(Integer nit);

    Optional<Empresa> findByCorreo(String correo);

    Page<Empresa> findByNombreEmpresaContainingIgnoreCaseOrCorreoContainingIgnoreCase(
        String nombre, String correo, Pageable pageable);
}