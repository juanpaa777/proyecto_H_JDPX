package com.proyecto.servicios.service.onboarding;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Optional;

@Service
public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int SECRET_BYTES = 20;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generarSecretoBase32() {
        byte[] bytes = new byte[SECRET_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    public String generarOtpAuthUri(String correo, String secretBase32) {
        String label = URLEncoder.encode("GestoPago:" + correo, StandardCharsets.UTF_8)
                .replace("+", "%20");
        String issuer = URLEncoder.encode("GestoPago", StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "otpauth://totp/" + label
                + "?secret=" + secretBase32
                + "&issuer=" + issuer
                + "&algorithm=SHA1&digits=6&period=30";
    }

    public Optional<Long> verificarCodigo(String secretBase32, String codigoIngresado, Long ultimoPasoUsado) {
        if (secretBase32 == null || secretBase32.isBlank() || codigoIngresado == null) {
            return Optional.empty();
        }
        String cleanCode = codigoIngresado.trim();
        if (!cleanCode.matches("^[0-9]{6}$")) {
            return Optional.empty();
        }

        byte[] key = decodeBase32(secretBase32);
        long currentStep = (System.currentTimeMillis() / 1000L) / TIME_STEP_SECONDS;

        // Ventana de tolerancia +-1 paso (30s) por desfase de reloj
        for (long offset = -1; offset <= 1; offset++) {
            long step = currentStep + offset;
            if (ultimoPasoUsado != null && step <= ultimoPasoUsado) {
                continue; // Protección contra reutilización del código en la misma ventana
            }
            String expected = generarCodigoParaPaso(key, step);
            if (constantTimeEquals(expected, cleanCode)) {
                return Optional.of(step);
            }
        }
        return Optional.empty();
    }

    public String generarCodigoActual(String secretBase32) {
        byte[] key = decodeBase32(secretBase32);
        long currentStep = (System.currentTimeMillis() / 1000L) / TIME_STEP_SECONDS;
        return generarCodigoParaPaso(key, currentStep);
    }

    private String generarCodigoParaPaso(byte[] key, long step) {
        try {
            byte[] data = ByteBuffer.allocate(8).putLong(step).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Error al calcular código TOTP", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private String encodeBase32(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                sb.append(BASE32_ALPHABET.charAt(index));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            sb.append(BASE32_ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    private byte[] decodeBase32(String input) {
        String clean = input.trim().toUpperCase().replace("=", "");
        byte[] out = new byte[(clean.length() * 5) / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;
        for (int i = 0; i < clean.length(); i++) {
            int val = BASE32_ALPHABET.indexOf(clean.charAt(i));
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out[count++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return out;
    }
}
