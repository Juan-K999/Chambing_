package Chambing.demo.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "generos")
public class Genero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del género no puede estar vacío")
    @Size(max = 60)
    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    @Size(max = 200)
    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;

    public Genero() {}
    public Genero(String nombre) { this.nombre = nombre; }

    public Long    getId()                    { return id; }
    public void    setId(Long id)             { this.id = id; }
    public String  getNombre()                { return nombre; }
    public void    setNombre(String nombre)   { this.nombre = nombre; }
    public String  getDescripcion()           { return descripcion; }
    public void    setDescripcion(String d)   { this.descripcion = d; }
    public boolean isActivo()                 { return activo; }
    public void    setActivo(boolean activo)  { this.activo = activo; }
}
