/**
 * Google Apps Script — Envío de Tickets HTML para GestoPago
 *
 * Pasos para desplegar:
 * 1. Entra a https://script.google.com y crea un "Nuevo proyecto".
 * 2. Pega este código completo y guarda.
 * 3. Haz clic en "Implementar" -> "Nueva implementación".
 *    - Tipo: "Aplicación web"
 *    - Ejecutar como: "Yo" (tu cuenta de Gmail)
 *    - Quién tiene acceso: "Cualquier usuario"
 * 4. Autoriza los permisos de Gmail y copia la URL generada (https://script.google.com/macros/s/.../exec).
 * 5. En Render, agrega la variable de entorno:
 *    GOOGLE_SCRIPT_TICKET_URL = <la URL copiada>
 */
function doPost(e) {
  try {
    var raw = e && e.postData && e.postData.contents ? e.postData.contents : '{}';
    var data = JSON.parse(raw);

    if (!data.to || !data.subject || !data.htmlBody) {
      return ContentService
        .createTextOutput(JSON.stringify({ ok: false, error: 'Faltan campos: to, subject, htmlBody' }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    MailApp.sendEmail({
      to: String(data.to).trim(),
      subject: String(data.subject),
      htmlBody: String(data.htmlBody),
      name: data.senderName || 'GestoPago Banca Digital'
    });

    return ContentService
      .createTextOutput(JSON.stringify({ ok: true, to: data.to }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService
      .createTextOutput(JSON.stringify({ ok: false, error: String(err) }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}
