package Chambing.demo.Model;

import jakarta.validation.constraints.NotBlank;

public class Direccion {

    @NotBlank
    private String calle;

    private String ciudad;

    private String departamento;

    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
}