package com.proyecto.servicios.service.onboarding;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class Email2faService {

    private final JavaMailSender mailSender;
    private final String remitente;

    public Email2faService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String remitente
    ) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    public void enviarCodigo2fa(
            String destinatario,
            String nombreCompleto,
            String codigoOtp,
            String plataforma,
            String dispositivo
    ) {
        if (mailSender == null || remitente == null || remitente.isBlank()) {
            log.warn("Envío de correo 2FA omitido: MAIL_USERNAME / JavaMailSender no configurado.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(remitente, "GestoPago Banca Digital");
            helper.setTo(destinatario);
            helper.setSubject("Tu código de verificación GestoPago (2FA)");
            helper.setText(construirHtml2fa(nombreCompleto, codigoOtp, plataforma, dispositivo), true);

            mailSender.send(message);
            log.info("Correo de verificación 2FA enviado exitosamente.");
        } catch (Exception e) {
            log.error("Error al enviar el correo 2FA: {}", e.getMessage());
            throw new IllegalStateException("No pudimos enviar el código de verificación a tu correo. Intenta nuevamente en unos segundos.");
        }
    }

    private String construirHtml2fa(
            String nombreCompleto,
            String codigoOtp,
            String plataforma,
            String dispositivo
    ) {
        String nombreSeguro = escaparHtml(nombreCompleto != null && !nombreCompleto.isBlank() ? nombreCompleto : "Cliente");
        String platSegura = escaparHtml(plataforma != null ? plataforma : "Plataforma Digital");
        String dispSeguro = escaparHtml(dispositivo != null ? dispositivo : "Dispositivo");
        String otpSeguro = escaparHtml(codigoOtp);

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                </head>
                <body style="margin:0;padding:0;background-color:#F6F9FC;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#0F172A;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#F6F9FC;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:520px;background-color:#FFFFFF;border-radius:20px;overflow:hidden;border:1px solid #E2E8F0;box-shadow:0 10px 30px rgba(10,37,64,0.08);">
                          <!-- Header -->
                          <tr>
                            <td style="background:linear-gradient(135deg,#0A192F 0%%,#1E3A8A 60%%,#0B4CE6 100%%);padding:28px 32px;text-align:left;">
                              <table role="presentation" cellspacing="0" cellpadding="0">
                                <tr>
                                  <td style="background-color:rgba(255,255,255,0.16);border-radius:10px;padding:8px 12px;color:#FFFFFF;font-weight:900;font-size:15px;letter-spacing:1px;">
                                    GP
                                  </td>
                                  <td style="padding-left:12px;">
                                    <div style="color:#FFFFFF;font-size:18px;font-weight:800;line-height:1.2;">GestoPago</div>
                                    <div style="color:#BFDBFE;font-size:11px;font-weight:600;letter-spacing:0.6px;">BANCA DIGITAL SEGURA</div>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <!-- Body -->
                          <tr>
                            <td style="padding:32px;">
                              <h1 style="margin:0 0 10px 0;font-size:20px;font-weight:800;color:#0A2540;">
                                Código de verificación
                              </h1>
                              <p style="margin:0 0 20px 0;font-size:14px;line-height:1.6;color:#475569;">
                                Hola <strong style="color:#0F172A;">%s</strong>, recibimos una solicitud para iniciar sesión en tu cuenta. Usa el siguiente código temporal de un solo uso para confirmar tu identidad:
                              </p>

                              <!-- OTP Box -->
                              <div style="background-color:#F6F9FC;border:2px solid #0A2540;border-radius:16px;padding:22px;text-align:center;margin:0 0 22px 0;">
                                <div style="font-size:11px;font-weight:700;letter-spacing:1.2px;color:#64748B;text-transform:uppercase;margin-bottom:8px;">
                                  Tu código 2FA
                                </div>
                                <div style="font-family:'Courier New',Courier,monospace;font-size:34px;font-weight:900;letter-spacing:8px;color:#0A2540;">
                                  %s
                                </div>
                                <div style="font-size:12px;color:#64748B;margin-top:8px;">
                                  Válido durante <strong>5 minutos</strong> • Máximo 3 intentos
                                </div>
                              </div>

                              <!-- Device Info -->
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#F8FAFC;border:1px solid #E2E8F0;border-radius:12px;margin-bottom:20px;">
                                <tr>
                                  <td style="padding:12px 16px;font-size:12px;color:#475569;">
                                    <div style="margin-bottom:4px;"><strong>Plataforma:</strong> %s</div>
                                    <div><strong>Dispositivo:</strong> %s</div>
                                  </td>
                                </tr>
                              </table>

                              <!-- Security Notice -->
                              <div style="background-color:#FFFBEB;border:1px solid #FDE68A;border-radius:12px;padding:12px 16px;font-size:12px;line-height:1.5;color:#92400E;">
                                <strong>Aviso de seguridad:</strong> Nunca compartas este código con nadie. GestoPago jamás te lo solicitará por teléfono, WhatsApp o redes sociales. Si no reconoces este intento, cambia tu contraseña de inmediato.
                              </div>
                            </td>
                          </tr>
                          <!-- Footer -->
                          <tr>
                            <td style="background-color:#F8FAFC;border-top:1px solid #E2E8F0;padding:18px 32px;text-align:center;font-size:11px;color:#94A3B8;">
                              Este correo fue enviado automáticamente por el sistema de seguridad de GestoPago Digital Commerce.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(nombreSeguro, otpSeguro, platSegura, dispSeguro);
    }

    private String escaparHtml(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
