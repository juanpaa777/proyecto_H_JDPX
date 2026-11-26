package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.dto.onboarding.*;
import com.proyecto.servicios.security.onboarding.AuthenticatedClientGuard;
import com.proyecto.servicios.service.onboarding.ClienteService;
import com.proyecto.servicios.service.onboarding.OnboardingService;
import com.proyecto.servicios.service.onboarding.RegistroConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final RegistroConsultaService registroConsultaService;

    @PostMapping("/verificar-disponibilidad")
    @Operation(summary = "Verificar disponibilidad temprana de correo, CURP o RFC", description = "Valida si el dato ya existe sin exponer información de otra cuenta y con límite de intentos")
    public ResponseEntity<VerificarDisponibilidadResponse> verificarDisponibilidad(
            @Valid @RequestBody VerificarDisponibilidadRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientKey = httpRequest.getHeader("X-Forwarded-For");
        if (clientKey == null || clientKey.isBlank()) {
            clientKey = httpRequest.getRemoteAddr();
        }
        return ResponseEntity.ok(registroConsultaService.verificarDisponibilidad(request, clientKey));
    }

    @PostMapping("/cp")
    @Operation(summary = "Consultar estado, municipio y colonias por código postal", description = "Consulta Postali enviando únicamente el código postal de 5 dígitos")
    public ResponseEntity<CodigoPostalResponse> consultarCodigoPostal(
            @Valid @RequestBody CodigoPostalRequest request
    ) {
        return ResponseEntity.ok(registroConsultaService.consultarCodigoPostal(request.getCodigoPostal()));
    }

    @PostMapping("/registrar")
    @Operation(summary = "Registrar un nuevo cliente", description = "Crea el cliente, su domicilio, cuenta bancaria inicial y usuario con contraseña cifrada")
    public ResponseEntity<ClienteResponse> registrarCliente(@Valid @RequestBody RegistroClienteRequest request) {
        ClienteResponse response = onboardingService.registrarNuevoCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/consultar")
    @Operation(summary = "Consultar clientes con filtros combinables y paginación", description = "Único endpoint de consulta combinando criterios (id, nombres, curp, rfc, correo, activo, fechas) y paginación mediante RequestBody")
    public ResponseEntity<PaginaResponse<ClienteResponse>> consultarClientes(@Valid @RequestBody ConsultaClienteRequest request) {
        Long authClienteId = AuthenticatedClientGuard.obtenerClienteAutenticadoOrNull();
        if (authClienteId != null) {
            request.setId(authClienteId);
        }
        Page<ClienteResponse> pagina = clienteService.buscarClientesPaginados(request);
        return ResponseEntity.ok(PaginaResponse.from(pagina));
    }

    @PostMapping("/detalle")
    @Operation(summary = "Consultar detalle de un cliente", description = "Retorna el cliente por ID, CURP, RFC o número de cuenta enviado en RequestBody")
    public ResponseEntity<ClienteResponse> consultarDetalle(@Valid @RequestBody DetalleClienteRequest request) {
        Long authClienteId = AuthenticatedClientGuard.obtenerClienteAutenticadoOrNull();
        if (authClienteId != null) {
            if (request.getId() != null) {
                AuthenticatedClientGuard.verificarPropiedadCliente(request.getId());
            } else {
                request.setId(authClienteId);
            }
        }
        return ResponseEntity.ok(clienteService.obtenerClienteDetalle(request));
    }

    @PutMapping("/actualizar")
    @Operation(summary = "Actualizar parcialmente la información de un cliente", description = "Modifica datos personales, contacto, domicilio o laborales. Bloquea modificación de CURP, RFC y Cuenta.")
    public ResponseEntity<ClienteResponse> actualizarCliente(@Valid @RequestBody ClienteUpdateRequest request) {
        AuthenticatedClientGuard.verificarPropiedadCliente(request.getId());
        return ResponseEntity.ok(clienteService.actualizarClienteParcial(request));
    }

    @PutMapping("/baja")
    @Operation(summary = "Baja lógica de un cliente", description = "Desactiva en cascada al cliente, sus cuentas y su usuario sin borrar datos físicos")
    public ResponseEntity<ClienteResponse> bajaLogica(@Valid @RequestBody EliminaClienteRequest request) {
        AuthenticatedClientGuard.verificarPropiedadCliente(request.getId());
        return ResponseEntity.ok(clienteService.darDeBajaClienteLogica(request));
    }
}