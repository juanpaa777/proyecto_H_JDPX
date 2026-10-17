package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.dto.onboarding.*;
import com.proyecto.servicios.service.onboarding.ClienteService;
import com.proyecto.servicios.service.onboarding.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Endpoints para Onboarding y gestión del ciclo de vida sin PathVariable ni RequestParam (100% RequestBody)")
public class ClienteController {

    private final OnboardingService onboardingService;
    private final ClienteService clienteService;

    @PostMapping("/registrar")
    @Operation(summary = "Registrar un nuevo cliente", description = "Crea el cliente, su domicilio, cuenta bancaria inicial y usuario con contraseña cifrada")
    public ResponseEntity<ClienteResponse> registrarCliente(@Valid @RequestBody RegistroClienteRequest request) {
        ClienteResponse response = onboardingService.registrarNuevoCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/consultar")
    @Operation(summary = "Consultar clientes con filtros combinables y paginación", description = "Único endpoint de consulta combinando criterios (id, nombres, curp, rfc, correo, activo, fechas) y paginación mediante RequestBody")
    public ResponseEntity<PaginaResponse<ClienteResponse>> consultarClientes(@Valid @RequestBody ConsultaClienteRequest request) {
        Page<ClienteResponse> pagina = clienteService.buscarClientesPaginados(request);
        return ResponseEntity.ok(PaginaResponse.from(pagina));
    }

    @PostMapping("/detalle")
    @Operation(summary = "Consultar detalle de un cliente", description = "Retorna el cliente por ID, CURP, RFC o número de cuenta enviado en RequestBody")
    public ResponseEntity<ClienteResponse> consultarDetalle(@Valid @RequestBody DetalleClienteRequest request) {
        return ResponseEntity.ok(clienteService.obtenerClienteDetalle(request));
    }

    @PutMapping("/actualizar")
    @Operation(summary = "Actualizar parcialmente la información de un cliente", description = "Modifica datos personales, contacto, domicilio o laborales. Bloquea modificación de CURP, RFC y Cuenta.")
    public ResponseEntity<ClienteResponse> actualizarCliente(@Valid @RequestBody ClienteUpdateRequest request) {
        return ResponseEntity.ok(clienteService.actualizarClienteParcial(request));
    }

    @PutMapping("/baja")
    @Operation(summary = "Baja lógica de un cliente", description = "Desactiva en cascada al cliente, sus cuentas y su usuario sin borrar datos físicos")
    public ResponseEntity<ClienteResponse> bajaLogica(@Valid @RequestBody EliminaClienteRequest request) {
        return ResponseEntity.ok(clienteService.darDeBajaClienteLogica(request));
    }
}