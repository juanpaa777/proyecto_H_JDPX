package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.dto.onboarding.UsuarioCreateRequest;
import com.proyecto.servicios.dto.onboarding.UsuarioResponse;
import com.proyecto.servicios.service.onboarding.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Endpoints para consulta, filtrado y creación de usuarios de acceso al sistema")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/filtro")
    @Operation(summary = "Filtrar usuarios", description = "Permite filtrar usuarios del sistema por ID de cliente, correo o estatus activo")
    public ResponseEntity<List<UsuarioResponse>> filtrarUsuarios(
            @Parameter(description = "ID del cliente")
            @RequestParam(required = false) Long clienteId,

            @Parameter(description = "Correo del usuario")
            @RequestParam(required = false) String correo,

            @Parameter(description = "Estatus activo (true/false)")
            @RequestParam(required = false) Boolean activo
    ) {
        return ResponseEntity.ok(usuarioService.filtrarUsuarios(clienteId, correo, activo));
    }

    @PutMapping("/agregar")
    @Operation(summary = "Agregar usuario de acceso a un cliente", description = "Registra un usuario con contraseña cifrada en BCrypt para un cliente activo existente")
    public ResponseEntity<UsuarioResponse> agregarUsuario(@Valid @RequestBody UsuarioCreateRequest request) {
        return ResponseEntity.ok(usuarioService.agregarUsuario(request));
    }
}
