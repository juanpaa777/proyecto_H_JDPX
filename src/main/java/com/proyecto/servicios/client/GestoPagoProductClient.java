package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoProductClient", url = "${gestopago.products.url}")
public interface GestoPagoProductClient {

    @GetMapping(
            value = "/sistema/service/getProductList.do",
            consumes = MediaType.ALL_VALUE,
            produces = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.ALL_VALUE}
    )
    String getProductList(
            @RequestHeader("Authorization") String authorization
    );
}
