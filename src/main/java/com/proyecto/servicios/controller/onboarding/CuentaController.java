package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.dto.onboarding.*;
import com.proyecto.servicios.service.onboarding.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
@Tag(name = "Cuentas", description = "Endpoints para la gestión, consulta paginada y actualización de cuentas sin PathVariable ni RequestParam (100% RequestBody)")
public class CuentaController {

    private final CuentaService cuentaService;

    @PostMapping("/crear")
    @Operation(summary = "Crear una cuenta asociada a un cliente", description = "Genera una nueva cuenta bancaria para un cliente activo existente mediante RequestBody")
    public ResponseEntity<CuentaResponse> crearCuenta(@Valid @RequestBody CuentaCreateRequest request) {
        CuentaResponse response = cuentaService.crearCuentaBancaria(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/consultar")
    @Operation(summary = "Consultar cuentas con paginación y filtros", description = "Único endpoint para consultar cuentas por clienteId o estatus con paginación mediante RequestBody")
    public ResponseEntity<PaginaResponse<CuentaResponse>> consultarCuentas(@Valid @RequestBody ConsultaCuentaRequest request) {
        Page<CuentaResponse> pagina = cuentaService.listarCuentasPaginadas(request);
        return ResponseEntity.ok(PaginaResponse.from(pagina));
    }

    @PostMapping("/detalle")
    @Operation(summary = "Consultar detalle de una cuenta", description = "Obtiene los detalles de la cuenta enviando el número de cuenta en RequestBody")
    public ResponseEntity<CuentaResponse> consultarPorNumeroCuenta(@Valid @RequestBody DetalleCuentaRequest request) {
        return ResponseEntity.ok(cuentaService.obtenerCuentaDetalle(request));
    }

    @PostMapping("/saldo")
    @Operation(summary = "Consultar saldo de una cuenta", description = "Obtiene el saldo disponible y estatus de una cuenta mediante RequestBody")
    public ResponseEntity<SaldoResponse> consultarSaldo(@Valid @RequestBody DetalleCuentaRequest request) {
        return ResponseEntity.ok(cuentaService.obtenerSaldoDetalle(request));
    }

    @PutMapping("/actualizar")
    @Operation(summary = "Actualizar información de una cuenta", description = "Modifica estatus o saldo de una cuenta mediante RequestBody")
    public ResponseEntity<CuentaResponse> actualizarCuenta(@Valid @RequestBody CuentaUpdateRequest request) {
        return ResponseEntity.ok(cuentaService.actualizarCuentaParcial(request));
    }
}
