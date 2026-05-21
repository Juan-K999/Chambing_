package Chambing.demo.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "tipos_documento")
public class TipoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El código no puede estar vacío")
    @Size(max = 10)
    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @Size(max = 250)
    @Column(length = 250)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;

    public TipoDocumento() {}
    public TipoDocumento(String codigo, String nombre) { this.codigo = codigo; this.nombre = nombre; }

    public Long    getId()                     { return id; }
    public void    setId(Long id)              { this.id = id; }
    public String  getCodigo()                 { return codigo; }
    public void    setCodigo(String codigo)    { this.codigo = codigo; }
    public String  getNombre()                 { return nombre; }
    public void    setNombre(String nombre)    { this.nombre = nombre; }
    public String  getDescripcion()            { return descripcion; }
    public void    setDescripcion(String d)    { this.descripcion = d; }
    public boolean isActivo()                  { return activo; }
    public void    setActivo(boolean activo)   { this.activo = activo; }
}
