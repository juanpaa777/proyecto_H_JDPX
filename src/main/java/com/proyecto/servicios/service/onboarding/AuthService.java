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

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String correo = request.getCorreo().trim().toLowerCase();
        log.info("Intento de inicio de sesión para el correo: {}", correo);

        // 1. Validar que el usuario exista
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new CredencialesInvalidasException("Credenciales incorrectas. Verifique su correo o contraseña."));

        // 2. Denegar el acceso a usuarios inactivos
        if (!usuario.getActivo()) {
            throw new UsuarioInactivoException("El usuario se encuentra inactivo. Comuníquese con la institución financiera.");
        }

        // 3. Validar que el cliente asociado esté activo
        Cliente cliente = usuario.getCliente();
        if (cliente != null && !cliente.getActivo()) {
            throw new UsuarioInactivoException("La cuenta del cliente se encuentra inactiva.");
        }

        // 4. Validar que las credenciales coincidan con BCrypt
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            log.warn("Fallo de autenticación: contraseña incorrecta para {}", correo);
            throw new CredencialesInvalidasException("Credenciales incorrectas. Verifique su correo o contraseña.");
        }

        // 5. Token temporal listo para cabeceras de autorización (Bearer Token)
        String nombreCompleto = cliente != null ? (cliente.getNombre() + " " + cliente.getApellidoPaterno()) : correo;
        Long clienteId = cliente != null ? cliente.getId() : null;
        String token = "Bearer-" + UUID.randomUUID().toString();

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
