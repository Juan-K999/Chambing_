package Chambing.demo.repository;

import Chambing.demo.Model.Genero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneroRepository extends JpaRepository<Genero, Long> {
    List<Genero> findByActivoTrue();
    boolean existsByNombreIgnoreCase(String nombre);
}
