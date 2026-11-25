package com.proyecto.servicios.security.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.onboarding.SesionActiva;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.repositorys.onboarding.SesionActivaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtSessionFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SesionActivaRepository sesionActivaRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/auth/login")
                || path.startsWith("/auth/verify-2fa")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String rawToken = authHeader.substring(7).trim();
            if (!rawToken.isEmpty() && !rawToken.startsWith("Bearer-")) {
                try {
                    String jti = jwtService.extractJti(rawToken);
                    if (jti != null) {
                        Optional<SesionActiva> sesionOpt = sesionActivaRepository.findByTokenJti(jti);
                        if (sesionOpt.isPresent()) {
                            SesionActiva sesion = sesionOpt.get();
                            if (!Boolean.TRUE.equals(sesion.getActiva())
                                    || sesion.getFechaExpiracion().isBefore(LocalDateTime.now())) {
                                writeRevokedResponse(response, "Tu sesión fue cerrada porque se inició sesión en otro dispositivo o ha expirado.");
                                return;
                            }
                            sesion.setFechaUltimoAcceso(LocalDateTime.now());
                            sesionActivaRepository.save(sesion);
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Token JWT inválido o expirado en filtro de sesión: {}", ex.getMessage());
                    writeRevokedResponse(response, "Token de sesión inválido o expirado. Inicie sesión nuevamente.");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeRevokedResponse(HttpServletResponse response, String mensaje) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "*");

        GenericResponse body = new GenericResponse(
                HttpStatus.UNAUTHORIZED.value(),
                mensaje,
                Map.of("codigoError", "SESSION_REVOKED_BY_OTHER_DEVICE")
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

