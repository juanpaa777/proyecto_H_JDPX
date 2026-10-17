package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.dto.onboarding.*;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.onboarding.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "API REST para la administración de usuarios sin PathVariable ni RequestParam (100% RequestBody)")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/crear")
    @Operation(summary = "Crear nuevo usuario de acceso", description = "Crea un usuario con contraseña cifrada en BCrypt asociado a un cliente activo existente")
    public ResponseEntity<UsuarioResponse> crearUsuario(@Valid @RequestBody UsuarioCreateRequest request) {
        UsuarioResponse response = usuarioService.crearUsuarioAcceso(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/consultar")
    @Operation(summary = "Búsqueda dinámica y paginada de usuarios", description = "Único endpoint para consultar usuarios combinando filtros opcionales (id, clienteId, correo, activo, fechas) y paginación")
    public ResponseEntity<PaginaResponse<UsuarioResponse>> consultarUsuarios(@Valid @RequestBody ConsultaUsuarioRequest request) {
        Page<UsuarioResponse> pagina = usuarioService.buscarUsuariosPaginados(request);
        return ResponseEntity.ok(PaginaResponse.from(pagina));
    }

    @PostMapping("/detalle")
    @Operation(summary = "Consultar detalle de un usuario", description = "Obtiene la información completa de un usuario por su ID enviado en el cuerpo JSON")
    public ResponseEntity<UsuarioResponse> obtenerDetalle(@Valid @RequestBody DetalleUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.obtenerUsuarioDetalle(request));
    }

    @PutMapping("/actualizar")
    @Operation(summary = "Actualizar información de usuario", description = "Permite modificar correo, contraseña y estatus impidiendo la alteración de campos del servidor")
    public ResponseEntity<UsuarioResponse> actualizarUsuario(@Valid @RequestBody UsuarioUpdateRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(request));
    }

    @PutMapping("/eliminar")
    @Operation(summary = "Baja lógica de usuario", description = "Desactiva al usuario sin eliminar el registro físico en base de datos")
    public ResponseEntity<GenericResponse> eliminarUsuario(@Valid @RequestBody EliminaUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.darDeBajaUsuario(request));
    }
}
