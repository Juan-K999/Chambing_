package Chambing.demo.Model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "postulaciones")
public class Postulacion {

    @Id
    private String idPostulacion;

    private String usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;

    private String propuestaId;
    private String propuestaTitulo;

    private String estadoPostulacion;

    private LocalDateTime fechaPostulacion;

    public String getIdPostulacion() { return idPostulacion; }
    public void setIdPostulacion(String idPostulacion) { this.idPostulacion = idPostulacion; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public String getUsuarioEmail() { return usuarioEmail; }
    public void setUsuarioEmail(String usuarioEmail) { this.usuarioEmail = usuarioEmail; }

    public String getPropuestaId() { return propuestaId; }
    public void setPropuestaId(String propuestaId) { this.propuestaId = propuestaId; }

    public String getPropuestaTitulo() { return propuestaTitulo; }
    public void setPropuestaTitulo(String propuestaTitulo) { this.propuestaTitulo = propuestaTitulo; }

    public String getEstadoPostulacion() { return estadoPostulacion; }
    public void setEstadoPostulacion(String estadoPostulacion) { this.estadoPostulacion = estadoPostulacion; }

    public LocalDateTime getFechaPostulacion() { return fechaPostulacion; }
    public void setFechaPostulacion(LocalDateTime fechaPostulacion) { this.fechaPostulacion = fechaPostulacion; }
}