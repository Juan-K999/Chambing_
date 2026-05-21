package Chambing.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import Chambing.demo.Model.Postulacion;

public interface PostulacionRepository extends MongoRepository<Postulacion, String> {

    boolean existsByUsuarioIdAndPropuestaId(String idUsuario, String idPropuesta);

    List<Postulacion> findByPropuestaId(String idPropuesta);

    List<Postulacion> findByUsuarioId(String idUsuario);
}