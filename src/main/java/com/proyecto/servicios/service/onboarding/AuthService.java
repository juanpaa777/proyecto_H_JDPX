package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.dto.onboarding.LoginRequest;
import com.proyecto.servicios.dto.onboarding.LoginResponse;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.onboarding.CredencialesInvalidasException;
import com.proyecto.servicios.exception.onboarding.UsuarioInactivoException;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public LoginResponse autenticarUsuario(LoginRequest request) {
        String correo = request.getCorreo().trim().toLowerCase();
        log.info("Intento de inicio de sesión para el correo: {}", correo);

        // 1. Validar existencia del usuario
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new CredencialesInvalidasException("Credenciales incorrectas. Verifique su correo o contraseña."));

        // 2. Denegar acceso a usuarios inactivos sin IF
        Optional.of(usuario)
                .filter(u -> !Boolean.TRUE.equals(u.getActivo()))
                .ifPresent(u -> { throw new UsuarioInactivoException("El usuario se encuentra inactivo. Comuníquese con la institución financiera."); });

        // 3. Validar cliente asociado activo sin IF
        Optional.ofNullable(usuario.getCliente())
                .filter(c -> !Boolean.TRUE.equals(c.getActivo()))
                .ifPresent(c -> { throw new UsuarioInactivoException("La cuenta del cliente se encuentra inactiva."); });

        // 4. Validar credenciales con BCrypt sin IF
        Optional.of(usuario)
                .filter(u -> !passwordEncoder.matches(request.getPassword(), u.getPassword()))
                .ifPresent(u -> {
                    log.warn("Fallo de autenticación: contraseña incorrecta para {}", correo);
                    throw new CredencialesInvalidasException("Credenciales incorrectas. Verifique su correo o contraseña.");
                });

        // 5. Generar token y respuesta
        String nombreCompleto = Optional.ofNullable(usuario.getCliente())
                .map(c -> c.getNombre() + " " + c.getApellidoPaterno())
                .orElse(correo);

        Long clienteId = Optional.ofNullable(usuario.getCliente())
                .map(Cliente::getId)
                .orElse(null);

        String token = "Bearer-" + UUID.randomUUID();

        log.info("Inicio de sesión exitoso para: {}", correo);
        return new LoginResponse(
                token,
                86400L,
                clienteId,
                usuario.getCorreo(),
                nombreCompleto
        );
    }
}
