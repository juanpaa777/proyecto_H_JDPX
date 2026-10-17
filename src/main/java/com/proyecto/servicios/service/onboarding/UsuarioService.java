package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.dto.onboarding.*;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.onboarding.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.onboarding.CorreoDuplicadoException;
import com.proyecto.servicios.exception.onboarding.UsuarioNoEncontradoException;
import com.proyecto.servicios.exception.onboarding.ValidacionNegocioException;
import com.proyecto.servicios.mapper.onboarding.ClienteMapper;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.repositorys.onboarding.specification.UsuarioSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Búsqueda dinámica y paginada mediante único endpoint de consulta.
     * Cero sentencias if.
     */
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> buscarUsuariosPaginados(ConsultaUsuarioRequest request) {
        int page = Optional.ofNullable(request.getPage()).orElse(0);
        int size = Optional.ofNullable(request.getSize()).orElse(10);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        return usuarioRepository.findAll(UsuarioSpecification.conFiltros(request), pageable)
                .map(ClienteMapper::toUsuarioResponse);
    }

    /**
     * Consulta de detalle por ID sin usar PathVariable ni RequestParam.
     */
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuarioDetalle(DetalleUsuarioRequest request) {
        return usuarioRepository.findById(request.getId())
                .map(ClienteMapper::toUsuarioResponse)
                .orElseThrow(() -> new UsuarioNoEncontradoException(request.getId()));
    }

    /**
     * Crear y asociar un usuario de acceso con contraseña cifrada en BCrypt.
     * Cero sentencias if.
     */
    @Transactional(rollbackFor = Exception.class)
    public UsuarioResponse crearUsuarioAcceso(UsuarioCreateRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException(request.getClienteId()));

        Optional.of(cliente)
                .filter(c -> !Boolean.TRUE.equals(c.getActivo()))
                .ifPresent(c -> { throw new ValidacionNegocioException("No se puede crear un usuario para un cliente que se encuentra inactivo"); });

        usuarioRepository.findByClienteId(cliente.getId())
                .ifPresent(u -> { throw new ValidacionNegocioException("El cliente ya cuenta con un usuario de acceso registrado"); });

        String correo = request.getCorreo().trim().toLowerCase();
        Optional.of(correo)
                .filter(usuarioRepository::existsByCorreo)
                .ifPresent(c -> { throw new CorreoDuplicadoException(c); });

        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setCorreo(correo);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setActivo(true);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        cliente.setUsuario(usuarioGuardado);

        log.info("Usuario creado exitosamente con ID: {} para cliente ID: {}", usuarioGuardado.getId(), cliente.getId());
        return ClienteMapper.toUsuarioResponse(usuarioGuardado);
    }

    /**
     * Actualización de usuario impidiendo modificación de campos controlados por el servidor.
     * Cero sentencias if.
     */
    @Transactional(rollbackFor = Exception.class)
    public UsuarioResponse actualizarUsuario(UsuarioUpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException(request.getId()));

        Optional.ofNullable(request.getCorreo())
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .map(String::toLowerCase)
                .filter(nuevoCorreo -> !nuevoCorreo.equalsIgnoreCase(usuario.getCorreo()))
                .ifPresent(nuevoCorreo -> {
                    Optional.of(nuevoCorreo)
                            .filter(usuarioRepository::existsByCorreo)
                            .ifPresent(c -> { throw new CorreoDuplicadoException(c); });
                    usuario.setCorreo(nuevoCorreo);
                });

        Optional.ofNullable(request.getPassword())
                .filter(Predicate.not(String::isBlank))
                .ifPresent(p -> usuario.setPassword(passwordEncoder.encode(p)));

        Optional.ofNullable(request.getActivo())
                .ifPresent(usuario::setActivo);

        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        log.info("Usuario ID: {} actualizado exitosamente", usuarioActualizado.getId());
        return ClienteMapper.toUsuarioResponse(usuarioActualizado);
    }

    /**
     * Baja lógica de usuario sin borrado físico.
     * Cero sentencias if.
     */
    @Transactional(rollbackFor = Exception.class)
    public GenericResponse darDeBajaUsuario(EliminaUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException(request.getId()));

        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        log.info("Baja lógica aplicada al usuario ID: {}", request.getId());
        return new GenericResponse(0, "Usuario desactivado correctamente");
    }
}
