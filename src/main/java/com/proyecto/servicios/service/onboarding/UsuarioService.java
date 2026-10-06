package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.dto.onboarding.UsuarioCreateRequest;
import com.proyecto.servicios.dto.onboarding.UsuarioResponse;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.onboarding.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.onboarding.CorreoDuplicadoException;
import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import com.proyecto.servicios.mapper.onboarding.ClienteMapper;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> filtrarUsuarios(Long clienteId, String correo, Boolean activo) {
        return usuarioRepository.filtrarUsuarios(clienteId, correo, activo).stream()
                .map(ClienteMapper::toUsuarioResponse)
                .collect(Collectors.toList());
    }

    /**
     * Endpoint PUT /usuarios/agregar
     * Permite registrar o asociar un usuario de acceso a un cliente.
     */
    @Transactional(rollbackFor = Exception.class)
    public UsuarioResponse agregarUsuario(UsuarioCreateRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException(request.getClienteId()));

        if (!cliente.getActivo()) {
            throw new ValidacionNegocioException("No se puede crear un usuario para un cliente que se encuentra inactivo");
        }

        // Cada cliente podrá tener únicamente un usuario asociado
        if (usuarioRepository.findByClienteId(cliente.getId()).isPresent()) {
            throw new ValidacionNegocioException("El cliente ya cuenta con un usuario de acceso registrado");
        }

        String correo = request.getCorreo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new CorreoDuplicadoException(correo);
        }

        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setCorreo(correo);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setActivo(true);

        usuario = usuarioRepository.save(usuario);
        cliente.setUsuario(usuario);

        log.info("Usuario creado exitosamente para el cliente ID: {}", cliente.getId());
        return ClienteMapper.toUsuarioResponse(usuario);
    }
}
