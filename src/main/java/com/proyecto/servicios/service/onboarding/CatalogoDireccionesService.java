package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.client.SepomexApiClient;
import com.proyecto.servicios.dto.onboarding.CodigoPostalResponse;
import com.proyecto.servicios.dto.onboarding.sepomex.ZippoPostalResponse;
import com.proyecto.servicios.entity.onboarding.CatalogoSepomex;
import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import com.proyecto.servicios.repositorys.onboarding.CatalogoSepomexRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogoDireccionesService {

    private final CatalogoSepomexRepository sepomexRepository;
    private final SepomexApiClient sepomexApiClient;

    @Transactional
    public CodigoPostalResponse consultarPorCodigoPostal(String codigoPostal) {
        String cpValido = Optional.ofNullable(codigoPostal)
                .map(String::trim)
                .filter(cp -> cp.matches("^[0-9]{5}$"))
                .orElseThrow(() -> new ValidacionNegocioException("El código postal debe contener exactamente 5 dígitos numéricos"));

        List<CatalogoSepomex> registros = Optional.of(sepomexRepository.findByCodigoPostal(cpValido))
                .filter(Predicate.not(List::isEmpty))
                .orElseGet(() -> {
                    log.info("Código postal {} no encontrado en base de datos local. Sincronizando desde API externa...", cpValido);
                    return sincronizarDesdeApi(cpValido);
                });

        Optional.of(registros)
                .filter(Predicate.not(List::isEmpty))
                .orElseThrow(() -> new ValidacionNegocioException("No se encontró información para el código postal: " + cpValido));

        CatalogoSepomex muestra = registros.get(0);
        List<String> colonias = registros.stream()
                .map(CatalogoSepomex::getAsentamiento)
                .distinct()
                .sorted()
                .toList();

        return CodigoPostalResponse.builder()
                .codigoPostal(cpValido)
                .estado(muestra.getEstado())
                .municipio(muestra.getMunicipio())
                .ciudad(Optional.ofNullable(muestra.getCiudad()).orElse(muestra.getMunicipio()))
                .pais("México")
                .colonias(colonias)
                .build();
    }

    @Transactional
    public List<CatalogoSepomex> sincronizarDesdeApi(String codigoPostal) {
        try {
            return Optional.ofNullable(sepomexApiClient.consultarCodigoPostal(codigoPostal))
                    .map(ZippoPostalResponse::getPlaces)
                    .filter(Predicate.not(List::isEmpty))
                    .map(places -> {
                        List<CatalogoSepomex> entidades = places.stream()
                                .map(place -> {
                                    String nombreLugar = place.getPlaceName();
                                    String estado = Optional.ofNullable(place.getState()).orElse("México");
                                    return CatalogoSepomex.builder()
                                            .codigoPostal(codigoPostal)
                                            .asentamiento(nombreLugar)
                                            .tipoAsentamiento("Colonia")
                                            .municipio(nombreLugar)
                                            .estado(estado)
                                            .ciudad(nombreLugar)
                                            .build();
                                })
                                .toList();

                        List<CatalogoSepomex> guardados = sepomexRepository.saveAll(entidades);
                        log.info("Sincronización exitosa: se guardaron {} asentamientos en BD para el CP {}", guardados.size(), codigoPostal);
                        return guardados;
                    })
                    .orElseGet(() -> {
                        log.warn("La API externa no devolvió asentamientos para el CP {}", codigoPostal);
                        return List.of();
                    });

        } catch (FeignException.NotFound e) {
            log.warn("Código postal {} no existe en el catálogo externo", codigoPostal);
            return List.of();
        } catch (Exception e) {
            log.error("Error al conectar con la API de códigos postales para el CP {}: {}", codigoPostal, e.getMessage());
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<String> obtenerEstados() {
        return sepomexRepository.findDistinctEstados();
    }

    @Transactional(readOnly = true)
    public List<String> obtenerMunicipiosPorEstado(String estado) {
        String estadoLimpio = Optional.ofNullable(estado)
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .orElseThrow(() -> new ValidacionNegocioException("El nombre del estado es obligatorio"));

        return sepomexRepository.findDistinctMunicipiosByEstado(estadoLimpio);
    }

    @Transactional(readOnly = true)
    public List<String> obtenerColoniasPorCodigoPostal(String codigoPostal) {
        String cpValido = Optional.ofNullable(codigoPostal)
                .map(String::trim)
                .filter(cp -> cp.matches("^[0-9]{5}$"))
                .orElseThrow(() -> new ValidacionNegocioException("El código postal debe contener exactamente 5 dígitos numéricos"));

        return sepomexRepository.findColoniasByCodigoPostal(cpValido);
    }
}