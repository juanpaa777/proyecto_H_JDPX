package com.proyecto.servicios.service.pagos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.pagos.PagoServicio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class TicketEmailService {

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final String googleScriptUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public TicketEmailService(
            @Value("${gestopago.email.google-script-url:}") String googleScriptUrl
    ) {
        this.googleScriptUrl = googleScriptUrl != null ? googleScriptUrl.trim() : "";
    }

    public boolean isConfigured() {
        return !googleScriptUrl.isBlank();
    }

    public boolean enviarTicket(PagoServicio pago, String correoDestino) {
        if (correoDestino == null || correoDestino.isBlank()) {
            log.warn("No se envió ticket por correo: correo destino vacío para UPC {}", pago.getUpc());
            return false;
        }
        if (!isConfigured()) {
            log.info("GOOGLE_SCRIPT_TICKET_URL no configurado; envío de ticket por correo omitido para UPC {}", pago.getUpc());
            return false;
        }

        String estatus = Optional.ofNullable(pago.getEstatus()).orElse("EXITOSA").toUpperCase();
        boolean esFallido = "FALLIDOS".equals(estatus) || "RECHAZADA".equals(estatus);
        String subject = (esFallido ? "Comprobante de Pago Fallido - " : "Comprobante de Pago Exitoso - ")
                + Optional.ofNullable(pago.getNombreProducto()).orElse("GestoPago");
        String htmlBody = construirHtmlTicket(pago);

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "to", correoDestino.trim(),
                    "subject", subject,
                    "htmlBody", htmlBody,
                    "senderName", "GestoPago Banca Digital"
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(googleScriptUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Ticket enviado por Google Apps Script a {} (UPC: {})", correoDestino, pago.getUpc());
                return true;
            }
            log.error("Google Apps Script respondió HTTP {} al enviar ticket {}: {}",
                    response.statusCode(), pago.getUpc(), response.body());
            return false;
        } catch (Exception ex) {
            log.error("Error enviando ticket por Google Apps Script para UPC {}: {}", pago.getUpc(), ex.getMessage());
            return false;
        }
    }

    public String construirHtmlTicket(PagoServicio p) {
        String estatus = Optional.ofNullable(p.getEstatus()).orElse("EXITOSA").toUpperCase();
        boolean esFallido = "FALLIDOS".equals(estatus) || "RECHAZADA".equals(estatus);
        String colorEstado = esFallido ? "#DC2626" : "#059669";
        String bgEstado = esFallido ? "#FEF2F2" : "#ECFDF5";
        String tituloEstado = esFallido ? "Pago Fallido — Saldo no descontado" : "Pago Exitoso";

        String fecha = p.getFechaCreacion() != null ? p.getFechaCreacion().format(FECHA_FMT) : "Reciente";
        String producto = escapar(Optional.ofNullable(p.getNombreProducto()).orElse("Servicio GestoPago"));
        String servicio = escapar(Optional.ofNullable(p.getNombreServicio()).orElse("GestoPago"));
        String auth = escapar(Optional.ofNullable(p.getNumAutorizacion()).orElse(esFallido ? "-1" : "1"));
        String referencia = escapar(Optional.ofNullable(p.getReferencia()).orElse(Optional.ofNullable(p.getTelefono()).orElse("—")));
        String upc = escapar(Optional.ofNullable(p.getUpc()).orElse("—"));
        String monto = "$" + Optional.ofNullable(p.getMontoPago()).orElse(BigDecimal.ZERO) + " MXN";
        String saldoPosterior = "$" + Optional.ofNullable(p.getSaldoPosterior()).orElse(BigDecimal.ZERO) + " MXN";
        String leyendaRow = (p.getLeyenda() != null && !p.getLeyenda().isBlank())
                ? "<tr><td style=\"padding:8px 0;color:#64748B;\">Leyenda del Servicio</td><td style=\"padding:8px 0;text-align:right;color:#334155;font-size:12px;\">"
                + escapar(p.getLeyenda().trim())
                + "</td></tr>"
                : "";
        String mensajeProv = esFallido && p.getMensajeRespuesta() != null
                ? "<tr><td style=\"padding:8px 0;color:#64748B;\">Respuesta GestoPago</td><td style=\"padding:8px 0;text-align:right;color:#DC2626;font-weight:600;\">"
                + escapar(p.getCodigoRespuesta() != null ? "[" + p.getCodigoRespuesta() + "] " : "")
                + escapar(p.getMensajeRespuesta().length() > 140 ? p.getMensajeRespuesta().substring(0, 140) : p.getMensajeRespuesta())
                + "</td></tr>"
                : "";

        return """
                <!doctype html>
                <html lang="es">
                <body style="margin:0;padding:24px;background:#F1F5F9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#0F172A;">
                  <div style="max-width:460px;margin:0 auto;background:#FFFFFF;border-radius:16px;border:1px solid #E2E8F0;overflow:hidden;">
                    <div style="background:#0A2540;padding:22px 24px;text-align:center;color:#FFFFFF;">
                      <div style="font-size:20px;font-weight:800;letter-spacing:-0.3px;">GestoPago</div>
                      <div style="font-size:12px;color:#CBD5E1;margin-top:4px;">Comprobante Oficial de Operación</div>
                    </div>
                    <div style="padding:24px;">
                      <div style="background:%s;color:%s;padding:12px 14px;border-radius:12px;text-align:center;font-weight:800;font-size:14px;margin-bottom:18px;">
                        %s
                      </div>
                      <table style="width:100%%;border-collapse:collapse;font-size:13px;">
                        <tr><td style="padding:8px 0;color:#64748B;">Fecha</td><td style="padding:8px 0;text-align:right;font-weight:600;">%s</td></tr>
                        <tr><td style="padding:8px 0;color:#64748B;">Servicio</td><td style="padding:8px 0;text-align:right;font-weight:600;">%s</td></tr>
                        <tr><td style="padding:8px 0;color:#64748B;">Producto</td><td style="padding:8px 0;text-align:right;font-weight:700;">%s</td></tr>
                        <tr><td style="padding:8px 0;color:#64748B;">Estatus</td><td style="padding:8px 0;text-align:right;font-weight:700;color:%s;">%s</td></tr>
                        <tr><td style="padding:8px 0;color:#64748B;">Autorización GestoPago</td><td style="padding:8px 0;text-align:right;font-family:monospace;font-weight:800;">%s</td></tr>
                        <tr><td style="padding:8px 0;color:#64748B;">Referencia / Teléfono</td><td style="padding:8px 0;text-align:right;font-family:monospace;">%s</td></tr>
                        <tr><td style="padding:8px 0;color:#64748B;">Folio UPC</td><td style="padding:8px 0;text-align:right;font-family:monospace;font-size:12px;">%s</td></tr>
                        %s
                        %s
                        <tr style="border-top:1px dashed #CBD5E1;"><td style="padding:12px 0 6px;color:#64748B;font-weight:700;">Importe</td><td style="padding:12px 0 6px;text-align:right;font-weight:900;font-size:15px;">%s</td></tr>
                        <tr><td style="padding:6px 0;color:#64748B;">%s</td><td style="padding:6px 0;text-align:right;font-weight:700;color:%s;">%s</td></tr>
                      </table>
                      <div style="margin-top:20px;padding-top:14px;border-top:1px solid #E2E8F0;text-align:center;font-size:11px;color:#94A3B8;">
                        Este comprobante fue generado automáticamente por GestoPago Digital Commerce.
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                bgEstado, colorEstado, tituloEstado,
                fecha, servicio, producto,
                colorEstado, escapar(estatus),
                auth, referencia, upc, leyendaRow, mensajeProv,
                monto,
                esFallido ? "Saldo Actual (Sin cambios)" : "Nuevo Saldo Disponible",
                esFallido ? "#334155" : "#059669",
                saldoPosterior
        );
    }

    private String escapar(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
