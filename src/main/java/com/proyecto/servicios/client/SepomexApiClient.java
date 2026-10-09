package com.proyecto.servicios.client;

import com.proyecto.servicios.dto.onboarding.sepomex.ZippoPostalResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "sepomexApiClient", url = "${sepomex.api.url:http://api.zippopotam.us}")
public interface SepomexApiClient {

    @GetMapping("/mx/{codigoPostal}")
    ZippoPostalResponse consultarCodigoPostal(@PathVariable("codigoPostal") String codigoPostal);
}