package com.proyecto.servicios.dto.onboarding;

import com.proyecto.servicios.validation.onboarding.MayorDeEdad;
import com.proyecto.servicios.validation.onboarding.PasswordSeguro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class RegistroClienteRequest {

    // --- Datos Personales ---
    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[\\p{L}\\s]{2,50}$", message = "El nombre debe contener solo letras y espacios, entre 2 y 50 caracteres")
    private String nombre;

    @Pattern(regexp = "^$|^[\\p{L}\\s]{2,50}$", message = "El segundo nombre debe contener solo letras y espacios, entre 2 y 50 caracteres")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[\\p{L}\\s]{2,50}$", message = "El apellido paterno debe contener solo letras y espacios, entre 2 y 50 caracteres")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^[\\p{L}\\s]{2,50}$", message = "El apellido materno debe contener solo letras y espacios, entre 2 y 50 caracteres")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @MayorDeEdad
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(regexp = "^[A-Za-z]{4}[0-9]{6}[HhMm][A-Za-z]{5}[0-9A-Za-z][0-9]$", message = "La CURP debe tener un formato oficial válido de 18 caracteres")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = "^[A-Za-zÑñ&]{3,4}[0-9]{6}[A-Za-z0-9]{3}$", message = "El RFC debe tener un formato oficial válido de 12 o 13 caracteres")
    private String rfc;

    @NotBlank(message = "El sexo es obligatorio")
    @Size(max = 15, message = "El sexo no puede exceder 15 caracteres")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    @Size(max = 50, message = "La nacionalidad no puede exceder 50 caracteres")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    @Size(max = 20, message = "El estado civil no puede exceder 20 caracteres")
    private String estadoCivil;

    // --- Datos de Contacto ---
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe ser válido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String correo;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono móvil debe contener exactamente 10 dígitos numéricos")
    private String telefonoMovil;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "El teléfono alternativo debe contener exactamente 10 dígitos numéricos")
    private String telefonoAlternativo;

    // --- Domicilio ---
    @NotNull(message = "Los datos del domicilio son obligatorios")
    @Valid
    private DomicilioDto domicilio;

    // --- Información Laboral ---
    @NotBlank(message = "La ocupación es obligatoria")
    @Size(max = 80, message = "La ocupación no puede exceder 80 caracteres")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(max = 100, message = "La empresa no puede exceder 100 caracteres")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;

    // --- Contraseña de Acceso (Usuario automático) ---
    @NotBlank(message = "La contraseña de acceso es obligatoria")
    @PasswordSeguro
    private String password;

    // --- Saldo Inicial (Opcional, con default del sistema) ---
    @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
    private BigDecimal saldoInicial;
}