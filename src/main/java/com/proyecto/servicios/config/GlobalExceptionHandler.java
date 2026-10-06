package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.onboarding.*;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        GenericResponse response = new GenericResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Error en las validaciones de los campos enviados.",
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({
            CurpDuplicadaException.class,
            RfcDuplicadoException.class,
            CorreoDuplicadoException.class
    })
    public ResponseEntity<GenericResponse> handleDuplicateResourceException(RuntimeException ex) {
        log.warn("Conflicto por duplicidad: {}", ex.getMessage());
        GenericResponse response = new GenericResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({
            ClienteNoEncontradoException.class,
            CuentaNoEncontradaException.class,
            UsuarioNoEncontradoException.class
    })
    public ResponseEntity<GenericResponse> handleNotFoundResourceException(RuntimeException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        GenericResponse response = new GenericResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<GenericResponse> handleInvalidCredentials(CredencialesInvalidasException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        GenericResponse response = new GenericResponse(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<GenericResponse> handleInactiveUser(UsuarioInactivoException ex) {
        log.warn("Usuario inactivo: {}", ex.getMessage());
        GenericResponse response = new GenericResponse(HttpStatus.FORBIDDEN.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler({
            ContrasenaInvalidaException.class,
            ModificacionNoPermitidaException.class,
            ValidacionNegocioException.class
    })
    public ResponseEntity<GenericResponse> handleBadRequestBusinessExceptions(RuntimeException ex) {
        log.warn("Regla de negocio no cumplida: {}", ex.getMessage());
        GenericResponse response = new GenericResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GenericResponse> handleNoResourceFound(NoResourceFoundException ex) {
        GenericResponse response = new GenericResponse(
                HttpStatus.NOT_FOUND.value(),
                "Recurso no encontrado: " + ex.getResourcePath()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<GenericResponse> handleDatabaseException(DataAccessException ex) {
        log.error("Error de acceso a base de datos: {}", ex.getMessage());
        GenericResponse response = new GenericResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "El servicio de base de datos no está disponible en este momento. Verifique la conexión."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGeneralException(Exception ex) {
        log.error("Error no controlado en la aplicación: {}", ex.getMessage(), ex);
        GenericResponse response = new GenericResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ocurrió un error inesperado al procesar la solicitud: " + ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
