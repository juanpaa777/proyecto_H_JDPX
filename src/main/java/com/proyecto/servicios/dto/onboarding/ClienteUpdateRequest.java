package com.proyecto.servicios.dto.onboarding;

import com.proyecto.servicios.validation.onboarding.MayorDeEdad;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ClienteUpdateRequest {

    // Datos Personales
    @Pattern(regexp = "^$|^[\\p{L}\\s]{2,50}$", message = "El nombre debe contener solo letras y espacios, entre 2 y 50 caracteres")
    private String nombre;

    @Pattern(regexp = "^$|^[\\p{L}\\s]{2,50}$", message = "El segundo nombre debe contener solo letras y espacios, entre 2 y 50 caracteres")
    private String segundoNombre;

    @Pattern(regexp = "^$|^[\\p{L}\\s]{2,50}$", message = "El apellido paterno debe contener solo letras y espacios, entre 2 y 50 caracteres")
    private String apellidoPaterno;

    @Pattern(regexp = "^$|^[\\p{L}\\s]{2,50}$", message = "El apellido materno debe contener solo letras y espacios, entre 2 y 50 caracteres")
    private String apellidoMaterno;

    @MayorDeEdad
    private LocalDate fechaNacimiento;

    @Size(max = 15, message = "El sexo no puede exceder 15 caracteres")
    private String sexo;

    @Size(max = 50, message = "La nacionalidad no puede exceder 50 caracteres")
    private String nacionalidad;

    @Size(max = 20, message = "El estado civil no puede exceder 20 caracteres")
    private String estadoCivil;

    // Contacto
    @Pattern(regexp = "^$|^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "El correo electrónico debe ser válido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String correo;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "El teléfono móvil debe contener exactamente 10 dígitos numéricos")
    private String telefonoMovil;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "El teléfono alternativo debe contener exactamente 10 dígitos numéricos")
    private String telefonoAlternativo;

    // Domicilio
    @Valid
    private DomicilioDto domicilio;

    // Laboral
    @Size(max = 80, message = "La ocupación no puede exceder 80 caracteres")
    private String ocupacion;

    @Size(max = 100, message = "La empresa no puede exceder 100 caracteres")
    private String empresa;

    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;

    // Campos prohibidos para modificación (si se envían, se validará inmutabilidad)
    private String curp;
    private String rfc;
    private String numeroCuenta;
}