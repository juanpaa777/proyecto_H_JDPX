package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.dto.onboarding.CuentaCreateRequest;
import com.proyecto.servicios.dto.onboarding.CuentaResponse;
import com.proyecto.servicios.dto.onboarding.CuentaUpdateRequest;
import com.proyecto.servicios.dto.onboarding.SaldoResponse;
import com.proyecto.servicios.service.onboarding.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
@Tag(name = "Cuentas", description = "Endpoints para la creación, consulta y actualización de cuentas bancarias")
public class CuentaController {

    private final CuentaService cuentaService;

    @PostMapping
    @Operation(summary = "Crear una cuenta asociada a un cliente", description = "Genera una nueva cuenta bancaria para un cliente activo existente")
    public ResponseEntity<CuentaResponse> crearCuenta(@Valid @RequestBody CuentaCreateRequest request) {
        CuentaResponse response = cuentaService.crearCuenta(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{numeroCuenta}")
    @Operation(summary = "Consultar una cuenta por número de cuenta", description = "Obtiene los detalles de la cuenta por su identificador único de cuenta")
    public ResponseEntity<CuentaResponse> consultarPorNumeroCuenta(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.consultarPorNumeroCuenta(numeroCuenta));
    }

    @GetMapping("/{numeroCuenta}/saldo")
    @Operation(summary = "Consultar saldo de una cuenta", description = "Obtiene el saldo disponible y estatus de una cuenta")
    public ResponseEntity<SaldoResponse> consultarSaldo(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.consultarSaldo(numeroCuenta));
    }

    @GetMapping
    @Operation(summary = "Consultar cuentas por cliente o estatus", description = "Permite filtrar cuentas por clienteId o estatus (ej. ACTIVA)")
    public ResponseEntity<List<CuentaResponse>> consultarCuentas(
            @Parameter(description = "ID del cliente titular")
            @RequestParam(required = false) Long clienteId,

            @Parameter(description = "Estatus de la cuenta (ej. ACTIVA, INACTIVA)")
            @RequestParam(required = false) String estatus
    ) {
        if (clienteId != null) {
            return ResponseEntity.ok(cuentaService.consultarPorClienteId(clienteId));
        }
        if (estatus != null && !estatus.trim().isEmpty()) {
            return ResponseEntity.ok(cuentaService.consultarPorEstatus(estatus));
        }
        return ResponseEntity.ok(cuentaService.consultarCuentasActivas());
    }

    @PatchMapping("/{numeroCuenta}")
    @Operation(summary = "Actualizar parcialmente la información de una cuenta (PATCH)", description = "Permite modificar estatus o saldo. Bloquea modificación del número de cuenta.")
    public ResponseEntity<CuentaResponse> actualizarParcialPatch(
            @PathVariable String numeroCuenta,
            @Valid @RequestBody CuentaUpdateRequest request
    ) {
        return ResponseEntity.ok(cuentaService.actualizarParcial(numeroCuenta, request));
    }

    @PutMapping("/{numeroCuenta}")
    @Operation(summary = "Actualizar información de una cuenta (PUT)", description = "Permite modificar información de la cuenta protegiendo el número de cuenta")
    public ResponseEntity<CuentaResponse> actualizarParcialPut(
            @PathVariable String numeroCuenta,
            @Valid @RequestBody CuentaUpdateRequest request
    ) {
        return ResponseEntity.ok(cuentaService.actualizarParcial(numeroCuenta, request));
    }
}
