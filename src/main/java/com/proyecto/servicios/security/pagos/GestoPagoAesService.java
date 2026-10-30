package com.proyecto.servicios.security.pagos;

import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Servicio criptográfico para firmar/cifrar el payload de transacciones hacia GestoPago
 * utilizando AES-256-CBC (AES/CBC/PKCS5Padding) conforme a la especificación oficial.
 * Las llaves se inyectan desde configuración externa y jamás se exponen en logs.
 */
@Service
@Slf4j
public class GestoPagoAesService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private final String secretKeyBase64;
    private final String ivBase64;

    public GestoPagoAesService(
            @Value("${gestopago.tx.aes-secret-key:HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=}") String secretKeyBase64,
            @Value("${gestopago.tx.aes-iv:p5ldmBPdd/9pjC0bDC/nSg==}") String ivBase64
    ) {
        this.secretKeyBase64 = secretKeyBase64;
        this.ivBase64 = ivBase64;
    }

    public String cifrarPayload(String jsonPayload) {
        String payloadValido = Optional.ofNullable(jsonPayload)
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .orElseThrow(() -> new ValidacionNegocioException("El payload de la transacción a cifrar no puede estar vacío"));

        try {
            byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64.trim());
            byte[] ivBytes = Base64.getDecoder().decode(ivBase64.trim());

            SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encryptedBytes = cipher.doFinal(payloadValido.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Error interno al cifrar el payload de transacción con AES-256");
            throw new IllegalStateException("No fue posible firmar criptográficamente la solicitud de pago");
        }
    }

    public String descifrarPayload(String encryptedBase64) {
        String cifradoValido = Optional.ofNullable(encryptedBase64)
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .orElseThrow(() -> new ValidacionNegocioException("El contenido cifrado no puede estar vacío"));

        try {
            byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64.trim());
            byte[] ivBytes = Base64.getDecoder().decode(ivBase64.trim());

            SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decodedBytes = Base64.getDecoder().decode(cifradoValido);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error interno al descifrar el payload con AES-256");
            throw new IllegalStateException("No fue posible descifrar el payload de la transacción");
        }
    }
}

