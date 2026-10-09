package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.client.SepomexApiClient;
import com.proyecto.servicios.dto.onboarding.CodigoPostalResponse;
import com.proyecto.servicios.dto.onboarding.sepomex.ZippoPlaceResponse;
import com.proyecto.servicios.dto.onboarding.sepomex.ZippoPostalResponse;
import com.proyecto.servicios.entity.onboarding.CatalogoSepomex;
import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import com.proyecto.servicios.repositorys.onboarding.CatalogoSepomexRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogoDireccionesService {

    private final CatalogoSepomexRepository sepomexRepository;
    private final SepomexApiClient sepomexApiClient;

    @Transactional
    public CodigoPostalResponse consultarPorCodigoPostal(String codigoPostal) {
        if (codigoPostal == null || !codigoPostal.matches("^[0-9]{5}$")) {
            throw new ValidacionNegocioException("El código postal debe contener exactamente 5 dígitos numéricos");
        }

        List<CatalogoSepomex> registros = sepomexRepository.findByCodigoPostal(codigoPostal);

        // Si no está en base de datos local, consultamos la API externa y lo guardamos (Cache-Aside / Sync)
        if (registros.isEmpty()) {
            log.info("Código postal {} no encontrado en base de datos local. Consultando API externa...", codigoPostal);
            registros = sincronizarDesdeApi(codigoPostal);
        }

        if (registros.isEmpty()) {
            throw new ValidacionNegocioException("No se encontró información para el código postal: " + codigoPostal);
        }

        CatalogoSepomex muestra = registros.get(0);
        List<String> colonias = registros.stream()
                .map(CatalogoSepomex::getAsentamiento)
                .distinct()
                .sorted()
                .toList();

        return CodigoPostalResponse.builder()
                .codigoPostal(codigoPostal)
                .estado(muestra.getEstado())
                .municipio(muestra.getMunicipio())
                .ciudad(muestra.getCiudad() != null ? muestra.getCiudad() : muestra.getMunicipio())
                .pais("México")
                .colonias(colonias)
                .build();
    }

    @Transactional
    public List<CatalogoSepomex> sincronizarDesdeApi(String codigoPostal) {
        try {
            ZippoPostalResponse response = sepomexApiClient.consultarCodigoPostal(codigoPostal);
            if (response == null || response.getPlaces() == null || response.getPlaces().isEmpty()) {
                log.warn("La API externa no devolvió asentamientos para el CP {}", codigoPostal);
                return List.of();
            }

            List<CatalogoSepomex> entidadesAGuardar = new ArrayList<>();
            for (ZippoPlaceResponse place : response.getPlaces()) {
                String nombreLugar = place.getPlaceName();
                String estado = place.getState() != null ? place.getState() : "México";

                CatalogoSepomex registro = CatalogoSepomex.builder()
                        .codigoPostal(codigoPostal)
                        .asentamiento(nombreLugar)
                        .tipoAsentamiento("Colonia")
                        .municipio(nombreLugar)
                        .estado(estado)
                        .ciudad(nombreLugar)
                        .build();

                entidadesAGuardar.add(registro);
            }

            List<CatalogoSepomex> guardados = sepomexRepository.saveAll(entidadesAGuardar);
            log.info("Sincronización exitosa: se guardaron {} asentamientos en base de datos para el CP {}", guardados.size(), codigoPostal);
            return guardados;

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
        if (estado == null || estado.trim().isEmpty()) {
            throw new ValidacionNegocioException("El nombre del estado es obligatorio");
        }
        return sepomexRepository.findDistinctMunicipiosByEstado(estado.trim());
    }

    @Transactional(readOnly = true)
    public List<String> obtenerColoniasPorCodigoPostal(String codigoPostal) {
        if (codigoPostal == null || !codigoPostal.matches("^[0-9]{5}$")) {
            throw new ValidacionNegocioException("El código postal debe contener exactamente 5 dígitos numéricos");
        }
        return sepomexRepository.findColoniasByCodigoPostal(codigoPostal);
    }
}