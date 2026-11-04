package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoTxClient", url = "${gestopago.tx.url:https://gestopago.portalventas.net}")
public interface GestoPagoTxClient {

    @PostMapping(
            value = "/sistema/service/sendTx.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.ALL_VALUE}
    )
    String sendTx(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-API-Key") String apiKey,
            @RequestBody String formUrlEncodedBody
    );

    @PostMapping(
            value = "/sistema/service/confirmTx.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.ALL_VALUE}
    )
    String confirmTx(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-API-Key") String apiKey,
            @RequestBody String formUrlEncodedBody
    );
}

