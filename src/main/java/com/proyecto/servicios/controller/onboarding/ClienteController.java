package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.dto.onboarding.ClienteResponse;
import com.proyecto.servicios.dto.onboarding.ClienteUpdateRequest;
import com.proyecto.servicios.dto.onboarding.RegistroClienteRequest;
import com.proyecto.servicios.service.onboarding.ClienteService;
import com.proyecto.servicios.service.onboarding.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Endpoints para el Onboarding, consultas y gestión del ciclo de vida de clientes personas físicas")
public class ClienteController {

    private final OnboardingService onboardingService;
    private final ClienteService clienteService;

    @PostMapping
    @Operation(summary = "Registrar un nuevo cliente", description = "Crea el cliente, su domicilio, su cuenta bancaria inicial activa y su usuario de acceso con contraseña cifrada")
    public ResponseEntity<ClienteResponse> registrarCliente(@Valid @RequestBody RegistroClienteRequest request) {
        ClienteResponse response = onboardingService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Consultar clientes con filtros dinámicos", description = "Permite obtener todos los clientes o filtrar por nombre, apellido paterno, apellido materno, CURP, RFC, correo o estatus activo")
    public ResponseEntity<List<ClienteResponse>> consultarClientes(
            @Parameter(description = "Buscar clientes por nombre")
            @RequestParam(required = false) String nombre,

            @Parameter(description = "Buscar clientes por apellido paterno")
            @RequestParam(required = false) String apellidoPaterno,

            @Parameter(description = "Buscar clientes por apellido materno")
            @RequestParam(required = false) String apellidoMaterno,

            @Parameter(description = "Buscar un cliente por CURP")
            @RequestParam(required = false) String curp,

            @Parameter(description = "Buscar un cliente por RFC")
            @RequestParam(required = false) String rfc,

            @Parameter(description = "Buscar cliente por correo electrónico")
            @RequestParam(required = false) String correo,

            @Parameter(description = "Filtrar por estatus activo (true/false)")
            @RequestParam(required = false) Boolean activo
    ) {
        if (curp != null && !curp.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.singletonList(clienteService.consultarPorCurp(curp)));
        }
        if (rfc != null && !rfc.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.singletonList(clienteService.consultarPorRfc(rfc)));
        }
        if (correo != null && !correo.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.singletonList(clienteService.consultarPorCorreo(correo)));
        }
        if (nombre != null && !nombre.trim().isEmpty()) {
            return ResponseEntity.ok(clienteService.buscarPorNombre(nombre));
        }
        if (apellidoPaterno != null && !apellidoPaterno.trim().isEmpty()) {
            return ResponseEntity.ok(clienteService.buscarPorApellidoPaterno(apellidoPaterno));
        }
        if (apellidoMaterno != null && !apellidoMaterno.trim().isEmpty()) {
            return ResponseEntity.ok(clienteService.buscarPorApellidoMaterno(apellidoMaterno));
        }
        if (activo != null && activo) {
            return ResponseEntity.ok(clienteService.consultarActivos());
        }
        return ResponseEntity.ok(clienteService.consultarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un cliente por su identificador", description = "Retorna el cliente con su domicilio, cuentas bancarias y usuario asociado")
    public ResponseEntity<ClienteResponse> consultarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.consultarPorId(id));
    }

    @GetMapping("/por-cuenta/{numeroCuenta}")
    @Operation(summary = "Consultar cliente por número de cuenta", description = "Obtiene los datos del cliente titular a partir de su número de cuenta bancaria")
    public ResponseEntity<ClienteResponse> consultarPorNumeroCuenta(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(clienteService.consultarPorNumeroCuenta(numeroCuenta));
    }

    @GetMapping("/rango-fechas")
    @Operation(summary = "Obtener clientes registrados en un rango de fechas", description = "Filtra clientes por fecha de registro entre fechaInicio y fechaFin")
    public ResponseEntity<List<ClienteResponse>> consultarPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin
    ) {
        return ResponseEntity.ok(clienteService.consultarPorRangoFechas(fechaInicio, fechaFin));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Actualizar parcialmente la información de un cliente (PATCH)", description = "Permite modificar datos personales, contacto, domicilio o laborales. Bloquea modificación de CURP y RFC.")
    public ResponseEntity<ClienteResponse> actualizarParcialPatch(@PathVariable Long id, @Valid @RequestBody ClienteUpdateRequest request) {
        return ResponseEntity.ok(clienteService.actualizarParcial(id, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar información de un cliente (PUT)", description = "Permite modificar datos del cliente manteniendo inmutables CURP y RFC")
    public ResponseEntity<ClienteResponse> actualizarParcialPut(@PathVariable Long id, @Valid @RequestBody ClienteUpdateRequest request) {
        return ResponseEntity.ok(clienteService.actualizarParcial(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Baja lógica de un cliente (DELETE)", description = "Desactiva al cliente, sus cuentas y su usuario sin borrar información física de la base de datos")
    public ResponseEntity<ClienteResponse> bajaLogicaDelete(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.bajaLogica(id));
    }

    @PatchMapping("/{id}/baja")
    @Operation(summary = "Baja lógica de un cliente (PATCH)", description = "Ruta alternativa explícita para desactivar al cliente y sus cuentas")
    public ResponseEntity<ClienteResponse> bajaLogicaPatch(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.bajaLogica(id));
    }
}
