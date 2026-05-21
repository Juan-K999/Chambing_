package Chambing.demo.repository;

import Chambing.demo.Model.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Long> {
    List<TipoDocumento> findByActivoTrue();
    boolean existsByCodigoIgnoreCase(String codigo);
}
