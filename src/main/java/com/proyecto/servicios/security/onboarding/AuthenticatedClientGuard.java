package com.proyecto.servicios.security.onboarding;

import com.proyecto.servicios.exception.onboarding.UsuarioInactivoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Guardia centralizado de propiedad (Anti-IDOR) para asegurar que un usuario autenticado
 * únicamente pueda consultar o modificar recursos pertenecientes a su propio clienteId.
 */
public final class AuthenticatedClientGuard {

    public static final String ATTR_CLIENTE_ID = "AUTH_CLIENTE_ID";

    private AuthenticatedClientGuard() {
    }

    public static Long obtenerClienteAutenticadoOrNull() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest req = servletAttrs.getRequest();
            Object val = req.getAttribute(ATTR_CLIENTE_ID);
            if (val instanceof Long id) {
                return id;
            }
            if (val instanceof Number num) {
                return num.longValue();
            }
        }
        return null;
    }

    public static void verificarPropiedadCliente(Long clienteIdSolicitado) {
        Long authClienteId = obtenerClienteAutenticadoOrNull();
        if (authClienteId != null && (clienteIdSolicitado == null || !authClienteId.equals(clienteIdSolicitado))) {
            throw new UsuarioInactivoException("Credenciales inválidas o acceso no autorizado.");
        }
    }
}
