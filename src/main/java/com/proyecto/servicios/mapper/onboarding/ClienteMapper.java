package com.proyecto.servicios.mapper.onboarding;

import com.proyecto.servicios.dto.onboarding.*;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.entity.onboarding.Usuario;

import java.util.Collections;
import java.util.stream.Collectors;

public class ClienteMapper {

    public static ClienteResponse toResponse(Cliente cliente) {
        if (cliente == null) {
            return null;
        }

        ClienteResponse response = new ClienteResponse();
        response.setId(cliente.getId());
        response.setNombre(cliente.getNombre());
        response.setSegundoNombre(cliente.getSegundoNombre());
        response.setApellidoPaterno(cliente.getApellidoPaterno());
        response.setApellidoMaterno(cliente.getApellidoMaterno());

        StringBuilder sb = new StringBuilder(cliente.getNombre());
        if (cliente.getSegundoNombre() != null && !cliente.getSegundoNombre().trim().isEmpty()) {
            sb.append(" ").append(cliente.getSegundoNombre());
        }
        sb.append(" ").append(cliente.getApellidoPaterno());
        sb.append(" ").append(cliente.getApellidoMaterno());
        response.setNombreCompleto(sb.toString());

        response.setFechaNacimiento(cliente.getFechaNacimiento());
        response.setCurp(cliente.getCurp());
        response.setRfc(cliente.getRfc());
        response.setSexo(cliente.getSexo());
        response.setNacionalidad(cliente.getNacionalidad());
        response.setEstadoCivil(cliente.getEstadoCivil());

        response.setCorreo(cliente.getCorreo());
        response.setTelefonoMovil(cliente.getTelefonoMovil());
        response.setTelefonoAlternativo(cliente.getTelefonoAlternativo());

        if (cliente.getDomicilio() != null) {
            response.setDomicilio(toDomicilioDto(cliente.getDomicilio()));
        }

        response.setOcupacion(cliente.getOcupacion());
        response.setEmpresa(cliente.getEmpresa());
        response.setIngresoMensual(cliente.getIngresoMensual());

        response.setActivo(cliente.getActivo());
        response.setFechaCreacion(cliente.getFechaCreacion());
        response.setFechaActualizacion(cliente.getFechaActualizacion());

        if (cliente.getCuentas() != null) {
            response.setCuentas(cliente.getCuentas().stream()
                    .map(ClienteMapper::toCuentaResponse)
                    .collect(Collectors.toList()));
        } else {
            response.setCuentas(Collections.emptyList());
        }

        if (cliente.getUsuario() != null) {
            response.setUsuario(toUsuarioResponse(cliente.getUsuario()));
        }

        return response;
    }

    public static DomicilioDto toDomicilioDto(Domicilio d) {
        if (d == null) return null;
        DomicilioDto dto = new DomicilioDto();
        dto.setId(d.getId());
        dto.setCalle(d.getCalle());
        dto.setNumeroExterior(d.getNumeroExterior());
        dto.setNumeroInterior(d.getNumeroInterior());
        dto.setColonia(d.getColonia());
        dto.setMunicipio(d.getMunicipio());
        dto.setEstado(d.getEstado());
        dto.setCodigoPostal(d.getCodigoPostal());
        dto.setPais(d.getPais());
        return dto;
    }

    public static CuentaResponse toCuentaResponse(Cuenta c) {
        if (c == null) return null;
        CuentaResponse response = new CuentaResponse();
        response.setId(c.getId());
        response.setClienteId(c.getCliente() != null ? c.getCliente().getId() : null);
        response.setNumeroCuenta(c.getNumeroCuenta());
        response.setSaldo(c.getSaldo());
        response.setEstatus(c.getEstatus());
        response.setFechaCreacion(c.getFechaCreacion());
        response.setFechaActualizacion(c.getFechaActualizacion());
        return response;
    }

    public static UsuarioResponse toUsuarioResponse(Usuario u) {
        if (u == null) return null;
        UsuarioResponse response = new UsuarioResponse();
        response.setId(u.getId());
        response.setClienteId(u.getCliente() != null ? u.getCliente().getId() : null);
        response.setCorreo(u.getCorreo());
        response.setActivo(u.getActivo());
        response.setFechaCreacion(u.getFechaCreacion());
        response.setFechaActualizacion(u.getFechaActualizacion());
        return response;
    }
}
