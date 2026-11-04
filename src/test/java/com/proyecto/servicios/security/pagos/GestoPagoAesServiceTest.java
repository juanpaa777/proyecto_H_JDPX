package com.proyecto.servicios.security.pagos;

import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GestoPagoAesServiceTest {

    private GestoPagoAesService aesService;

    @BeforeEach
    void setUp() {
        aesService = new GestoPagoAesService(
                "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=",
                "p5ldmBPdd/9pjC0bDC/nSg=="
        );
    }

    @Test
    @DisplayName("Debe cifrar y descifrar correctamente un payload JSON con AES-256-CBC oficial de GestoPago")
    void debeCifrarYDescifrarPayloadJson() {
        String jsonOriginal = "{\"idProducto\":205,\"idServicio\":76,\"telefono\":\"1111111111\",\"horaLocal\":\"20261030164500\",\"referencia\":\"25720381\",\"montoPago\":366.99,\"upc\":\"GP83_TEST01\",\"unidad\":\"083\"}";

        String cifradoBase64 = aesService.cifrarPayload(jsonOriginal);
        assertNotNull(cifradoBase64);
        assertFalse(cifradoBase64.isBlank());
        assertNotEquals(jsonOriginal, cifradoBase64);

        String descifrado = aesService.descifrarPayload(cifradoBase64);
        assertEquals(jsonOriginal, descifrado);
    }

    @Test
    @DisplayName("Debe lanzar ValidacionNegocioException si el payload a cifrar está vacío o es nulo")
    void debeRechazarPayloadVacio() {
        assertThrows(ValidacionNegocioException.class, () -> aesService.cifrarPayload("   "));
        assertThrows(ValidacionNegocioException.class, () -> aesService.cifrarPayload(null));
    }
}

