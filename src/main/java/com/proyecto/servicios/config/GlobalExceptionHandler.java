package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.onboarding.*;
import com.proyecto.servicios.exception.pagos.*;
import com.proyecto.servicios.model.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * En caso de que alguna validación Bean Validation (@Valid) no se cumpla para algún DTO,
     * se dispara MethodArgumentNotValidException. Este método procesa los errores de cada campo
     * devolviendo una respuesta uniforme con código y descripción detallada.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> dtoValidation(MethodArgumentNotValidException ex) {
        GenericResponse genericResponse = new GenericResponse();
        Map<String, String> listError = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            listError.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        genericResponse.setCodigo(1);
        genericResponse.setMensaje("Datos no validos: " + listError);
        genericResponse.setDetalles(listError);
        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
            UsuarioNoEncontradoException.class,
            ClienteNoEncontradoException.class,
            CuentaNoEncontradaException.class,
            ProductoNoValidoException.class
    })
    public ResponseEntity<GenericResponse> handleNotFoundResourceException(RuntimeException ex) {
        log.warn("Recurso no encontrado o no válido: {}", ex.getMessage());
        GenericResponse response = new GenericResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler({
            CorreoDuplicadoException.class,
            CurpDuplicadaException.class,
            RfcDuplicadoException.class,
            TransaccionDuplicadaException.class
    })
    public ResponseEntity<GenericResponse> handleDuplicateResourceException(RuntimeException ex) {
        log.warn("Conflicto de unicidad o duplicidad: {}", ex.getMessage());
        GenericResponse response = new GenericResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<GenericResponse> handleInactiveUser(UsuarioInactivoException ex) {
        log.warn("Acceso restringido por estatus inactivo: {}", ex.getMessage());
        GenericResponse response = new GenericResponse(HttpStatus.FORBIDDEN.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<GenericResponse> handleInvalidCredentials(CredencialesInvalidasException ex) {
        log.warn("Fallo de autenticación: {}", ex.getMessage());
        GenericResponse response = new GenericResponse(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler({
            ModificacionNoPermitidaException.class,
            ValidacionNegocioException.class,
            ContrasenaInvalidaException.class,
            MontoInvalidoException.class
    })
    public ResponseEntity<GenericResponse> handleBadRequestBusinessExceptions(RuntimeException ex) {
        log.warn("Regla de negocio no cumplida: {}", ex.getMessage());
        GenericResponse response = new GenericResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({
            SaldoInsuficienteException.class,
            PagoRechazadoProveedorException.class
    })
    public ResponseEntity<GenericResponse> handleUnprocessableOperationExceptions(RuntimeException ex) {
        log.warn("Operación financiera rechazada: {}", ex.getMessage());
        GenericResponse response = new GenericResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(ProveedorExternoTimeoutException.class)
    public ResponseEntity<GenericResponse> handleProviderTimeoutException(ProveedorExternoTimeoutException ex) {
        log.error("Timeout en proveedor externo para UPC {}: {}", ex.getUpc(), ex.getMessage());
        Map<String, String> detalles = Map.of(
                "upc", ex.getUpc(),
                "estatus", "PENDIENTE_CONCILIACION",
                "accionRecomendada", "Ejecute el endpoint de conciliación (/api/v1/pagos-servicios/conciliar) después de 62 segundos para verificar si el cargo fue aplicado por el proveedor."
        );
        GenericResponse response = new GenericResponse(
                HttpStatus.GATEWAY_TIMEOUT.value(),
                ex.getMessage(),
                detalles
        );
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GenericResponse> handleNoResourceFound(NoResourceFoundException ex) {
        GenericResponse response = new GenericResponse(
                HttpStatus.NOT_FOUND.value(),
                "Recurso no encontrado: " + ex.getResourcePath()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Manejo de excepciones de acceso a datos (JPA / SQL / Hibernate).
     * Se protege al cliente no devolviendo trazas técnicas, consultas SQL ni detalles internos.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<GenericResponse> handleDatabaseException(DataAccessException ex) {
        log.error("Excepción de persistencia capturada en servidor: {}", ex.getMessage());
        GenericResponse response = new GenericResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "No fue posible completar la operación en la base de datos. Intente más tarde."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Manejo centralizado de excepciones no controladas.
     * Nunca devuelve trazas técnicas ni volcados de pila al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGeneralException(Exception ex) {
        log.error("Error no controlado capturado en el servidor: {}", ex.getMessage(), ex);
        GenericResponse response = new GenericResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ocurrió un error inesperado al procesar la solicitud. Contacte a soporte si el problema persiste."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
