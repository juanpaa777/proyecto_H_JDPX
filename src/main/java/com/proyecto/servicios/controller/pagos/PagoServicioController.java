package com.proyecto.servicios.controller.pagos;

import com.proyecto.servicios.dto.onboarding.PaginaResponse;
import com.proyecto.servicios.dto.pagos.ConciliarPagoRequest;
import com.proyecto.servicios.dto.pagos.ConsultaPagoServicioRequest;
import com.proyecto.servicios.dto.pagos.PagoServicioRequest;
import com.proyecto.servicios.dto.pagos.PagoServicioResponse;
import com.proyecto.servicios.security.onboarding.AuthenticatedClientGuard;
import com.proyecto.servicios.service.pagos.PagoServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pagos-servicios")
@RequiredArgsConstructor
@Tag(name = "Pagos de Servicios (GestoPago)", description = "Integración de pago de servicios y recargas mediante sendTx.do y confirmTx.do con firma AES-256 (100% RequestBody)")
public class PagoServicioController {

    private final PagoServicioService pagoServicioService;

    @PostMapping("/pagar")
    @Operation(
            summary = "Ejecutar pago de servicio o recarga",
            description = "Valida cuenta activa, saldo, PIN BCrypt, producto en catálogo MongoDB, cifra el payload con AES-256, consume sendTx.do y descuenta el saldo únicamente si el proveedor responde código 01."
    )
    public ResponseEntity<PagoServicioResponse> realizarPago(@Valid @RequestBody PagoServicioRequest request) {
        return ResponseEntity.ok(pagoServicioService.procesarPagoServicio(request));
    }

    @PostMapping("/conciliar")
    @Operation(
            summary = "Conciliar transacción pendiente por timeout (confirmTx.do)",
            description = "Verifica con GestoPago si una transacción en estatus PENDIENTE_CONCILIACION fue aplicada (código 06) o no aplicada (código 70), ajustando el saldo de forma consistente."
    )
    public ResponseEntity<PagoServicioResponse> conciliarPago(@Valid @RequestBody ConciliarPagoRequest request) {
        return ResponseEntity.ok(pagoServicioService.conciliarPagoServicio(request));
    }

    @PostMapping("/consultar")
    @Operation(
            summary = "Consultar historial de pagos de servicios con filtros dinámicos y paginación",
            description = "Endpoint único de búsqueda combinando filtros opcionales (clienteId, numeroCuenta, upc, idServicio, idProducto, estatus, fechas) mediante RequestBody."
    )
    public ResponseEntity<PaginaResponse<PagoServicioResponse>> consultarPagos(@Valid @RequestBody ConsultaPagoServicioRequest request) {
        Long authClienteId = AuthenticatedClientGuard.obtenerClienteAutenticadoOrNull();
        if (authClienteId != null) {
            if (request.getClienteId() != null && !authClienteId.equals(request.getClienteId())) {
                AuthenticatedClientGuard.verificarPropiedadCliente(request.getClienteId());
            }
            request.setClienteId(authClienteId);
        }
        Page<PagoServicioResponse> pagina = pagoServicioService.consultarPagosPaginados(request);
        return ResponseEntity.ok(PaginaResponse.from(pagina));
    }

    @PostMapping("/enviar-ticket")
    @Operation(
            summary = "Enviar comprobante de pago (ticket HTML) al correo mediante Google Apps Script",
            description = "Envía el ticket formateado de una operación (exitosa o fallida) al correo del cliente titular o al correo indicado."
    )
    public ResponseEntity<com.proyecto.servicios.model.GenericResponse> enviarTicketCorreo(
            @RequestBody java.util.Map<String, String> body
    ) {
        String upc = body != null ? body.get("upc") : null;
        String correoDestino = body != null ? body.get("correoDestino") : null;
        boolean enviado = pagoServicioService.enviarTicketPorCorreo(upc, correoDestino);
        if (!enviado) {
            return ResponseEntity.badRequest().body(new com.proyecto.servicios.model.GenericResponse(
                    400,
                    "No se pudo enviar el ticket. Verifica que GOOGLE_SCRIPT_TICKET_URL esté configurado en el servidor."
            ));
        }
        return ResponseEntity.ok(new com.proyecto.servicios.model.GenericResponse(
                200,
                "Ticket enviado correctamente al correo."
        ));
    }
}

