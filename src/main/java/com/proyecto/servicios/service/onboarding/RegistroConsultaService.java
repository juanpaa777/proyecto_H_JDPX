package com.proyecto.servicios.service.onboarding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.dto.onboarding.CodigoPostalResponse;
import com.proyecto.servicios.dto.onboarding.VerificarDisponibilidadRequest;
import com.proyecto.servicios.dto.onboarding.VerificarDisponibilidadResponse;
import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistroConsultaService {

    private static final int MAX_CONSULTAS_POR_MINUTO = 25;
    private static final long VENTANA_MS = 60_000L;
    private static final String POSTALI_URL = "https://postali.app/api/v1/mx/cp/";

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private final Map<String, Deque<Long>> rateLimitBuckets = new ConcurrentHashMap<>();

    public VerificarDisponibilidadResponse verificarDisponibilidad(
            VerificarDisponibilidadRequest request,
            String clientKey
    ) {
        aplicarRateLimit(clientKey);

        String campo = request.getCampo().trim().toUpperCase();
        String valor = request.getValor().trim();

        boolean existe = switch (campo) {
            case "CORREO" -> {
                String normalized = valor.toLowerCase();
                yield usuarioRepository.existsByCorreo(normalized) || clienteRepository.existsByCorreo(normalized);
            }
            case "CURP" -> clienteRepository.existsByCurp(valor.toUpperCase());
            case "RFC" -> clienteRepository.existsByRfc(valor.toUpperCase());
            default -> throw new ValidacionNegocioException("Campo de verificación no soportado");
        };

        if (existe) {
            String msg = switch (campo) {
                case "CORREO" -> "Este correo electrónico ya está registrado. Puedes iniciar sesión o usar otro correo.";
                case "CURP" -> "Esta CURP ya está registrada en una cuenta existente.";
                case "RFC" -> "Este RFC ya está registrado en una cuenta existente.";
                default -> "El dato ya se encuentra registrado.";
            };
            return VerificarDisponibilidadResponse.builder()
                    .disponible(false)
                    .mensaje(msg)
                    .build();
        }

        return VerificarDisponibilidadResponse.builder()
                .disponible(true)
                .mensaje("Disponible")
                .build();
    }

    public CodigoPostalResponse consultarCodigoPostal(String cp) {
        String cleanCp = cp.trim();
        try {
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(POSTALI_URL + cleanCp))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200 || resp.body() == null || resp.body().isBlank()) {
                return CodigoPostalResponse.builder()
                        .encontrado(false)
                        .codigoPostal(cleanCp)
                        .colonias(List.of())
                        .mensaje("No se encontró el código postal; puedes capturar estado, municipio y colonia manualmente.")
                        .build();
            }

            JsonNode root = objectMapper.readTree(resp.body());
            String estado = root.path("estado").asText("").trim();
            String municipio = root.path("municipio").asText("").trim();
            Set<String> coloniasSet = new LinkedHashSet<>();
            JsonNode asentamientos = root.path("asentamientos");
            if (asentamientos.isArray()) {
                for (JsonNode a : asentamientos) {
                    String nombre = a.path("nombre").asText("").trim();
                    if (!nombre.isEmpty()) {
                        coloniasSet.add(nombre);
                    }
                }
            }

            if (estado.isEmpty() && municipio.isEmpty() && coloniasSet.isEmpty()) {
                return CodigoPostalResponse.builder()
                        .encontrado(false)
                        .codigoPostal(cleanCp)
                        .colonias(List.of())
                        .mensaje("Sin resultados para ese código postal; captura los datos manualmente.")
                        .build();
            }

            return CodigoPostalResponse.builder()
                    .encontrado(true)
                    .codigoPostal(cleanCp)
                    .estado(estado)
                    .municipio(municipio)
                    .colonias(new ArrayList<>(coloniasSet))
                    .mensaje("Código postal encontrado")
                    .build();
        } catch (Exception e) {
            log.warn("No se pudo consultar Postali para CP {}: {}", cleanCp, e.getMessage());
            return CodigoPostalResponse.builder()
                    .encontrado(false)
                    .codigoPostal(cleanCp)
                    .colonias(List.of())
                    .mensaje("No pudimos consultar el código postal en línea. Puedes escribir tu domicilio manualmente.")
                    .build();
        }
    }

    private void aplicarRateLimit(String clientKey) {
        String key = (clientKey == null || clientKey.isBlank()) ? "default" : clientKey;
        long now = System.currentTimeMillis();
        Deque<Long> deque = rateLimitBuckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && (now - deque.peekFirst()) > VENTANA_MS) {
                deque.pollFirst();
            }
            if (deque.size() >= MAX_CONSULTAS_POR_MINUTO) {
                throw new ValidacionNegocioException(
                        "Demasiadas verificaciones en poco tiempo. Espera un minuto antes de intentar nuevamente."
                );
            }
            deque.addLast(now);
        }
    }
}
