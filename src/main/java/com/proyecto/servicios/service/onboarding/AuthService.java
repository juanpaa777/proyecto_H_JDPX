package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.dto.onboarding.LoginRequest;
import com.proyecto.servicios.dto.onboarding.LoginResponse;
import com.proyecto.servicios.dto.onboarding.SessionStatusResponse;
import com.proyecto.servicios.dto.onboarding.Verify2faRequest;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Codigo2fa;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.SesionActiva;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.onboarding.CredencialesInvalidasException;
import com.proyecto.servicios.exception.onboarding.SesionConflictoException;
import com.proyecto.servicios.exception.onboarding.SesionInvalidaException;
import com.proyecto.servicios.exception.onboarding.UsuarioInactivoException;
import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import com.proyecto.servicios.repositorys.onboarding.Codigo2faRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.SesionActivaRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.security.onboarding.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_INTENTOS_2FA = 3;
    private static final int MAX_SOLICITUDES_2FA_POR_VENTANA = 5;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SesionActivaRepository sesionActivaRepository;
    private final Codigo2faRepository codigo2faRepository;
    private final CuentaRepository cuentaRepository;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this(usuarioRepository, passwordEncoder, null, null, null, null);
    }

    @Autowired
    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            SesionActivaRepository sesionActivaRepository,
            Codigo2faRepository codigo2faRepository,
            CuentaRepository cuentaRepository,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.sesionActivaRepository = sesionActivaRepository;
        this.codigo2faRepository = codigo2faRepository;
        this.cuentaRepository = cuentaRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse autenticarUsuario(LoginRequest request) {
        String correo = request.getCorreo().trim().toLowerCase();
        log.info("Intento de inicio de sesión para el correo: {}", correo);

        // 1. Validar existencia del usuario
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new CredencialesInvalidasException("Credenciales incorrectas. Verifique su correo o contraseña."));

        // 2. Denegar acceso a usuarios inactivos
        Optional.of(usuario)
                .filter(u -> !Boolean.TRUE.equals(u.getActivo()))
                .ifPresent(u -> {
                    throw new UsuarioInactivoException("El usuario se encuentra inactivo. Comuníquese con la institución financiera.");
                });

        // 3. Validar cliente asociado activo
        Optional.ofNullable(usuario.getCliente())
                .filter(c -> !Boolean.TRUE.equals(c.getActivo()))
                .ifPresent(c -> {
                    throw new UsuarioInactivoException("La cuenta del cliente se encuentra inactiva.");
                });

        // 4. Validar credenciales con BCrypt
        Optional.of(usuario)
                .filter(u -> !passwordEncoder.matches(request.getPassword(), u.getPassword()))
                .ifPresent(u -> {
                    log.warn("Fallo de autenticación: contraseña incorrecta para {}", correo);
                    throw new CredencialesInvalidasException("Credenciales incorrectas. Verifique su correo o contraseña.");
                });

        // 5. Verificar exclusividad de sesión activa en el servidor
        boolean forzarCierre = Boolean.TRUE.equals(request.getForzarCierreSesionPrevia());
        verificarConflictoSesionActiva(usuario.getId(), forzarCierre);

        String plataforma = Optional.ofNullable(request.getPlataforma())
                .filter(p -> !p.isBlank())
                .map(String::toUpperCase)
                .orElse("WEB");

        String dispositivo = Optional.ofNullable(request.getDispositivo())
                .filter(d -> !d.isBlank())
                .orElse("Cliente " + plataforma);

        // 6. Si el cliente solicita flujo 2FA (Módulo 2) y el repositorio está disponible
        if (Boolean.TRUE.equals(request.getRequerir2fa()) && codigo2faRepository != null) {
            return generarDesafio2fa(usuario, plataforma, dispositivo, forzarCierre);
        }

        // 7. Emisión directa de sesión y token (para compatibilidad o tras validación)
        return emitirSesionYToken(usuario, plataforma, dispositivo);
    }

    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public LoginResponse verificarCodigo2fa(Verify2faRequest request) {
        if (codigo2faRepository == null) {
            throw new ValidacionNegocioException("El servicio de validación 2FA no está disponible.");
        }

        Codigo2fa desafio = codigo2faRepository.findByDesafioId(request.getDesafioId().trim())
                .orElseThrow(() -> new CredencialesInvalidasException("El identificador de desafío 2FA no es válido."));

        if (Boolean.TRUE.equals(desafio.getUsado())) {
            throw new CredencialesInvalidasException("Este código 2FA ya fue utilizado anteriormente y no puede reutilizarse.");
        }

        if (desafio.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new CredencialesInvalidasException("El código 2FA ha expirado. Por favor inicie sesión nuevamente.");
        }

        if (desafio.getIntentosFallidos() >= MAX_INTENTOS_2FA) {
            throw new CredencialesInvalidasException("Ha superado el máximo de 3 intentos fallidos. El código fue bloqueado.");
        }

        if (!passwordEncoder.matches(request.getCodigoOtp().trim(), desafio.getCodigoHash())) {
            int nuevosIntentos = desafio.getIntentosFallidos() + 1;
            desafio.setIntentosFallidos(nuevosIntentos);
            codigo2faRepository.save(desafio);
            int restantes = Math.max(0, MAX_INTENTOS_2FA - nuevosIntentos);
            throw new CredencialesInvalidasException(
                    "Código de verificación 2FA incorrecto. Intentos restantes: " + restantes
            );
        }

        Usuario usuario = usuarioRepository.findById(desafio.getUsuarioId())
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario asociado al desafío no encontrado."));

        boolean forzar = Boolean.TRUE.equals(desafio.getForzarCierrePrevia())
                || Boolean.TRUE.equals(request.getForzarCierreSesionPrevia());
        verificarConflictoSesionActiva(usuario.getId(), forzar);

        desafio.setUsado(true);
        codigo2faRepository.save(desafio);

        return emitirSesionYToken(usuario, desafio.getPlataforma(), desafio.getDispositivoInfo());
    }

    @Transactional
    public void cerrarSesion(String authHeader) {
        String jti = extraerJtiDeHeader(authHeader);
        if (jti != null && sesionActivaRepository != null) {
            sesionActivaRepository.findByTokenJti(jti).ifPresent(sesion -> {
                sesion.setActiva(false);
                sesion.setMotivoCierre("LOGOUT_EXPLICITO_USUARIO");
                sesion.setFechaUltimoAcceso(LocalDateTime.now());
                sesionActivaRepository.save(sesion);
                log.info("Sesión {} cerrada exitosamente", jti);
            });
        }
    }

    @Transactional(readOnly = true)
    public SessionStatusResponse consultarEstadoSesion(String authHeader) {
        String jti = extraerJtiDeHeader(authHeader);
        if (jti == null || sesionActivaRepository == null) {
            throw new SesionInvalidaException("No se proporcionó un token de sesión válido.", "TOKEN_MISSING");
        }

        SesionActiva sesion = sesionActivaRepository.findByTokenJti(jti)
                .orElseThrow(() -> new SesionInvalidaException("La sesión no existe o es inválida.", "SESSION_NOT_FOUND"));

        if (!Boolean.TRUE.equals(sesion.getActiva()) || sesion.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new SesionInvalidaException(
                    "Tu sesión fue cerrada porque iniciaste sesión en otro dispositivo.",
                    "SESSION_REVOKED_BY_OTHER_DEVICE"
            );
        }

        Usuario usuario = usuarioRepository.findById(sesion.getUsuarioId())
                .orElseThrow(() -> new SesionInvalidaException("Usuario no encontrado.", "USER_NOT_FOUND"));

        String nombreCompleto = Optional.ofNullable(usuario.getCliente())
                .map(c -> c.getNombre() + " " + c.getApellidoPaterno())
                .orElse(usuario.getCorreo());

        Optional<Cuenta> cuentaOpt = obtenerCuentaPrincipal(sesion.getClienteId());

        return SessionStatusResponse.builder()
                .activa(true)
                .sessionId(sesion.getTokenJti())
                .clienteId(sesion.getClienteId())
                .correo(usuario.getCorreo())
                .nombreCompleto(nombreCompleto)
                .numeroCuenta(cuentaOpt.map(Cuenta::getNumeroCuenta).orElse(null))
                .saldo(cuentaOpt.map(Cuenta::getSaldo).orElse(BigDecimal.ZERO))
                .plataforma(sesion.getPlataforma())
                .dispositivo(sesion.getDispositivoInfo())
                .fechaInicio(sesion.getFechaInicio().format(DATE_FMT))
                .mensaje("Sesión activa y verificada en el servidor")
                .build();
    }

    private void verificarConflictoSesionActiva(Long usuarioId, boolean forzarCierre) {
        if (sesionActivaRepository == null || usuarioId == null) {
            return;
        }
        List<SesionActiva> sesiones = sesionActivaRepository.findByUsuarioIdAndActivaTrue(usuarioId);
        LocalDateTime ahora = LocalDateTime.now();

        for (SesionActiva s : sesiones) {
            if (s.getFechaExpiracion().isBefore(ahora)) {
                s.setActiva(false);
                s.setMotivoCierre("EXPIRADA_AUTOMATICAMENTE");
                sesionActivaRepository.save(s);
            } else if (!forzarCierre) {
                throw new SesionConflictoException(
                        "Ya existe una sesión activa en " + s.getPlataforma() + " (" + s.getDispositivoInfo() + "). ¿Desea finalizar la sesión anterior para continuar?",
                        s.getPlataforma(),
                        s.getDispositivoInfo(),
                        s.getFechaInicio().format(DATE_FMT)
                );
            } else {
                // El usuario confirmó forzar el cierre: revocamos la sesión previa de inmediato
                s.setActiva(false);
                s.setMotivoCierre("FORZADA_POR_NUEVO_INICIO_SESION");
                s.setFechaUltimoAcceso(ahora);
                sesionActivaRepository.save(s);
            }
        }
    }

    private LoginResponse generarDesafio2fa(Usuario usuario, String plataforma, String dispositivo, boolean forzarCierre) {
        LocalDateTime hace5Min = LocalDateTime.now().minusMinutes(5);
        long solicitudesRecientes = codigo2faRepository.countByUsuarioIdAndFechaCreacionAfter(usuario.getId(), hace5Min);
        if (solicitudesRecientes >= MAX_SOLICITUDES_2FA_POR_VENTANA) {
            throw new ValidacionNegocioException("Demasiadas solicitudes de código 2FA. Por seguridad, espere 5 minutos antes de intentar nuevamente.");
        }

        String codigoOtp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        String desafioId = UUID.randomUUID().toString();

        Codigo2fa nuevoCodigo = Codigo2fa.builder()
                .usuarioId(usuario.getId())
                .desafioId(desafioId)
                .codigoHash(passwordEncoder.encode(codigoOtp))
                .plataforma(plataforma)
                .dispositivoInfo(dispositivo)
                .intentosFallidos(0)
                .usado(false)
                .forzarCierrePrevia(forzarCierre)
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .build();

        codigo2faRepository.save(nuevoCodigo);

        String nombreCompleto = Optional.ofNullable(usuario.getCliente())
                .map(c -> c.getNombre() + " " + c.getApellidoPaterno())
                .orElse(usuario.getCorreo());

        Long clienteId = Optional.ofNullable(usuario.getCliente())
                .map(Cliente::getId)
                .orElse(null);

        log.info("Desafío 2FA generado para {} [DesafioId: {}, OTP: {}]", usuario.getCorreo(), desafioId, codigoOtp);

        return LoginResponse.builder()
                .requiere2fa(true)
                .desafioId(desafioId)
                .codigoOtpDemo(codigoOtp)
                .expira2faSegundos(300L)
                .clienteId(clienteId)
                .correo(usuario.getCorreo())
                .nombreCompleto(nombreCompleto)
                .mensaje("Se ha enviado un código de verificación de 6 dígitos (2FA). Valídelo para activar su sesión.")
                .build();
    }

    private LoginResponse emitirSesionYToken(Usuario usuario, String plataforma, String dispositivo) {
        String nombreCompleto = Optional.ofNullable(usuario.getCliente())
                .map(c -> c.getNombre() + " " + c.getApellidoPaterno())
                .orElse(usuario.getCorreo());

        Long clienteId = Optional.ofNullable(usuario.getCliente())
                .map(Cliente::getId)
                .orElse(null);

        // Revocar sesiones anteriores si existen
        if (sesionActivaRepository != null && usuario.getId() != null) {
            List<SesionActiva> anteriores = sesionActivaRepository.findByUsuarioIdAndActivaTrue(usuario.getId());
            for (SesionActiva anterior : anteriores) {
                anterior.setActiva(false);
                anterior.setMotivoCierre("REEMPLAZADA_POR_SESION_EN_" + plataforma);
                anterior.setFechaUltimoAcceso(LocalDateTime.now());
                sesionActivaRepository.save(anterior);
            }
        }

        String tokenJti = UUID.randomUUID().toString();
        String token;
        if (jwtService != null) {
            token = jwtService.generateToken(clienteId, usuario.getCorreo(), nombreCompleto, tokenJti);
        } else {
            token = "Bearer-" + tokenJti;
        }

        if (sesionActivaRepository != null && usuario.getId() != null && clienteId != null) {
            SesionActiva nuevaSesion = SesionActiva.builder()
                    .usuarioId(usuario.getId())
                    .clienteId(clienteId)
                    .tokenJti(tokenJti)
                    .plataforma(plataforma)
                    .dispositivoInfo(dispositivo)
                    .activa(true)
                    .fechaInicio(LocalDateTime.now())
                    .fechaUltimoAcceso(LocalDateTime.now())
                    .fechaExpiracion(LocalDateTime.now().plusHours(24))
                    .build();
            sesionActivaRepository.save(nuevaSesion);
        }

        Optional<Cuenta> cuentaOpt = obtenerCuentaPrincipal(clienteId);

        log.info("Inicio de sesión completado para {} en plataforma {}", usuario.getCorreo(), plataforma);
        return LoginResponse.builder()
                .token(token)
                .tipo("Bearer")
                .expiresIn(86400L)
                .clienteId(clienteId)
                .correo(usuario.getCorreo())
                .nombreCompleto(nombreCompleto)
                .numeroCuenta(cuentaOpt.map(Cuenta::getNumeroCuenta).orElse(null))
                .saldo(cuentaOpt.map(Cuenta::getSaldo).orElse(BigDecimal.ZERO))
                .sessionId(tokenJti)
                .requiere2fa(false)
                .mensaje("Sesión iniciada exitosamente")
                .build();
    }

    private Optional<Cuenta> obtenerCuentaPrincipal(Long clienteId) {
        if (cuentaRepository == null || clienteId == null) {
            return Optional.empty();
        }
        return cuentaRepository.findByClienteId(clienteId, PageRequest.of(0, 5))
                .getContent()
                .stream()
                .filter(c -> "ACTIVA".equalsIgnoreCase(c.getEstatus()))
                .findFirst();
    }

    private String extraerJtiDeHeader(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        String raw = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader.trim();
        if (raw.startsWith("Bearer-")) {
            return raw.substring(7);
        }
        if (jwtService != null) {
            try {
                return jwtService.extractJti(raw);
            } catch (Exception e) {
                return raw;
            }
        }
        return raw;
    }
}
